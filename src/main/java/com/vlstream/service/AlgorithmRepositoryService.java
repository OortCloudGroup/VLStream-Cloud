package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.AlgorithmRepository;

import java.util.List;

/**
 * 算法仓库服务接口
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
public interface AlgorithmRepositoryService extends IService<AlgorithmRepository> {

    /**
     * 分页查询算法仓库列表
     * 
     * @param page 分页参数
     * @param name 仓库名称（模糊查询）
     * @param repositoryType 仓库类型
     * @param status 状态
     * @return 分页结果
     */
    IPage<AlgorithmRepository> selectRepositoryPage(Page<AlgorithmRepository> page, 
                                                   String name, 
                                                   String repositoryType, 
                                                   String status);

    /**
     * 查询所有启用的算法仓库
     * 
     * @return 启用的算法仓库列表
     */
    List<AlgorithmRepository> getEnabledRepositories();

    /**
     * 根据类型查询算法仓库
     * 
     * @param repositoryType 仓库类型
     * @return 算法仓库列表
     */
    List<AlgorithmRepository> getByRepositoryType(String repositoryType);

    /**
     * 创建算法仓库
     * 
     * @param repository 算法仓库信息
     * @return 是否成功
     */
    boolean createRepository(AlgorithmRepository repository);

    /**
     * 更新算法仓库
     * 
     * @param repository 算法仓库信息
     * @return 是否成功
     */
    boolean updateRepository(AlgorithmRepository repository);

    /**
     * 删除算法仓库
     * 
     * @param id 仓库ID
     * @return 是否成功
     */
    boolean deleteRepository(Long id);

    /**
     * 批量删除算法仓库
     * 
     * @param ids 仓库ID列表
     * @return 是否成功
     */
    boolean batchDeleteRepositories(List<Long> ids);

    /**
     * 更新仓库状态
     * 
     * @param id 仓库ID
     * @param status 新状态
     * @return 是否成功
     */
    boolean updateRepositoryStatus(Long id, String status);

    /**
     * 批量更新仓库状态
     * 
     * @param ids 仓库ID列表
     * @param status 新状态
     * @return 是否成功
     */
    boolean batchUpdateRepositoryStatus(List<Long> ids, String status);

    /**
     * 统计算法仓库数量
     * 
     * @return 仓库总数
     */
    Long countRepositories();

    /**
     * 更新仓库的算法数量
     * 
     * @param repositoryId 仓库ID
     */
    void updateAlgorithmCount(Long repositoryId);
} 