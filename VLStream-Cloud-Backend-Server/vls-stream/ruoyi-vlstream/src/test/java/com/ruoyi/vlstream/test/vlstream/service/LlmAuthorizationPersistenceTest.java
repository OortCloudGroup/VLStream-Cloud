package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmProviderMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import org.apache.ibatis.session.SqlSession;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real mapper updates against an isolated database, including cancellation races. */
@Tag("dev")
class LlmAuthorizationPersistenceTest {
    private JdbcTemplate jdbc;
    private SqlSession session;
    private LlmSystemProviderService service;
    private LlmApiKeyCipher cipher;

    @BeforeEach void setup() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:llm-" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        String sql = new String(Files.readAllBytes(Paths.get("../ruoyi-admin/src/main/resources/db/migration/V1_2_0_006__algorithm_llm_review.sql")), StandardCharsets.UTF_8);
        jdbc.execute(sql.substring(0, sql.indexOf(';')).replaceAll("(?s)\\) ENGINE=.*", ")"));
        for (String column : new String[] {"platform_user_id varchar(128)", "platform_user_name varchar(200)", "authorized_at datetime"}) {
            jdbc.execute("ALTER TABLE vls_llm_provider ADD " + column);
        }
        VlsLlmReviewProperties properties = new VlsLlmReviewProperties();
        jdbc.update("INSERT INTO vls_llm_provider (id,tenant_id,name,base_url,model_name,api_key_ciphertext,enabled,timeout_seconds) VALUES (1,'fixture','OortCloud',?,?, '',1,?)",
            properties.getBaseUrl(), properties.getModelName(), properties.getTimeoutSeconds());
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source); factory.setConfiguration(configuration);
        org.apache.ibatis.session.SqlSessionFactory built = factory.getObject();
        built.getConfiguration().addMapper(LlmProviderMapper.class);
        session = built.openSession(true);
        cipher = mock(LlmApiKeyCipher.class);
        service = new LlmSystemProviderService(session.getMapper(LlmProviderMapper.class), properties, cipher);
    }

    @AfterEach void cleanup() { if (session != null) session.close(); }

    @Test void cancellationSurvivesReloadAndDefaultLoginUntilExplicitRestore() {
        when(cipher.encrypt("sk-first")).thenReturn("cipher-first");
        assertTrue(service.isAuthorized(service.authorize("first-user", "First", "first", true)));
        service.selectModel("chosen-vision");
        LlmProvider cancelled = service.revoke();
        assertFalse(service.isAuthorized(cancelled));
        assertFalse(cancelled.getEnabled());
        assertEquals("", cancelled.getApiKeyCiphertext());
        assertNull(cancelled.getPlatformUserId());
        assertNull(cancelled.getAuthorizedAt());
        session.clearCache();
        assertFalse(service.isAuthorized(service.authorize("second-user", "Second", "second", true)));
        verify(cipher, never()).encrypt("sk-second");
        when(cipher.encrypt("sk-second")).thenReturn("cipher-second");
        LlmProvider restored = service.authorize("second-user", "Second", "second", false);
        assertTrue(service.isAuthorized(restored));
        assertEquals("second-user", restored.getPlatformUserId());
        session.clearCache();
        assertEquals("chosen-vision", service.getOrCreate().getModelName());
    }

    @Test void cancellationWinsAgainstAlreadyStartedAutomaticAuthorization() {
        when(cipher.encrypt("sk-first")).thenReturn("cipher-first");
        service.authorize("first-user", "First", "first", true);
        when(cipher.encrypt("sk-late")).thenAnswer(invocation -> {
            // A second transaction cancels after the default flow's initial status read.
            jdbc.update("UPDATE vls_llm_provider SET enabled=0,api_key_ciphertext='' WHERE id=1");
            return "cipher-late";
        });
        LlmProvider result = service.authorize("late-user", "Late", "late", true);
        assertFalse(service.isAuthorized(result));
        assertFalse(result.getEnabled());
        assertEquals("", jdbc.queryForObject("SELECT api_key_ciphertext FROM vls_llm_provider WHERE id=1", String.class));
    }

    @Test void defaultLoginReplacesExistingTenantAccountAndCredential() {
        when(cipher.encrypt("sk-first")).thenReturn("cipher-first");
        service.authorize("first-user", "First", "first", true);
        service.selectModel("chosen-vision");
        when(cipher.encrypt("sk-other")).thenReturn("cipher-other");
        LlmProvider updated = service.authorize("other-user", "Other", "other", true);
        assertTrue(service.isAuthorized(updated));
        assertEquals("other-user", updated.getPlatformUserId());
        assertEquals("Other", updated.getPlatformUserName());
        assertEquals("cipher-other", updated.getApiKeyCiphertext());
        assertEquals("chosen-vision", updated.getModelName());
        session.clearCache();
        assertEquals("other-user", service.getOrCreate().getPlatformUserId());
        assertEquals("cipher-other", service.getOrCreate().getApiKeyCiphertext());
    }
}
