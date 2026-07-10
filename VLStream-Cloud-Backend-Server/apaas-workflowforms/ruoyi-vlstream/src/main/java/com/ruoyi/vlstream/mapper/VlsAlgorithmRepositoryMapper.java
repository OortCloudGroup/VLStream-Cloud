package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.AlgorithmRepository;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Mapper for VLS algorithm repositories.
 */
@Mapper
public interface VlsAlgorithmRepositoryMapper extends BaseMapperPlus<VlsAlgorithmRepositoryMapper, AlgorithmRepository, AlgorithmRepository> {

    @Select("SELECT * FROM vls_algorithm_repository WHERE is_deleted = 0 AND status = 1 ORDER BY id")
    List<AlgorithmRepository> selectEnabledRepositories();

    @Select("SELECT * FROM vls_algorithm_repository WHERE is_deleted = 0 AND repository_type = #{repositoryType} ORDER BY id")
    List<AlgorithmRepository> selectByRepositoryType(@Param("repositoryType") String repositoryType);

    @Select("SELECT COUNT(*) FROM vls_algorithm_repository WHERE is_deleted = 0")
    Long countRepositories();

    @Select("SELECT COUNT(*) FROM vls_algorithm WHERE repository_id = #{repositoryId} AND is_deleted = 0")
    Long countAlgorithmsByRepositoryId(@Param("repositoryId") Long repositoryId);
}
