package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.AnnotationInstance;

import java.util.List;

/**
 * 标注实例Service接口
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
public interface AnnotationInstanceService extends IService<AnnotationInstance> {

    /**
     * 根据标注项目ID和图片名称查询标注实例
     * 
     * @param annotationId 标注项目ID
     * @param imageName 图片名称
     * @return 标注实例列表
     */
    List<AnnotationInstance> getByAnnotationIdAndImageName(Long annotationId, String imageName);

    /**
     * 保存标注实例
     * 
     * @param annotationId 标注项目ID
     * @param labelId 标签ID
     * @param imageId 图片id
     * @param annotationType 标注类型
     * @param annotationData 标注数据（JSON格式）
     * @return 保存的标注实例
     */
    AnnotationInstance saveAnnotation(Long annotationId, Long labelId, Long imageId, String annotationType, String annotationData);

    /**
     * 更新标注实例
     * 
     * @param instanceId 实例ID
     * @param labelId 标签ID
     * @param annotationType 标注类型
     * @param annotationData 标注数据（JSON格式）
     * @return 更新后的标注实例
     */
    AnnotationInstance updateAnnotation(Long instanceId, Long labelId, 
                                       String annotationType, String annotationData);

    /**
     * 删除标注实例
     * 
     * @param instanceId 实例ID
     * @return 是否删除成功
     */
    boolean deleteAnnotation(Long instanceId);

    /**
     * 批量保存标注实例
     * 
     * @param annotationId 标注项目ID
     * @param imageId 图片ID
     * @param annotations 标注实例列表
     * @return 是否保存成功
     */
    boolean batchSaveAnnotations(Long annotationId, Long imageId, List<AnnotationInstance> annotations);

    /**
     * 根据标注项目ID查询所有标注实例
     * 
     * @param annotationId 标注项目ID
     * @return 标注实例列表
     */
    List<AnnotationInstance> getByAnnotationId(Long annotationId);

    /**
     * 根据标签ID统计使用次数
     *
     * @param labelId 标签ID
     * @return 使用次数
     */
    Integer countByLabelId(Long labelId);

    /**
     * 删除图片及其相关的所有数据
     * 包括：annotation_image、annotation_instance、更新annotation_label的使用计数
     *
     * @param annotationId 标注项目ID
     * @param imageId 图片ID
     * @return 删除结果
     */
    boolean deleteImageAndRelatedData(Long annotationId, Long imageId);
}