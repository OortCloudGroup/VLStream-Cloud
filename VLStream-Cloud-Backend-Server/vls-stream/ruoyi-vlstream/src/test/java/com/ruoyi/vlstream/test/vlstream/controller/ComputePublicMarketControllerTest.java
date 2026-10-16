package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.compute.ComputePublicMarketService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputePublicMarketControllerTest {
    @Test void rateLimitHasBoundedWindowAndRejectsExcess() {
        ComputePublicMarketController controller = new ComputePublicMarketController(mock(ComputePublicMarketService.class));
        for (int i=0;i<120;i++) assertTrue(controller.admit(60000));
        assertFalse(controller.admit(60001)); assertTrue(controller.admit(120000));
    }
    @Test void invalidPageAndInternalErrorsNeverLeakDetails() {
        ComputePublicMarketService service = mock(ComputePublicMarketService.class);
        ComputePublicMarketController controller = new ComputePublicMarketController(service);
        assertEquals(400, controller.list(0,20,new MockHttpServletResponse()).getStatusCodeValue());
        verifyNoInteractions(service);
        when(service.list(1,20)).thenThrow(new IllegalStateException("secret internal IP SQL"));
        org.springframework.http.ResponseEntity<?> response = controller.list(1,20,new MockHttpServletResponse());
        assertEquals(503,response.getStatusCodeValue()); assertFalse(response.getBody().toString().contains("secret"));
    }
}
