package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmRepositoryMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class SharedAlgorithmCatalogTest {
    private final VlsAlgorithmMapper algorithms = mock(VlsAlgorithmMapper.class);
    private final VlsAlgorithmRepositoryMapper repositories = mock(VlsAlgorithmRepositoryMapper.class);
    private final SharedAlgorithmCatalog catalog = new SharedAlgorithmCatalog(algorithms, repositories, "multi");

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void showsCanonicalPresetAndKeepsOwnedSubcategories() {
        TenantContextHolder.setTenantId("tenant-a");
        AlgorithmRepository canonical = repository(1L, 0L, 1L);
        AlgorithmRepository copy = repository(2L, 0L, 1L);
        AlgorithmRepository child = repository(3L, 2L, null);
        when(repositories.selectSharedCatalogs()).thenReturn(Collections.singletonList(canonical));

        assertEquals(Arrays.asList(canonical, child), catalog.categories(Arrays.asList(copy, child)));
        assertEquals(Long.valueOf(1), child.getParentId());
        assertThrows(ServiceException.class, () -> catalog.checkCatalogWritable(copy));
        assertDoesNotThrow(() -> catalog.checkCatalogWritable(child));
    }

    @Test
    void resolvesSharedTrainingAndKeepsOwnedModelConfiguration() {
        TenantContextHolder.setTenantId("tenant-a");
        Algorithm external = new Algorithm();
        external.setCatalogReadOnly(true);
        when(algorithms.selectSharedCatalogAlgorithm(20L, "tenant-a")).thenReturn(external);
        Algorithm owned = new Algorithm();
        owned.setPtModelFilePath("private-owner-model");
        when(algorithms.selectById(21L)).thenReturn(owned);

        assertSame(external, catalog.forTraining(20L));
        assertNull(external.getPtModelFilePath());
        assertSame(owned, catalog.forTraining(21L));
        verify(algorithms, never()).selectSharedCatalogAlgorithm(21L, "tenant-a");
        assertNull(catalog.forTraining(22L));
        owned.setCatalogSeed(true);
        assertThrows(ServiceException.class, () -> catalog.checkAlgorithmWritable(owned));
    }

    @Test
    void neverQueriesAcrossTenantsWithoutTrustedContextOrInSingleMode() {
        assertNull(catalog.visible(20L));
        TenantContextHolder.setTenantId("tenant-a");
        SharedAlgorithmCatalog single = new SharedAlgorithmCatalog(algorithms, repositories, "single");
        assertNull(single.visible(20L));
        verifyNoInteractions(algorithms, repositories);
    }

    private AlgorithmRepository repository(Long id, Long parentId, Long sharedId) {
        AlgorithmRepository row = new AlgorithmRepository();
        row.setId(id);
        row.setParentId(parentId);
        row.setSharedCatalogId(sharedId);
        return row;
    }
}
