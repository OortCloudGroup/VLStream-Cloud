package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * Remote Server Configuration Table Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_remote_servers")
@Schema(description = "VlsRemoteServersEntity object")
@EqualsAndHashCode(callSuper = true)
public class RemoteServers extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Server Name
	 */
	@Schema(description = "Server Name")
	private String serverName;
	/**
	 * Server IP Address
	 */
	@Schema(description = "Server IP Address")
	private String serverIp;
	/**
	 * SSH port
	 */
	@Schema(description = "SSH port")
	private Integer serverPort;
	/**
	 * Username
	 */
	@Schema(description = "Username")
	private String username;
	/**
	 * Password (encrypted)
	 */
	@Schema(description = "Password (encrypted)")
	private String password;
	/**
	 * Conda environment name
	 */
	@Schema(description = "Conda environment name")
	private String condaEnv;
	/**
	 * Working directory
	 */
	@Schema(description = "Working directory")
	private String workDir;

}
