package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.SceneGovernance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 场景治理数据访问层接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface SceneGovernanceMapper extends BaseMapper<SceneGovernance> {

    /**
     * 根据名称查询场景治理信息
     *
     * @param name 场景名称
     * @return 场景治理信息
     */
    @Select("SELECT * FROM scene_governance WHERE name = #{name} AND deleted = 0")
    SceneGovernance selectByName(@Param("name") String name);

    /**
     * 根据状态查询场景治理列表
     *
     * @param status 场景状态
     * @return 场景治理列表
     */
    @Select("SELECT * FROM scene_governance WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<SceneGovernance> selectByStatus(@Param("status") String status);

    /**
     * 获取场景治理总数
     *
     * @return 总数
     */
    @Select("SELECT COUNT(*) FROM scene_governance WHERE deleted = 0")
    Long getTotalCount();

    /**
     * 获取启用的场景治理数量
     *
     * @return 启用数量
     */
    @Select("SELECT COUNT(*) FROM scene_governance WHERE status = 'enabled' AND deleted = 0")
    Long getEnabledCount();

    /**
     * 获取禁用的场景治理数量
     *
     * @return 禁用数量
     */
    @Select("SELECT COUNT(*) FROM scene_governance WHERE status = 'disabled' AND deleted = 0")
    Long getDisabledCount();
} 