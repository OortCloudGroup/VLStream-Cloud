package com.ruoyi.web.controller.compat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Read-only display projection. Never writes current device coordinates into a historical event. */
@Service
public class EventLocationService {
    private final WvpVlStreamDeviceProperties properties;
    private final RestTemplate http;
    private final ObjectMapper json;

    @Autowired
    public EventLocationService(WvpVlStreamDeviceProperties properties, ObjectMapper json) {
        this(properties, json, client());
    }

    EventLocationService(WvpVlStreamDeviceProperties properties, ObjectMapper json, RestTemplate http) {
        this.properties = properties;
        this.json = json;
        this.http = http;
    }

    public void enrich(List<Map<String, Object>> events, String tenantId, String verifiedToken) {
        if (events.isEmpty() || tenantId == null || tenantId.trim().isEmpty()) return;
        Set<String> ids = new LinkedHashSet<>();
        for (Map<String, Object> event : events) {
            if (!tenantId.equals(text(event.get("tenant_id")))) continue;
            Map<String, Object> point = map(event.get("point"));
            if (text(point.get("address")).isEmpty() && coordinates(point) == null
                && !text(event.get("device_id")).isEmpty()) ids.add(text(event.get("device_id")));
        }
        Map<String, Map<String, Object>> devices = locations(ids, tenantId, verifiedToken);
        for (Map<String, Object> event : events) {
            if (!tenantId.equals(text(event.get("tenant_id")))) continue;
            Map<String, Object> point = new LinkedHashMap<>(map(event.get("point")));
            Map<String, Object> location = new LinkedHashMap<>();
            String address = text(point.get("address"));
            Coordinate coordinate = coordinates(point);
            location.put("source", "EVENT");
            location.put("observed_at", event.get("created_at"));
            location.put("status", address.isEmpty() ? "MISSING" : "RESOLVED");
            if (address.isEmpty() && coordinate == null) {
                location.put("source", "NONE");
                location.put("observed_at", null);
                Map<String, Object> device = devices.get(text(event.get("device_id")));
                if (device != null) {
                    location.put("heartbeat_at", device.get("heartbeatAt"));
                    location.put("status", text(device.get("status")));
                    Object received = device.get("heartbeatAt");
                    boolean knownTime = received instanceof Number && ((Number) received).longValue() > 0
                        && ((Number) received).longValue() <= System.currentTimeMillis();
                    if (knownTime && ("FRESH".equals(device.get("status")) || "STALE".equals(device.get("status")))) {
                        Map<String, Object> heartbeatPoint = new LinkedHashMap<>();
                        heartbeatPoint.put("lng", device.get("longitude"));
                        heartbeatPoint.put("lat", device.get("latitude"));
                        heartbeatPoint.put("coord_system_type", 1);
                        coordinate = coordinates(heartbeatPoint);
                        if (coordinate != null) {
                            location.put("source", "DEVICE_HEARTBEAT");
                            location.put("stale", "STALE".equals(device.get("status")));
                            point = heartbeatPoint;
                        }
                    }
                }
            }
            location.put("address", address);
            if (coordinate != null) {
                location.put("longitude", coordinate.lng);
                location.put("latitude", coordinate.lat);
                location.put("coordinate_system", coordinate.system);
                if (address.isEmpty()) {
                    location.put("status", "COORDINATES");
                    if (coordinate.system == 0) location.put("status", "UNKNOWN_COORDINATE_SYSTEM");
                }
            }
            // Keep point unchanged: legacy details/workflows retain the actual event position.
            event.put("display_location", location);
        }
    }

    private Map<String, Map<String, Object>> locations(Set<String> ids, String tenant, String verifiedToken) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        if (ids.isEmpty() || ids.size() > 100) return result;
        for (String id : ids) result.put(id, Collections.singletonMap("status", "UNAVAILABLE"));
        if (verifiedToken == null || verifiedToken.trim().isEmpty()) return result;
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("tenantId", tenant);
        request.put("deviceIds", ids);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-WVP-Auth-Source", "vlstream");
            headers.set("X-VLStream-Token", verifiedToken);
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
                javax.servlet.http.HttpServletRequest incoming = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
                for (String name : new String[]{"accesstoken", "requesttype", "appid", "secretkey", "tenantid"}) {
                    String value = incoming.getHeader(name);
                    if (value != null) headers.set(name, value);
                }
            }
            String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .pathSegment("internal", "vlstream", "device", "locations").toUriString();
            Map<String, Object> response = json.readValue(http.postForObject(url, new HttpEntity<>(request, headers), String.class), Map.class);
            if (!Integer.valueOf(200).equals(response.get("code")) || !(response.get("data") instanceof Collection)) return result;
            result.clear();
            for (Object item : (Collection<?>) response.get("data")) {
                Map<String, Object> device = map(item);
                String id = text(device.get("deviceId"));
                // Never match WVP row IDs, device names, serials or a returned foreign tenant.
                if (ids.contains(id) && tenant.equals(text(device.get("tenantId")))) result.put(id, device);
            }
        } catch (Exception ignored) {
            // Do not log RestTemplate exceptions: their URLs/bodies may contain secrets.
        }
        return result;
    }

    static Coordinate coordinates(Map<String, Object> point) {
        BigDecimal lng = decimal(point.get("lng")), lat = decimal(point.get("lat"));
        Integer system = integer(point.get("coord_system_type"));
        BigDecimal convertedLng = decimal(point.get("lng_change")), convertedLat = decimal(point.get("lat_change"));
        Integer convertedSystem = integer(point.get("coord_system_type_change"));
        if (convertedLng != null && convertedLat != null && convertedSystem != null && convertedSystem >= 1 && convertedSystem <= 3
            && convertedLng.abs().doubleValue() <= 180 && convertedLat.abs().doubleValue() <= 90) {
            return new Coordinate(convertedLng, convertedLat, convertedSystem);
        }
        if (lng == null || lat == null || lng.abs().compareTo(new BigDecimal("180")) > 0
            || lat.abs().compareTo(new BigDecimal("90")) > 0) return null;
        // The legacy pointValue manufactured (0,0,type=0) for absent values.
        // An explicitly typed WGS84/GCJ02/BD09 zero coordinate remains valid.
        if ((system == null || system == 0) && lng.signum() == 0 && lat.signum() == 0) return null;
        return new Coordinate(lng, lat, system == null || system < 1 || system > 3 ? 0 : system);
    }

    static BigDecimal decimal(Object value) {
        if (!(value instanceof Number) && !(value instanceof String)) return null;
        try { return new BigDecimal(value.toString().trim()); } catch (NumberFormatException ignored) { return null; }
    }

    private static Integer integer(Object value) {
        BigDecimal number = decimal(value);
        try { return number == null ? null : number.intValueExact(); } catch (ArithmeticException ignored) { return null; }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }

    static String text(Object value) { return value instanceof String ? ((String) value).trim() : ""; }

    static RestTemplate client() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(1500);
        return new RestTemplate(factory);
    }

    static final class Coordinate {
        final BigDecimal lng;
        final BigDecimal lat;
        final int system;
        Coordinate(BigDecimal lng, BigDecimal lat, int system) { this.lng = lng; this.lat = lat; this.system = system; }
    }
}
