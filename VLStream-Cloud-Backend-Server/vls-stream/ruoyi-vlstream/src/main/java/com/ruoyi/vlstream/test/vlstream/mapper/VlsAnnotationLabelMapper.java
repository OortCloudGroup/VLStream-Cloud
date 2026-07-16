package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationLabelExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationLabelVO;

import java.util.List;

/**
 * 标注标签实体类 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAnnotationLabelMapper extends BaseMapper<AnnotationLabel> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAnnotationLabel 查询参数
	 * @return List<VlsAnnotationLabelVO>
	 */
	List<AnnotationLabelVO> selectVlsAnnotationLabelPage(IPage page, AnnotationLabelVO vlsAnnotationLabel);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAnnotationLabelExcel>
	 */
	List<VlsAnnotationLabelExcel> exportVlsAnnotationLabel(@Param("ew") Wrapper<AnnotationLabel> queryWrapper);

	/**
	 * 根据标注项目ID查询标签列表（包含使用次数统计）
	 *
	 * @param annotationId 标注项目ID
	 * @return 标签列表
	 */
	@Select("SELECT al.*, " +
		"COALESCE((SELECT COUNT(*) FROM vls_annotation_instance ai " +
		"WHERE ai.label_id = al.id AND ai.is_deleted = 0), 0) as usage_count " +
		"FROM vls_annotation_label al " +
		"WHERE al.annotation_id = #{annotationId} AND al.is_deleted = 0 " +
		"ORDER BY al.sort_order ASC, al.id ASC")
	List<AnnotationLabel> selectByAnnotationIdWithUsageCount(@Param("annotationId") Long annotationId);

	/**
	 * 更新标签的使用次数
	 *
	 * @param labelId 标签ID
	 * @param usageCount 使用次数
	 * @return 更新行数
	 */
	@Update("UPDATE vls_annotation_label SET usage_count = #{usageCount} WHERE id = #{labelId}")
	int updateUsageCount(@Param("labelId") Long labelId, @Param("usageCount") Integer usageCount);

}
