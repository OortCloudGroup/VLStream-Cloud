package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.SceneGovernance;

import java.util.List;
import java.util.Map;

/**
 * 场景治理服务层接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface SceneGovernanceService extends IService<SceneGovernance> {

    /**
     * 分页查询场景治理信息
     *
     * @param page      分页对象
     * @param name      场景名称
     * @param status    场景状态
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 场景治理信息分页列表
     */
    IPage<SceneGovernance> getSceneGovernancePage(Page<SceneGovernance> page,
                                                 String name,
                                                 String status,
                                                 String startDate,
                                                 String endDate);

    /**
     * 根据名称查询场景治理信息
     *
     * @param name 场景名称
     * @return 场景治理信息
     */
    SceneGovernance getByName(String name);

    /**
     * 新增场景治理信息
     *
     * @param sceneGovernance 场景治理信息
     * @return 是否成功
     */
    boolean addSceneGovernance(SceneGovernance sceneGovernance);

    /**
     * 更新场景治理信息
     *
     * @param sceneGovernance 场景治理信息
     * @return 是否成功
     */
    boolean updateSceneGovernance(SceneGovernance sceneGovernance);

    /**
     * 删除场景治理信息
     *
     * @param id 场景ID
     * @return 是否成功
     */
    boolean deleteSceneGovernance(Long id);

    /**
     * 批量删除场景治理信息
     *
     * @param ids 场景ID列表
     * @return 是否成功
     */
    boolean deleteSceneGovernanceBatch(List<Long> ids);

    /**
     * 更新场景治理状态
     *
     * @param id     场景ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateSceneGovernanceStatus(Long id, String status);

    /**
     * 批量更新场景治理状态
     *
     * @param ids    场景ID列表
     * @param status 状态
     * @return 是否成功
     */
    boolean updateSceneGovernanceStatusBatch(List<Long> ids, String status);

    /**
     * 根据状态获取场景治理列表
     *
     * @param status 场景状态
     * @return 场景治理列表
     */
    List<SceneGovernance> getSceneGovernancesByStatus(String status);

    /**
     * 根据执行类型获取场景治理列表
     *
     * @param executeType 执行类型
     * @return 场景治理列表
     */
    List<SceneGovernance> getSceneGovernancesByExecuteType(String executeType);

    /**
     * 检查场景名称是否存在
     *
     * @param name 场景名称
     * @param id   场景ID（编辑时排除自己）
     * @return 是否存在
     */
    boolean checkSceneNameExists(String name, Long id);

    /**
     * 获取场景治理统计信息
     *
     * @return 统计信息
     */
    Map<String, Object> getSceneGovernanceStatistics();

    /**
     * 获取所有执行类型列表
     *
     * @return 执行类型列表
     */
    List<String> getAllExecuteTypes();

    /**
     * 启用场景治理
     *
     * @param id 场景ID
     * @return 是否成功
     */
    boolean enableSceneGovernance(Long id);

    /**
     * 禁用场景治理
     *
     * @param id 场景ID
     * @return 是否成功
     */
    boolean disableSceneGovernance(Long id);

    /**
     * 批量启用场景治理
     *
     * @param ids 场景ID列表
     * @return 是否成功
     */
    boolean enableSceneGovernanceBatch(List<Long> ids);

    /**
     * 批量禁用场景治理
     *
     * @param ids 场景ID列表
     * @return 是否成功
     */
    boolean disableSceneGovernanceBatch(List<Long> ids);

    /**
     * 执行场景治理
     *
     * @param id 场景ID
     * @return 执行结果
     */
    Map<String, Object> executeSceneGovernance(Long id);

    /**
     * 验证场景治理配置
     *
     * @param sceneGovernance 场景治理信息
     * @return 验证结果
     */
    Map<String, Object> validateSceneGovernance(SceneGovernance sceneGovernance);

    /**
     * 导出场景治理信息
     *
     * @param sceneIds 场景ID列表，为空时导出所有场景
     * @return 导出数据
     */
    List<SceneGovernance> exportSceneGovernances(List<Long> sceneIds);

    /**
     * 批量导入场景治理
     *
     * @param sceneGovernanceList 场景治理列表
     * @return 导入结果
     */
    Map<String, Object> batchImportSceneGovernances(List<SceneGovernance> sceneGovernanceList);

    /**
     * 获取场景治理执行历史
     *
     * @param id 场景ID
     * @return 执行历史
     */
    List<Map<String, Object>> getSceneGovernanceExecuteHistory(Long id);

    /**
     * 复制场景治理
     *
     * @param id   源场景ID
     * @param name 新场景名称
     * @return 是否成功
     */
    boolean copySceneGovernance(Long id, String name);
} 