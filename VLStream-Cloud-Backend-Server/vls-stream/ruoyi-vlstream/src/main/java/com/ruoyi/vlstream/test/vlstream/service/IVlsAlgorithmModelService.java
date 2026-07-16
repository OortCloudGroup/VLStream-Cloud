package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmModelExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmModelVO;

import java.util.List;

/**
 * 算法模型表 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmModelService extends BaseService<AlgorithmModel> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAlgorithmModel 查询参数
	 * @return IPage<VlsAlgorithmModelVO>
	 */
	IPage<AlgorithmModelVO> selectVlsAlgorithmModelPage(IPage<AlgorithmModelVO> page, AlgorithmModelVO vlsAlgorithmModel);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAlgorithmModelExcel>
	 */
	List<VlsAlgorithmModelExcel> exportVlsAlgorithmModel(Wrapper<AlgorithmModel> queryWrapper);

	/**
	 * 根据ID查询算法模型详情
	 *
	 * @param id 模型ID
	 * @return 算法模型
	 */
	AlgorithmModel getModelById(Long id);

	/**
	 * 创建算法模型
	 *
	 * @param createDTO 创建参数
	 * @return 创建成功的模型
	 */
	AlgorithmModel createModel(AlgorithmModelVO createDTO);

	/**
	 * 删除算法模型
	 *
	 * @param id 模型ID
	 * @return 是否成功
	 */
	boolean deleteModel(Long id);

	/**
	 * 批量删除算法模型
	 *
	 * @param ids 模型ID列表
	 * @return 是否成功
	 */
	boolean batchDeleteModel(List<Long> ids);

	/**
	 * 根据算法ID查询模型列表
	 *
	 * @param algorithmId 算法ID
	 * @return 模型列表
	 */
	List<AlgorithmModel> getModelsByAlgorithmId(Long algorithmId);

	/**
	 * 根据训练任务ID查询模型列表
	 *
	 * @param trainingId 训练任务ID
	 * @return 模型列表
	 */
	List<AlgorithmModel> getModelsByTrainingId(Long trainingId);

	/**
	 * 根据状态查询模型列表
	 *
	 * @param status 状态
	 * @return 模型列表
	 */
	List<AlgorithmModel> getModelsByStatus(String status);

	/**
	 * 发布模型
	 *
	 * @param id 模型ID
	 * @return 是否成功
	 */
	boolean publishModel(Long id);

	/**
	 * 撤销发布模型
	 *
	 * @param id 模型ID
	 * @return 是否成功
	 */
	boolean unpublishModel(Long id);

	/**
	 * 批量发布模型
	 *
	 * @param ids 模型ID列表
	 * @return 是否成功
	 */
	boolean batchPublishModel(List<Long> ids);

	/**
	 * 下载模型
	 *
	 * @param id 模型ID
	 * @return 模型文件路径
	 */
	String downloadModel(Long id);

	/**
	 * 部署模型
	 *
	 * @param id 模型ID
	 * @return 是否成功
	 */
	boolean deployModel(Long id);

	/**
	 * 检查模型名称和版本是否存在
	 *
	 * @param modelName 模型名称
	 * @param version 版本
	 * @param excludeId 排除的ID（更新时使用）
	 * @return 是否存在
	 */
	boolean checkModelNameAndVersion(String modelName, Integer version, Long excludeId);

	/**
	 * 根据算法ID和版本查询模型
	 *
	 * @param algorithmId 算法ID
	 * @param version 版本
	 * @return 算法模型
	 */
	AlgorithmModel getModelByAlgorithmIdAndVersion(Long algorithmId, Integer version);

	/**
	 * 获取算法下最新版本的模型
	 *
	 * @param algorithmId 算法ID
	 * @return 算法模型
	 */
	AlgorithmModel getLatestModelByAlgorithmId(Long algorithmId);

	/**
	 * 查询热门模型（按下载次数排序）
	 *
	 * @param limit 限制数量
	 * @return 模型列表
	 */
	List<AlgorithmModel> getPopularModels(Integer limit);

	/**
	 * 根据创建人查询模型数量
	 *
	 * @param createdBy 创建人ID
	 * @return 模型数量
	 */
	Long countModelsByCreatedBy(Long createdBy);

	/**
	 * 获取算法模型的总大小
	 *
	 * @return 总大小（字节）
	 */
	Long getTotalModelSize();

}
