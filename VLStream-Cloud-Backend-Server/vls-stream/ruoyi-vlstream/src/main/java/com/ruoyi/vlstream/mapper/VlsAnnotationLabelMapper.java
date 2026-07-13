/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.AnnotationLabel;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Mapper for VLS annotation labels.
 */
public interface VlsAnnotationLabelMapper extends BaseMapperPlus<VlsAnnotationLabelMapper, AnnotationLabel, AnnotationLabel> {

    /**
     * Query labels by annotation task and include live annotation instance usage counts.
     */
    @Select({
        "<script>",
        "SELECT al.*,",
        "COALESCE((",
        "  SELECT COUNT(*) FROM vls_annotation_instance ai",
        "  WHERE ai.label_id = al.id AND ai.is_deleted = 0",
        "), al.usage_count, 0) AS usage_count",
        "FROM vls_annotation_label al",
        "WHERE al.annotation_id = #{annotationId} AND al.is_deleted = 0",
        "<if test='keyword != null and keyword != \"\"'>",
        "  AND al.name LIKE CONCAT('%', #{keyword}, '%')",
        "</if>",
        "ORDER BY al.sort_order ASC, al.id ASC",
        "</script>"
    })
    List<AnnotationLabel> selectByAnnotationIdWithUsageCount(@Param("annotationId") Long annotationId,
                                                             @Param("keyword") String keyword);

    /**
     * Count active annotation instances that still reference a label.
     */
    @Select("SELECT COUNT(*) FROM vls_annotation_instance WHERE label_id = #{labelId} AND is_deleted = 0")
    Integer countActiveInstancesByLabelId(@Param("labelId") Long labelId);
}
