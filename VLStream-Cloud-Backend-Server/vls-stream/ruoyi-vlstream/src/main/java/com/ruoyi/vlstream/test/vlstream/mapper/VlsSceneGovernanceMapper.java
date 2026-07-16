/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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
 * 场景治理表 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsSceneGovernanceMapper extends BaseMapper<SceneGovernance> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsSceneGovernance 查询参数
	 * @return List<VlsSceneGovernanceVO>
	 */
	List<SceneGovernanceVO> selectVlsSceneGovernancePage(IPage page, SceneGovernanceVO vlsSceneGovernance);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsSceneGovernanceExcel>
	 */
	List<VlsSceneGovernanceExcel> exportVlsSceneGovernance(@Param("ew") Wrapper<SceneGovernance> queryWrapper);

	/**
	 * 根据名称查询场景治理信息
	 *
	 * @param name 场景名称
	 * @return 场景治理信息
	 */
	@Select("SELECT * FROM vls_scene_governance WHERE name = #{name} AND is_deleted = 0")
	SceneGovernance selectByName(@Param("name") String name);

	/**
	 * 根据状态查询场景治理列表
	 *
	 * @param status 场景状态
	 * @return 场景治理列表
	 */
	@Select("SELECT * FROM vls_scene_governance WHERE status = #{status} AND is_deleted = 0 ORDER BY created_at DESC")
	List<SceneGovernance> selectByStatus(@Param("status") String status);

	/**
	 * 获取场景治理总数
	 *
	 * @return 总数
	 */
	@Select("SELECT COUNT(*) FROM vls_scene_governance WHERE is_deleted = 0")
	Long getTotalCount();

	/**
	 * 获取启用的场景治理数量
	 *
	 * @return 启用数量
	 */
	@Select("SELECT COUNT(*) FROM vls_scene_governance WHERE status = 'enabled' AND is_deleted = 0")
	Long getEnabledCount();

	/**
	 * 获取禁用的场景治理数量
	 *
	 * @return 禁用数量
	 */
	@Select("SELECT COUNT(*) FROM vls_scene_governance WHERE status = 'disabled' AND is_deleted = 0")
	Long getDisabledCount();

}
