package org.springblade.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ssh configuration
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.ssh")
public class VlsSshProperties {

	private String host;

	private Integer port;

	private String username;

	private String password;

}
