/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.TagManagement;
import com.ruoyi.vlstream.service.IVlsTagManagementService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsTagManagementControllerTest {

    @Test
    void exposesFrontendTagManagementRoutes() throws Exception {
        RequestMapping classMapping = VlsTagManagementController.class.getAnnotation(RequestMapping.class);
        Method page = VlsTagManagementController.class.getDeclaredMethod(
            "page", long.class, long.class, String.class, String.class, Integer.class, Long.class, Long.class);
        Method tree = VlsTagManagementController.class.getDeclaredMethod("tree");
        Method getTag = VlsTagManagementController.class.getDeclaredMethod("getTag", Long.class);
        Method createTag = VlsTagManagementController.class.getDeclaredMethod("createTag", TagManagement.class);
        Method updateTag = VlsTagManagementController.class.getDeclaredMethod("updateTag", Long.class, TagManagement.class);
        Method deleteTag = VlsTagManagementController.class.getDeclaredMethod("deleteTag", Long.class);
        Method deleteTags = VlsTagManagementController.class.getDeclaredMethod("deleteTags", List.class);
        Method statistics = VlsTagManagementController.class.getDeclaredMethod("statistics");
        Method stats = VlsTagManagementController.class.getDeclaredMethod("stats", Long.class);
        Method checkName = VlsTagManagementController.class.getDeclaredMethod("checkName", String.class, Long.class, Long.class);
        Method children = VlsTagManagementController.class.getDeclaredMethod("children", Long.class);
        Method moveTag = VlsTagManagementController.class.getDeclaredMethod("moveTag", Long.class, Map.class);

        assertArrayEquals(new String[] {"/vlsTagManagement"}, classMapping.value());
        assertArrayEquals(new String[] {"/page"}, page.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/tree"}, tree.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, getTag.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {}, createTag.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, updateTag.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, deleteTag.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/batch"}, deleteTags.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/statistics"}, statistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/stats"}, stats.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/check-name"}, checkName.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{parentId}/children"}, children.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/move"}, moveTag.getAnnotation(PutMapping.class).value());
    }

    @Test
    void pageReturnsBladePageEnvelopeFromService() {
        IVlsTagManagementService service = mock(IVlsTagManagementService.class);
        VlsTagManagementController controller = new VlsTagManagementController(service);
        TagManagement tag = new TagManagement();
        tag.setId(7L);
        tag.setTagName("camera");
        BladePage<TagManagement> page = BladePage.of(Arrays.asList(tag), 1L, 10L, 1L);

        when(service.getTagManagementPage(1L, 10L, "cam", "own", 2, 3L, null)).thenReturn(page);

        BladeResult<BladePage<TagManagement>> result = controller.page(1L, 10L, "cam", "own", 2, 3L, null);

        assertEquals(200, result.getCode());
        assertEquals(page, result.getData());
        assertEquals("camera", result.getData().getRecords().get(0).getTagName());
        verify(service).getTagManagementPage(1L, 10L, "cam", "own", 2, 3L, null);
    }

    @Test
    void moveTagReadsFrontendNewParentIdBody() {
        IVlsTagManagementService service = mock(IVlsTagManagementService.class);
        VlsTagManagementController controller = new VlsTagManagementController(service);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("newParentId", 20);
        body.put("targetPosition", 4);

        when(service.moveTag(10L, 20L, 4)).thenReturn(true);

        BladeResult<Void> result = controller.moveTag(10L, body);

        assertEquals(200, result.getCode());
        verify(service).moveTag(10L, 20L, 4);
    }

    @Test
    void checkNameReturnsBooleanEnvelope() {
        IVlsTagManagementService service = mock(IVlsTagManagementService.class);
        VlsTagManagementController controller = new VlsTagManagementController(service);

        when(service.isTagNameDuplicate("camera", 3L, 7L)).thenReturn(true);

        BladeResult<Boolean> result = controller.checkName("camera", 3L, 7L);

        assertEquals(200, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(service).isTagNameDuplicate("camera", 3L, 7L);
    }
}
