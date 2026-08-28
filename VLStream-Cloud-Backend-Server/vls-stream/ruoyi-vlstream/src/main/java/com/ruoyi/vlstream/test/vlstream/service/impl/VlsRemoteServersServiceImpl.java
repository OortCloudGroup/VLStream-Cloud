/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import javax.annotation.Resource;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.excel.VlsRemoteServersExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsRemoteServersMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.RemoteServersVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsRemoteServersService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * service configuration service
 *
 * @author Oort
 * @since 2025-12-23
 */
@Service
public class VlsRemoteServersServiceImpl extends BaseServiceImpl<VlsRemoteServersMapper, RemoteServers> implements IVlsRemoteServersService {

	@Resource
	private VlsRemoteServersMapper remoteServerMapper;

	@Override
	public IPage<RemoteServersVO> selectVlsRemoteServersPage(IPage<RemoteServersVO> page, RemoteServersVO vlsRemoteServers) {
		return page.setRecords(baseMapper.selectVlsRemoteServersPage(page, vlsRemoteServers));
	}

	@Override
	public List<VlsRemoteServersExcel> exportVlsRemoteServers(Wrapper<RemoteServers> queryWrapper) {
		List<VlsRemoteServersExcel> vlsRemoteServersList = baseMapper.exportVlsRemoteServers(queryWrapper);
		//vlsRemoteServersList.forEach(vlsRemoteServers -> {
		//	vlsRemoteServers.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsRemoteServersEntity.getType()));
		//});
		return vlsRemoteServersList;
	}

	/**
	 * Query service configuration
	 *
	 * @param id service configurationprimary key
	 * @return service configuration
	 */
	@Override
	public RemoteServers selectRemoteServerById(Long id) {
		return remoteServerMapper.selectRemoteServerById(id);
	}

	/**
	 * Query service configuration list
	 *
	 * @param remoteServer service configuration
	 * @return service configuration
	 */
	@Override
	public List<RemoteServers> selectRemoteServerList(RemoteServers remoteServer) {
		return remoteServerMapper.selectRemoteServerList(remoteServer);
	}

	/**
	 * Add service configuration
	 *
	 * @param remoteServer service configuration
	 * @return
	 */
	@Override
	public int insertRemoteServer(RemoteServers remoteServer) {
		return save(remoteServer) ? 1 : 0;
	}

	/**
	 * Update service configuration
	 *
	 * @param remoteServer service configuration
	 * @return
	 */
	@Override
	public int updateRemoteServer(RemoteServers remoteServer) {
		return updateById(remoteServer) ? 1 : 0;
	}

	/**
	 * Batch delete service configuration
	 *
	 * @param ids need to Delete service configurationprimary key
	 * @return
	 */
	@Override
	public int deleteRemoteServerByIds(Long[] ids) {
		return remoteServerMapper.deleteRemoteServerByIds(ids);
	}

	/**
	 * Delete service configurationinfo
	 *
	 * @param id service configurationprimary key
	 * @return
	 */
	@Override
	public int deleteRemoteServerById(Long id) {
		return remoteServerMapper.deleteRemoteServerById(id);
	}

}
