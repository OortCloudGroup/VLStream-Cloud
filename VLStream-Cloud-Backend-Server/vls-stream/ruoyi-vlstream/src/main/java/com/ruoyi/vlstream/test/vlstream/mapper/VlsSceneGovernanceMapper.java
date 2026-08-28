/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ruoyi.vlstream.test.vlstream.excel.VlsSceneGovernanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.SceneGovernance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.SceneGovernanceVO;

import java.util.List;

/**
 * Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsSceneGovernanceMapper extends BaseMapper<SceneGovernance> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsSceneGovernance Query parameter
	 * @return List<VlsSceneGovernanceVO>
	 */
	List<SceneGovernanceVO> selectVlsSceneGovernancePage(IPage page, SceneGovernanceVO vlsSceneGovernance);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsSceneGovernanceExcel>
	 */
	List<VlsSceneGovernanceExcel> exportVlsSceneGovernance(@Param("ew") Wrapper<SceneGovernance> queryWrapper);

	/**
	 * Query info
	 *
	 * @param name
	 * @return info
	 */
	@Select("SELECT * FROM vls_scene_governance WHERE name = #{name} AND is_deleted = 0")
	SceneGovernance selectByName(@Param("name") String name);

	/**
	 * Query list
	 *
	 * @param status
	 * @return
	 */
	@Select("SELECT * FROM vls_scene_governance WHERE status = #{status} AND is_deleted = 0 ORDER BY created_at DESC")
	List<SceneGovernance> selectByStatus(@Param("status") String status);

	/**
	 * Get
	 *
	 * @return
	 */
	@Select("SELECT COUNT(*) FROM vls_scene_governance WHERE is_deleted = 0")
	Long getTotalCount();

	/**
	 * Get
	 *
	 * @return
	 */
	@Select("SELECT COUNT(*) FROM vls_scene_governance WHERE status = 'enabled' AND is_deleted = 0")
	Long getEnabledCount();

	/**
	 * Get
	 *
	 * @return
	 */
	@Select("SELECT COUNT(*) FROM vls_scene_governance WHERE status = 'disabled' AND is_deleted = 0")
	Long getDisabledCount();

}
