package com.ruoyi.vlstream.test.vlstream.cloudai;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class CloudAiSchedulerTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CloudAiService service = mock(CloudAiService.class);
    private final CloudAiScheduler scheduler = new CloudAiScheduler(jdbc, mock(TransactionTemplate.class), service);
    private Map<String,Object> task, scene;
    private final long start = Instant.parse("2026-10-30T10:00:00Z").toEpochMilli();
    private final long end = start + 60000;

    @BeforeEach void setup() {
        task = new HashMap<>(); task.put("scene_id", "scene"); task.put("tenant_id", "tenant");
        task.put("actor_id", "owner"); task.put("next_start", start); task.put("next_end", end); task.put("schedule_json", "rule");
        scene = new HashMap<>(); scene.put("enabled", 1); scene.put("deleted", 0);
        when(jdbc.queryForList(contains("FROM vls_cloud_ai_task"), eq("task"), anyLong())).thenReturn(Collections.singletonList(task));
        when(jdbc.queryForList(contains("FROM vls_cloud_ai_scene"), eq("scene"), eq("tenant"), eq("owner"))).thenReturn(Collections.singletonList(scene));
        CloudAiSchedule.Definition rule = new CloudAiSchedule.Definition(); rule.kind = "ONCE";
        rule.onceStart = "2026-10-30T18:00:00"; rule.onceEnd = "2026-10-30T18:01:00";
        when(service.schedule("rule")).thenReturn(new CloudAiSchedule(rule));
    }

    @Test void restartAfterWindowRecordsSkipInsteadOfRunningLate() {
        scheduler.occurrence("task", end);
        verify(service).createRun(task, scene, Long.toString(start / 1000), start, end, null, "SKIPPED", "执行窗口已结束，未补跑");
        verify(jdbc).update(contains("SET next_start"), isNull(), isNull(), eq(false), eq("task"));
    }

    @Test void restartInsideWindowRetainsOriginalDeadline() {
        scheduler.occurrence("task", start + 45000);
        verify(service).createRun(task, scene, Long.toString(start / 1000), start, end, null, "QUEUED", "等待GPU调度");
    }

    @Test void disabledSceneIsRecordedWithoutDispatching() {
        scene.put("enabled", 0); scheduler.occurrence("task", start);
        verify(service).createRun(task, scene, Long.toString(start / 1000), start, end, null, "SKIPPED", "场景未启用，本次已跳过");
    }

    @Test void secondScannerAfterLockedAdvanceDoesNothing() {
        when(jdbc.queryForList(contains("FROM vls_cloud_ai_task"), eq("task"), anyLong())).thenReturn(Collections.emptyList());
        scheduler.occurrence("task", start); verifyNoInteractions(service);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void sceneOwnerMismatchDisablesTheTaskWithoutUsingAnotherActorsScene() {
        when(jdbc.queryForList(contains("FROM vls_cloud_ai_scene"), eq("scene"), eq("tenant"), eq("owner"))).thenReturn(Collections.emptyList());
        scheduler.occurrence("task", start);
        verify(jdbc).update("UPDATE vls_cloud_ai_task SET enabled=0 WHERE id=?", "task");
        verifyNoInteractions(service);
    }
}
