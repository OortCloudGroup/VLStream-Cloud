package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.servlet.model.SaRequestForServlet;
import cn.dev33.satoken.servlet.model.SaResponseForServlet;
import cn.dev33.satoken.servlet.model.SaStorageForServlet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.service.VideoSquareSceneService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class VideoSquareSceneControllerTest {
    private final ObjectMapper json = new ObjectMapper();
    private final VideoSquareSceneService service = mock(VideoSquareSceneService.class);
    private final VideoSquareSceneController controller = new VideoSquareSceneController(service, json);
    private final VideoSquareSceneService.GenerateRequest instruction = new VideoSquareSceneService.GenerateRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private SaTokenContext oldContext;

    @BeforeEach
    void signIn() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        SaStorageForServlet storage = new SaStorageForServlet(request);
        storage.set(LoginHelper.USER_KEY, "operator");
        oldContext = SaManager.getSaTokenContext();
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request), new SaResponseForServlet(response), storage);
        TenantContextHolder.setTenantId("tenant");
    }

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        SaTokenContextForThreadLocalStorage.clearBox();
        SaManager.setSaTokenContext(oldContext);
    }

    @Test
    void storageFailureAfterPreviewEndsTheStreamWithAReloadableError() throws Exception {
        when(service.generate(eq("tenant"), eq("operator"), eq("bim"), same(instruction), any())).thenAnswer(call -> {
            call.<Consumer<ObjectNode>>getArgument(4).accept(json.createObjectNode().put("title", "临时场景"));
            throw new DataAccessResourceFailureException("database connection details");
        });
        controller.generate("bim", instruction, response);
        String[] lines = response.getContentAsString().trim().split("\n");
        assertEquals(3, lines.length);
        assertEquals("started", json.readTree(lines[0]).get("type").asText());
        assertEquals("preview", json.readTree(lines[1]).get("type").asText());
        JsonNode error = json.readTree(lines[2]);
        assertEquals("error", error.get("type").asText());
        assertEquals(500, error.get("code").intValue());
        assertTrue(error.get("reload").booleanValue());
        assertFalse(error.get("message").asText().contains("database"));
    }

    @Test
    void disconnectDuringPreviewStopsWithoutWritingAnotherEvent() throws Exception {
        HttpServletResponse disconnected = mock(HttpServletResponse.class);
        ServletOutputStream output = mock(ServletOutputStream.class);
        when(disconnected.getOutputStream()).thenReturn(output);
        doNothing().doThrow(new IOException("closed")).when(output).write(any(byte[].class), anyInt(), anyInt());
        when(service.generate(anyString(), anyString(), anyString(), same(instruction), any())).thenAnswer(call -> {
            call.<Consumer<ObjectNode>>getArgument(4).accept(json.createObjectNode());
            throw new AssertionError("generation continued after preview disconnected");
        });
        controller.generate("bim", instruction, disconnected);
        verify(output, times(2)).write(any(byte[].class), anyInt(), anyInt());
    }
}
