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
 * 远程服务器配置表 服务实现类
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
	 * 查询远程服务器配置
	 *
	 * @param id 远程服务器配置主键
	 * @return 远程服务器配置
	 */
	@Override
	public RemoteServers selectRemoteServerById(Long id) {
		return remoteServerMapper.selectRemoteServerById(id);
	}

	/**
	 * 查询远程服务器配置列表
	 *
	 * @param remoteServer 远程服务器配置
	 * @return 远程服务器配置
	 */
	@Override
	public List<RemoteServers> selectRemoteServerList(RemoteServers remoteServer) {
		return remoteServerMapper.selectRemoteServerList(remoteServer);
	}

	/**
	 * 新增远程服务器配置
	 *
	 * @param remoteServer 远程服务器配置
	 * @return 结果
	 */
	@Override
	public int insertRemoteServer(RemoteServers remoteServer) {
		return remoteServerMapper.insertRemoteServer(remoteServer);
	}

	/**
	 * 修改远程服务器配置
	 *
	 * @param remoteServer 远程服务器配置
	 * @return 结果
	 */
	@Override
	public int updateRemoteServer(RemoteServers remoteServer) {
		return remoteServerMapper.updateRemoteServer(remoteServer);
	}

	/**
	 * 批量删除远程服务器配置
	 *
	 * @param ids 需要删除的远程服务器配置主键
	 * @return 结果
	 */
	@Override
	public int deleteRemoteServerByIds(Long[] ids) {
		return remoteServerMapper.deleteRemoteServerByIds(ids);
	}

	/**
	 * 删除远程服务器配置信息
	 *
	 * @param id 远程服务器配置主键
	 * @return 结果
	 */
	@Override
	public int deleteRemoteServerById(Long id) {
		return remoteServerMapper.deleteRemoteServerById(id);
	}

}
