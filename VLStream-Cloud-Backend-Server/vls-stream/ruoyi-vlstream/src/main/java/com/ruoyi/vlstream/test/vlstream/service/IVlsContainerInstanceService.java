package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsContainerInstanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.ContainerInstanceVO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 容器实例表 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsContainerInstanceService extends BaseService<ContainerInstance> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsContainerInstance 查询参数
	 * @return IPage<VlsContainerInstanceVO>
	 */
	IPage<ContainerInstanceVO> selectVlsContainerInstancePage(IPage<ContainerInstanceVO> page, ContainerInstanceVO vlsContainerInstance);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsContainerInstanceExcel>
	 */
	List<VlsContainerInstanceExcel> exportVlsContainerInstance(Wrapper<ContainerInstance> queryWrapper);

	/**
	 * 删除容器实例
	 *
	 * @param id 容器实例ID
	 * @return 是否删除成功
	 */
	boolean deleteContainerInstance(Long id);

	/**
	 * 批量删除容器实例
	 *
	 * @param ids 容器实例ID列表
	 * @return 是否删除成功
	 */
	boolean deleteContainerInstanceBatch(List<Long> ids);

	/**
	 * 根据容器ID查询容器实例
	 *
	 * @param containerId 容器ID
	 * @return 容器实例
	 */
	ContainerInstance getByContainerId(String containerId);

	/**
	 * 根据算法ID查询容器实例列表
	 *
	 * @param algorithmId 算法ID
	 * @return 容器实例列表
	 */
	List<ContainerInstance> getByAlgorithmId(Long algorithmId);

	/**
	 * 根据状态查询容器实例列表
	 *
	 * @param instanceStatus 实例状态
	 * @return 容器实例列表
	 */
	List<ContainerInstance> getByStatus(String instanceStatus);

	/**
	 * 启动容器实例
	 *
	 * @param id 容器实例ID
	 * @param containerId 容器ID
	 * @return 是否启动成功
	 */
	boolean startContainer(Long id, String containerId);

	/**
	 * 停止容器实例
	 *
	 * @param id 容器实例ID
	 * @return 是否停止成功
	 */
	boolean stopContainer(Long id);

	/**
	 * 重启容器实例
	 *
	 * @param id 容器实例ID
	 * @return 是否重启成功
	 */
	boolean restartContainer(Long id);

	/**
	 * 更新容器实例状态
	 *
	 * @param id 容器实例ID
	 * @param instanceStatus 实例状态
	 * @param healthStatus 健康状态
	 * @param containerId 容器ID
	 * @param startTime 启动时间
	 * @param stopTime 停止时间
	 * @return 是否更新成功
	 */
	boolean updateInstanceStatus(Long id, String instanceStatus, String healthStatus,
								 String containerId, Date startTime, Date stopTime);

	/**
	 * 更新容器监控数据
	 *
	 * @param id 容器实例ID
	 * @param cpuUsage CPU使用率
	 * @param memoryUsage 内存使用率
	 * @param gpuUsage GPU使用率
	 * @return 是否更新成功
	 */
	boolean updateMonitoringData(Long id, BigDecimal cpuUsage, BigDecimal memoryUsage, BigDecimal gpuUsage);

	/**
	 * 增加重启次数
	 *
	 * @param id 容器实例ID
	 * @return 是否更新成功
	 */
	boolean increaseRestartCount(Long id);

	/**
	 * 获取容器实例统计信息
	 *
	 * @return 统计结果
	 */
	Map<String, Object> getStatistics();

	/**
	 * 检查实例名称是否重复
	 *
	 * @param instanceName 实例名称
	 * @param excludeId 排除的ID（更新时使用）
	 * @return 是否重复
	 */
	boolean checkInstanceNameExists(String instanceName, Long excludeId);

	/**
	 * 获取运行中的容器实例列表
	 *
	 * @return 运行中的容器实例列表
	 */
	List<ContainerInstance> getRunningInstances();

	/**
	 * 获取异常状态的容器实例列表
	 *
	 * @return 异常状态的容器实例列表
	 */
	List<ContainerInstance> getErrorInstances();

}
