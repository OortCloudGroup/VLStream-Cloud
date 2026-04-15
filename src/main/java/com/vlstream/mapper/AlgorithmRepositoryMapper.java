package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.entity.AlgorithmRepository;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 算法仓库Mapper接口
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface AlgorithmRepositoryMapper extends BaseMapper<AlgorithmRepository> {

    /**
     * 分页查询算法仓库列表
     */
    @Select("SELECT r.*, " +
            "(SELECT COUNT(*) FROM algorithm a WHERE a.repository_id = r.id AND a.deleted = 0) as algorithm_count " +
            "FROM algorithm_repository r " +
            "WHERE r.deleted = 0 " +
            "AND (#{name} IS NULL OR r.name LIKE CONCAT('%', #{name}, '%')) " +
            "AND (#{repositoryType} IS NULL OR r.repository_type = #{repositoryType}) " +
            "AND (#{status} IS NULL OR r.status = #{status}) " +
            "ORDER BY r.id ASC")
    IPage<AlgorithmRepository> selectRepositoryPage(Page<AlgorithmRepository> page, 
                                                   @Param("name") String name,
                                                   @Param("repositoryType") String repositoryType,
                                                   @Param("status") String status);

    /**
     * 查询所有启用的算法仓库
     */
    @Select("SELECT * FROM algorithm_repository WHERE deleted = 0 AND status = 'enabled' ORDER BY id")
    List<AlgorithmRepository> selectEnabledRepositories();

    /**
     * 根据类型查询算法仓库
     */
    @Select("SELECT * FROM algorithm_repository WHERE deleted = 0 AND repository_type = #{repositoryType} ORDER BY id")
    List<AlgorithmRepository> selectByRepositoryType(@Param("repositoryType") String repositoryType);

    /**
     * 统计算法仓库数量
     */
    @Select("SELECT COUNT(*) FROM algorithm_repository WHERE deleted = 0")
    Long countRepositories();
} 