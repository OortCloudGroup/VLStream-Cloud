package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask;
import com.ruoyi.vlstream.test.vlstream.service.ModelDispatchTaskService;
import com.ruoyi.vlstream.test.vlstream.service.ModelDownloadSignatureService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class VlsModelDispatchControllerTest {
    @Test
    void classDownloadUsesDurableSnapshotAndSeparateSignature() throws Exception {
        ModelDispatchTaskService tasks = mock(ModelDispatchTaskService.class);
        ModelDownloadSignatureService signatures = mock(ModelDownloadSignatureService.class);
        VlsModelDispatchController controller = new VlsModelDispatchController();
        ReflectionTestUtils.setField(controller, "taskService", tasks);
        ReflectionTestUtils.setField(controller, "signatureService", signatures);
        ModelDispatchTask task = new ModelDispatchTask();
        task.setDownloadExpiresAt(100L);
        task.setClassFileName("类别.yaml");
        task.setClassFileContent("nc: 1\r\nnames: ['安全绳']\r\n");
        task.setClassFileSha256("abc");
        when(tasks.getByRequestId("r1")).thenReturn(task);
        when(signatures.verify("r1:classes", 100L, "class-signature")).thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadClasses("r1", 100L, "class-signature", response);
        assertEquals(200, response.getStatus());
        assertArrayEquals(task.getClassFileContent().getBytes(StandardCharsets.UTF_8), response.getContentAsByteArray());
        assertEquals("abc", response.getHeader("X-Class-File-SHA256"));
        assertEquals("private, no-store", response.getHeader("Cache-Control"));
        verify(tasks).getByRequestId("r1");
        verifyNoMoreInteractions(tasks);

        MockHttpServletResponse invalid = new MockHttpServletResponse();
        controller.downloadClasses("r1", 100L, "model-signature", invalid);
        assertEquals(403, invalid.getStatus());
        MockHttpServletResponse expired = new MockHttpServletResponse();
        controller.downloadClasses("r1", 101L, "class-signature", expired);
        assertEquals(403, expired.getStatus());
        task.setClassFileContent(null);
        MockHttpServletResponse legacy = new MockHttpServletResponse();
        controller.downloadClasses("r1", 100L, "class-signature", legacy);
        assertEquals(404, legacy.getStatus());
    }
}
