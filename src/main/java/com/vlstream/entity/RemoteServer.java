package com.vlstream.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 远程服务器配置实体类
 */
@Data
public class RemoteServer {
    
    private Long id;
    
    /**
     * 服务器名称
     */
    private String serverName;
    
    /**
     * 服务器IP地址
     */
    private String serverIp;
    
    /**
     * SSH端口
     */
    private Integer serverPort;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码(加密存储)
     */
    private String password;
    
    /**
     * Conda环境名称
     */
    private String condaEnv;
    
    /**
     * 工作目录
     */
    private String workDir;
    
    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
} 