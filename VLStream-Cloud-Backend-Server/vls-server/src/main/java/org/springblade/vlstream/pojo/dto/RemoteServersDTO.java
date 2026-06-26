package org.springblade.vlstream.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.RemoteServers;

import java.io.Serial;

/**
 * Remote Server Configuration Table Data Transfer Object Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RemoteServersDTO extends RemoteServers {
	@Serial
	private static final long serialVersionUID = 1L;

}
