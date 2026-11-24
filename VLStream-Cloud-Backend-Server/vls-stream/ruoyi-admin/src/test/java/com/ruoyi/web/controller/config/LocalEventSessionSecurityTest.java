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
import com.ruoyi.system.domain.VlsPublicFile;
import com.ruoyi.system.service.PublicFileService;
import com.ruoyi.web.controller.system.PublicFileController;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("dev")
class LocalEventSessionSecurityTest {
    private static final String FILE_ID = "ec930076-e000-4dc0-a2bd-081d84ec8b2a";
    private static final String PUBLIC_FILE = PublicFileController.PUBLIC_PATH + FILE_ID;
    private SaTokenContext oldContext;
    private SaTokenDao oldDao;
    private StpLogic oldLogic;
    private SaTokenDaoDefaultImpl memory;
    private TokenProperties properties;
    private String token;
    private MockMvc mvc;
    private PublicFileService files;

    @BeforeEach void setup() {
        oldContext = SaManager.getSaTokenContext(); oldDao = SaManager.getSaTokenDao(); oldLogic = StpUtil.getStpLogic();
        memory = new SaTokenDaoDefaultImpl(); SaManager.setSaTokenDao(memory);
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal()); StpUtil.setStpLogic(new StpLogic("login"));
        context(new MockHttpServletRequest(), new MockHttpServletResponse());
        StpUtil.login("fixture-local-user", new SaLoginModel().setTimeout(-1));
        token = StpUtil.getTokenValue();
        LoginUser user = new LoginUser(); user.setTenantId("tenant-a"); user.setUserId("fixture-local-user");
        StpUtil.getTokenSession().set(LoginHelper.LOGIN_USER_KEY, user);
        SaTokenContextForThreadLocalStorage.clearBox();
        properties = new TokenProperties(); properties.setTenantType("multi");
        LocalSessionLifetimeInterceptor lifetime = new LocalSessionLifetimeInterceptor(properties, 3600);
        TenantContextInterceptor tenant = new TenantContextInterceptor(properties);
        SaInterceptor login = new SaInterceptor(handler -> WebMvcConfigToken.requireLoginUnlessPublicRead());
        files = mock(PublicFileService.class);
        mvc = MockMvcBuilders.standaloneSetup(new LocalEventController(),
                new PublicFileController(files, "https://files.example.test/bus/apaas-vls-server/"))
            .setControllerAdvice(new Errors())
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

    @Test void fileUploadRequiresLoginAndUsesTheTrustedTenant() throws Exception {
        MockMultipartFile part = new MockMultipartFile("file", "说明.txt", "text/plain", new byte[]{1, 2, 3});
        mvc.perform(multipart("/system/oss/upload").file(part)).andExpect(jsonPath("$.code").value(401));
        mvc.perform(multipart("/system/oss/upload").file(part).header(StpUtil.getTokenName(), token)
                .header("tenantid", "tenant-b"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(files);
        when(files.upload(any(), eq("tenant-a"), eq("fixture-local-user"))).thenReturn(publicFile());
        mvc.perform(multipart("/system/oss/upload").file(part).header(StpUtil.getTokenName(), token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.fileId").value(FILE_ID))
            .andExpect(jsonPath("$.data.fileName").value("说明.txt")).andExpect(jsonPath("$.data.size").value(3))
            .andExpect(jsonPath("$.data.url").value("https://files.example.test/bus/apaas-vls-server" + PUBLIC_FILE));
    }

    @Test void fileUploadRejectsEmptyMultipleAndOversizedFiles() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);
        MockMultipartFile normal = new MockMultipartFile("file", new byte[]{1});
        MockMultipartFile oversized = new MockMultipartFile("file", new byte[]{1}) {
            @Override public long getSize() { return PublicFileService.MAX_FILE_SIZE + 1; }
        };
        mvc.perform(multipart("/system/oss/upload").file(empty).header(StpUtil.getTokenName(), token))
            .andExpect(status().isBadRequest());
        mvc.perform(multipart("/system/oss/upload").file(normal).file(normal).header(StpUtil.getTokenName(), token))
            .andExpect(status().isBadRequest());
        mvc.perform(multipart("/system/oss/upload").file(oversized).header(StpUtil.getTokenName(), token))
            .andExpect(status().isPayloadTooLarge());
        verifyNoInteractions(files);
    }

    @ParameterizedTest
    @CsvSource({"single, configured-single", "multi, tenant-a"})
    void fileCanBeUploadedAndDownloadedInBothTenantModes(String mode, String expectedTenant) throws Exception {
        properties.setTenantType(mode);
        properties.setSingleTenantId("configured-single");
        VlsPublicFile file = publicFile();
        MockMultipartFile part = new MockMultipartFile("file", "说明.txt", "text/plain",
            "abc".getBytes(StandardCharsets.UTF_8));
        when(files.upload(any(), eq(expectedTenant), eq("fixture-local-user"))).thenReturn(file);
        mvc.perform(multipart("/system/oss/upload").file(part).header(StpUtil.getTokenName(), token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.fileId").value(FILE_ID));
        verify(files).upload(part, expectedTenant, "fixture-local-user");

        when(files.find(FILE_ID)).thenReturn(file);
        when(files.open(file)).thenReturn(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)));
        mvc.perform(get(PUBLIC_FILE)).andExpect(status().isOk()).andExpect(content().string("abc"))
            .andExpect(content().contentType("application/octet-stream"))
            .andExpect(header().string("Content-Length", "3"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=%E8%AF%B4%E6%98%8E.txt;"
                + "filename*=utf-8''%E8%AF%B4%E6%98%8E.txt"));
        mvc.perform(head(PUBLIC_FILE)).andExpect(status().isOk())
            .andExpect(header().string("Content-Length", "3")).andExpect(content().string(""));
        verify(files).open(file);
        verify(files).checkExists(file);
    }

    @Test void publicLinkIsReadableWithAStaleTenantHeader() throws Exception {
        VlsPublicFile file = publicFile();
        when(files.find(FILE_ID)).thenReturn(file);
        when(files.open(file)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        mvc.perform(get(PUBLIC_FILE).header(StpUtil.getTokenName(), token).header("tenantid", "tenant-b"))
            .andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1, 2, 3}));
        mvc.perform(head(PUBLIC_FILE).header(StpUtil.getTokenName(), token).header("tenantid", "tenant-b"))
            .andExpect(status().isOk());
    }

    @Test void unknownPublicFileReturnsNotFoundAndPublicAccessOnlyAllowsReads() throws Exception {
        mvc.perform(get(PUBLIC_FILE)).andExpect(status().isNotFound());
        mvc.perform(post(PUBLIC_FILE)).andExpect(status().isMethodNotAllowed());
        assertFalse(WebMvcConfigToken.isPublicRead("POST", PUBLIC_FILE));
        assertFalse(WebMvcConfigToken.isPublicRead("GET", PUBLIC_FILE + "/other"));
        assertFalse(WebMvcConfigToken.isPublicRead("GET", "/system/oss/download/123"));
    }

    private static VlsPublicFile publicFile() {
        VlsPublicFile file = new VlsPublicFile();
        file.setFileId(FILE_ID);
        file.setOriginalName("说明.txt");
        file.setFileSize(3);
        return file;
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
