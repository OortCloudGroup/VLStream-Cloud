package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceStreamMapper;
import com.ruoyi.vlstream.test.vlstream.service.FirmwareDeploymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class RetiredDevicePreviewTest {
    @InjectMocks
    private VlsDeviceInfoController devices;

    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        // Unrelated device services are never invoked by the retired paths.
        try (AutoCloseable ignored = MockitoAnnotations.openMocks(this)) {
            MqttDeviceStreamMapper streams = mock(MqttDeviceStreamMapper.class);
            when(streams.selectList(any())).thenReturn(Collections.emptyList());
            VlsMqttDeviceController legacy = new VlsMqttDeviceController(
                mock(MqttDeviceMapper.class), streams, mock(FirmwareDeploymentService.class));
            mvc = MockMvcBuilders.standaloneSetup(devices, legacy).build();
        }
    }

    @Test
    void oldPreviewAndMediaStatusPathsCannotCreateOrManageProxies() throws Exception {
        mvc.perform(post("/vlsDeviceInfo/1/preview")).andExpect(status().isNotFound());
        mvc.perform(post("/vlsMqttDevice/1/preview")
            .contentType("application/json").content("{\"streamId\":2}"))
            .andExpect(status().isNotFound());
        mvc.perform(delete("/vlsMqttDevice/1/streams/2/preview"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/vlsMqttDevice/media/status")).andExpect(status().isNotFound());
    }

    @Test
    void legacyStreamListingStillWorksWithoutAMediaProxy() throws Exception {
        mvc.perform(get("/vlsMqttDevice/1/streams")).andExpect(status().isOk());
    }
}
