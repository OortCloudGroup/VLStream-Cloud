package com.vlstream.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Remote Server Configuration Entity Class
 */
@Data
public class RemoteServer {
    
    private Long id;
    
    /**
     * Server name
     */
    private String serverName;
    
    /**
     * Server IP address
     */
    private String serverIp;
    
    /**
     * SSH port
     */
    private Integer serverPort;
    
    /**
     * Username
     */
    private String username;
    
    /**
     * Password (encrypted storage)
     */
    private String password;
    
    /**
     * Conda environment name
     */
    private String condaEnv;
    
    /**
     * Working directory
     */
    private String workDir;
    
    /**
     * Status: 0-Disabled, 1-Enabled
     */
    private Integer status;
    
    /**
     * Creation time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    
    /**
     * Update time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
} 