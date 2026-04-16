package com.vlstream.controller;

import com.vlstream.entity.AnnotationImage;
import com.vlstream.service.AnnotationImageService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Annotation Image Management Controller
 */
@RestController
@RequestMapping("/api/annotation-images")
@CrossOrigin(origins = "*")
@Api(tags = "Annotation Image Management")
public class AnnotationImageController {

    private static final Logger log = LoggerFactory.getLogger(AnnotationImageController.class);

    @Autowired
    private AnnotationImageService annotationImageService;

    /**
     * Upload annotation images
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImages(
            @RequestPart("files") MultipartFile[] files,
            @RequestParam("annotationId") Long annotationId) {
        try {
            List<AnnotationImage> images = annotationImageService.uploadImages(files, annotationId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Images uploaded successfully");
            response.put("data", images);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to upload images: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get all images of dataset
     */
    @GetMapping("/dataset/{annotationId}")
    public ResponseEntity<?> getImagesByDataset(@PathVariable Long annotationId) {
        try {
            List<AnnotationImage> images = annotationImageService.getImagesByDataset(annotationId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", images);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get image list: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get image details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getImageById(@PathVariable Long id) {
        try {
            AnnotationImage image = annotationImageService.getImageById(id);
            if (image != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", image);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get image details: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Update image annotation information
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateImage(@PathVariable Long id, @RequestBody AnnotationImage image) {
        try {
            image.setId(id);
            AnnotationImage updatedImage = annotationImageService.updateImage(image);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image information updated successfully");
            response.put("data", updatedImage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update image information: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Delete image
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id) {
        try {
            annotationImageService.deleteImage(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete image: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Batch delete images
     */
    @DeleteMapping("/batch")
    public ResponseEntity<?> batchDeleteImages(@RequestBody List<Long> ids) {
        try {
            annotationImageService.batchDeleteImages(ids);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Batch deletion successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to batch delete: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get dataset statistics
     */
    @GetMapping("/dataset/{datasetId}/stats")
    public ResponseEntity<?> getDatasetStats(@PathVariable Long datasetId) {
        try {
            Map<String, Object> stats = annotationImageService.getDatasetStats(datasetId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get statistics: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Batch save image information to annotation_image table
     *
     * @param annotationImages Image information list
     * @return Save result
     */
    @PostMapping("/images/batch")
    public ResponseEntity<?> batchSaveImages(@RequestBody List<AnnotationImage> annotationImages) {
        try {
            log.info("批量保存图片信息，数量: {}", annotationImages.size());

            // 设置创建时间
            LocalDateTime now = LocalDateTime.now();
            annotationImages.forEach(image -> {
                image.setCreateTime(now);
                image.setUpdateTime(now);
            });

            boolean success = annotationImageService.batchSaveImages(annotationImages);
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Batch save image information successful");
                response.put("data", annotationImages.size());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Failed to batch save image information");
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            log.error("Failed to batch save image information", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to batch save image information: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Access image file
     * Access image files stored in data/images directory by dataset ID and image name
     *
     * @param datasetId Dataset ID
     * @param imageName Image name
     * @return Image file stream
     */
    @GetMapping("/dataset/{datasetId}/image/{imageName}")
    public ResponseEntity<?> getImageFile(
            @PathVariable Long datasetId,
            @PathVariable String imageName) {
        try {
            log.info("请求访问图片文件: datasetId={}, imageName={}", datasetId, imageName);
            
            // 构建图片文件路径
            String imagePath = "data/images/" + imageName;
            java.io.File imageFile = new java.io.File(imagePath);
            
            if (!imageFile.exists()) {
                log.warn("图片文件不存在: {}", imagePath);
                return ResponseEntity.notFound().build();
            }
            
            // 读取图片文件
            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
            
            // 根据文件扩展名确定Content-Type
            String contentType = "image/jpeg"; // 默认
            if (imageName.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (imageName.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            } else if (imageName.toLowerCase().endsWith(".bmp")) {
                contentType = "image/bmp";
            }
            
            log.info("成功读取图片文件: {}, 大小: {} bytes", imagePath, imageBytes.length);
            
            return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(imageBytes);
                
        } catch (Exception e) {
            log.error("Failed to access image file: datasetId={}, imageName={}", datasetId, imageName, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to access image file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
