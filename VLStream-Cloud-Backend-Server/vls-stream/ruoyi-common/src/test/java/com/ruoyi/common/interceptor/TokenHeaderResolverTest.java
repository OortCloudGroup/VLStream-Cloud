package com.ruoyi.common.interceptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("dev")
class TokenHeaderResolverTest {

    @Test
    void resolvesAuthorizationBeforeBladeAuthAndAccessToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer authorization-token");
        request.addHeader("blade-auth", "Bearer blade-token");
        request.addHeader("accesstoken", "access-token");

        assertEquals("authorization-token", TokenHeaderResolver.resolve(request));
    }

    @Test
    void fallsBackToBladeAuthThenAccessToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("blade-auth", "Bearer blade-token");
        request.addHeader("accesstoken", "access-token");

        assertEquals("blade-token", TokenHeaderResolver.resolve(request));
    }

    @Test
    void normalizesBearerPrefixCaseInsensitively() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "bearer MixedCaseToken");

        assertEquals("MixedCaseToken", TokenHeaderResolver.resolve(request));
    }

    @Test
    void normalizesMergedDuplicateAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer merged-token, Bearer merged-token");

        assertEquals("merged-token", TokenHeaderResolver.resolve(request));
    }

    @Test
    void requestWrapperExposesSingleBearerAuthorizationForMergedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer merged-token, Bearer merged-token");

        TokenHeaderRequestWrapper wrapped = new TokenHeaderRequestWrapper(request);

        assertEquals("Bearer merged-token", wrapped.getHeader("Authorization"));
        assertEquals("Bearer merged-token", wrapped.getHeader("authorization"));
        assertEquals("merged-token", wrapped.getHeader("AccessToken"));
    }

    @Test
    void normalizesRawAuthorizationHeaderForControllerUse() {
        assertEquals("controller-token", TokenHeaderResolver.normalize("Bearer controller-token"));
    }

    @Test
    void returnsNullWhenNoTokenHeaderExists() {
        assertNull(TokenHeaderResolver.resolve(new MockHttpServletRequest()));
    }

    @Test
    void ignoresBasicClientAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic c2FiZXI6c2FiZXJfc2VjcmV0");

        assertNull(TokenHeaderResolver.resolve(request));
    }
}
