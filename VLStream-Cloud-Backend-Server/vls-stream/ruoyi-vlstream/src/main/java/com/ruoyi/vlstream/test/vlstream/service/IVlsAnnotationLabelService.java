package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationLabelExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationLabelVO;

import java.util.List;

/**
 * 标注标签实体类 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAnnotationLabelService extends BaseService<AnnotationLabel> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAnnotationLabel 查询参数
	 * @return IPage<VlsAnnotationLabelVO>
	 */
	IPage<AnnotationLabelVO> selectVlsAnnotationLabelPage(IPage<AnnotationLabelVO> page, AnnotationLabelVO vlsAnnotationLabel);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAnnotationLabelExcel>
	 */
	List<VlsAnnotationLabelExcel> exportVlsAnnotationLabel(Wrapper<AnnotationLabel> queryWrapper);

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
