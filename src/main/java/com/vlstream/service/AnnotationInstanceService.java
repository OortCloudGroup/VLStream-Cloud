package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.AnnotationInstance;

import java.util.List;

/**
 * Annotation Instance Service Interface
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
public interface AnnotationInstanceService extends IService<AnnotationInstance> {

    /**
     * Query annotation instances by annotation project ID and image name
     * 
     * @param annotationId Annotation project ID
     * @param imageName Image name
     * @return Annotation instance list
     */
    List<AnnotationInstance> getByAnnotationIdAndImageName(Long annotationId, String imageName);

    /**
     * Save annotation instance
     * 
     * @param annotationId Annotation project ID
     * @param labelId Label ID
     * @param imageId Image ID
     * @param annotationType Annotation type
     * @param annotationData Annotation data (JSON format)
     * @return Saved annotation instance
     */
    AnnotationInstance saveAnnotation(Long annotationId, Long labelId, Long imageId, String annotationType, String annotationData);

    /**
     * Update annotation instance
     * 
     * @param instanceId Instance ID
     * @param labelId Label ID
     * @param annotationType Annotation type
     * @param annotationData Annotation data (JSON format)
     * @return Updated annotation instance
     */
    AnnotationInstance updateAnnotation(Long instanceId, Long labelId, 
                                       String annotationType, String annotationData);

    /**
     * Delete annotation instance
     * 
     * @param instanceId Instance ID
     * @return Whether deletion successful
     */
    boolean deleteAnnotation(Long instanceId);

    /**
     * Batch save annotation instances
     * 
     * @param annotationId Annotation project ID
     * @param imageId Image ID
     * @param annotations Annotation instance list
     * @return Whether save successful
     */
    boolean batchSaveAnnotations(Long annotationId, Long imageId, List<AnnotationInstance> annotations);

    /**
     * Query all annotation instances by annotation project ID
     * 
     * @param annotationId Annotation project ID
     * @return Annotation instance list
     */
    List<AnnotationInstance> getByAnnotationId(Long annotationId);

    /**
     * Count usage by label ID
     *
     * @param labelId Label ID
     * @return Usage count
     */
    Integer countByLabelId(Long labelId);

    /**
     * Delete image and all related data
     * Including: annotation_image, annotation_instance, update annotation_label usage count
     *
     * @param annotationId Annotation project ID
     * @param imageId Image ID
     * @return Deletion result
     */
    boolean deleteImageAndRelatedData(Long annotationId, Long imageId);
}