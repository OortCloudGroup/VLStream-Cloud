package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmVO;

import java.util.List;
import java.util.Map;

/**
 * 算法表 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmService extends BaseService<Algorithm> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAlgorithm 查询参数
	 * @return IPage<VlsAlgorithmVO>
	 */
	IPage<AlgorithmVO> selectVlsAlgorithmPage(IPage<AlgorithmVO> page, AlgorithmVO vlsAlgorithm);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAlgorithmExcel>
	 */
	List<VlsAlgorithmExcel> exportVlsAlgorithm(Wrapper<Algorithm> queryWrapper);

	/**
	 * 分页查询算法列表
	 *
	 * @param page 分页参数
	 * @param repositoryId 仓库ID
	 * @param name 算法名称（模糊查询）
	 * @param category 算法类型
	 * @param deployStatus 部署状态
	 * @return 分页结果
	 */
	IPage<Algorithm> selectAlgorithmPage(Page<Algorithm> page,
										 Long repositoryId,
										 String name,
										 String category,
										 String deployStatus);

	/**
	 * 根据仓库ID查询算法列表
	 *
	 * @param repositoryId 仓库ID
	 * @return 算法列表
	 */
	List<Algorithm> getByRepositoryId(Long repositoryId);

	/**
	 * 根据分类查询算法列表
	 *
	 * @param category 算法分类
	 * @return 算法列表
	 */
	List<Algorithm> getByCategory(String category);

	/**
	 * 创建算法
	 *
	 * @param algorithm 算法信息
	 * @return 是否成功
	 */
	boolean createAlgorithm(Algorithm algorithm);

	/**
	 * 更新算法
	 *
	 * @param algorithm 算法信息
	 * @return 是否成功
	 */
	boolean updateAlgorithm(Algorithm algorithm);

	/**
	 * 删除算法
	 *
	 * @param id 算法ID
	 * @return 是否成功
	 */
	boolean deleteAlgorithm(Long id);

	/**
	 * 批量删除算法
	 *
	 * @param ids 算法ID列表
	 * @return 是否成功
	 */
	boolean batchDeleteAlgorithms(List<Long> ids);

	/**
	 * 更新部署状态
	 *
	 * @param id 算法ID
	 * @param deployStatus 新部署状态
	 * @return 是否成功
	 */
	boolean updateDeployStatus(Long id, String deployStatus);

	/**
	 * 批量更新部署状态
	 *
	 * @param ids 算法ID列表
	 * @param deployStatus 新部署状态
	 * @return 是否成功
	 */
	boolean batchUpdateDeployStatus(List<Long> ids, String deployStatus);

	/**
	 * 部署算法到设备
	 *
	 * @param algorithmId 算法ID
	 * @param deviceIds 设备ID列表
	 * @return 是否成功
	 */
	boolean deployAlgorithmToDevices(Long algorithmId, List<Long> deviceIds);

	/**
	 * 统计某仓库下的算法数量
	 *
	 * @param repositoryId 仓库ID
	 * @return 算法数量
	 */
	Long countByRepositoryId(Long repositoryId);

	/**
	 * 获取算法分类统计
	 *
	 * @return 分类统计信息
	 */
	List<Map<String, Object>> getCategoryStatistics();

	/**
	 * 获取算法类型统计
	 *
	 * @return 类型统计信息
	 */
	List<Map<String, Object>> getTypeStatistics();

	/**
	 * 获取部署状态统计
	 *
	 * @return 部署状态统计信息
	 */
	List<Map<String, Object>> getDeployStatusStatistics();

	/**
	 * 算法评估
	 *
	 * @param algorithmId 算法ID
	 * @return 评估结果
	 */
	Map<String, Object> evaluateAlgorithm(Long algorithmId);

}
