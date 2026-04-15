package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.entity.Algorithm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 算法Mapper接口
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface AlgorithmMapper extends BaseMapper<Algorithm> {

    /**
     * 分页查询算法列表
     */
    @Select("SELECT a.*, r.name as repository_name " +
            "FROM algorithm a " +
            "LEFT JOIN algorithm_repository r ON a.repository_id = r.id " +
            "WHERE a.deleted = 0 " +
            "AND (#{repositoryId} IS NULL OR a.repository_id = #{repositoryId}) " +
            "AND (#{name} IS NULL OR a.name LIKE CONCAT('%', #{name}, '%')) " +
            "AND (#{category} IS NULL OR a.category = #{category}) " +
            "AND (#{deployStatus} IS NULL OR a.deploy_status = #{deployStatus}) ")
    IPage<Algorithm> selectAlgorithmPage(Page<Algorithm> page,
                                       @Param("repositoryId") Long repositoryId,
                                       @Param("name") String name,
                                       @Param("category") String category,
                                       @Param("deployStatus") String deployStatus);

    /**
     * 根据仓库ID查询算法列表
     */
    @Select("SELECT * FROM algorithm WHERE deleted = 0 AND repository_id = #{repositoryId} ORDER BY created_time DESC")
    List<Algorithm> selectByRepositoryId(@Param("repositoryId") Long repositoryId);

    /**
     * 根据分类查询算法列表
     */
    @Select("SELECT * FROM algorithm WHERE deleted = 0 AND category = #{category} ORDER BY created_time DESC")
    List<Algorithm> selectByCategory(@Param("category") String category);

    /**
     * 统计某仓库下的算法数量
     */
    @Select("SELECT COUNT(*) FROM algorithm WHERE deleted = 0 AND repository_id = #{repositoryId}")
    Long countByRepositoryId(@Param("repositoryId") Long repositoryId);

    /**
     * 查询算法分类统计
     */
    @Select("SELECT category, COUNT(*) as count FROM algorithm WHERE deleted = 0 GROUP BY category")
    List<Map<String, Object>> selectCategoryStatistics();

    /**
     * 查询算法类型统计
     */
    @Select("SELECT type, COUNT(*) as count FROM algorithm WHERE deleted = 0 GROUP BY type")
    List<Map<String, Object>> selectTypeStatistics();

    /**
     * 查询部署状态统计
     */
    @Select("SELECT deploy_status, COUNT(*) as count FROM algorithm WHERE deleted = 0 GROUP BY deploy_status")
    List<Map<String, Object>> selectDeployStatusStatistics();
} 