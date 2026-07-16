/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsRemoteServersExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.RemoteServersVO;

import java.util.List;

/**
 * 远程服务器配置表 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsRemoteServersService extends BaseService<RemoteServers> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsRemoteServers 查询参数
	 * @return IPage<VlsRemoteServersVO>
	 */
	IPage<RemoteServersVO> selectVlsRemoteServersPage(IPage<RemoteServersVO> page, RemoteServersVO vlsRemoteServers);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsRemoteServersExcel>
	 */
	List<VlsRemoteServersExcel> exportVlsRemoteServers(Wrapper<RemoteServers> queryWrapper);

	/**
	 * 查询远程服务器配置
	 *
	 * @param id 远程服务器配置主键
	 * @return 远程服务器配置
	 */
	public RemoteServers selectRemoteServerById(Long id);

	/**
	 * 查询远程服务器配置列表
	 *
	 * @param remoteServer 远程服务器配置
	 * @return 远程服务器配置集合
	 */
	public List<RemoteServers> selectRemoteServerList(RemoteServers remoteServer);

	/**
	 * 新增远程服务器配置
	 *
	 * @param remoteServer 远程服务器配置
	 * @return 结果
	 */
	public int insertRemoteServer(RemoteServers remoteServer);

	/**
	 * 修改远程服务器配置
	 *
	 * @param remoteServer 远程服务器配置
	 * @return 结果
	 */
	public int updateRemoteServer(RemoteServers remoteServer);

	/**
	 * 批量删除远程服务器配置
	 *
	 * @param ids 需要删除的远程服务器配置主键集合
	 * @return 结果
	 */
	public int deleteRemoteServerByIds(Long[] ids);

	/**
	 * 删除远程服务器配置信息
	 *
	 * @param id 远程服务器配置主键
	 * @return 结果
	 */
	public int deleteRemoteServerById(Long id);

}
