package org.springblade.core.secure;

import lombok.Data;

import java.io.Serializable;

/**
 * Export-parameter compatibility object; it does not participate in authentication.
 */
@Data
public class BladeUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
}
