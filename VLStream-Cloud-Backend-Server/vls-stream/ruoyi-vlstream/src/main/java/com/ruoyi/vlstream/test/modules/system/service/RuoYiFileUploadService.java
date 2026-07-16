/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.modules.system.service;

import com.ruoyi.system.domain.vo.SysOssVo;
import com.ruoyi.system.service.ISysOssService;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FileResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Adapts the VLS upload contract to RuoYi's configured OSS implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuoYiFileUploadService implements IFileUploadService {

    private final ISysOssService sysOssService;

    /**
     * Writes the incoming multipart body to an isolated temporary directory.
     */
    @Override
    public File multipartFileToFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }
        String originalName = multipartFile.getOriginalFilename();
        String safeName = sanitizeFileName(originalName == null ? multipartFile.getName() : originalName);
        try {
            Path directory = Files.createTempDirectory("vlstream-upload-");
            File target = directory.resolve(safeName).toFile();
            multipartFile.transferTo(target);
            target.deleteOnExit();
            directory.toFile().deleteOnExit();
            return target;
        } catch (IOException exception) {
            log.error("Failed to materialize multipart file: {}", safeName, exception);
            return null;
        }
    }

    /**
     * Uploads through RuoYi OSS and maps its persisted URL to the VLS response DTO.
     * The legacy appId and secretKey arguments are intentionally ignored because
     * authentication is handled by the local configured OSS service.
     */
    @Override
    public FileResponseDto uploadFile(String appId, String secretKey, File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            SysOssVo uploaded = sysOssService.upload(new LocalFileMultipartFile(file));
            if (uploaded == null || uploaded.getUrl() == null) {
                return null;
            }
            FileResponseDto response = new FileResponseDto();
            response.setUrl(uploaded.getUrl());
            response.setPath(uploaded.getFileName());
            response.setSize(file.length());
            return response;
        } catch (RuntimeException exception) {
            log.error("RuoYi OSS upload failed: {}", file.getAbsolutePath(), exception);
            throw exception;
        }
    }

    /**
     * Removes path segments and unsafe characters from an uploaded filename.
     */
    private String sanitizeFileName(String fileName) {
        String normalized = fileName == null ? "upload.bin" : fileName.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        String leaf = separator >= 0 ? normalized.substring(separator + 1) : normalized;
        String safe = leaf.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isEmpty() ? "upload.bin" : safe;
    }

    /**
     * Read-only MultipartFile view over a local file for the OSS service.
     */
    private static final class LocalFileMultipartFile implements MultipartFile {

        private final File file;

        private LocalFileMultipartFile(File file) {
            this.file = file;
        }

        /** Returns the multipart field name expected by the OSS service. */
        @Override
        public String getName() {
            return "file";
        }

        /** Returns the original local filename. */
        @Override
        public String getOriginalFilename() {
            return file.getName();
        }

        /** Detects the content type when the local filesystem provides one. */
        @Override
        public String getContentType() {
            try {
                return Files.probeContentType(file.toPath());
            } catch (IOException ignored) {
                return null;
            }
        }

        /** Reports whether the local file contains no data. */
        @Override
        public boolean isEmpty() {
            return file.length() == 0L;
        }

        /** Returns the local file size. */
        @Override
        public long getSize() {
            return file.length();
        }

        /** Reads all local file bytes for the OSS client. */
        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(file.toPath());
        }

        /** Opens a stream over the local file. */
        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        /** Copies the local file to the requested destination. */
        @Override
        public void transferTo(File destination) throws IOException {
            Files.copy(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
