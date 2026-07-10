package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.SceneGovernance;
import com.ruoyi.vlstream.service.IVlsSceneGovernanceService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsSceneGovernanceControllerTest {

    @Test
    void exposesFrontendSceneGovernanceRoutes() throws Exception {
        Method list = VlsSceneGovernanceController.class.getDeclaredMethod("getSceneGovernanceList", Long.class, Long.class, String.class, String.class, String.class);
        Method remove = VlsSceneGovernanceController.class.getDeclaredMethod("removeSceneGovernance", String.class);
        Method submit = VlsSceneGovernanceController.class.getDeclaredMethod("submitSceneGovernance", SceneGovernance.class);

        assertArrayEquals(new String[] {"/list"}, list.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/remove"}, remove.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/submit"}, submit.getAnnotation(PostMapping.class).value());
    }

    @Test
    void listReturnsBladePageAndForwardsFilters() {
        IVlsSceneGovernanceService service = mock(IVlsSceneGovernanceService.class);
        VlsSceneGovernanceController controller = new VlsSceneGovernanceController(service);
        SceneGovernance scene = scene(7L, "night patrol");
        BladePage<SceneGovernance> page = BladePage.of(Collections.singletonList(scene), 1L, 20L, 1L);

        when(service.getSceneGovernanceList(1L, 20L, "night", "2026-01-01", "2026-01-31")).thenReturn(page);

        BladeResult<BladePage<SceneGovernance>> response = controller.getSceneGovernanceList(1L, 20L, "night", "2026-01-01", "2026-01-31");

        assertEquals(200, response.getCode());
        assertSame(page, response.getData());
        verify(service).getSceneGovernanceList(1L, 20L, "night", "2026-01-01", "2026-01-31");
    }

    @Test
    void submitAcceptsFrontendAliasesAndEnabledStatus() {
        IVlsSceneGovernanceService service = mock(IVlsSceneGovernanceService.class);
        VlsSceneGovernanceController controller = new VlsSceneGovernanceController(service);
        SceneGovernance scene = scene(null, "entrance");
        scene.setStatus("enabled");
        scene.setAlgorithmIds(Arrays.asList("101", "102"));
        scene.setCameraIds(Arrays.asList("13", "14"));
        scene.setAlgorithmName("detect, segment");
        scene.setCamerasName("gate-1, gate-2");
        scene.setDevices("gate-1, gate-2");
        scene.setRules("detect, segment");

        when(service.submitSceneGovernance(scene)).thenReturn(scene);

        BladeResult<SceneGovernance> response = controller.submitSceneGovernance(scene);

        assertEquals(200, response.getCode());
        assertSame(scene, response.getData());
        assertEquals(Integer.valueOf(1), scene.getStatus());
        assertEquals("13,14", scene.getCameras());
        verify(service).submitSceneGovernance(scene);
    }

    @Test
    void removeParsesFrontendCommaSeparatedIds() {
        IVlsSceneGovernanceService service = mock(IVlsSceneGovernanceService.class);
        VlsSceneGovernanceController controller = new VlsSceneGovernanceController(service);
        List<Long> ids = Arrays.asList(7L, 8L);

        when(service.removeSceneGovernance(ids)).thenReturn(true);

        BladeResult<Boolean> response = controller.removeSceneGovernance("7,8");

        assertEquals(200, response.getCode());
        assertEquals(Boolean.TRUE, response.getData());
        verify(service).removeSceneGovernance(ids);
    }

    private SceneGovernance scene(Long id, String name) {
        SceneGovernance scene = new SceneGovernance();
        scene.setId(id);
        scene.setName(name);
        scene.setDescription("Cron expression");
        scene.setCronExpression("0 0/5 * * * ? *");
        scene.setLocation("AreaA");
        scene.setCameras("13,14");
        return scene;
    }
}
