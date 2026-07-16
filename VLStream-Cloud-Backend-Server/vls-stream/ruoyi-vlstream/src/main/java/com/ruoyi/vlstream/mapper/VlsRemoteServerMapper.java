/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.RemoteServer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mapper for remote GPU training servers.
 */
@Mapper
public interface VlsRemoteServerMapper extends BaseMapperPlus<VlsRemoteServerMapper, RemoteServer, RemoteServer> {

    @Select("SELECT id, server_name, server_ip, server_port, username, password, conda_env, work_dir, status, create_time, update_time " +
        "FROM vls_remote_servers WHERE status = 1 ORDER BY create_time DESC LIMIT 1")
    RemoteServer selectActiveServer();

    @Update("CREATE TABLE IF NOT EXISTS `vls_remote_servers` (" +
        "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary key ID'," +
        "`server_name` varchar(100) NOT NULL COMMENT 'Server Name'," +
        "`server_ip` varchar(50) NOT NULL COMMENT 'Server IP Address'," +
        "`server_port` int(11) NOT NULL DEFAULT '22' COMMENT 'SSH port'," +
        "`username` varchar(50) NOT NULL COMMENT 'Username'," +
        "`password` varchar(255) NOT NULL COMMENT 'Password (encrypted)'," +
        "`conda_env` varchar(50) DEFAULT 'yolo8' COMMENT 'Conda environment name'," +
        "`work_dir` varchar(255) DEFAULT '/data/work/ultralytics_yolov8-main' COMMENT 'Working directory'," +
        "`status` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Status (0: Disabled, 1: Enabled)'," +
        "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time'," +
        "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time'," +
        "PRIMARY KEY (`id`), KEY `idx_server_ip` (`server_ip`), KEY `idx_status` (`status`)" +
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Remote Server Configuration Table'")
    void createTableIfNotExists();
}
