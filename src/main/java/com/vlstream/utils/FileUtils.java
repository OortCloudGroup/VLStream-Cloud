package com.vlstream.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * File utility class
 */
public class FileUtils {
    
    // 支持的图片格式
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
    );
    
    // 支持的图片扩展名
    private static final List<String> SUPPORTED_IMAGE_EXTENSIONS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    );
    
    /**
     * Check if the file is an image
     */
    public static boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        
        // 检查MIME类型
        if (contentType != null && SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return true;
        }
        
        // 检查文件扩展名
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            return SUPPORTED_IMAGE_EXTENSIONS.contains(extension);
        }
        
        return false;
    }
    
    /**
     * Get file extension
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
    
    /**
     * Generate unique file name
     */
    public static String generateUniqueFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
    }
    
    /**
     * Create directories
     */
    public static void createDirectories(String path) throws IOException {
        Path dirPath = Paths.get(path);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }
    
    /**
     * Delete file
     */
    public static boolean deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            return file.exists() && file.delete();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get human-readable file size format
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Check if the file name is safe
     */
    public static boolean isSafeFileName(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否包含危险字符
        String[] dangerousChars = {"..", "/", "\\", ":", "*", "?", "\"", "<", ">", "|"};
        for (String dangerousChar : dangerousChars) {
            if (filename.contains(dangerousChar)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Sanitize file name
     */
    public static String sanitizeFileName(String filename) {
        if (filename == null) {
            return "";
        }
        
        // 替换危险字符
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}