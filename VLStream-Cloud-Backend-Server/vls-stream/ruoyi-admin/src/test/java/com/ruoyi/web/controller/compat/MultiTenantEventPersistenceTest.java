package com.ruoyi.web.controller.compat;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.framework.config.CustomTenantLineHandler;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.config.VlsEventReportProperties;
import com.ruoyi.vlstream.test.vlstream.controller.VlsEventManagementController;
import com.ruoyi.vlstream.test.vlstream.service.IVlsDeviceInfoService;
import com.ruoyi.vlstream.test.vlstream.service.VlsDeviceEventReportService;
import com.ruoyi.vlstream.test.vlstream.job.EventReportOutboxWorker;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsEventManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.EventManagementVO;
import com.ruoyi.vlstream.test.vlstream.service.DeviceMediaUploadService;
import com.ruoyi.vlstream.test.vlstream.service.EventReportDeliveryStateService;
import com.ruoyi.vlstream.test.vlstream.service.ThirdPartyEventReportClient;
import com.ruoyi.vlstream.test.vlstream.service.VlsEventReportApplicationService;
import com.ruoyi.vlstream.test.vlstream.service.impl.VlsEventManagementServiceImpl;
import com.sun.net.httpserver.HttpServer;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real tenant SQL, transactions, outbox worker and loopback HTTP; no shared database or MQTT broker. */
@Tag("dev")
class MultiTenantEventPersistenceTest {
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactions;
    private VlsEventManagementMapper eventMapper;
    private EventReportOutboxMapper outboxMapper;
    private VlsEventReportApplicationService application;
    private VlsEventManagementServiceImpl events;
    private HttpServer server;

