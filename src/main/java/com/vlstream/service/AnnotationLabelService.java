package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.AnnotationLabel;

import java.util.List;

/**
 * Annotation Label Service Interface
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
public interface AnnotationLabelService extends IService<AnnotationLabel> {

    /**
     * Query label list by annotation project ID (including usage count statistics)
     * 
     * @param annotationId Annotation project ID
     * @return Label list
     */
    List<AnnotationLabel> getByAnnotationIdWithUsageCount(Long annotationId);

    /**
     * Create label
     * 
     * @param annotationId Annotation project ID
     * @param name Label name
     * @param color Label color
     * @param description Label description
     * @return Created label
     */
    AnnotationLabel createLabel(Long annotationId, String name, String color, String description);

    /**
     * Update label
     * 
     * @param labelId Label ID
     * @param name Label name
     * @param color Label color
     * @param description Label description
     * @return Updated label
     */
    AnnotationLabel updateLabel(Long labelId, String name, String color, String description);

    /**
     * Delete label
     * 
     * @param labelId Label ID
     * @return Whether deletion successful
     */
    boolean deleteLabel(Long labelId);

    /**
     * Update label usage count
     * 
     * @param labelId Label ID
     * @return Whether update successful
     */
    boolean updateUsageCount(Long labelId);

    /**
     * Batch update label sort order
     * 
     * @param annotationId Annotation project ID
     * @param labelIds Label ID list (in sort order)
     * @return Whether update successful
     */
    boolean updateSortOrder(Long annotationId, List<Long> labelIds);

    /**
     * Search labels by name
     * 
     * @param annotationId Annotation project ID
     * @param keyword Search keyword
     * @return Label list
     */
    List<AnnotationLabel> searchLabels(Long annotationId, String keyword);
} 