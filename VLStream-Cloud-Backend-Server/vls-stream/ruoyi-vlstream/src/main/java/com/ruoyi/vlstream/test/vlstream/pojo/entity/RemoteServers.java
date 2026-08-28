/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;


/**
 * service configuration
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
	 * service
	 */
	@Schema(description = "服务器名称")
	private String serverName;
	/**
	 * service IP
	 */
	@Schema(description = "服务器IP地址")
	private String serverIp;
	/**
	 * SSH
	 */
	@Schema(description = "SSH端口")
	private Integer serverPort;
	/**
	 * user
	 */
	@Schema(description = "用户名")
	private String username;
	/**
	 * ( )
	 */
	@Schema(description = "密码(加密)")
	private String password;
	/**
	 * Conda
	 */
	@Schema(description = "Conda环境名称")
	private String condaEnv;
	/**
	 *
	 */
	@Schema(description = "工作目录")
	private String workDir;

}
