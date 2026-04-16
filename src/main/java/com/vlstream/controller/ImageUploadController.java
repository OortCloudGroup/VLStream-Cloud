package com.vlstream.controller;

import com.vlstream.common.Result;
import com.vlstream.dto.FileResponseDto;
import com.vlstream.service.IFileUploadService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Image Upload Controller
 */
@Api(tags = "Image Upload Management")
@RestController
@RequestMapping("/api/image")
@CrossOrigin(origins = "*")
public class ImageUploadController {

    @Autowired
    private IFileUploadService fileUploadService;

    /**
     * Upload image
     *
     * @param files     Image files (supports multiple files)
     * @param fileNames File names (optional, corresponding to each file)
     * @return Upload result
     */
    @PostMapping("/upload")
    public Result<List<Map<String, Object>>> uploadImage(@RequestPart("files") MultipartFile[] files, @RequestPart(value = "fileNames", required = false) List<String> fileNames) {

        if (files == null || files.length == 0) {
            return Result.error("File is empty");
        }

        try {
            List<Map<String, Object>> results = new ArrayList<>();

            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (file.isEmpty()) {
                    return Result.error("File is empty");
                }

                // Check file type
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return Result.error("Please upload image files");
                }

                // Check file size (10MB limit)
                long maxSize = 10 * 1024 * 1024;
                if (file.getSize() > maxSize) {
                    return Result.error("Image size cannot exceed 10MB");
                }

                FileResponseDto fileResponse = fileUploadService.uploadFile("818301f0e77f4cd8a117414cbeb32d9e", "5f0de11687d744bc95e84e207d319493", fileUploadService.multipartFileToFile(file));

                String originalName = file.getOriginalFilename();
                if (originalName == null || originalName.trim().isEmpty()) {
                    throw new RuntimeException("File name cannot be empty");
                }
                originalName = Paths.get(originalName).getFileName().toString();
                originalName = originalName.replace("\\", "_")
                        .replace("/", "_")
                        .replaceAll("[<>:\"|?*]", "_");

                String fileName = System.currentTimeMillis() + "_" + originalName;

                // 构建返回数据
                Map<String, Object> result = new HashMap<>();
                result.put("fileName", fileName);
                result.put("originalName", fileName);
                result.put("localPath", fileResponse.getUrl());
                result.put("fileSize", file.getSize());
                result.put("contentType", contentType);

                results.add(result);
            }

            return Result.success(results);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Image upload failed: " + e.getMessage());
        }
    }

}
