/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

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
