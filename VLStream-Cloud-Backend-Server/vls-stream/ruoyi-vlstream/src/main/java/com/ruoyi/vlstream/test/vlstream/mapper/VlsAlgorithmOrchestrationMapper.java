/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmOrchestrationExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmOrchestration;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmOrchestrationVO;

import java.util.List;

/**
 * 算法编排表 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmOrchestrationMapper extends BaseMapper<AlgorithmOrchestration> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAlgorithmOrchestration 查询参数
	 * @return List<VlsAlgorithmOrchestrationVO>
	 */
	List<AlgorithmOrchestrationVO> selectVlsAlgorithmOrchestrationPage(IPage page, AlgorithmOrchestrationVO vlsAlgorithmOrchestration);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAlgorithmOrchestrationExcel>
	 */
	List<VlsAlgorithmOrchestrationExcel> exportVlsAlgorithmOrchestration(@Param("ew") Wrapper<AlgorithmOrchestration> queryWrapper);

}
