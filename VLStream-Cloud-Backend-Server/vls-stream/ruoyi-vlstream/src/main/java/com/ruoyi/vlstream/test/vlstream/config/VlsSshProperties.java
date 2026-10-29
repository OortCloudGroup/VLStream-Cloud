/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SSH defaults for the remote GPU training server.
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.ssh")
public class VlsSshProperties {

    private String host = "192.168.88.173";

    private Integer port = 22;

    private String username = "oort";

    private String password;
    /** Dedicated legacy executor identity. Separate from configured shared GPU node IDs. */
    private Long serverId = -1L;
    private java.util.List<Long> legacyServerIds = new java.util.ArrayList<>();
    private String condaEnv = "yolo8";
    private String workDir = "/data/work/ultralytics_yolov8-main/datasets";

    /** Never reads or populates vls_remote_servers. */
    public com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers requireServer() {
        if(serverId==null || serverId!=-1L || legacyServerIds==null
            || legacyServerIds.stream().anyMatch(id -> id==null || id<=0)
            || new java.util.HashSet<>(legacyServerIds).size()!=legacyServerIds.size()
            || host==null || !host.matches("[a-zA-Z0-9][a-zA-Z0-9.-]{0,252}")
            || port==null || port<1 || port>65535 || username==null || !username.matches("[a-zA-Z_][a-zA-Z0-9_-]{0,63}")
            || password==null || password.isEmpty() || condaEnv==null || !condaEnv.matches("[a-zA-Z0-9_-]+"))
            throw new com.ruoyi.common.exception.ServiceException("旧远程执行器 YAML 的身份、认证或 Python 环境未正确配置");
        com.ruoyi.vlstream.test.vlstream.compute.ComputeNodeService.validatePath(workDir);
        if("/".equals(workDir))throw new com.ruoyi.common.exception.ServiceException("远程执行器须使用专用工作目录");
        com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers server=new com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers();
        server.setId(serverId);server.setServerName("YAML 远程执行器");server.setServerIp(host);server.setServerPort(port);
        server.setUsername(username);server.setCondaEnv(condaEnv);server.setWorkDir(workDir);server.setStatus(1);
        return server;
    }

    public void requireReference(com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance instance) {
        com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers server=requireServer();
        if(instance==null || !(serverId.equals(instance.getServerId()) || legacyServerIds.contains(instance.getServerId()))
            || !java.util.Objects.equals(server.getServerIp(),instance.getServerIp()))
            throw new com.ruoyi.common.exception.ServiceException("旧任务执行器尚未映射到 YAML，保留原任务等待管理员核对");
        if(serverId.equals(instance.getServerId())) {
            try {
                com.fasterxml.jackson.databind.JsonNode config=new com.fasterxml.jackson.databind.ObjectMapper().readTree(instance.getEnvConfig());
                if(config==null || !executionFingerprint().equals(config.path("executionFingerprint").asText()))throw new IllegalArgumentException();
            } catch(Exception invalid) {throw new com.ruoyi.common.exception.ServiceException("任务冻结执行目标与 YAML 不一致，保留任务等待核对");}
        }
    }

    public String executionFingerprint() {
        requireServer();
        return com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore.hash(serverId+"\n"+host+"\n"+port+"\n"+username+"\n"+condaEnv+"\n"+workDir);
    }
}
