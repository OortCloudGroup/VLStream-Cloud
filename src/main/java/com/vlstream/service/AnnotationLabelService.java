package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.AnnotationLabel;

import java.util.List;

/**
 * 标注标签Service接口
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
public interface AnnotationLabelService extends IService<AnnotationLabel> {

    /**
     * 根据标注项目ID查询标签列表（包含使用次数统计）
     * 
     * @param annotationId 标注项目ID
     * @return 标签列表
     */
    List<AnnotationLabel> getByAnnotationIdWithUsageCount(Long annotationId);

    /**
     * 创建标签
     * 
     * @param annotationId 标注项目ID
     * @param name 标签名称
     * @param color 标签颜色
     * @param description 标签描述
     * @return 创建的标签
     */
    AnnotationLabel createLabel(Long annotationId, String name, String color, String description);

    /**
     * 更新标签
     * 
     * @param labelId 标签ID
     * @param name 标签名称
     * @param color 标签颜色
     * @param description 标签描述
     * @return 更新后的标签
     */
    AnnotationLabel updateLabel(Long labelId, String name, String color, String description);

    /**
     * 删除标签
     * 
     * @param labelId 标签ID
     * @return 是否删除成功
     */
    boolean deleteLabel(Long labelId);

    /**
     * 更新标签的使用次数
     * 
     * @param labelId 标签ID
     * @return 是否更新成功
     */
    boolean updateUsageCount(Long labelId);

    /**
     * 批量更新标签排序
     * 
     * @param annotationId 标注项目ID
     * @param labelIds 标签ID列表（按排序顺序）
     * @return 是否更新成功
     */
    boolean updateSortOrder(Long annotationId, List<Long> labelIds);

    /**
     * 根据名称搜索标签
     * 
     * @param annotationId 标注项目ID
     * @param keyword 搜索关键词
     * @return 标签列表
     */
    List<AnnotationLabel> searchLabels(Long annotationId, String keyword);
} 