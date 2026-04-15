package com.vlstream.service.impl;

import com.vlstream.entity.RemoteServer;
import com.vlstream.mapper.RemoteServerMapper;
import com.vlstream.service.RemoteServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 远程服务器配置Service业务层处理
 */
@Service
public class RemoteServerServiceImpl implements RemoteServerService {
    
    @Autowired
    private RemoteServerMapper remoteServerMapper;
    
    /**
     * 查询远程服务器配置
     * 
     * @param id 远程服务器配置主键
     * @return 远程服务器配置
     */
    @Override
    public RemoteServer selectRemoteServerById(Long id) {
        return remoteServerMapper.selectRemoteServerById(id);
    }
    
    /**
     * 查询远程服务器配置列表
     * 
     * @param remoteServer 远程服务器配置
     * @return 远程服务器配置
     */
    @Override
    public List<RemoteServer> selectRemoteServerList(RemoteServer remoteServer) {
        return remoteServerMapper.selectRemoteServerList(remoteServer);
    }
    
    /**
     * 新增远程服务器配置
     * 
     * @param remoteServer 远程服务器配置
     * @return 结果
     */
    @Override
    public int insertRemoteServer(RemoteServer remoteServer) {
        return remoteServerMapper.insertRemoteServer(remoteServer);
    }
    
    /**
     * 修改远程服务器配置
     * 
     * @param remoteServer 远程服务器配置
     * @return 结果
     */
    @Override
    public int updateRemoteServer(RemoteServer remoteServer) {
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