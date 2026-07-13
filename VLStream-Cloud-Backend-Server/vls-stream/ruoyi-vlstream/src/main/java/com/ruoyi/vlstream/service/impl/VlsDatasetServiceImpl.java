/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.ruoyi.vlstream.service.IVlsDatasetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Local dataset adapter for the frontend compatibility surface.
 */
@Service
public class VlsDatasetServiceImpl implements IVlsDatasetService {

    @Value("${vls.dataset.local-root:}")
    private String configuredLocalRoot;

    @Override
    public Map<String, Object> connectToServer(String host, String username, String password, String path) {
        Path directory = resolvePath(path);
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare dataset path: " + ex.getMessage(), ex);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("connected", true);
        result.put("success", true);
        result.put("host", host);
        result.put("username", username);
        result.put("path", path);
        result.put("mode", "local-adapter");
        result.put("message", "Server connection successful");
        return result;
    }

    @Override
    public List<Map<String, Object>> getDatasetFiles(String host, String path) {
        Path directory = resolvePath(path);
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return new ArrayList<Map<String, Object>>();
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                .sorted()
                .map(this::fileInfo)
                .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to get file list: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getFileContent(String host, String path, String filename) {
        Path file = resolveFile(path, filename);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("File does not exist");
        }
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read file content: " + ex.getMessage(), ex);
        }
    }

    @Override
    public ResponseEntity<Object> downloadFile(String host, String path, String filename) {
        Path file = resolveFile(path, filename);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file.toFile());
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFilename(filename) + "\"")
            .body(resource);
    }

    @Override
    public Map<String, Object> uploadFile(String host, String path, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is required");
        }
        Path directory = resolvePath(path);
        String filename = safeFilename(file.getOriginalFilename());
        Path target = directory.resolve(filename).normalize();
        ensureInsideRoot(target);
        try {
            Files.createDirectories(directory);
            file.transferTo(target.toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to upload file: " + ex.getMessage(), ex);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("host", host);
        result.put("path", path);
        result.put("filename", filename);
        result.put("size", file.getSize());
        result.put("message", "File uploaded successfully");
        return result;
    }

    @Override
    public Map<String, Object> createDirectory(String host, String path) {
        Path directory = resolvePath(path);
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create directory: " + ex.getMessage(), ex);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("created", true);
        result.put("host", host);
        result.put("path", path);
        result.put("message", "Directory created successfully");
        return result;
    }

    @Override
    public Map<String, Object> deleteFile(String host, String path) {
        Path target = resolvePath(path);
        boolean existed = Files.exists(target);
        if (existed) {
            deleteRecursively(target);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("deleted", existed);
        result.put("host", host);
        result.put("path", path);
        result.put("message", existed ? "Delete successful" : "Path does not exist");
        return result;
    }

    private Map<String, Object> fileInfo(Path path) {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        boolean directory = Files.isDirectory(path);
        long size = 0L;
        long modified = 0L;
        try {
            size = directory ? 0L : Files.size(path);
            modified = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            size = 0L;
        }
        info.put("name", path.getFileName().toString());
        info.put("type", directory ? "directory" : "file");
        info.put("size", formatFileSize(size));
        info.put("rawSize", size);
        info.put("modifiedTime", new Date(modified));
        return info;
    }

    private void deleteRecursively(Path target) {
        ensureInsideRoot(target);
        try (Stream<Path> stream = Files.walk(target)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete path: " + ex.getMessage(), ex);
        }
    }

    private Path resolveFile(String path, String filename) {
        Path directory = resolvePath(path);
        Path file = directory.resolve(safeFilename(filename)).normalize();
        ensureInsideRoot(file);
        return file;
    }

    private Path resolvePath(String path) {
        Path root = localRoot();
        String safePath = path == null ? "" : path.trim().replace('\\', '/');
        while (safePath.startsWith("/")) {
            safePath = safePath.substring(1);
        }
        safePath = safePath.replace(":", "");
        Path resolved = root.resolve(safePath).normalize();
        ensureInsideRoot(resolved);
        return resolved;
    }

    private Path localRoot() {
        if (StringUtils.hasText(configuredLocalRoot)) {
            return Paths.get(configuredLocalRoot).toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("java.io.tmpdir"), "vlstream-dataset").toAbsolutePath().normalize();
    }

    private void ensureInsideRoot(Path path) {
        if (!path.toAbsolutePath().normalize().startsWith(localRoot())) {
            throw new IllegalArgumentException("Invalid dataset path");
        }
    }

    private String safeFilename(String filename) {
        String value = StringUtils.hasText(filename) ? filename.trim() : "file";
        value = value.replace("\\", "/");
        int slash = value.lastIndexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        return value.replace("\"", "");
    }

    private String formatFileSize(long size) {
        if (size < 1024L) {
            return size + " B";
        }
        if (size < 1024L * 1024L) {
            return String.format("%.1f KB", size / 1024.0);
        }
        if (size < 1024L * 1024L * 1024L) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
}
