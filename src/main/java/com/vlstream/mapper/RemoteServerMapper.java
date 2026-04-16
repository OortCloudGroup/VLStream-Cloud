package com.vlstream.mapper;

import com.vlstream.entity.RemoteServer;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Remote Server Configuration Mapper Interface
 */
@Mapper
public interface RemoteServerMapper {
    
    /**
     * Query remote server configuration list
     */
    List<RemoteServer> selectRemoteServerList(RemoteServer remoteServer);
    
    /**
     * Query remote server configuration details
     */
    RemoteServer selectRemoteServerById(Long id);
    
    /**
     * Add new remote server configuration
     */
    int insertRemoteServer(RemoteServer remoteServer);
    
    /**
     * Modify remote server configuration
     */
    int updateRemoteServer(RemoteServer remoteServer);
    
    /**
     * Delete remote server configuration
     */
    int deleteRemoteServerById(Long id);
    
    /**
     * Batch delete remote server configurations
     */
    int deleteRemoteServerByIds(Long[] ids);
    
    /**
     * Query enabled server configuration
     */
    RemoteServer selectActiveServer();
    
    /**
     * Count servers
     */
    int count();

    /**
     * Create table (if not exists)
     */
    void createTableIfNotExists();
} 