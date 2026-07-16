/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vlstream.test.vlstream.excel.VlsRemoteServersExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.RemoteServersVO;

import java.util.List;

/**
 * 远程服务器配置表 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsRemoteServersMapper extends BaseMapper<RemoteServers> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsRemoteServers 查询参数
	 * @return List<VlsRemoteServersVO>
	 */
	List<RemoteServersVO> selectVlsRemoteServersPage(IPage page, RemoteServersVO vlsRemoteServers);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsRemoteServersExcel>
	 */
	List<VlsRemoteServersExcel> exportVlsRemoteServers(@Param("ew") Wrapper<RemoteServers> queryWrapper);

	/**
	 * 查询远程服务器配置列表
	 */
	List<RemoteServers> selectRemoteServerList(RemoteServers remoteServer);

	/**
	 * 查询远程服务器配置详细
	 */
	RemoteServers selectRemoteServerById(Long id);

	/**
	 * 新增远程服务器配置
	 */
	int insertRemoteServer(RemoteServers remoteServer);

	/**
	 * 修改远程服务器配置
	 */
	int updateRemoteServer(RemoteServers remoteServer);

	/**
	 * 删除远程服务器配置
	 */
	int deleteRemoteServerById(Long id);

	/**
	 * 批量删除远程服务器配置
	 */
	int deleteRemoteServerByIds(Long[] ids);

	/**
	 * 查询启用的服务器配置
	 */
	RemoteServers selectActiveServer();

	/**
	 * 统计服务器数量
	 */
	int count();

	/**
	 * 创建表（如果不存在）
	 */
	void createTableIfNotExists();

}
