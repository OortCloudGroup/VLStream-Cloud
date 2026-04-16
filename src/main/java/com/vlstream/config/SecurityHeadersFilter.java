package com.vlstream.config;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Security Headers Filter
 * Adds CORS headers required for SharedArrayBuffer
 */
@Component
@Order(1)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Add CORS headers required for SharedArrayBuffer
        httpResponse.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
        httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        
        // Continue filter chain
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization method
    }

    @Override
    public void destroy() {
        // Destroy method
    }
} 