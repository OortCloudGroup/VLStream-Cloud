package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsModelDispatchTaskMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask;
import org.apache.ibatis.session.SqlSession;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Actual mapper/conditional SQL against an isolated H2 store, with no application or MQTT startup. */
@Tag("dev")
class ModelDispatchPersistenceTest {
    JdbcTemplate jdbc;
    SqlSession session;
    ModelDispatchTaskService service;
    ModelDispatchTask task;
    @BeforeEach void setup() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:dispatch-" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        String sql = new String(Files.readAllBytes(Paths.get("../ruoyi-admin/src/main/resources/db/migration/V1_1_3_001__model_dispatch_protocol_v2.sql")), StandardCharsets.UTF_8);
        sql = sql.substring(0, sql.indexOf(';')).replaceAll("(?s)\\) ENGINE=.*", ")");
        jdbc.execute(sql);
        for (String column : new String[]{"class_file_name VARCHAR(255)", "class_file_size BIGINT", "class_file_sha256 CHAR(64)", "class_file_content TEXT"}) {
            jdbc.execute("ALTER TABLE vls_model_dispatch_task ADD " + column);
        }
        MybatisConfiguration configuration = new MybatisConfiguration(); configuration.setMapUnderscoreToCamelCase(true);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean(); factory.setDataSource(source); factory.setConfiguration(configuration);
        org.apache.ibatis.session.SqlSessionFactory built = factory.getObject(); built.getConfiguration().addMapper(VlsModelDispatchTaskMapper.class);
        session = built.openSession(true);
        service = new ModelDispatchTaskService(); Field mapper = service.getClass().getDeclaredField("taskMapper"); mapper.setAccessible(true);
        mapper.set(service, session.getMapper(VlsModelDispatchTaskMapper.class));
        task = new ModelDispatchTask(); task.setRequestId("request-1"); task.setMqttMessageId("message-1"); task.setTenantId("tenant-a");
        task.setDeviceRowId(101L); task.setDeviceId("CAM-A"); task.setAlgorithmId(7L); task.setTrainingId(70L); task.setModelType("om");
        task.setRemotePath("fixture.om"); task.setFileName("fixture.om"); task.setFileSize(10L); task.setSha256(String.join("", java.util.Collections.nCopies(64, "a")));
        task.setClassFileSha256(String.join("", java.util.Collections.nCopies(64, "b"))); task.setDispatchStatus("CREATED"); task.setDownloadExpiresAt(1800L);
        service.create(task);
    }
    @AfterEach void cleanup() { if (session != null) session.close(); }
    String status() { session.clearCache(); return service.getByRequestId("request-1").getDispatchStatus(); }
    void hardware(String status) { session.clearCache(); assertTrue(service.applyHardwareReply("message-1", "request-1", "CAM-A", status, task.getSha256(), task.getClassFileSha256(), "fixture", "{}")); }
    @Test void lateLocalPublishAndDownloadsNeverRegressHardwareProgressOrSuccess() {
        hardware("DEPLOYING"); service.markPublished("request-1", "fixture"); service.markDownloadStarted("request-1"); service.markDownloadCompleted("request-1");
        assertEquals("DEPLOYING", status()); hardware("SUCCESS");
        service.markPublished("request-1", "fixture"); service.markUncertain("request-1", "transport lost"); service.markDownloadCompleted("request-1");
        assertEquals("SUCCESS", status());
    }
    @Test void terminalFailureIsStickyAndOnlyMatchingReplyCanConfirmSuccess() {
        assertFalse(service.applyHardwareReply("wrong-message", "request-1", "CAM-A", "SUCCESS", task.getSha256(), task.getClassFileSha256(), "bad", "{}"));
        assertFalse(service.applyHardwareReply("message-1", "request-1", "CAM-B", "SUCCESS", task.getSha256(), task.getClassFileSha256(), "bad", "{}"));
        assertEquals("CREATED", status()); hardware("FAILED"); hardware("SUCCESS"); assertEquals("FAILED", status());
    }
    @Test void bothHashesAreRequiredAndUncertainTaskRetainsDeviceGuard() {
        service.markUncertain("request-1", "fixture timeout"); assertEquals("UNKNOWN", status());
        assertThrows(com.ruoyi.common.exception.ServiceException.class, () -> service.requireNoActiveTask("CAM-A"));
        session.clearCache(); assertTrue(service.applyHardwareReply("message-1", "request-1", "CAM-A", "SUCCESS", task.getSha256(), "wrong", "bad classes", "{}"));
        assertEquals("FAILED", status()); assertDoesNotThrow(() -> service.requireNoActiveTask("CAM-A"));
    }
    @Test void duplicateSuccessDoesNotChangeConfirmedDeploymentTimeAndOldProgressIsIgnored() {
        hardware("SUCCESS"); Object deployed = jdbc.queryForObject("SELECT deployed_at FROM vls_model_dispatch_task WHERE request_id='request-1'", Object.class);
        hardware("RECEIVED"); hardware("SUCCESS");
        assertEquals("SUCCESS", status()); assertEquals(deployed, jdbc.queryForObject("SELECT deployed_at FROM vls_model_dispatch_task WHERE request_id='request-1'", Object.class));
    }
}
