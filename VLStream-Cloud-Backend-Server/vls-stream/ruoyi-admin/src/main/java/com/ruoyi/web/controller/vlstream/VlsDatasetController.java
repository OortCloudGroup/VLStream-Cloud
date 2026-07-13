/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.service.IVlsDatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Dataset routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dataset")
public class VlsDatasetController {

    private final IVlsDatasetService datasetService;

    @PostMapping("/connect")
    public BladeResult<Map<String, Object>> connectToServer(@RequestParam String host,
                                                            @RequestParam String username,
                                                            @RequestParam String password,
                                                            @RequestParam String path) {
        try {
            return BladeResult.success(datasetService.connectToServer(host, username, password, path));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/files")
    public BladeResult<List<Map<String, Object>>> getDatasetFiles(@RequestParam String host,
                                                                  @RequestParam String path) {
        try {
            return BladeResult.success(datasetService.getDatasetFiles(host, path));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/file-content")
    public BladeResult<String> getFileContent(@RequestParam String host,
                                              @RequestParam String path,
                                              @RequestParam String filename) {
        try {
            return BladeResult.success(datasetService.getFileContent(host, path, filename));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam String host,
                                          @RequestParam String path,
                                          @RequestParam String filename) {
        return datasetService.downloadFile(host, path, filename);
    }

    @PostMapping("/upload")
    public BladeResult<Map<String, Object>> uploadFileToServer(@RequestParam String host,
                                                               @RequestParam String path,
                                                               @RequestParam("file") MultipartFile file) {
        try {
            return BladeResult.success(datasetService.uploadFile(host, path, file));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create-directory")
    public BladeResult<Map<String, Object>> createRemoteDirectory(@RequestBody Map<String, Object> body) {
        try {
            return BladeResult.success(datasetService.createDirectory(text(body, "host"), text(body, "path")));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public BladeResult<Map<String, Object>> deleteRemoteFile(@RequestParam String host,
                                                             @RequestParam String path) {
        try {
            return BladeResult.success(datasetService.deleteFile(host, path));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    private String text(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        String value = String.valueOf(body.get(key)).trim();
        return value.isEmpty() ? null : value;
    }
}
