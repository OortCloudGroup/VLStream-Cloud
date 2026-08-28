/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
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
 * service configuration service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsRemoteServersService extends BaseService<RemoteServers> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsRemoteServers Query parameter
	 * @return IPage<VlsRemoteServersVO>
	 */
	IPage<RemoteServersVO> selectVlsRemoteServersPage(IPage<RemoteServersVO> page, RemoteServersVO vlsRemoteServers);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsRemoteServersExcel>
	 */
	List<VlsRemoteServersExcel> exportVlsRemoteServers(Wrapper<RemoteServers> queryWrapper);

	/**
	 * Query service configuration
	 *
	 * @param id service configurationprimary key
	 * @return service configuration
	 */
	public RemoteServers selectRemoteServerById(Long id);

	/**
	 * Query service configuration list
	 *
	 * @param remoteServer service configuration
	 * @return service configurationcollection
	 */
	public List<RemoteServers> selectRemoteServerList(RemoteServers remoteServer);

	/**
	 * Add service configuration
	 *
	 * @param remoteServer service configuration
	 * @return
	 */
	public int insertRemoteServer(RemoteServers remoteServer);

	/**
	 * Update service configuration
	 *
	 * @param remoteServer service configuration
	 * @return
	 */
	public int updateRemoteServer(RemoteServers remoteServer);

	/**
	 * Batch delete service configuration
	 *
	 * @param ids need to Delete service configurationprimary keycollection
	 * @return
	 */
	public int deleteRemoteServerByIds(Long[] ids);

	/**
	 * Delete service configurationinfo
	 *
	 * @param id service configurationprimary key
	 * @return
	 */
	public int deleteRemoteServerById(Long id);

}
