/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.modules.system.service;

import com.ruoyi.vlstream.test.vlstream.pojo.dto.FileResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * File upload boundary retained by the copied VLS business code.
 */
public interface IFileUploadService {

    /**
     * Materializes a multipart upload as a temporary local file.
     */
    File multipartFileToFile(MultipartFile multipartFile);

    /**
     * Uploads a real file through the configured RuoYi OSS service.
     */
    FileResponseDto uploadFile(String appId, String secretKey, File file);
}
