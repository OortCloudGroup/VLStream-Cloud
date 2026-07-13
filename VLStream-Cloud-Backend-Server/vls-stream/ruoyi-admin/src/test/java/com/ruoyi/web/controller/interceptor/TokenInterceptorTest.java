/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.interceptor;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.enums.TenantType;
import com.ruoyi.system.service.ISysUserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Tag("dev")
class TokenInterceptorTest {

    @Test
    void allowsLocallyCachedWorkflowTokenBeforeExternalSsoVerificationInMultiTenantMode() throws Exception {
        SysUser cachedUser = new SysUser();
        cachedUser.setUserId("1");
        cachedUser.setTenantId("000000");
        cachedUser.setUserName("admin");
        TokenInterceptor interceptor = new LocalTokenInterceptor(cachedUser);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/workflow/task/todoPage");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer local-sa-token");

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
    }

    private static class LocalTokenInterceptor extends TokenInterceptor {

        private final SysUser cachedUser;

        LocalTokenInterceptor(SysUser cachedUser) {
            super(mock(ISysUserService.class), "http://127.0.0.1:1/verify", "http://127.0.0.1:1/single",
                TenantType.MULTI_TENANT.getType(), "000000");
            this.cachedUser = cachedUser;
        }

        @Override
        protected SysUser getCachedUser(String accessToken) {
            return "local-sa-token".equals(accessToken) ? cachedUser : null;
        }
    }
}
