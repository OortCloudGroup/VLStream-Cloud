/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package org.springblade.core.mp.support;

import lombok.Data;

/**
 * Pagination request compatible with SpringBlade controller parameters.
 */
@Data
public class Query {

    private long current = 1L;

    private long size = 10L;

    private String ascs;

    private String descs;
}
