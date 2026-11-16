package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsEventManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.EventManagementVO;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.apache.ibatis.session.SqlSession;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class EventReviewVisibilityTest {
    @Test
    void multiTenantMapperFiltersOnlyRejectedReviewsAndRestoresCorrectedEvents() throws Exception {
        JdbcDataSource database = new JdbcDataSource();
        database.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(database);
        jdbc.execute("CREATE TABLE vls_event_management (id BIGINT,tenant_id VARCHAR(64),is_deleted INT,create_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE vls_llm_review_task (tenant_id VARCHAR(64),formal_event_id VARCHAR(100),review_status VARCHAR(30))");
        jdbc.update("INSERT INTO vls_event_management VALUES (123,'tenant-a',0,CURRENT_TIMESTAMP),(456,'tenant-b',0,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO vls_llm_review_task VALUES ('tenant-b','123','REJECTED'),('tenant-a','123','MANUAL_REVIEW')");
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override public Expression getTenantId() { return new StringValue("tenant-a"); }
        }));
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(database);
        factory.setConfiguration(new MybatisConfiguration());
        factory.setPlugins(interceptor);
        factory.setMapperLocations(new FileSystemResource("src/main/java/com/ruoyi/vlstream/test/vlstream/mapper/VlsEventManagementMapper.xml"));
        try (SqlSession session = factory.getObject().openSession(true)) {
            VlsEventManagementMapper mapper = session.getMapper(VlsEventManagementMapper.class);
            assertEquals(1, mapper.selectVlsEventManagementPage(new Page<>(), new EventManagementVO()).size());
            jdbc.update("UPDATE vls_llm_review_task SET review_status='REJECTED' WHERE tenant_id='tenant-a'");
            session.clearCache();
            assertEquals(0, mapper.selectVlsEventManagementPage(new Page<>(), new EventManagementVO()).size());
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM vls_event_management WHERE tenant_id='tenant-a' AND "
                + VlsEventManagementMapper.EXCLUDE_REJECTED_REVIEWS, Integer.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM vls_llm_review_task", Integer.class));
            jdbc.update("UPDATE vls_llm_review_task SET review_status='CONFIRMED' WHERE tenant_id='tenant-a'");
            session.clearCache();
            assertEquals(1, mapper.selectVlsEventManagementPage(new Page<>(), new EventManagementVO()).size());
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM vls_event_management WHERE is_deleted=0", Integer.class));
        }
    }
}
