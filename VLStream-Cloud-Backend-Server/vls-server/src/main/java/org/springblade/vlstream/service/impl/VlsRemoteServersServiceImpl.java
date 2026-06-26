package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.excel.VlsRemoteServersExcel;
import org.springblade.vlstream.mapper.VlsRemoteServersMapper;
import org.springblade.vlstream.pojo.entity.RemoteServers;
import org.springblade.vlstream.pojo.vo.RemoteServersVO;
import org.springblade.vlstream.service.IVlsRemoteServersService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Remote Server Configuration Table Service Implementation Class
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
	 * Query Remote Server Configuration
	 *
	 * @param id remote server configuration primary key
	 * @return Remote server configuration
	 */
	@Override
	public RemoteServers selectRemoteServerById(Long id) {
		return remoteServerMapper.selectRemoteServerById(id);
	}

	/**
	 * Query Remote Server Configuration List
	 *
	 * @param remoteServer remote server configuration
	 * @return Remote server configuration
	 */
	@Override
	public List<RemoteServers> selectRemoteServerList(RemoteServers remoteServer) {
		return remoteServerMapper.selectRemoteServerList(remoteServer);
	}

	/**
	 * Add remote server configuration
	 *
	 * @param remoteServer remote server configuration
	 * @return result
	 */
	@Override
	public int insertRemoteServer(RemoteServers remoteServer) {
		return remoteServerMapper.insertRemoteServer(remoteServer);
	}

	/**
	 * Modify remote server configuration
	 *
	 * @param remoteServer remote server configuration
	 * @return result
	 */
	@Override
	public int updateRemoteServer(RemoteServers remoteServer) {
		return remoteServerMapper.updateRemoteServer(remoteServer);
	}

	/**
	 * Batch delete remote server configurations
	 *
	 * @param ids primary keys of remote server configurations to be deleted
	 * @return result
	 */
	@Override
	public int deleteRemoteServerByIds(Long[] ids) {
		return remoteServerMapper.deleteRemoteServerByIds(ids);
	}

	/**
	 * Delete remote server configuration info
	 *
	 * @param id remote server configuration primary key
	 * @return result
	 */
	@Override
	public int deleteRemoteServerById(Long id) {
		return remoteServerMapper.deleteRemoteServerById(id);
	}

}
