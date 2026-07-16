/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;


/**
 * 远程服务器配置表 实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName(value = "vls_remote_servers", excludeProperty = {
	"tenantId", "createUser", "createDept", "updateUser", "isDeleted"
})
@Schema(description = "VlsRemoteServersEntity对象")
@EqualsAndHashCode(callSuper = true)
public class RemoteServers extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 服务器名称
	 */
	@Schema(description = "服务器名称")
	private String serverName;
	/**
	 * 服务器IP地址
	 */
	@Schema(description = "服务器IP地址")
	private String serverIp;
	/**
	 * SSH端口
	 */
	@Schema(description = "SSH端口")
	private Integer serverPort;
	/**
	 * 用户名
	 */
	@Schema(description = "用户名")
	private String username;
	/**
	 * 密码(加密)
	 */
	@Schema(description = "密码(加密)")
	private String password;
	/**
	 * Conda环境名称
	 */
	@Schema(description = "Conda环境名称")
	private String condaEnv;
	/**
	 * 工作目录
	 */
	@Schema(description = "工作目录")
	private String workDir;

}
