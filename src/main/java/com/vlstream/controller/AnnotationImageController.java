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
 * 标注图片管理控制器
 */
@RestController
@RequestMapping("/api/annotation-images")
@CrossOrigin(origins = "*")
@Api(tags = "标注图片管理")
public class AnnotationImageController {

    private static final Logger log = LoggerFactory.getLogger(AnnotationImageController.class);

    @Autowired
    private AnnotationImageService annotationImageService;

    /**
     * 上传标注图片
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImages(
            @RequestPart("files") MultipartFile[] files,
            @RequestParam("annotationId") Long annotationId) {
        try {
            List<AnnotationImage> images = annotationImageService.uploadImages(files, annotationId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "图片上传成功");
            response.put("data", images);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "图片上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取数据集的所有图片
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
            errorResponse.put("message", "获取图片列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取图片详情
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
            errorResponse.put("message", "获取图片详情失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 更新图片标注信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateImage(@PathVariable Long id, @RequestBody AnnotationImage image) {
        try {
            image.setId(id);
            AnnotationImage updatedImage = annotationImageService.updateImage(image);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "图片信息更新成功");
            response.put("data", updatedImage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "更新图片信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 删除图片
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id) {
        try {
            annotationImageService.deleteImage(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "图片删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "删除图片失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 批量删除图片
     */
    @DeleteMapping("/batch")
    public ResponseEntity<?> batchDeleteImages(@RequestBody List<Long> ids) {
        try {
            annotationImageService.batchDeleteImages(ids);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "批量删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取数据集统计信息
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
            errorResponse.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 批量保存图片信息到annotation_image表
     *
     * @param annotationImages 图片信息列表
     * @return 保存结果
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
                response.put("message", "批量保存图片信息成功");
                response.put("data", annotationImages.size());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "批量保存图片信息失败");
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            log.error("批量保存图片信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "批量保存图片信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 访问图片文件
     * 通过数据集ID和图片名称访问存储在data/images目录中的图片文件
     *
     * @param datasetId 数据集ID
     * @param imageName 图片名称
     * @return 图片文件流
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
            log.error("访问图片文件失败: datasetId={}, imageName={}", datasetId, imageName, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "访问图片文件失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
