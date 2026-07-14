/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AlgorithmRepository;
import com.ruoyi.vlstream.service.IVlsAlgorithmRepositoryService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsAlgorithmRepositoryControllerTest {

    @Test
    void exposesFrontendAlgorithmRepositoryRoutes() throws Exception {
        Method page = VlsAlgorithmRepositoryController.class.getDeclaredMethod(
            "getRepositoryPage", Long.class, Long.class, String.class, String.class, String.class);
        Method enabled = VlsAlgorithmRepositoryController.class.getDeclaredMethod("getEnabledRepositories");
        Method byType = VlsAlgorithmRepositoryController.class.getDeclaredMethod("getRepositoriesByType", String.class);
        Method byId = VlsAlgorithmRepositoryController.class.getDeclaredMethod("getRepositoryById", Long.class);
        Method create = VlsAlgorithmRepositoryController.class.getDeclaredMethod("createRepository", AlgorithmRepository.class);
        Method update = VlsAlgorithmRepositoryController.class.getDeclaredMethod("updateRepository", Long.class, AlgorithmRepository.class);
        Method delete = VlsAlgorithmRepositoryController.class.getDeclaredMethod("deleteRepository", Long.class);
        Method batchDelete = VlsAlgorithmRepositoryController.class.getDeclaredMethod("batchDeleteRepositories", List.class);
        Method updateStatus = VlsAlgorithmRepositoryController.class.getDeclaredMethod("updateRepositoryStatus", Long.class, String.class);
        Method batchStatus = VlsAlgorithmRepositoryController.class.getDeclaredMethod("batchUpdateRepositoryStatus", List.class, String.class);
        Method count = VlsAlgorithmRepositoryController.class.getDeclaredMethod("countRepositories");
        Method refreshCount = VlsAlgorithmRepositoryController.class.getDeclaredMethod("refreshAlgorithmCount", Long.class);

        assertArrayEquals(new String[] {"/page"}, page.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/enabled"}, enabled.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/type/{repositoryType}"}, byType.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, byId.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {}, create.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, update.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, delete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/batch"}, batchDelete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/status"}, updateStatus.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/batch/status"}, batchStatus.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/count"}, count.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/refresh-count"}, refreshCount.getAnnotation(PutMapping.class).value());
    }

    @Test
    void pageAcceptsFrontendPaginationAliases() {
        IVlsAlgorithmRepositoryService service = mock(IVlsAlgorithmRepositoryService.class);
        VlsAlgorithmRepositoryController controller = new VlsAlgorithmRepositoryController(service);
        BladePage<AlgorithmRepository> page = BladePage.of(Collections.<AlgorithmRepository>emptyList(), 0L, 20L, 2L);

        when(service.getRepositoryPage(2L, 20L, "fire", "extended", "enabled")).thenReturn(page);

        BladeResult<BladePage<AlgorithmRepository>> result =
            controller.getRepositoryPage(2L, 20L, "fire", "extended", "enabled");

        assertEquals(page, result.getData());
        verify(service).getRepositoryPage(2L, 20L, "fire", "extended", "enabled");
    }

    @Test
    void createUpdateDeleteAndStatusForwardToService() {
        IVlsAlgorithmRepositoryService service = mock(IVlsAlgorithmRepositoryService.class);
        VlsAlgorithmRepositoryController controller = new VlsAlgorithmRepositoryController(service);
        AlgorithmRepository repository = new AlgorithmRepository();
        repository.setName("Fire");
        List<Long> ids = Arrays.asList(1L, 2L);

        when(service.createRepository(repository)).thenReturn(repository);
        when(service.updateRepository(7L, repository)).thenReturn(repository);
        when(service.deleteRepository(7L)).thenReturn(true);
        when(service.deleteRepositories(ids)).thenReturn(true);
        when(service.updateRepositoryStatus(7L, "disabled")).thenReturn(true);
        when(service.updateRepositoryStatus(ids, "enabled")).thenReturn(true);

        assertEquals(repository, controller.createRepository(repository).getData());
        assertEquals(repository, controller.updateRepository(7L, repository).getData());
        assertEquals(Boolean.TRUE, controller.deleteRepository(7L).getData());
        assertEquals(Boolean.TRUE, controller.batchDeleteRepositories(ids).getData());
        assertEquals(Boolean.TRUE, controller.updateRepositoryStatus(7L, "disabled").getData());
        assertEquals(Boolean.TRUE, controller.batchUpdateRepositoryStatus(ids, "enabled").getData());

        verify(service).createRepository(repository);
        verify(service).updateRepository(7L, repository);
        verify(service).deleteRepository(7L);
        verify(service).deleteRepositories(ids);
        verify(service).updateRepositoryStatus(7L, "disabled");
        verify(service).updateRepositoryStatus(ids, "enabled");
    }

    @Test
    void listAndCountRoutesReturnServiceData() {
        IVlsAlgorithmRepositoryService service = mock(IVlsAlgorithmRepositoryService.class);
        VlsAlgorithmRepositoryController controller = new VlsAlgorithmRepositoryController(service);
        AlgorithmRepository repository = new AlgorithmRepository();
        repository.setId(3L);
        List<AlgorithmRepository> repositories = Collections.singletonList(repository);

        when(service.getEnabledRepositories()).thenReturn(repositories);
        when(service.getRepositoriesByType("basic")).thenReturn(repositories);
        when(service.getRepositoryById(3L)).thenReturn(repository);
        when(service.countRepositories()).thenReturn(5L);
        when(service.refreshAlgorithmCount(3L)).thenReturn(true);

        assertEquals(repositories, controller.getEnabledRepositories().getData());
        assertEquals(repositories, controller.getRepositoriesByType("basic").getData());
        assertEquals(repository, controller.getRepositoryById(3L).getData());
        assertEquals(Long.valueOf(5L), controller.countRepositories().getData());
        assertEquals(Boolean.TRUE, controller.refreshAlgorithmCount(3L).getData());

        verify(service).getEnabledRepositories();
        verify(service).getRepositoriesByType("basic");
        verify(service).getRepositoryById(3L);
        verify(service).countRepositories();
        verify(service).refreshAlgorithmCount(3L);
    }
}
