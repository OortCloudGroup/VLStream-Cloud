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
import com.ruoyi.vlstream.test.vlstream.excel.VlsRemoteServersExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.RemoteServersVO;

import java.util.List;

/**
 * service configuration Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsRemoteServersMapper extends BaseMapper<RemoteServers> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsRemoteServers Query parameter
	 * @return List<VlsRemoteServersVO>
	 */
	List<RemoteServersVO> selectVlsRemoteServersPage(IPage page, RemoteServersVO vlsRemoteServers);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsRemoteServersExcel>
	 */
	List<VlsRemoteServersExcel> exportVlsRemoteServers(@Param("ew") Wrapper<RemoteServers> queryWrapper);

	/**
	 * Query service configuration list
	 */
	List<RemoteServers> selectRemoteServerList(RemoteServers remoteServer);

	/**
	 * Query service configuration
	 */
	RemoteServers selectRemoteServerById(Long id);

	/**
	 * Add service configuration
	 */
	int insertRemoteServer(RemoteServers remoteServer);

	/**
	 * Update service configuration
	 */
	int updateRemoteServer(RemoteServers remoteServer);

	/**
	 * Delete service configuration
	 */
	int deleteRemoteServerById(Long id);

	/**
	 * Batch delete service configuration
	 */
	int deleteRemoteServerByIds(Long[] ids);

	/**
	 * Query service configuration
	 */
	RemoteServers selectActiveServer();

	/**
	 * service
	 */
	int count();

	/**
	 * (if in )
	 */
	void createTableIfNotExists();

}
