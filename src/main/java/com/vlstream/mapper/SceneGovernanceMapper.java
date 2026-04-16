package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.SceneGovernance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Scene Governance Data Access Layer Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface SceneGovernanceMapper extends BaseMapper<SceneGovernance> {

    /**
     * Query scene governance information by name
     *
     * @param name Scene name
     * @return Scene governance information
     */
    @Select("SELECT * FROM scene_governance WHERE name = #{name} AND deleted = 0")
    SceneGovernance selectByName(@Param("name") String name);

    /**
     * Query scene governance list by status
     *
     * @param status Scene status
     * @return Scene governance list
     */
    @Select("SELECT * FROM scene_governance WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<SceneGovernance> selectByStatus(@Param("status") String status);

    /**
     * Get total scene governance count
     *
     * @return Total count
     */
    @Select("SELECT COUNT(*) FROM scene_governance WHERE deleted = 0")
    Long getTotalCount();

    /**
     * Get enabled scene governance count
     *
     * @return Enabled count
     */
    @Select("SELECT COUNT(*) FROM scene_governance WHERE status = 'enabled' AND deleted = 0")
    Long getEnabledCount();

    /**
     * Get disabled scene governance count
     *
     * @return Disabled count
     */
    @Select("SELECT COUNT(*) FROM scene_governance WHERE status = 'disabled' AND deleted = 0")
    Long getDisabledCount();
} 