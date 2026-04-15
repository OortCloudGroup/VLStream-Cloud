package com.vlstream.service;

import com.vlstream.entity.RemoteServer;

import java.util.List;

/**
 * 远程服务器配置Service接口
 */
public interface RemoteServerService {
    
    /**
     * 查询远程服务器配置
     * 
     * @param id 远程服务器配置主键
     * @return 远程服务器配置
     */
    public RemoteServer selectRemoteServerById(Long id);
    
    /**
     * 查询远程服务器配置列表
     * 
     * @param remoteServer 远程服务器配置
     * @return 远程服务器配置集合
     */
    public List<RemoteServer> selectRemoteServerList(RemoteServer remoteServer);
    
    /**
     * 新增远程服务器配置
     * 
     * @param remoteServer 远程服务器配置
     * @return 结果
     */
    public int insertRemoteServer(RemoteServer remoteServer);
    
    /**
     * 修改远程服务器配置
     * 
     * @param remoteServer 远程服务器配置
     * @return 结果
     */
    public int updateRemoteServer(RemoteServer remoteServer);
    
    /**
     * 批量删除远程服务器配置
     * 
     * @param ids 需要删除的远程服务器配置主键集合
     * @return 结果
     */
    public int deleteRemoteServerByIds(Long[] ids);
    
    /**
     * 删除远程服务器配置信息
     * 
     * @param id 远程服务器配置主键
     * @return 结果
     */
    public int deleteRemoteServerById(Long id);
} 