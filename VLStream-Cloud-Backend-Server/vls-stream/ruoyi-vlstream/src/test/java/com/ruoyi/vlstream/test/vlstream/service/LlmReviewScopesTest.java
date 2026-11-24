package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.mapper.LlmReviewTaskMapper;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class LlmReviewScopesTest {
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void workerClaimsOnlyItsSinkAndCompatibleLegacyTasks() throws Exception {
        DriverManagerDataSource data = new DriverManagerDataSource(
            "jdbc:h2:mem:review_worker_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(data);
        jdbc.execute("CREATE TABLE vls_llm_review_task(id BIGINT PRIMARY KEY, event_sink VARCHAR, review_status VARCHAR,"
            + " locked_by VARCHAR, locked_at TIMESTAMP, update_time TIMESTAMP, next_retry_time TIMESTAMP)");
        jdbc.execute("INSERT INTO vls_llm_review_task(id,event_sink,review_status,next_retry_time) VALUES"
            + "(1,'LOCAL','PENDING',CURRENT_TIMESTAMP),(2,'VLS','PENDING',CURRENT_TIMESTAMP),(3,NULL,'PENDING',CURRENT_TIMESTAMP)");
        Configuration config = new Configuration(
            new Environment("review", new JdbcTransactionFactory(), data));
        config.addMapper(LlmReviewTaskMapper.class);
        try (SqlSession session = new SqlSessionFactoryBuilder()
                .build(config).openSession(true)) {
            LlmReviewTaskMapper mapper =
                session.getMapper(LlmReviewTaskMapper.class);
            Date now = new Date(System.currentTimeMillis() + 1000);
            assertEquals(2, mapper.claimBatch("local", now, new Date(0), 10, "LOCAL"));
            assertEquals(1, mapper.claimBatch("multi", now, new Date(0), 10, "VLS"));
            assertEquals("multi", jdbc.queryForObject("SELECT locked_by FROM vls_llm_review_task WHERE id=2", String.class));
        }
    }

    @Test void matchesCurrentMembershipAndIndependentSwitchesWithinTenant() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:scopes_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE vls_llm_review_scope(tenant_id VARCHAR,scope_key VARCHAR,PRIMARY KEY(tenant_id,scope_key))");
        LlmReviewScopes scopes = new LlmReviewScopes(jdbc, null, null);
        TenantContextHolder.setTenantId("tenant-a");
        DeviceInfo device = new DeviceInfo();
        device.setTenantId("tenant-a"); device.setDeviceId("camera");
        for (String key : Arrays.asList("REGION:1","GROUP:2","TAG:3")) {
            scopes.setEnabled(key, true);
            assertThrows(RuntimeException.class, () -> scopes.matchesDevice(device));
            device.setCategoryKeys(Collections.emptyList());
            assertFalse(scopes.matchesDevice(device));
            device.setCategoryKeys(Collections.singletonList(key));
            assertTrue(scopes.matchesDevice(device));
            device.setTenantId("tenant-b");
            assertFalse(scopes.matchesDevice(device));
            device.setTenantId("tenant-a");
            scopes.setEnabled("DEVICE:camera", true);
            scopes.setEnabled(key, false);
            assertTrue(scopes.matchesDevice(device));
            scopes.setEnabled("DEVICE:camera", false);
            assertFalse(scopes.matchesDevice(device));
            device.setCategoryKeys(null);
        }
        scopes.setEnabled("SCENE:a", true);
        assertFalse(scopes.matchesDevice(device));
        assertFalse(scopes.enabled("tenant-a", "SCENE:b"));
        assertTrue(scopes.enabled("tenant-a", "SCENE:a"));
    }
}
