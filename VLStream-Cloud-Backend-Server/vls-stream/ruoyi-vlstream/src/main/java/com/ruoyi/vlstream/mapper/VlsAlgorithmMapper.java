package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.Algorithm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * Mapper for VLS algorithms.
 */
@Mapper
public interface VlsAlgorithmMapper extends BaseMapperPlus<VlsAlgorithmMapper, Algorithm, Algorithm> {

    @Select("SELECT COUNT(*) FROM vls_algorithm WHERE repository_id = #{repositoryId} AND is_deleted = 0")
    Long countByRepositoryId(@Param("repositoryId") Long repositoryId);

    @Select("SELECT category, COUNT(*) AS count FROM vls_algorithm WHERE is_deleted = 0 GROUP BY category")
    List<Map<String, Object>> selectCategoryStatistics();

    @Select("SELECT COALESCE(input_format, 'unknown') AS type, COUNT(*) AS count FROM vls_algorithm WHERE is_deleted = 0 GROUP BY input_format")
    List<Map<String, Object>> selectTypeStatistics();

    @Select("SELECT CASE WHEN status = 1 THEN 'available' ELSE 'disabled' END AS deployStatus, COUNT(*) AS count FROM vls_algorithm WHERE is_deleted = 0 GROUP BY status")
    List<Map<String, Object>> selectDeployStatusStatistics();
}