    @BeforeEach void setup() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:multi-event-" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        transactions = new DataSourceTransactionManager(source);
        jdbc.execute("CREATE TABLE vls_event_management (id BIGINT PRIMARY KEY, tenant_id VARCHAR(64), mqtt_message_id VARCHAR(128), "
            + "device_event_id VARCHAR(128), media_id VARCHAR(64), event_desc VARCHAR(1000), event_type VARCHAR(100), "
            + "report_location VARCHAR(255), report_device VARCHAR(100), report_img VARCHAR(1000), report_time TIMESTAMP, "
            + "event_level VARCHAR(30), event_status VARCHAR(30), executor VARCHAR(100), executor_ids VARCHAR(255), event_data VARCHAR(4000), handle_result VARCHAR(1000), "
            + "feedback_info VARCHAR(1000), feedback_img VARCHAR(1000), feedback_status INT, is_report INT, create_user VARCHAR(64), "
            + "create_dept VARCHAR(64), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_user VARCHAR(64), "
            + "update_time TIMESTAMP, status INT DEFAULT 1, is_deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE vls_event_report_outbox (id BIGINT PRIMARY KEY, tenant_id VARCHAR(64), event_id BIGINT, "
            + "idempotency_key VARCHAR(128), payload_json VARCHAR(4000), status VARCHAR(20), retry_count INT, "
            + "next_retry_time TIMESTAMP, locked_by VARCHAR(64), locked_at TIMESTAMP, last_error VARCHAR(2000), "
            + "reported_at TIMESTAMP, create_time TIMESTAMP, update_time TIMESTAMP)");
        TokenProperties token = new TokenProperties(); token.setTenantType("multi");
        CustomTenantLineHandler tenant = new CustomTenantLineHandler(token);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenant));
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true); configuration.addInterceptor(interceptor);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source); factory.setConfiguration(configuration);
        org.apache.ibatis.session.SqlSessionFactory built = factory.getObject();
        built.getConfiguration().addMapper(VlsEventManagementMapper.class);
        built.getConfiguration().addMapper(EventReportOutboxMapper.class);
        tenant.afterSingletonsInstantiated();
        SqlSessionTemplate session = new SqlSessionTemplate(built);
        eventMapper = session.getMapper(VlsEventManagementMapper.class);
        outboxMapper = session.getMapper(EventReportOutboxMapper.class);
        events = new VlsEventManagementServiceImpl();
        ReflectionTestUtils.setField(events, "baseMapper", eventMapper);
        VlsEventReportApplicationService target = new VlsEventReportApplicationService(events, eventMapper, outboxMapper, mock(IFileUploadService.class));
        ReflectionTestUtils.setField(target, "tenantType", "multi");
        application = transactional(target);
        TenantContextHolder.setTenantId("tenant-a");
    }

    @AfterEach void cleanup() { if (server != null) server.stop(0); TenantContextHolder.clear(); }

    @Test void feedbackPatchUpdatesOnlyDispositionAndKeepsOmittedExecutorAndDeliveryState() throws Exception {
        Long id = createStatusFixture();
        jdbc.update("UPDATE vls_event_management SET executor='existing-handler' WHERE id=?", id);
        statusHttp().perform(patch("/vlsEventManagement/{id}/status", id)
                .param("status", "processing").param("handleResult", "checked locally"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").value(true));
        assertEquals("processing", jdbc.queryForObject("SELECT event_status FROM vls_event_management WHERE id=?", String.class, id));
        assertEquals("checked locally", jdbc.queryForObject("SELECT handle_result FROM vls_event_management WHERE id=?", String.class, id));
        assertEquals("existing-handler", jdbc.queryForObject("SELECT executor FROM vls_event_management WHERE id=?", String.class, id));
        assertEquals("{}", jdbc.queryForObject("SELECT event_data FROM vls_event_management WHERE id=?", String.class, id));
        assertEquals("message-1", jdbc.queryForObject("SELECT mqtt_message_id FROM vls_event_management WHERE id=?", String.class, id));
        assertEquals(0, jdbc.queryForObject("SELECT is_report FROM vls_event_management WHERE id=?", Integer.class, id));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM vls_event_report_outbox WHERE event_id=?", String.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM vls_event_report_outbox", Integer.class));
    }

    @Test void feedbackPatchCannotChangeAnotherTenantDeletedOrMissingEvent() throws Exception {
        Long id = createStatusFixture();
        TenantContextHolder.setTenantId("tenant-b");
        statusHttp().perform(patch("/vlsEventManagement/{id}/status", id).param("status", "completed"))
            .andExpect(jsonPath("$.code").value(404));
        TenantContextHolder.clear();
        statusHttp().perform(patch("/vlsEventManagement/{id}/status", id).param("status", "completed"))
            .andExpect(jsonPath("$.code").value(404));
        TenantContextHolder.setTenantId("tenant-a");
        jdbc.update("UPDATE vls_event_management SET is_deleted=1 WHERE id=?", id);
        statusHttp().perform(patch("/vlsEventManagement/{id}/status", id).param("status", "completed"))
            .andExpect(jsonPath("$.code").value(404));
        statusHttp().perform(patch("/vlsEventManagement/1/status").param("status", "completed"))
            .andExpect(jsonPath("$.code").value(404));
        assertEquals("pending", jdbc.queryForObject("SELECT event_status FROM vls_event_management WHERE id=?", String.class, id));
    }

    @Test void feedbackPatchRejectsInvalidOrMissingStatusWithoutChangingTheEvent() throws Exception {
        Long id = createStatusFixture();
        statusHttp().perform(patch("/vlsEventManagement/{id}/status", id).param("status", "invalid"))
            .andExpect(status().isBadRequest());
        statusHttp().perform(patch("/vlsEventManagement/{id}/status", id))
            .andExpect(status().isBadRequest());
        assertEquals("pending", jdbc.queryForObject("SELECT event_status FROM vls_event_management WHERE id=?", String.class, id));
    }

    private Long createStatusFixture() {
        DeviceInfo device = new DeviceInfo(); device.setDeviceId("CAM-1"); device.setTenantId("tenant-a");
        return application.persistActiveSafetyEvent(report(), device).getId();
    }

    private MockMvc statusHttp() {
        return MockMvcBuilders.standaloneSetup(new VlsEventManagementController(events,
            mock(IVlsDeviceInfoService.class), application, mock(VlsDeviceEventReportService.class))).build();
    }

    @Test void persistsVisibleTenantEventThenRecordsOnlyBusinessConfirmedDelivery() throws Exception {
        DeviceInfo device = new DeviceInfo(); device.setDeviceId("CAM-1"); device.setTenantId("tenant-a");
        application.persistActiveSafetyEvent(report(), device);
        assertEquals(1, eventMapper.selectVlsEventManagementPage(new Page<EventManagementVO>(1, 10), new EventManagementVO()).size());
        TenantContextHolder.setTenantId("tenant-b");
        assertEquals(0, eventMapper.selectVlsEventManagementPage(new Page<EventManagementVO>(1, 10), new EventManagementVO()).size());
        TenantContextHolder.clear();
        AtomicReference<String> response = new AtomicReference<>("{\"code\":4101,\"msg\":\"参数错误 name 长度错误,name 的长度必须小于 10\"}");
        AtomicReference<JSONObject> request = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/report", exchange -> {
            ByteArrayOutputStream body = new ByteArrayOutputStream(); byte[] buffer = new byte[1024]; int n;
            while ((n = exchange.getRequestBody().read(buffer)) != -1) body.write(buffer, 0, n);
            request.set(JSONUtil.parseObj(new String(body.toByteArray(), StandardCharsets.UTF_8)));
            byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        VlsEventReportProperties properties = new VlsEventReportProperties();
        properties.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/report");
        EventReportDeliveryStateService state = transactional(new EventReportDeliveryStateService(outboxMapper, eventMapper));
        EventReportOutboxWorker worker = new EventReportOutboxWorker(outboxMapper, state,
            new ThirdPartyEventReportClient(properties), mock(DeviceMediaUploadService.class), properties);
        ReflectionTestUtils.setField(worker, "tenantType", "multi");
        worker.deliverPendingEvents();
        assertEquals("RETRY", jdbc.queryForObject("SELECT status FROM vls_event_report_outbox", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT is_report FROM vls_event_management", Integer.class));
        TenantContextHolder.setTenantId("tenant-a");
        assertEquals(1, eventMapper.selectVlsEventManagementPage(new Page<EventManagementVO>(1, 10), new EventManagementVO()).size(),
            "External business rejection must not hide a locally accepted event from its owning tenant");
        TenantContextHolder.clear();
        // This is a synthetic in-memory retry deadline, never a shared outbox replay.
        jdbc.update("UPDATE vls_event_report_outbox SET next_retry_time=?", new Date(0));
        response.set("{\"code\":200}");
        worker.deliverPendingEvents();
        assertEquals("SUCCESS", jdbc.queryForObject("SELECT status FROM vls_event_report_outbox", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT is_report FROM vls_event_management", Integer.class));
        assertEquals("tenant-a", request.get().getJSONArray("event_report").getJSONObject(0).getStr("device_tenant_id"));
        assertEquals("IEV1805", request.get().getJSONArray("event_report").getJSONObject(0).getStr("item"));
        assertNull(TenantContextHolder.getTenantId());
    }

    @Test void listIncludesUnreportedOwnedEventsButExcludesDeletedAndOtherTenantEvents() {
        DeviceInfo ownDevice = new DeviceInfo(); ownDevice.setDeviceId("CAM-1"); ownDevice.setTenantId("tenant-a");
        application.persistActiveSafetyEvent(report(), ownDevice);
        com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement deleted = application.persistActiveSafetyEvent(report(), ownDevice);
        jdbc.update("UPDATE vls_event_management SET is_deleted=1 WHERE id=?", deleted.getId());
        DeviceInfo otherDevice = new DeviceInfo(); otherDevice.setDeviceId("CAM-1"); otherDevice.setTenantId("tenant-b");
        TenantContextHolder.setTenantId("tenant-b");
        application.persistActiveSafetyEvent(report(), otherDevice);
        TenantContextHolder.setTenantId("tenant-a");
        assertEquals(1, eventMapper.selectVlsEventManagementPage(new Page<EventManagementVO>(1, 10), new EventManagementVO()).size());
        TenantContextHolder.setTenantId("tenant-with-no-events");
        assertEquals(0, eventMapper.selectVlsEventManagementPage(new Page<EventManagementVO>(1, 10), new EventManagementVO()).size());
        TenantContextHolder.clear();
        assertEquals(0, eventMapper.selectVlsEventManagementPage(new Page<EventManagementVO>(1, 10), new EventManagementVO()).size());
    }

    @Test void outboxFailureRollsBackTheEventInsert() {
        jdbc.execute("ALTER TABLE vls_event_report_outbox ADD CONSTRAINT reject_fixture CHECK (event_id < 0)");
        DeviceInfo device = new DeviceInfo(); device.setDeviceId("CAM-1"); device.setTenantId("tenant-a");
        assertThrows(RuntimeException.class, () -> application.persistActiveSafetyEvent(report(), device));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM vls_event_management", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM vls_event_report_outbox", Integer.class));
    }

    @Test void cameraBatchPersistsEachOwnedTenantAndRestoresTheCallingContext() {
        TenantContextHolder.setTenantId("ui-tenant");
        application.persistCameraEvents(java.util.Arrays.asList(camera("CAM-1","tenant-a"),camera("CAM-2","tenant-b")));
        assertEquals("ui-tenant",TenantContextHolder.getTenantId());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_event_management WHERE tenant_id='tenant-a'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_event_report_outbox WHERE tenant_id='tenant-b'",Integer.class));
    }

    @Test void failureInTheSecondCameraRollsBackTheWholeBatch() {
        jdbc.execute("ALTER TABLE vls_event_report_outbox ADD CONSTRAINT reject_second CHECK (tenant_id <> 'tenant-b')");
        TenantContextHolder.setTenantId("ui-tenant");
        assertThrows(RuntimeException.class,()->application.persistCameraEvents(java.util.Arrays.asList(camera("CAM-1","tenant-a"),camera("CAM-2","tenant-b"))));
        assertEquals("ui-tenant",TenantContextHolder.getTenantId());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_event_management",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_event_report_outbox",Integer.class));
    }

    private com.ruoyi.vlstream.test.vlstream.pojo.dto.ResolvedCameraEvent camera(String id,String tenant) {
        DeviceInfo device=new DeviceInfo();device.setDeviceId(id);device.setTenantId(tenant);
        com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement event=new com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement();
        event.setReportDevice(id);event.setEventType("IEV1805");
        return new com.ruoyi.vlstream.test.vlstream.pojo.dto.ResolvedCameraEvent(event,device,
            new JSONObject().set("item","IEV1805").set("device_tenant_id","untrusted-overwritten").set("pics",Collections.emptyList()));
    }

    @SuppressWarnings("unchecked") private <T> T transactional(T target) {
        ProxyFactory factory = new ProxyFactory(target); factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return (T) factory.getProxy();
    }

    private ActiveSafetyEventReport report() {
        return ActiveSafetyEventReport.builder().sourceMessageId("message-1").deviceEventId("event-1").deviceId("CAM-1")
            .deviceName("camera").mediaId("external-image-1").eventType("IEV1805").description("a complete long description")
            .eventTime(new Date()).mqttPayloadJson("{}").externalMedia(true)
            .imageMediaIds(Collections.emptyList()).videoMediaIds(Collections.emptyList()).build();
    }
}
