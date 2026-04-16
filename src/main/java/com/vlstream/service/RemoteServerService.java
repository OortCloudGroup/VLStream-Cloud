package com.vlstream.service;

import com.vlstream.entity.RemoteServer;

import java.util.List;

/**
 * Remote server configuration service interface
 */
public interface RemoteServerService {
    
    /**
     * Query remote server configuration
     * 
     * @param id Remote server configuration primary key
     * @return Remote server configuration
     */
    public RemoteServer selectRemoteServerById(Long id);
    
    /**
     * Query remote server configuration list
     * 
     * @param remoteServer Remote server configuration
     * @return Remote server configuration collection
     */
    public List<RemoteServer> selectRemoteServerList(RemoteServer remoteServer);
    
    /**
     * Add remote server configuration
     * 
     * @param remoteServer Remote server configuration
     * @return Result
     */
    public int insertRemoteServer(RemoteServer remoteServer);
    
    /**
     * Update remote server configuration
     * 
     * @param remoteServer Remote server configuration
     * @return Result
     */
    public int updateRemoteServer(RemoteServer remoteServer);
    
    /**
     * Batch delete remote server configurations
     * 
     * @param ids Primary key collection of remote server configurations to delete
     * @return Result
     */
    public int deleteRemoteServerByIds(Long[] ids);
    
    /**
     * Delete remote server configuration information
     * 
     * @param id Remote server configuration primary key
     * @return Result
     */
    public int deleteRemoteServerById(Long id);
} 