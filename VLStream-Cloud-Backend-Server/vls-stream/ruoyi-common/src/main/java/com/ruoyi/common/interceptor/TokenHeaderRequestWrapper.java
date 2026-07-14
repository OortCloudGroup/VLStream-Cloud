/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Exposes normalized token headers for downstream auth libraries.
 */
public class TokenHeaderRequestWrapper extends HttpServletRequestWrapper {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BLADE_AUTH = "blade-auth";
    private static final String ACCESS_TOKEN = "accesstoken";
    private static final String BEARER_PREFIX = "Bearer ";

    private final String normalizedToken;

    public TokenHeaderRequestWrapper(HttpServletRequest request) {
        super(request);
        this.normalizedToken = TokenHeaderResolver.resolve(request);
    }

    @Override
    public String getHeader(String name) {
        if (normalizedToken != null && isTokenHeader(name)) {
            return isAccessTokenHeader(name) ? normalizedToken : BEARER_PREFIX + normalizedToken;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (normalizedToken != null && isTokenHeader(name)) {
            return Collections.enumeration(Collections.singleton(getHeader(name)));
        }
        return super.getHeaders(name);
    }

    private static boolean isTokenHeader(String name) {
        return equalsIgnoreCase(name, AUTHORIZATION)
            || equalsIgnoreCase(name, BLADE_AUTH)
            || isAccessTokenHeader(name);
    }

    private static boolean isAccessTokenHeader(String name) {
        return equalsIgnoreCase(name, ACCESS_TOKEN);
    }

    private static boolean equalsIgnoreCase(String value, String expected) {
        return value != null && value.equalsIgnoreCase(expected);
    }
}
