package com.ruoyi.web.controller.compat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@Tag("dev")
class EventLocationServiceTest {
    private final RestTemplate http = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();
    private final EventLocationService service = new EventLocationService(new WvpVlStreamDeviceProperties(),
        new ObjectMapper(), http);

    static Map<String, Object> row(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) result.put((String) entries[i], entries[i + 1]);
        return result;
    }

    private Map<String, Object> event(String device, Map<String, Object> point) {
        return row("tenant_id", "tenant-a", "device_id", device, "point", point, "created_at", "2026-01-01 00:00:00");
    }

    @Test void preservesEventAddressAndCoordinatesWithoutConsultingCurrentDevice() {
        Map<String, Object> original = row("address", "历史事件地址", "lng", 114.1, "lat", 22.5, "coord_system_type", 1);
        Map<String, Object> event = event("business-id", original);
        service.enrich(Collections.singletonList(event), "tenant-a", "synthetic-token");
        Map<String, Object> location = EventLocationService.map(event.get("display_location"));
        assertEquals("历史事件地址", location.get("address"));
        assertEquals("EVENT", location.get("source"));
        assertSame(original, event.get("point"));
        server.verify();
    }

    @Test void batchesDistinctBusinessIdsAndRejectsForeignTenantAndWrongId() {
        server.expect(requestTo("http://127.0.0.1:9080/internal/vlstream/device/locations"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-WVP-Auth-Source", "vlstream"))
            .andExpect(header("X-VLStream-Token", "synthetic-token"))
            .andExpect(content().json("{\"tenantId\":\"tenant-a\",\"deviceIds\":[\"A\",\"B\"]}"))
            .andRespond(withSuccess("{\"code\":200,\"data\":["
                + "{\"deviceId\":\"A\",\"tenantId\":\"tenant-a\",\"longitude\":0,\"latitude\":0,\"heartbeatAt\":123,\"status\":\"FRESH\"},"
                + "{\"deviceId\":\"B\",\"tenantId\":\"tenant-b\",\"longitude\":100,\"latitude\":20,\"status\":\"FRESH\"},"
                + "{\"deviceId\":\"1\",\"tenantId\":\"tenant-a\",\"longitude\":100,\"latitude\":20,\"status\":\"FRESH\"}]}" , MediaType.APPLICATION_JSON));
        Map<String, Object> first = event("A", row()), duplicate = event("A", row()), foreign = event("B", row());
        service.enrich(Arrays.asList(first, duplicate, foreign), "tenant-a", "synthetic-token");
        Map<String, Object> location = EventLocationService.map(first.get("display_location"));
        assertEquals("DEVICE_HEARTBEAT", location.get("source"));
        assertEquals(0, ((Number) location.get("longitude")).intValue());
        assertNull(location.get("observed_at"));
        assertEquals(123, location.get("heartbeat_at"));
        assertEquals("NONE", EventLocationService.map(foreign.get("display_location")).get("source"));
        assertTrue(EventLocationService.map(first.get("point")).isEmpty());
        server.verify();
    }

    @Test void staleCoordinatesRemainLabeledLastKnownAndInvalidCoordinatesAreExcluded() {
        server.expect(anything()).andRespond(withSuccess("{\"code\":200,\"data\":["
            + "{\"deviceId\":\"A\",\"tenantId\":\"tenant-a\",\"longitude\":114,\"latitude\":22,\"heartbeatAt\":123,\"status\":\"STALE\"},"
            + "{\"deviceId\":\"B\",\"tenantId\":\"tenant-a\",\"longitude\":181,\"latitude\":22,\"status\":\"FRESH\"}]}" , MediaType.APPLICATION_JSON));
        Map<String, Object> stale = event("A", row()), invalid = event("B", row());
        service.enrich(Arrays.asList(stale, invalid), "tenant-a", "synthetic-token");
        assertEquals(true, EventLocationService.map(stale.get("display_location")).get("stale"));
        assertEquals("DEVICE_HEARTBEAT", EventLocationService.map(stale.get("display_location")).get("source"));
        assertEquals(114, ((Number) EventLocationService.map(stale.get("display_location")).get("longitude")).intValue());
        assertFalse(EventLocationService.map(invalid.get("display_location")).containsKey("longitude"));
    }

    @Test void wvpFailureDegradesWithoutFailingEventListAndNoUntrustedTenantIsRead() {
        server.expect(anything()).andRespond(withServerError());
        Map<String, Object> missing = event("A", row());
        Map<String, Object> foreign = event("B", row());
        foreign.put("tenant_id", "tenant-b");
        assertDoesNotThrow(() -> service.enrich(Arrays.asList(missing, foreign), "tenant-a", "synthetic-token"));
        assertEquals("UNAVAILABLE", EventLocationService.map(missing.get("display_location")).get("status"));
        assertFalse(foreign.containsKey("display_location"));
        server.verify();
    }

    @Test void usesStructuredZeroCoordinatesAndConvertedPairWithoutDoubleConversion() {
        EventLocationService.Coordinate zero = EventLocationService.coordinates(row("lng", 0, "lat", 0, "coord_system_type", 1));
        assertNotNull(zero);
        assertNull(EventLocationService.coordinates(row("lng", 0, "lat", 0, "coord_system_type", 0)));
        EventLocationService.Coordinate converted = EventLocationService.coordinates(row("lng", 114, "lat", 22,
            "coord_system_type", 1, "lng_change", 0, "lat_change", 0, "coord_system_type_change", 2));
        assertEquals(2, converted.system);
        assertEquals(0, converted.lng.intValue());
        assertNull(EventLocationService.coordinates(row("lng", "经度114.1177", "lat", 22)));
        assertNull(EventLocationService.coordinates(row("lng", "NaN", "lat", 22)));
        assertNull(EventLocationService.coordinates(row("lng", true, "lat", 22)));
    }

    @Test void unknownEventCoordinateSystemPreservesCoordinatesAndDoesNotGuessHeartbeat() {
        Map<String, Object> event = event("A", row("lng", 114, "lat", 22, "coord_system_type", 0));
        service.enrich(Collections.singletonList(event), "tenant-a", "synthetic-token");
        assertEquals("EVENT", EventLocationService.map(event.get("display_location")).get("source"));
        assertEquals("UNKNOWN_COORDINATE_SYSTEM", EventLocationService.map(event.get("display_location")).get("status"));
        server.verify();
    }

    @Test void missingVerifiedTokenDoesNotQueryDeviceLocations() {
        Map<String, Object> event = event("A", row());
        service.enrich(Collections.singletonList(event), "tenant-a", "");
        assertEquals("UNAVAILABLE", EventLocationService.map(event.get("display_location")).get("status"));
        server.verify();
    }
}
