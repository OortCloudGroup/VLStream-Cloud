package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationImageExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationImageVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 标注图片信息表 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAnnotationImageService extends BaseService<AnnotationImage> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAnnotationImage 查询参数
	 * @return IPage<VlsAnnotationImageVO>
	 */
	IPage<AnnotationImageVO> selectVlsAnnotationImagePage(IPage<AnnotationImageVO> page, AnnotationImageVO vlsAnnotationImage);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAnnotationImageExcel>
	 */
	List<VlsAnnotationImageExcel> exportVlsAnnotationImage(Wrapper<AnnotationImage> queryWrapper);

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
