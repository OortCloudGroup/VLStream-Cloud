package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsRemoteServersExcel;
import org.springblade.vlstream.pojo.entity.RemoteServers;
import org.springblade.vlstream.pojo.vo.RemoteServersVO;

import java.util.List;

/**
 * Remote Server Configuration Table Service Class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsRemoteServersService extends BaseService<RemoteServers> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsRemoteServers query parameters
	 * @return IPage<VlsRemoteServersVO>
	 */
	IPage<RemoteServersVO> selectVlsRemoteServersPage(IPage<RemoteServersVO> page, RemoteServersVO vlsRemoteServers);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsRemoteServersExcel>
	 */
	List<VlsRemoteServersExcel> exportVlsRemoteServers(Wrapper<RemoteServers> queryWrapper);

	/**
	 * Query Remote Server Configuration
	 *
	 * @param id remote server configuration primary key
	 * @return Remote server configuration
	 */
	public RemoteServers selectRemoteServerById(Long id);

	/**
	 * Query Remote Server Configuration List
	 *
	 * @param remoteServer remote server configuration
	 * @return Collection of remote server configurations
	 */
	public List<RemoteServers> selectRemoteServerList(RemoteServers remoteServer);

	/**
	 * Add remote server configuration
	 *
	 * @param remoteServer remote server configuration
	 * @return result
	 */
	public int insertRemoteServer(RemoteServers remoteServer);

	/**
	 * Modify remote server configuration
	 *
	 * @param remoteServer remote server configuration
	 * @return result
	 */
	public int updateRemoteServer(RemoteServers remoteServer);

	/**
	 * Batch delete remote server configurations
	 *
	 * @param ids set of primary keys of remote server configurations to be deleted
	 * @return result
	 */
	public int deleteRemoteServerByIds(Long[] ids);

	/**
	 * Delete remote server configuration info
	 *
	 * @param id remote server configuration primary key
	 * @return result
	 */
	public int deleteRemoteServerById(Long id);

}
