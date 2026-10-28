package com.ruoyi.web.controller.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.servlet.model.SaRequestForServlet;
import cn.dev33.satoken.servlet.model.SaResponseForServlet;
import cn.dev33.satoken.servlet.model.SaStorageForServlet;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.framework.config.properties.TokenProperties;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("dev")
class LocalEventSessionSecurityTest {
    private SaTokenContext oldContext;
    private SaTokenDao oldDao;
    private StpLogic oldLogic;
    private SaTokenDaoDefaultImpl memory;
    private TokenProperties properties;
    private String token;
    private MockMvc mvc;

    @BeforeEach void setup() {
        oldContext = SaManager.getSaTokenContext(); oldDao = SaManager.getSaTokenDao(); oldLogic = StpUtil.getStpLogic();
        memory = new SaTokenDaoDefaultImpl(); SaManager.setSaTokenDao(memory);
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal()); StpUtil.setStpLogic(new StpLogic("login"));
        context(new MockHttpServletRequest(), new MockHttpServletResponse());
        StpUtil.login("fixture-local-user", new SaLoginModel().setTimeout(-1));
        token = StpUtil.getTokenValue();
        LoginUser user = new LoginUser(); user.setTenantId("tenant-a");
        StpUtil.getTokenSession().set(LoginHelper.LOGIN_USER_KEY, user);
        SaTokenContextForThreadLocalStorage.clearBox();
        properties = new TokenProperties(); properties.setTenantType("multi");
        LocalSessionLifetimeInterceptor lifetime = new LocalSessionLifetimeInterceptor(properties, 3600);
        TenantContextInterceptor tenant = new TenantContextInterceptor(properties);
        SaInterceptor login = new SaInterceptor(handler -> WebMvcConfigToken.requireLoginUnlessPublicRead());
        mvc = MockMvcBuilders.standaloneSetup(new LocalEventController()).setControllerAdvice(new Errors())
            .addInterceptors(new HandlerInterceptor() {
                public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                    context(request, response);
                    return login.preHandle(request, response, handler) && lifetime.preHandle(request, response, handler)
                        && tenant.preHandle(request, response, handler);
                }
                public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception error) {
                    TenantContextHolder.clear(); SaTokenContextForThreadLocalStorage.clearBox();
                }
            }).build();
    }

    @AfterEach void cleanup() {
        TenantContextHolder.clear(); SaTokenContextForThreadLocalStorage.clearBox();
        StpUtil.setStpLogic(oldLogic); SaManager.setSaTokenContext(oldContext); SaManager.setSaTokenDao(oldDao);
        memory.endRefreshThread();
    }

    @Test void localEventReadUsesTrustedLocalTenantWithoutAPlatformCall() throws Exception {
        mvc.perform(get("/vlsEventManagement/page").header(StpUtil.getTokenName(), token).header("tenantid", "tenant-a"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tenant").value("tenant-a"));
    }

    @Test void missingLocalLoginIsRejected() throws Exception {
        mvc.perform(get("/vlsEventManagement/page")).andExpect(jsonPath("$.code").value(401));
    }

    @Test void aForgedTenantHeaderCannotBroadenLocalReads() throws Exception {
        mvc.perform(get("/vlsEventManagement/page").header(StpUtil.getTokenName(), token).header("tenantid", "tenant-b"))
            .andExpect(status().isForbidden());
    }

    @Test void anUnlimitedLegacyTokenStillHasABoundedLocalReadLifetime() throws Exception {
        StpUtil.getStpLogic().getTokenSessionByToken(token, false).setCreateTime(System.currentTimeMillis() - 3600001L);
        mvc.perform(get("/vlsEventManagement/page").header(StpUtil.getTokenName(), token))
            .andExpect(jsonPath("$.code").value(401));
    }

    @Test void aRevokedLocalLoginIsRejectedWithoutRemoteFallback() throws Exception {
        StpUtil.logoutByTokenValue(token);
        mvc.perform(get("/vlsEventManagement/page").header(StpUtil.getTokenName(), token))
            .andExpect(jsonPath("$.code").value(401));
    }

    @Test void singleTenantKeepsItsExistingSessionPolicy() throws Exception {
        properties.setTenantType("single");
        StpUtil.getStpLogic().getTokenSessionByToken(token, false).setCreateTime(System.currentTimeMillis() - 3600001L);
        mvc.perform(get("/vlsEventManagement/page").header(StpUtil.getTokenName(), token))
            .andExpect(jsonPath("$.tenant").value("000000"));
    }

    @Test void missingServerSessionIsRejectedWithoutCreatingAReplacement() throws Exception {
        String id = StpUtil.getStpLogic().getTokenSessionByToken(token, false).getId();
        memory.deleteSession(id);
        mvc.perform(get("/vlsEventManagement/page").header(StpUtil.getTokenName(), token))
            .andExpect(jsonPath("$.code").value(401));
        assertNull(memory.getSession(id));
    }

    @Test void publicMarketRemainsAnAnonymousRead() throws Exception {
        mvc.perform(get("/vlsCompute/public-market")).andExpect(status().isOk())
            .andExpect(jsonPath("$.public").value(true));
    }

    @Test void runtimeJwtModeCannotRestoreAnAgedOrRevokedSession() throws Exception {
        String oldKey = SaManager.getConfig().getJwtSecretKey();
        try {
            SaManager.getConfig().setJwtSecretKey("fixture-local-session-jwt-key-20261028");
            StpUtil.setStpLogic(new StpLogicJwtForSimple());
            context(new MockHttpServletRequest(), new MockHttpServletResponse());
            StpUtil.login("fixture-jwt-user", new SaLoginModel().setTimeout(-1));
            String jwt = StpUtil.getTokenValue();
            StpUtil.getTokenSession().setCreateTime(System.currentTimeMillis() - 3600001L);
            SaTokenContextForThreadLocalStorage.clearBox();
            mvc.perform(get("/vlsEventManagement/page").header(StpUtil.getTokenName(), jwt))
                .andExpect(jsonPath("$.code").value(401));
            context(new MockHttpServletRequest(), new MockHttpServletResponse());
            StpUtil.logoutByTokenValue(jwt);
            SaTokenContextForThreadLocalStorage.clearBox();
            mvc.perform(get("/vlsEventManagement/page").header(StpUtil.getTokenName(), jwt))
                .andExpect(jsonPath("$.code").value(401));
        } finally { SaManager.getConfig().setJwtSecretKey(oldKey); }
    }

    private static void context(HttpServletRequest request, HttpServletResponse response) {
        if (request instanceof MockHttpServletRequest && request.getServletPath().isEmpty()) {
            ((MockHttpServletRequest) request).setServletPath(request.getRequestURI());
        }
        SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request), new SaResponseForServlet(response),
            new SaStorageForServlet(request));
    }

    @RestController static class LocalEventController {
        @GetMapping("/vlsEventManagement/page") Map<String, Object> page() {
            return Collections.singletonMap("tenant", TenantContextHolder.getTenantId());
        }
        @GetMapping("/vlsCompute/public-market") Map<String, Object> market() {
            return Collections.singletonMap("public", true);
        }
    }
    @RestControllerAdvice static class Errors {
        @ExceptionHandler(NotLoginException.class) Map<String, Object> unauthenticated() {
            return Collections.singletonMap("code", 401);
        }
    }
}
