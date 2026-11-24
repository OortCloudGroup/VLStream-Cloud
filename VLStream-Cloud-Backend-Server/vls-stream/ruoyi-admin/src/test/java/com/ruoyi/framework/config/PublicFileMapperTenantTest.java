package com.ruoyi.framework.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.system.domain.VlsPublicFile;
import com.ruoyi.system.mapper.VlsPublicFileMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("dev")
class PublicFileMapperTenantTest {

    @ParameterizedTest
    @CsvSource({"single, configured-single", "multi, platform-tenant"})
    void publishedLookupWorksWithoutChangingTenantScopedQueries(String mode, String ownerTenant) {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        new JdbcTemplate(source).execute("CREATE TABLE vls_public_file (file_id VARCHAR(36) PRIMARY KEY,"
            + " tenant_id VARCHAR(64), uploader_id VARCHAR(64), original_name VARCHAR(255),"
            + " file_size BIGINT, storage_config_key VARCHAR(64))");

        TokenProperties properties = new TokenProperties();
        properties.setTenantType(mode);
        properties.setSingleTenantId("configured-single");
        CustomTenantLineHandler tenantHandler = new CustomTenantLineHandler(properties);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TrainingPageTenantInterceptor(tenantHandler));
        MybatisConfiguration configuration = new MybatisConfiguration(
            new Environment("public-file-test", new JdbcTransactionFactory(), source));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        configuration.addInterceptor(interceptor);
        configuration.addMapper(VlsPublicFileMapper.class);
        tenantHandler.afterSingletonsInstantiated();

        VlsPublicFile file = new VlsPublicFile();
        file.setFileId(UUID.randomUUID().toString());
        file.setTenantId(ownerTenant);
        file.setUploaderId("uploader-a");
        file.setOriginalName("report.pdf");
        file.setFileSize(3);
        file.setStorageConfigKey("vls-minio");
        try (SqlSession session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true)) {
            VlsPublicFileMapper mapper = session.getMapper(VlsPublicFileMapper.class);
            TenantContextHolder.setTenantId(ownerTenant);
            assertEquals(1, mapper.insert(file));
            assertEquals(ownerTenant, mapper.selectById(file.getFileId()).getTenantId());

            TenantContextHolder.setTenantId("another-tenant");
            assertNull(mapper.selectById(file.getFileId()));
            VlsPublicFile published = mapper.findPublishedFile(file.getFileId());
            assertEquals("vls-minio", published.getStorageConfigKey());
            assertNull(published.getTenantId());
            assertNull(published.getUploaderId());

            TenantContextHolder.clear();
            assertEquals("report.pdf", mapper.findPublishedFile(file.getFileId()).getOriginalName());
        } finally {
            TenantContextHolder.clear();
        }
    }
}
