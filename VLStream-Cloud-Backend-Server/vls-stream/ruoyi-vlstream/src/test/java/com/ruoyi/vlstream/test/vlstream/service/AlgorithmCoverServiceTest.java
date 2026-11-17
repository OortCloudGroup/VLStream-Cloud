package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.controller.VlsAlgorithmController;
import com.ruoyi.vlstream.test.vlstream.data.DataMediaStorage;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("dev")
class AlgorithmCoverServiceTest {
    private static final byte[] PNG = new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    private static final String REFERENCE = "minio://algorithm-covers/tenant-a/42/12345678-1234-1234-1234-123456789abc.png";
    private VlsAlgorithmMapper algorithms;
    private DataMediaStorage storage;
    private AlgorithmCoverService covers;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId("tenant-a");
        algorithms = mock(VlsAlgorithmMapper.class);
        storage = mock(DataMediaStorage.class);
        covers = new AlgorithmCoverService(algorithms, storage);
        when(algorithms.selectOne(any(Wrapper.class))).thenReturn(algorithm(REFERENCE));
        when(storage.preview(anyString())).thenReturn("https://storage.test/preview?signature=first");
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void uploadReturnsStableReferenceAndPreviewWithoutSavingAlgorithm() throws Exception {
        Algorithm result = covers.upload(42L, file("image/png", PNG));
        assertTrue(result.getImageUrl().startsWith("minio://algorithm-covers/tenant-a/42/"));
        assertTrue(result.getImageUrl().endsWith(".png"));
        assertEquals("https://storage.test/preview?signature=first", result.getImagePreviewUrl());
        verify(storage).put(startsWith("algorithm-covers/tenant-a/42/"), eq(PNG), eq("image/png"));
        verify(algorithms, never()).updateById(any());
        verify(algorithms, never()).insert(any());
        verify(algorithms).selectOne(argThat(query -> {
            String sql = query.getCustomSqlSegment();
            return sql.contains("tenant_id") && sql.contains("is_deleted") && sql.contains("id");
        }));
    }

    @Test
    void acceptsJpegAndWebpWithTheirActualContentTypes() throws Exception {
        assertTrue(covers.upload(42L, file("image/jpeg", new byte[]{(byte) 255, (byte) 216, (byte) 255})).getImageUrl().endsWith(".jpg"));
        assertTrue(covers.upload(42L, file("image/webp", "RIFF0000WEBP".getBytes(StandardCharsets.US_ASCII))).getImageUrl().endsWith(".webp"));
    }

    @Test
    void rejectsEmptyOversizeUnsupportedAndDisguisedFiles() {
        assertThrows(ServiceException.class, () -> covers.upload(42L, file("image/png", new byte[0])));
        assertThrows(ServiceException.class, () -> covers.upload(42L, file("image/png", new byte[5 * 1024 * 1024 + 1])));
        assertThrows(ServiceException.class, () -> covers.upload(42L, file("image/svg+xml", PNG)));
        assertThrows(ServiceException.class, () -> covers.upload(42L, file("image/png", "text".getBytes(StandardCharsets.UTF_8))));
        verifyNoInteractions(storage);
    }

    @Test
    void acceptsExactlyFiveMiB() throws Exception {
        byte[] bytes = new byte[5 * 1024 * 1024];
        System.arraycopy(PNG, 0, bytes, 0, PNG.length);
        assertNotNull(covers.upload(42L, file("image/png", bytes)));
    }

    @Test
    void inaccessibleAlgorithmAndMissingTenantCannotUpload() {
        when(algorithms.selectOne(any(Wrapper.class))).thenReturn(null);
        assertThrows(ServiceException.class, () -> covers.upload(42L, file("image/png", PNG)));
        TenantContextHolder.clear();
        assertThrows(ServiceException.class, () -> covers.upload(42L, file("image/png", PNG)));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsOtherTenantOtherAlgorithmAndTraversalReferences() {
        assertThrows(ServiceException.class, () -> covers.validate(algorithm(REFERENCE.replace("tenant-a", "tenant-b"))));
        assertThrows(ServiceException.class, () -> covers.validate(algorithm(REFERENCE.replace("/42/", "/43/"))));
        assertThrows(ServiceException.class, () -> covers.validate(algorithm(REFERENCE.replace("12345678-1234-1234-1234-123456789abc.png", "../secret.png"))));
        covers.validate(algorithm(REFERENCE));
    }

    @Test
    void preparesFreshPreviewWithoutReplacingStoredReference() {
        Algorithm row = algorithm(REFERENCE);
        covers.prepare(row);
        assertEquals(REFERENCE, row.getImageUrl());
        assertEquals("https://storage.test/preview?signature=first", row.getImagePreviewUrl());
        when(storage.preview(anyString())).thenReturn("https://storage.test/preview?signature=second");
        covers.prepare(row);
        assertEquals("https://storage.test/preview?signature=second", row.getImagePreviewUrl());
    }

    @Test
    void oldUrlsAndClearedCoverRemainCompatible() {
        Algorithm row = algorithm("https://legacy.test/image.png");
        covers.validate(row);
        covers.prepare(row);
        assertEquals("https://legacy.test/image.png", row.getImageUrl());
        row.setImageUrl("");
        covers.validate(row);
        covers.prepare(row);
        assertNull(row.getImagePreviewUrl());
        verifyNoInteractions(storage);
    }

    @Test
    void previewFailureAndForeignReferencesDoNotBreakListing() {
        Algorithm row = algorithm(REFERENCE);
        row.setImagePreviewUrl("stale");
        when(storage.preview(anyString())).thenThrow(new IllegalStateException("unavailable"));
        assertDoesNotThrow(() -> covers.prepare(row));
        assertNull(row.getImagePreviewUrl());
        assertEquals(REFERENCE, row.getImageUrl());
        row.setImageUrl(REFERENCE.replace("tenant-a", "tenant-b"));
        covers.prepare(row);
        verify(storage, times(1)).preview(anyString());
    }

    @Test
    void removesNewObjectWhenPreviewCreationFails() {
        when(storage.preview(anyString())).thenThrow(new IllegalStateException("unavailable"));
        assertThrows(IllegalStateException.class, () -> covers.upload(42L, file("image/png", PNG)));
        verify(storage).remove(startsWith("algorithm-covers/tenant-a/42/"));
    }

    @Test
    void normalMultipartAndSaveRoutesPreserveStableReference() throws Exception {
        IVlsAlgorithmService service = mock(IVlsAlgorithmService.class);
        when(service.updateAlgorithm(any())).thenReturn(true);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new VlsAlgorithmController(service, covers)).build();
        mvc.perform(multipart("/vlsAlgorithm/42/image").file(file("image/png", PNG)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.imagePreviewUrl").value("https://storage.test/preview?signature=first"));
        mvc.perform(put("/vlsAlgorithm/42").contentType("application/json")
            .content("{\"imageUrl\":\"" + REFERENCE + "\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(service).updateAlgorithm(argThat(row -> Long.valueOf(42L).equals(row.getId()) && REFERENCE.equals(row.getImageUrl())));
    }

    private Algorithm algorithm(String reference) {
        Algorithm row = new Algorithm();
        row.setId(42L);
        row.setTenantId("tenant-a");
        row.setImageUrl(reference);
        return row;
    }

    private MockMultipartFile file(String type, byte[] bytes) {
        return new MockMultipartFile("file", "cover.png", type, bytes);
    }
}
