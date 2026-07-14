/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.interceptor;

import javax.servlet.http.HttpServletRequest;

/**
 * Resolves the frontend-compatible token header used by VLStream and workflows callers.
 */
public final class TokenHeaderResolver {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BLADE_AUTH = "blade-auth";
    private static final String ACCESS_TOKEN = "accesstoken";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BASIC_PREFIX = "Basic ";

    private TokenHeaderResolver() {
    }

    /**
     * Resolve a token from Authorization, blade-auth, or accesstoken in compatibility priority order.
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return firstNonBlank(
            normalize(request.getHeader(AUTHORIZATION)),
            normalize(request.getHeader(BLADE_AUTH)),
            normalize(request.getHeader(ACCESS_TOKEN))
        );
    }

    /**
     * Strip supported auth prefixes and reject non-user client authorization headers.
     */
    public static String normalize(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        String token = rawToken.trim();
        if (token.isEmpty()) {
            return null;
        }
        int commaIndex = token.indexOf(',');
        if (commaIndex >= 0) {
            token = token.substring(0, commaIndex).trim();
            if (token.isEmpty()) {
                return null;
            }
        }
        if (startsWithIgnoreCase(token, BASIC_PREFIX)) {
            return null;
        }
        if (startsWithIgnoreCase(token, BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length()).trim();
        }
        return token.isEmpty() ? null : token;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.length() >= prefix.length()
            && value.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
