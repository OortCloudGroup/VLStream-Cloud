package com.vlstream.service;

import com.vlstream.entity.AnnotationImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 标注图片服务接口
 */
public interface AnnotationImageService {
    
    /**
     * 上传图片
     */
    List<AnnotationImage> uploadImages(MultipartFile[] files, Long annotationId);
    
    /**
     * 根据数据集ID获取图片列表
     */
    List<AnnotationImage> getImagesByDataset(Long annotationId);
    
    /**
     * 根据ID获取图片详情
     */
    AnnotationImage getImageById(Long id);
    
    /**
     * 更新图片信息
     */
    AnnotationImage updateImage(AnnotationImage image);
    
    /**
     * 删除图片
     */
    void deleteImage(Long id);
    
    /**
     * 批量删除图片
     */
    void batchDeleteImages(List<Long> ids);
    
    /**
     * 获取数据集统计信息
     */
    Map<String, Object> getDatasetStats(Long datasetId);

    /**
     * 保存图片信息到annotation_image表
     */
    boolean saveImage(AnnotationImage annotationImage);

    /**
     * 批量保存图片信息到annotation_image表
     */
    boolean batchSaveImages(List<AnnotationImage> annotationImages);

    /**
     * 根据标注项目ID获取图片列表
     */
    List<AnnotationImage> getImagesByAnnotationId(Long annotationId);
}




