package com.vlstream.mapper;

import com.vlstream.entity.RemoteServer;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 远程服务器配置Mapper接口
 */
@Mapper
public interface RemoteServerMapper {
    
    /**
     * 查询远程服务器配置列表
     */
    List<RemoteServer> selectRemoteServerList(RemoteServer remoteServer);
    
    /**
     * 查询远程服务器配置详细
     */
    RemoteServer selectRemoteServerById(Long id);
    
    /**
     * 新增远程服务器配置
     */
    int insertRemoteServer(RemoteServer remoteServer);
    
    /**
     * 修改远程服务器配置
     */
    int updateRemoteServer(RemoteServer remoteServer);
    
    /**
     * 删除远程服务器配置
     */
    int deleteRemoteServerById(Long id);
    
    /**
     * 批量删除远程服务器配置
     */
    int deleteRemoteServerByIds(Long[] ids);
    
    /**
     * 查询启用的服务器配置
     */
    RemoteServer selectActiveServer();
    
    /**
     * 统计服务器数量
     */
    int count();

    /**
     * 创建表（如果不存在）
     */
    void createTableIfNotExists();
} 