package com.vlstream.service;

import com.vlstream.entity.AnnotationImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Annotation Image Service Interface
 */
public interface AnnotationImageService {
    
    /**
     * Upload images
     */
    List<AnnotationImage> uploadImages(MultipartFile[] files, Long annotationId);
    
    /**
     * Get image list by dataset ID
     */
    List<AnnotationImage> getImagesByDataset(Long annotationId);
    
    /**
     * Get image details by ID
     */
    AnnotationImage getImageById(Long id);
    
    /**
     * Update image information
     */
    AnnotationImage updateImage(AnnotationImage image);
    
    /**
     * Delete image
     */
    void deleteImage(Long id);
    
    /**
     * Batch delete images
     */
    void batchDeleteImages(List<Long> ids);
    
    /**
     * Get dataset statistics
     */
    Map<String, Object> getDatasetStats(Long datasetId);

    /**
     * Save image information to annotation_image table
     */
    boolean saveImage(AnnotationImage annotationImage);

    /**
     * Batch save image information to annotation_image table
     */
    boolean batchSaveImages(List<AnnotationImage> annotationImages);

    /**
     * Get image list by annotation project ID
     */
    List<AnnotationImage> getImagesByAnnotationId(Long annotationId);
}




