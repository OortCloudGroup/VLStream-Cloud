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
 * 图片上传控制器
 */
@Api(tags = "图片上传管理")
@RestController
@RequestMapping("/api/image")
@CrossOrigin(origins = "*")
public class ImageUploadController {

    @Autowired
    private IFileUploadService fileUploadService;

    /**
     * 上传图片
     *
     * @param files     图片文件（支持多文件）
     * @param fileNames 文件名（可选，对应每个文件）
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result<List<Map<String, Object>>> uploadImage(@RequestPart("files") MultipartFile[] files, @RequestPart(value = "fileNames", required = false) List<String> fileNames) {

        if (files == null || files.length == 0) {
            return Result.error("文件为空");
        }

        try {
            List<Map<String, Object>> results = new ArrayList<>();

            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (file.isEmpty()) {
                    return Result.error("文件为空");
                }

                // 检查文件类型
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return Result.error("请上传图片文件");
                }

                // 检查文件大小（10MB限制）
                long maxSize = 10 * 1024 * 1024;
                if (file.getSize() > maxSize) {
                    return Result.error("图片大小不能超过10MB");
                }

                FileResponseDto fileResponse = fileUploadService.uploadFile("818301f0e77f4cd8a117414cbeb32d9e", "5f0de11687d744bc95e84e207d319493", fileUploadService.multipartFileToFile(file));

                String originalName = file.getOriginalFilename();
                if (originalName == null || originalName.trim().isEmpty()) {
                    throw new RuntimeException("文件名不能为空");
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
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

}
