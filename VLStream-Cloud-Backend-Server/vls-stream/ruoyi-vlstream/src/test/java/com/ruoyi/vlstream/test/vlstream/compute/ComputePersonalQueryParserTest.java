package com.ruoyi.vlstream.test.vlstream.compute;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.ruoyi.common.helper.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Exercise the production tenant SQL parser, not only direct JDBC permission queries. */
@Tag("dev")
class ComputePersonalQueryParserTest {
    @BeforeEach void tenant() { TenantContextHolder.setTenantId("tenant-test"); }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }
    private ComputePublicTaskAccess access() {
        ComputePublicInventoryService inventory = mock(ComputePublicInventoryService.class);
        when(inventory.viewer()).thenReturn("platform-alice");
        return new ComputePublicTaskAccess(mock(JdbcTemplate.class), inventory);
    }

    private String parse(String table, QueryWrapper<Object> query) {
        String sql = "SELECT * FROM " + table + " "
            + query.getCustomSqlSegment().replaceAll("#\\{[^}]+\\}", "?");
        TenantLineInnerInterceptor interceptor = new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override public Expression getTenantId() { return new StringValue("tenant-test"); }
        });
        String parsed = assertDoesNotThrow(() -> interceptor.parserSingle(sql, null));
        assertTrue(parsed.contains("CAST("));
        assertTrue(parsed.contains("AS BINARY"));
        assertTrue(parsed.contains("public_owner_subject"));
        assertTrue(parsed.contains("'tenant-test'"));
        return parsed;
    }

    @Test void personalTrainingPageRetainsExactOwnershipThroughTenantParser() {
        QueryWrapper<Object> query = new QueryWrapper<>();
        query.eq("vls_algorithm_training.is_deleted", 0)
            .apply("CAST(vls_algorithm_training.tenant_id AS BINARY)=CAST({0} AS BINARY)", "tenant-test")
            .apply("CAST(vls_algorithm_training.create_user AS BINARY)=CAST({0} AS BINARY)", "alice");
        access().filter(query, "vls_algorithm_training.id");
        query.orderByDesc("create_time", "id");
        String parsed = parse("vls_algorithm_training", query);
        assertTrue(parsed.contains("JSON_VALID"));
        assertTrue(parsed.contains("SHARED_GPU_QUEUE"));
    }

    @Test void personalContainersRetainNestedLegacyAndSharedGuardsThroughTenantParser() {
        QueryWrapper<Object> query = new QueryWrapper<>();
        access().filterContainers(query);
        String parsed = parse("vls_container_instance", query);
        assertTrue(parsed.contains("lc.training_id"));
        assertTrue(parsed.contains("lt.create_user"));
    }
}
