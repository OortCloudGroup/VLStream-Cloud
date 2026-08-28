/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package org.springblade.core.log.exception;

/**
 * Business exception compatibility type handled by RuoYi's global exception flow.
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a business exception with a user-facing reason.
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Creates a business exception retaining its original cause.
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
