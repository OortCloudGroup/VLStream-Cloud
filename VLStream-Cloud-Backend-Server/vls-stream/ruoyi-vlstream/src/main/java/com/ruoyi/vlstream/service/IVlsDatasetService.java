/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Dataset file service contract used by compatibility controllers.
 */
public interface IVlsDatasetService {

    Map<String, Object> connectToServer(String host, String username, String password, String path);

    List<Map<String, Object>> getDatasetFiles(String host, String path);

    String getFileContent(String host, String path, String filename);

    ResponseEntity<Object> downloadFile(String host, String path, String filename);

    Map<String, Object> uploadFile(String host, String path, MultipartFile file);

    Map<String, Object> createDirectory(String host, String path);

    Map<String, Object> deleteFile(String host, String path);
}
