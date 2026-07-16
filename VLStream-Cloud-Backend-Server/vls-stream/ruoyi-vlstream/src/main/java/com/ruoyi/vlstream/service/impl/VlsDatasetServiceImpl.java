/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.ruoyi.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.service.IVlsDatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/** Real SFTP dataset adapter using the configured VLS SSH server. */
@Service
@RequiredArgsConstructor
public class VlsDatasetServiceImpl implements IVlsDatasetService {

    private static final int CONNECT_TIMEOUT_MILLIS = 30000;

    private final VlsSshProperties sshProperties;

    /** Verify SSH/SFTP credentials and ensure the requested remote directory is accessible. */
    @Override
    public Map<String, Object> connectToServer(String host, String username, String password, String path) {
        String resolvedHost = required(host, "SSH host is required");
        String resolvedUsername = required(username, "SSH username is required");
        String resolvedPassword = required(password, "SSH password is required");
        String resolvedPath = remotePath(path);
        try (SftpConnection connection = open(resolvedHost, resolvedUsername, resolvedPassword)) {
            ensureDirectory(connection.sftp, resolvedPath);
            connection.sftp.cd(resolvedPath);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("connected", true);
            result.put("success", true);
            result.put("host", resolvedHost);
            result.put("username", resolvedUsername);
            result.put("path", resolvedPath);
            result.put("mode", "sftp");
            result.put("message", "SSH/SFTP connection and directory access succeeded");
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("SSH/SFTP connection failed: " + ex.getMessage(), ex);
        }
    }

