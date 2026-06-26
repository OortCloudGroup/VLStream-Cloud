package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.RemoteServers;

import java.io.Serial;

/**
 * Remote Server Configuration Table View Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RemoteServersVO extends RemoteServers {
	@Serial
	private static final long serialVersionUID = 1L;

}
