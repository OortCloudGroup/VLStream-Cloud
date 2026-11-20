package com.ruoyi.framework.config;

import com.ruoyi.common.helper.TenantContextHolder;
import java.util.Collections;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class TrainingPageTenantInterceptorTest {
    private final Configuration configuration = new Configuration();
    private final TrainingPageTenantInterceptor interceptor = new TrainingPageTenantInterceptor(new CustomTenantLineHandler(null) {
        @Override public boolean ignoreTable(String name) { return false; }
    });
    private final MappedStatement statement = new MappedStatement.Builder(configuration,
        "com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmTrainingMapper.selectVlsAlgorithmTrainingPage",
        new StaticSqlSource(configuration, "SELECT 1"), SqlCommandType.SELECT).build();

    private String rewrite(String tenant, String sql) throws Exception {
        TenantContextHolder.setTenantId(tenant);
        BoundSql bound = new BoundSql(configuration, sql, Collections.emptyList(), null);
        interceptor.beforeQuery(null, statement, null, null, null, bound);
        return bound.getSql();
    }

    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void reusesOnlyMatchingTenantAndSqlShape() throws Exception {
        String sql = "SELECT * FROM vls_algorithm_training WHERE create_user = ?";
        String first = rewrite("tenant-a", sql);
        assertTrue(first.contains("'tenant-a'"));
        assertSame(first, rewrite("tenant-a", sql));
        String second = rewrite("tenant-b", sql);
        assertTrue(second.contains("'tenant-b'"));
        assertFalse(second.contains("'tenant-a'"));
        String filtered = rewrite("tenant-a", sql + " AND task_name LIKE ?");
        assertTrue(filtered.contains("task_name LIKE ?"));
        assertTrue(filtered.contains("create_user = ?"));
    }
}
