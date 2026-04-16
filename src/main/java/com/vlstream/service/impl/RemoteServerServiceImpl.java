package com.vlstream.service.impl;

import com.vlstream.entity.RemoteServer;
import com.vlstream.mapper.RemoteServerMapper;
import com.vlstream.service.RemoteServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Remote Server Configuration Service Implementation Class
 */
@Service
public class RemoteServerServiceImpl implements RemoteServerService {
    
    @Autowired
    private RemoteServerMapper remoteServerMapper;
    
    /**
     * Query remote server configuration
     * 
     * @param id Remote server configuration ID
     * @return Remote server configuration
     */
    @Override
    public RemoteServer selectRemoteServerById(Long id) {
        return remoteServerMapper.selectRemoteServerById(id);
    }
    
    /**
     * Query remote server configuration list
     * 
     * @param remoteServer Remote server configuration
     * @return Remote server configuration list
     */
    @Override
    public List<RemoteServer> selectRemoteServerList(RemoteServer remoteServer) {
        return remoteServerMapper.selectRemoteServerList(remoteServer);
    }
    
    /**
     * Add remote server configuration
     * 
     * @param remoteServer Remote server configuration
     * @return Result
     */
    @Override
    public int insertRemoteServer(RemoteServer remoteServer) {
        return remoteServerMapper.insertRemoteServer(remoteServer);
    }
    
    /**
     * Update remote server configuration
     * 
     * @param remoteServer Remote server configuration
     * @return Result
     */
    @Override
    public int updateRemoteServer(RemoteServer remoteServer) {
        return remoteServerMapper.updateRemoteServer(remoteServer);
    }
    
    /**
     * Batch delete remote server configurations
     * 
     * @param ids Remote server configuration IDs to delete
     * @return Result
     */
    @Override
    public int deleteRemoteServerByIds(Long[] ids) {
        return remoteServerMapper.deleteRemoteServerByIds(ids);
    }
    
    /**
     * Delete remote server configuration
     * 
     * @param id Remote server configuration ID
     * @return Result
     */
    @Override
    public int deleteRemoteServerById(Long id) {
        return remoteServerMapper.deleteRemoteServerById(id);
    }
} 