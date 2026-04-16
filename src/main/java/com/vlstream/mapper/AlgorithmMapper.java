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
 * Algorithm Mapper Interface
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface AlgorithmMapper extends BaseMapper<Algorithm> {

    /**
     * Paginated query algorithm list
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
     * Query algorithm list by repository ID
     */
    @Select("SELECT * FROM algorithm WHERE deleted = 0 AND repository_id = #{repositoryId} ORDER BY created_time DESC")
    List<Algorithm> selectByRepositoryId(@Param("repositoryId") Long repositoryId);

    /**
     * Query algorithm list by category
     */
    @Select("SELECT * FROM algorithm WHERE deleted = 0 AND category = #{category} ORDER BY created_time DESC")
    List<Algorithm> selectByCategory(@Param("category") String category);

    /**
     * Count algorithms under a repository
     */
    @Select("SELECT COUNT(*) FROM algorithm WHERE deleted = 0 AND repository_id = #{repositoryId}")
    Long countByRepositoryId(@Param("repositoryId") Long repositoryId);

    /**
     * Query algorithm category statistics
     */
    @Select("SELECT category, COUNT(*) as count FROM algorithm WHERE deleted = 0 GROUP BY category")
    List<Map<String, Object>> selectCategoryStatistics();

    /**
     * Query algorithm type statistics
     */
    @Select("SELECT type, COUNT(*) as count FROM algorithm WHERE deleted = 0 GROUP BY type")
    List<Map<String, Object>> selectTypeStatistics();

    /**
     * Query deployment status statistics
     */
    @Select("SELECT deploy_status, COUNT(*) as count FROM algorithm WHERE deleted = 0 GROUP BY deploy_status")
    List<Map<String, Object>> selectDeployStatusStatistics();
} 