    /** List actual files from the requested remote directory. */
    @Override
    public List<Map<String, Object>> getDatasetFiles(String host, String path) {
        String resolvedPath = remotePath(path);
        try (SftpConnection connection = openConfigured(host)) {
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> entries = connection.sftp.ls(resolvedPath);
            List<Map<String, Object>> files = new ArrayList<Map<String, Object>>();
            for (ChannelSftp.LsEntry entry : entries) {
                if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
                    continue;
                }
                Map<String, Object> info = new LinkedHashMap<String, Object>();
                info.put("name", entry.getFilename());
                info.put("type", entry.getAttrs().isDir() ? "directory" : "file");
                info.put("size", formatFileSize(entry.getAttrs().getSize()));
                info.put("rawSize", entry.getAttrs().getSize());
                info.put("modifiedTime", new Date(entry.getAttrs().getMTime() * 1000L));
                files.add(info);
            }
            return files;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to list remote dataset files: " + ex.getMessage(), ex);
        }
    }

    /** Read actual UTF-8 content from a remote dataset file. */
    @Override
    public String getFileContent(String host, String path, String filename) {
        String remoteFile = join(remotePath(path), safeFilename(filename));
        try (SftpConnection connection = openConfigured(host);
             InputStream input = connection.sftp.get(remoteFile);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copy(input, output);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read remote dataset file: " + ex.getMessage(), ex);
        }
    }

    /** Download actual bytes from a remote dataset file. */
    @Override
    public ResponseEntity<Object> downloadFile(String host, String path, String filename) {
        String safeName = safeFilename(filename);
        String remoteFile = join(remotePath(path), safeName);
        try (SftpConnection connection = openConfigured(host);
             InputStream input = connection.sftp.get(remoteFile);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copy(input, output);
            byte[] bytes = output.toByteArray();
            ByteArrayResource resource = new ByteArrayResource(bytes);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                .body(resource);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to download remote dataset file: " + ex.getMessage(), ex);
        }
    }

    /** Upload an actual file to the requested remote SFTP directory. */
    @Override
    public Map<String, Object> uploadFile(String host, String path, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is required");
        }
        String directory = remotePath(path);
        String filename = safeFilename(file.getOriginalFilename());
        try (SftpConnection connection = openConfigured(host);
             InputStream input = file.getInputStream()) {
            ensureDirectory(connection.sftp, directory);
            connection.sftp.put(input, join(directory, filename));
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("success", true);
            result.put("host", resolvedHost(host));
            result.put("path", directory);
            result.put("filename", filename);
            result.put("size", file.getSize());
            result.put("message", "Remote SFTP upload succeeded");
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Remote SFTP upload failed: " + ex.getMessage(), ex);
        }
    }

    /** Create and verify an actual remote directory. */
    @Override
    public Map<String, Object> createDirectory(String host, String path) {
        String directory = remotePath(path);
        try (SftpConnection connection = openConfigured(host)) {
            ensureDirectory(connection.sftp, directory);
            SftpATTRS attributes = connection.sftp.stat(directory);
            if (!attributes.isDir()) {
                throw new IllegalStateException("Remote path exists but is not a directory");
            }
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("success", true);
            result.put("created", true);
            result.put("host", resolvedHost(host));
            result.put("path", directory);
            result.put("message", "Remote SFTP directory is ready");
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create remote SFTP directory: " + ex.getMessage(), ex);
        }
    }

    /** Delete an existing remote file or directory and fail when it does not exist. */
    @Override
    public Map<String, Object> deleteFile(String host, String path) {
        String target = remotePath(path);
        if ("/".equals(target)) {
            throw new IllegalArgumentException("Refusing to delete the remote root directory");
        }
        try (SftpConnection connection = openConfigured(host)) {
            SftpATTRS attributes = connection.sftp.stat(target);
            if (attributes.isDir()) {
                deleteDirectory(connection.sftp, target);
            } else {
                connection.sftp.rm(target);
            }
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("success", true);
            result.put("deleted", true);
            result.put("host", resolvedHost(host));
            result.put("path", target);
            result.put("message", "Remote SFTP path deleted");
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to delete remote SFTP path: " + ex.getMessage(), ex);
        }
    }

    /** Open SFTP with configured credentials and an optional host override. */
    private SftpConnection openConfigured(String host) throws Exception {
        return open(resolvedHost(host), required(sshProperties.getUsername(), "Configured SSH username is required"),
            required(sshProperties.getPassword(), "Configured SSH password is required"));
    }

    /** Establish a real SSH session and SFTP channel. */
    private SftpConnection open(String host, String username, String password) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, sshProperties.getPort() == null ? 22 : sshProperties.getPort());
        session.setPassword(password);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(CONNECT_TIMEOUT_MILLIS);
        Channel channel = session.openChannel("sftp");
        channel.connect(CONNECT_TIMEOUT_MILLIS);
        return new SftpConnection(session, (ChannelSftp) channel);
    }

    /** Recursively ensure that a remote directory path exists. */
    private void ensureDirectory(ChannelSftp sftp, String path) throws SftpException {
        if ("/".equals(path)) {
            return;
        }
        String current = path.startsWith("/") ? "/" : "";
        for (String part : path.split("/")) {
            if (part.isEmpty()) {
                continue;
            }
            current = "/".equals(current) ? current + part : current + "/" + part;
            try {
                SftpATTRS attributes = sftp.stat(current);
                if (!attributes.isDir()) {
                    throw new SftpException(ChannelSftp.SSH_FX_FAILURE, current + " is not a directory");
                }
            } catch (SftpException ex) {
                if (ex.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    throw ex;
                }
                sftp.mkdir(current);
            }
        }
    }

    /** Recursively delete a remote directory after enumerating its real children. */
    private void deleteDirectory(ChannelSftp sftp, String path) throws SftpException {
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> entries = sftp.ls(path);
        for (ChannelSftp.LsEntry entry : entries) {
            if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
                continue;
            }
            String child = join(path, entry.getFilename());
            if (entry.getAttrs().isDir()) {
                deleteDirectory(sftp, child);
            } else {
                sftp.rm(child);
            }
        }
        sftp.rmdir(path);
    }

    /** Normalize and reject traversal in remote paths. */
    private String remotePath(String path) {
        String value = required(path, "Remote path is required").replace('\\', '/');
        if (value.contains("../") || value.endsWith("/..") || "..".equals(value)) {
            throw new IllegalArgumentException("Remote path traversal is not allowed");
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    /** Return a safe leaf filename. */
    private String safeFilename(String filename) {
        String value = required(filename, "Filename is required").replace('\\', '/');
        int slash = value.lastIndexOf('/');
        return slash >= 0 ? value.substring(slash + 1) : value;
    }

    /** Join a normalized remote directory and leaf name. */
    private String join(String directory, String name) {
        return directory.endsWith("/") ? directory + name : directory + "/" + name;
    }

    /** Resolve an optional host from the configured SSH host. */
    private String resolvedHost(String host) {
        return StringUtils.hasText(host) ? host.trim() : required(sshProperties.getHost(), "Configured SSH host is required");
    }

    /** Reject blank required inputs with an actionable failure. */
    private String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /** Copy a remote stream without reporting success before all bytes are received. */
    private void copy(InputStream input, ByteArrayOutputStream output) throws Exception {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
    }

    /** Format a verified remote file size for the frontend. */
    private String formatFileSize(long size) {
        if (size < 1024L) return size + " B";
        if (size < 1024L * 1024L) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024L * 1024L * 1024L) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    /** Own and close one SSH/SFTP connection. */
    private static final class SftpConnection implements AutoCloseable {
        private final Session session;
        private final ChannelSftp sftp;

        private SftpConnection(Session session, ChannelSftp sftp) {
            this.session = session;
            this.sftp = sftp;
        }

        @Override
        public void close() {
            if (sftp != null) sftp.disconnect();
            if (session != null) session.disconnect();
        }
    }
}
