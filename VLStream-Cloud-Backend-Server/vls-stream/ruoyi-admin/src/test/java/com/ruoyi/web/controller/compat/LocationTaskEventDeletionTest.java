package com.ruoyi.web.controller.compat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.service.DeviceMediaUploadService;
import com.ruoyi.workflow.service.IWfProcessService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Synthetic records live only in a unique in-memory database; no application context or devices. */
@Tag("dev")
class LocationTaskEventDeletionTest {
    private JdbcTemplate jdbc;
    private LocationTaskCompatService service;
    private DeviceMediaUploadService media;
    private IWfProcessService workflows;
    private final LocationTaskCompatService.UserContext user =
        new LocationTaskCompatService.UserContext("synthetic-token", "tenant-a", "viewer", "viewer", "web");

    @BeforeEach
    void setup() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE oort_task_event (id VARCHAR(100), `no` INT, tenant_id VARCHAR(64), uuid VARCHAR(64), "
            + "name VARCHAR(100), data VARCHAR(4000), status INT DEFAULT 2, item VARCHAR(100), client VARCHAR(30), "
            + "mod_type INT DEFAULT 2, mod_status INT DEFAULT 0, device_id VARCHAR(100), device_name VARCHAR(100), "
            + "device_tag VARCHAR(100), work_order_status INT DEFAULT 0, pic_len INT DEFAULT 0, "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "deleted_at BIGINT DEFAULT 0)");
        jdbc.execute("CREATE TABLE oort_task_event_user (task_event_id VARCHAR(150), tenant_id VARCHAR(64), "
            + "uuid VARCHAR(64), u_type INT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE oort_task_event_back (task_event_id VARCHAR(150), tenant_id VARCHAR(64), data VARCHAR(1000))");
        jdbc.execute("CREATE TABLE vls_device_media_upload (media_id VARCHAR(64), upload_status VARCHAR(20))");
        jdbc.update("INSERT INTO vls_device_media_upload VALUES ('media-1','BOUND')");
        workflows = mock(IWfProcessService.class);
        media = mock(DeviceMediaUploadService.class);
        LocationTaskCompatService target = new LocationTaskCompatService(dataSource, new ObjectMapper(),
            workflows, mock(LocationTaskWorkflowProperties.class));
        ReflectionTestUtils.setField(target, "deviceMediaUploadService", media);
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
            new AnnotationTransactionAttributeSource()));
        service = (LocationTaskCompatService) proxy.getProxy();
    }

    @Test
    void tenantViewerSoftDeletesDeviceEventAndOnlySameTenantRelations() {
        addEvent("event", "tenant-a", "camera-device", "{\"pics\":[\"vls-media://media-1\"]}");
        addRelations("event-20261015-1", "tenant-a");
        addRelations("event-20261015-1", "tenant-b");

        assertEquals(200, service.eventDelete(body("id", "event-20261015-1"), user).getCode());

        assertTrue(jdbc.queryForObject("SELECT deleted_at FROM oort_task_event WHERE tenant_id='tenant-a'", Long.class) > 0);
        assertEquals("{\"pics\":[\"vls-media://media-1\"]}",
            jdbc.queryForObject("SELECT data FROM oort_task_event WHERE tenant_id='tenant-a'", String.class));
        assertEquals(0, relationCount("oort_task_event_user", "tenant-a"));
        assertEquals(0, relationCount("oort_task_event_back", "tenant-a"));
        assertEquals(1, relationCount("oort_task_event_user", "tenant-b"));
        assertEquals(1, relationCount("oort_task_event_back", "tenant-b"));
        assertEquals("BOUND", jdbc.queryForObject("SELECT upload_status FROM vls_device_media_upload", String.class));
        verifyNoInteractions(media, workflows);
        assertEquals(40001, service.eventDelete(body("id", "event-20261015-1"), user).getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void eventListBatchesLocationProjectionAfterTrustedTenantQueryAndPreservesMissingCoordinates() {
        addEvent("local", "tenant-a", "camera", "{\"point\":{\"address\":\"\"}}");
        addEvent("foreign", "tenant-b", "camera", "{}");
        EventLocationService locations = mock(EventLocationService.class);
        ReflectionTestUtils.setField(service, "eventLocationService", locations);
        service.eventList(body("mod_type", 2), user);
        org.mockito.ArgumentCaptor<List<Map<String, Object>>> captured = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(locations, times(1)).enrich(captured.capture(), eq("tenant-a"), eq("synthetic-token"));
        assertEquals(1, captured.getValue().size());
        assertEquals("tenant-a", captured.getValue().get(0).get("tenant_id"));
        Map<String, Object> point = (Map<String, Object>) captured.getValue().get(0).get("point");
        assertNull(point.get("lng"));
        assertNull(point.get("lat"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void eventListConnectsDatabaseAndDeviceBatchWithoutWritingHistoricalPoint() {
        addEvent("position", "tenant-a", "camera", "{\"point\":{\"lng\":0,\"lat\":0,\"coord_system_type\":0}}");
        jdbc.update("UPDATE oort_task_event SET device_id='BUSINESS-CAM' WHERE tenant_id='tenant-a'");
        addEvent("foreign-position", "tenant-b", "camera", "{}");
        org.springframework.web.client.RestTemplate http = new org.springframework.web.client.RestTemplate();
        org.springframework.test.web.client.MockRestServiceServer server = org.springframework.test.web.client.MockRestServiceServer.bindTo(http).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://127.0.0.1:9080/internal/vlstream/device/locations"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                "{\"code\":200,\"data\":[{\"deviceId\":\"BUSINESS-CAM\",\"tenantId\":\"tenant-a\",\"longitude\":114.1177,\"latitude\":22.5543,\"heartbeatAt\":1793000000000,\"status\":\"FRESH\"}]}", org.springframework.http.MediaType.APPLICATION_JSON));
        com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties properties = new com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties();
        ReflectionTestUtils.setField(service, "eventLocationService", new EventLocationService(properties, new ObjectMapper(), http));
        Map<?, ?> page = (Map<?, ?>) service.eventList(body("mod_type", 2), user).getData();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) page.get("list");
        assertEquals(1, rows.size());
        Map<String, Object> display = (Map<String, Object>) rows.get(0).get("display_location");
        assertEquals("", display.get("address"));
        assertEquals(114.1177, ((Number) display.get("longitude")).doubleValue());
        assertEquals(1, display.get("coordinate_system"));
        assertEquals("DEVICE_HEARTBEAT", display.get("source"));
        assertEquals("{\"point\":{\"lng\":0,\"lat\":0,\"coord_system_type\":0}}",
            jdbc.queryForObject("SELECT data FROM oort_task_event WHERE tenant_id='tenant-a'", String.class));
        server.verify();
    }

    @Test
    void rejectsOtherTenantEvenWhenIdIsKnownAndRequestClaimsThatTenant() {
        addEvent("foreign", "tenant-b", "viewer", "{}");
        addRelations("foreign-20261015-1", "tenant-b");
        Map<String, Object> request = body("id", "foreign-20261015-1");
        request.put("tenant_id", "tenant-b");
        request.put("uuid", "viewer");

        assertEquals(40001, service.eventDelete(request, user).getCode());
        assertEquals(0L, jdbc.queryForObject("SELECT deleted_at FROM oort_task_event", Long.class));
        assertEquals(1, relationCount("oort_task_event_user", "tenant-b"));
        assertEquals(1, relationCount("oort_task_event_back", "tenant-b"));
        verifyNoInteractions(media, workflows);
    }

    @Test
    void eventDetailsRejectForeignTenantBeforeLocationLookup() {
        addEvent("foreign-detail", "tenant-b", "camera", "{\"point\":{\"lng\":114,\"lat\":22,\"coord_system_type\":1}}");
        EventLocationService locations = mock(EventLocationService.class);
        ReflectionTestUtils.setField(service, "eventLocationService", locations);
        assertEquals(40001, service.eventInfo(body("id", "foreign-detail-20261015-1"), user).getCode());
        verifyNoInteractions(locations);
    }

    @Test
    void rejectsMissingTrustedTenantOrUser() {
        addEvent("event", "tenant-a", "viewer", "{}");
        assertEquals(40001, service.eventDelete(body("id", "event-20261015-1"),
            new LocationTaskCompatService.UserContext("token", "", "viewer", "viewer", "web")).getCode());
        assertEquals(40001, service.eventDelete(body("id", "event-20261015-1"),
            new LocationTaskCompatService.UserContext("token", "tenant-a", "", "viewer", "web")).getCode());
        assertEquals(0L, jdbc.queryForObject("SELECT deleted_at FROM oort_task_event", Long.class));
    }

    @Test
    void relationFailureRollsBackSoftDeleteAndOtherRelationRemoval() {
        addEvent("event", "tenant-a", "camera", "{}");
        addRelations("event-20261015-1", "tenant-a");
        jdbc.execute("DROP TABLE oort_task_event_back");

        assertThrows(DataAccessException.class, () -> service.eventDelete(body("id", "event-20261015-1"), user));

        assertEquals(0L, jdbc.queryForObject("SELECT deleted_at FROM oort_task_event", Long.class));
        assertEquals(1, relationCount("oort_task_event_user", "tenant-a"));
        verifyNoInteractions(media, workflows);
    }

    @Test
    void listBatchesDistinctMediaAndKeepsHttpMissingAndExternalPicturesCompatible() {
        addEvent("first", "tenant-a", "camera", "{\"pics\":[\"vls-media://media-1\",\"https://example.invalid/legacy.jpg\"]}");
        addEvent("second", "tenant-a", "camera", "{\"pics\":[\"vls-media://media-1\",\"vls-media://missing\"]}");
        addEvent("external", "tenant-a", "camera", "{\"pics\":[],\"external_media\":true,\"media_id\":\"other-environment\"}");
        addEvent("foreign", "tenant-b", "camera", "{\"pics\":[\"vls-media://foreign\"]}");
        when(media.getPrivateViewUrls(eq("tenant-a"), anyCollection(), eq(3600)))
            .thenReturn(Collections.singletonMap("media-1", "https://example.invalid/signed.jpg"));

        Map<?, ?> page = (Map<?, ?>) service.eventList(body("mod_type", 2), user).getData();
        List<?> rows = (List<?>) page.get("list");
        assertEquals(3L, ((Number) page.get("count")).longValue());
        assertEquals(Arrays.asList("https://example.invalid/signed.jpg", "https://example.invalid/legacy.jpg"), pictures(rows, "first-20261015-1"));
        assertEquals(Arrays.asList("https://example.invalid/signed.jpg", "vls-media://missing"), pictures(rows, "second-20261015-1"));
        assertEquals(Collections.emptyList(), pictures(rows, "external-20261015-1"));
        verify(media).getPrivateViewUrls(eq("tenant-a"), argThat(ids -> ids.size() == 2
            && ids.contains("media-1") && ids.contains("missing")), eq(3600));
        verify(media, never()).getPrivateViewUrl(anyString(), anyInt());
        verifyNoInteractions(workflows);
    }

    @Test
    void mediaLookupFailureKeepsListAndStableReferenceAvailable() {
        addEvent("event", "tenant-a", "camera", "{\"pics\":[\"vls-media://media-1\"]}");
        when(media.getPrivateViewUrls(anyString(), anyCollection(), anyInt())).thenThrow(new IllegalStateException("synthetic failure"));
        Map<?, ?> page = (Map<?, ?>) service.eventList(body("mod_type", 2), user).getData();
        assertEquals(Collections.singletonList("vls-media://media-1"), pictures((List<?>) page.get("list"), "event-20261015-1"));
        verify(media, never()).getPrivateViewUrl(anyString(), anyInt());
    }

    @Test
    void signsVideoAlongWithPicturesAndPreservesSoftDeletedMedia() {
        addEvent("clip", "tenant-a", "camera", "{\"pics\":[],\"video\":[\"vls-media://video-1\"]}");
        addEvent("foreign-clip", "tenant-b", "camera", "{\"video\":[\"vls-media://foreign\"]}");
        when(media.getPrivateViewUrls(eq("tenant-a"), anyCollection(), eq(3600)))
            .thenReturn(Collections.singletonMap("video-1", "https://example.invalid/clip.mp4"));
        Map<?, ?> page = (Map<?, ?>) service.eventList(body("mod_type", 2), user).getData();
        List<?> rows = (List<?>) page.get("list");
        assertEquals(1, rows.size());
        Map<?, ?> row = (Map<?, ?>) rows.get(0);
        assertEquals("https://example.invalid/clip.mp4", row.get("videoUrl"));
        assertEquals(Collections.singletonList("https://example.invalid/clip.mp4"), row.get("video"));
        verify(media).getPrivateViewUrls(eq("tenant-a"), eq(Collections.singleton("video-1")), eq(3600));
        assertEquals(200, service.eventDelete(body("id", "clip-20261015-1"), user).getCode());
        assertTrue(jdbc.queryForObject("SELECT data FROM oort_task_event WHERE tenant_id='tenant-a'", String.class)
            .contains("vls-media://video-1"));
    }

    private void addEvent(String id, String tenant, String owner, String data) {
        jdbc.update("INSERT INTO oort_task_event (id,`no`,tenant_id,uuid,data,item) VALUES (?,1,?,?,?,'synthetic')",
            id + "-20261015", tenant, owner, data);
    }

    private void addRelations(String id, String tenant) {
        jdbc.update("INSERT INTO oort_task_event_user (task_event_id,tenant_id,uuid,u_type) VALUES (?,?,'synthetic',1)", id, tenant);
        jdbc.update("INSERT INTO oort_task_event_back VALUES (?,?,'synthetic')", id, tenant);
    }

    private int relationCount(String table, String tenant) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_id=?", Integer.class, tenant);
    }

    private static Object pictures(List<?> rows, String id) {
        return rows.stream().map(row -> (Map<?, ?>) row).filter(row -> id.equals(row.get("id")))
            .findFirst().orElseThrow(AssertionError::new).get("pics");
    }

    private static Map<String, Object> body(String key, Object value) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put(key, value);
        return body;
    }
}
