/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.filter;

import com.ruoyi.common.interceptor.TokenHeaderRequestWrapper;
import com.ruoyi.common.interceptor.TokenHeaderResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Normalizes compatible token headers before MVC interceptors read them.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TokenHeaderNormalizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (TokenHeaderResolver.resolve(request) == null) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(new TokenHeaderRequestWrapper(request), response);
    }
}
