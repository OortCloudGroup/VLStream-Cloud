package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.vlstream.excel.VlsRemoteServersExcel;
import org.springblade.vlstream.pojo.entity.RemoteServers;
import org.springblade.vlstream.pojo.vo.RemoteServersVO;

import java.util.List;

/**
 * Remote Server Configuration Table Mapper Interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsRemoteServersMapper extends BaseMapper<RemoteServers> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsRemoteServers query parameters
	 * @return List<VlsRemoteServersVO>
	 */
	List<RemoteServersVO> selectVlsRemoteServersPage(IPage page, RemoteServersVO vlsRemoteServers);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsRemoteServersExcel>
	 */
	List<VlsRemoteServersExcel> exportVlsRemoteServers(@Param("ew") Wrapper<RemoteServers> queryWrapper);

	/**
	 * Query Remote Server Configuration List
	 */
	List<RemoteServers> selectRemoteServerList(RemoteServers remoteServer);

	/**
	 * Query Remote Server Configuration Details
	 */
	RemoteServers selectRemoteServerById(Long id);

	/**
	 * Add remote server configuration
	 */
	int insertRemoteServer(RemoteServers remoteServer);

	/**
	 * Modify remote server configuration
	 */
	int updateRemoteServer(RemoteServers remoteServer);

	/**
	 * Delete remote server configuration
	 */
	int deleteRemoteServerById(Long id);

	/**
	 * Batch delete remote server configurations
	 */
	int deleteRemoteServerByIds(Long[] ids);

	/**
	 * Query Enabled Server Configuration
	 */
	RemoteServers selectActiveServer();

	/**
	 * Count number of servers
	 */
	int count();

	/**
	 * Create table (if not exists)
	 */
	void createTableIfNotExists();

}
