package com.vlstream.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 视频文件控制器
 * 提供视频文件的HTTP访问
 */
@Api(tags = "视频文件管理")
@RestController
@RequestMapping("/api/recordings")
@Slf4j
public class VideoFileController {

    @Value("${recording.storage.path:./recordings}")
    private String recordingStoragePath;

    @ApiOperation("获取视频文件")
    @GetMapping("/**")
    public ResponseEntity<Resource> getVideoFile(
            @ApiParam("文件路径") @RequestParam(required = false) String filePath,
            HttpServletRequest request
    ) {
        try {
            // 从请求URI中提取文件路径
            String requestURI = request.getRequestURI();
            String encodedRelativePath = requestURI.substring(requestURI.indexOf("/recordings/") + "/recordings/".length());
            
            // 对URL编码的路径进行解码，处理中文文件名
            String relativePath;
            try {
                relativePath = URLDecoder.decode(encodedRelativePath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error("URL解码失败: {}", encodedRelativePath, e);
                return ResponseEntity.badRequest().build();
            }
            
            // 构建完整的文件路径
            Path fullPath = Paths.get(recordingStoragePath, relativePath);
            File file = fullPath.toFile();
            
            log.info("Requesting video file - Original URI: {}", requestURI);
            log.info("Encoded relative path: {}", encodedRelativePath);
            log.info("Decoded relative path: {}", relativePath);
            log.info("Full file path: {}", fullPath);
            
            if (!file.exists()) {
                log.warn("Video file not found: {}", fullPath);
                return ResponseEntity.notFound().build();
            }
            
            if (!file.canRead()) {
                log.warn("Video file cannot be read: {}", fullPath);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // 创建资源
            Resource resource = new FileSystemResource(file);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            
            // 根据文件扩展名设置Content-Type
            String contentType = getContentType(file.getName());
            headers.setContentType(MediaType.parseMediaType(contentType));
            
            // 设置文件名
            headers.setContentDispositionFormData("inline", file.getName());
            
            // 设置缓存
            headers.setCacheControl("public, max-age=3600");
            
            log.info("Successfully serving video file: {}", fullPath);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error serving video file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 根据文件名获取Content-Type
     */
    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            case "wmv":
                return "video/x-ms-wmv";
            case "flv":
                return "video/x-flv";
            case "webm":
                return "video/webm";
            case "m4v":
                return "video/x-m4v";
            default:
                return "application/octet-stream";
        }
    }
} 