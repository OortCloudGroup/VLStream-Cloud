package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import javax.servlet.http.HttpServletResponse;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmAnnotationExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmAnnotationVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 算法标注数据表 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmAnnotationService extends BaseService<AlgorithmAnnotation> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAlgorithmAnnotation 查询参数
	 * @return IPage<VlsAlgorithmAnnotationVO>
	 */
	IPage<AlgorithmAnnotationVO> selectVlsAlgorithmAnnotationPage(IPage<AlgorithmAnnotationVO> page, AlgorithmAnnotationVO vlsAlgorithmAnnotation);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAlgorithmAnnotationExcel>
	 */
	List<VlsAlgorithmAnnotationExcel> exportVlsAlgorithmAnnotation(Wrapper<AlgorithmAnnotation> queryWrapper);

	/**
	 * 分页查询算法标注列表
	 *
	 * @param page 分页参数
	 * @param annotationName 标注名称（模糊查询）
	 * @param annotationType 标注类型
	 * @param annotationStatus 标注状态
	 * @return 分页结果
	 */
	IPage<AlgorithmAnnotation> selectAnnotationPage(Page<AlgorithmAnnotation> page,
													String annotationName,
													String annotationType,
													String annotationStatus);

	/**
	 * 根据标注类型查询标注列表
	 *
	 * @param annotationType 标注类型
	 * @return 标注列表
	 */
	List<AlgorithmAnnotation> getByAnnotationType(String annotationType);

	/**
	 * 根据标注状态查询标注列表
	 *
	 * @param annotationStatus 标注状态
	 * @return 标注列表
	 */
	List<AlgorithmAnnotation> getByAnnotationStatus(String annotationStatus);

	/**
	 * 创建算法标注
	 *
	 * @param annotation 标注信息
	 * @return 是否成功
	 */
	boolean createAnnotation(AlgorithmAnnotation annotation);

	/**
	 * 更新算法标注
	 *
	 * @param annotation 标注信息
	 * @return 是否成功
	 */
	boolean updateAnnotation(AlgorithmAnnotation annotation);

	/**
	 * 删除算法标注
	 *
	 * @param id 标注ID
	 * @return 是否成功
	 */
	boolean deleteAnnotation(Long id);

	/**
	 * 批量删除算法标注
	 *
	 * @param ids 标注ID列表
	 * @return 是否成功
	 */
	boolean batchDeleteAnnotations(List<Long> ids);

	/**
	 * 更新标注进度
	 *
	 * @param id 标注ID
	 * @param annotatedCount 已标注数量
	 * @return 是否成功
	 */
	boolean updateAnnotationProgress(Long id, Integer annotatedCount);

	/**
	 * 批量更新标注状态
	 *
	 * @param ids 标注ID列表
	 * @param annotationStatus 新标注状态
	 * @return 是否成功
	 */
	boolean batchUpdateAnnotationStatus(List<Long> ids, String annotationStatus);

	/**
	 * 开始标注任务
	 *
	 * @param id 标注ID
	 * @return 是否成功
	 */
	boolean startAnnotationTask(Long id);

	/**
	 * 完成标注任务
	 *
	 * @param id 标注ID
	 * @return 是否成功
	 */
	boolean completeAnnotationTask(Long id);

	/**
	 * 重置标注任务
	 *
	 * @param id 标注ID
	 * @return 是否成功
	 */
	boolean resetAnnotationTask(Long id);

	/**
	 * 导入标注数据
	 *
	 * @param id 标注ID
	 * @param dataPath 数据路径
	 * @return 导入结果
	 */
	Map<String, Object> importAnnotationData(Long id, String dataPath);

	/**
	 * Import annotation dataset from zip file.
	 *
	 * @param annotationId annotation id
	 * @param zipFile zip dataset file
	 * @return import result
	 */
	Map<String, Object> importAnnotationDatasetZip(Long annotationId, MultipartFile zipFile);

	/**
	 * 获取标注类型统计
	 *
	 * @return 标注类型统计信息
	 */
	List<Map<String, Object>> getAnnotationTypeStatistics();

	/**
	 * 获取标注状态统计
	 *
	 * @return 标注状态统计信息
	 */
	List<Map<String, Object>> getAnnotationStatusStatistics();

	/**
	 * 获取标注进度统计
	 *
	 * @return 标注进度统计信息
	 */
	List<Map<String, Object>> getProgressStatistics();

	/**
	 * 获取标注工作量统计
	 *
	 * @return 标注工作量统计信息
	 */
	Map<String, Object> getWorkloadStatistics();

	/**
	 * 验证标注数据
	 *
	 * @param id 标注ID
	 * @return 验证结果
	 */
	Map<String, Object> validateAnnotationData(Long id);

	/**
	 * 保存标注数据到数据集文件
	 *
	 * @param annotationId 标注ID
	 * @return 是否保存成功
	 */
	boolean saveAnnotationToDataset(Long annotationId);

	/**
	 * Download the annotation dataset as a zip package.
	 *
	 * @param id       annotation id
	 * @param response http response
	 */
	void downloadAnnotationDataset(Long id, HttpServletResponse response);

}
