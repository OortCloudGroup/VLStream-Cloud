package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.test.util.ReflectionTestUtils;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.Date;
import java.util.regex.*;

/** Isolated MySQL integration harness, enabled only with an explicit loopback test database URL. */
@org.springframework.boot.test.context.TestConfiguration
@EnableTransactionManagement(proxyTargetClass = true)
public class DataTestConfiguration {
    static Path root() {
        Path path = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (path != null && !Files.exists(path.resolve("BUSINESS_PROCESSES.md"))) path = path.getParent();
        if (path == null) throw new IllegalStateException("Workspace root not found");
        return path;
    }

    static Path artifacts() { return root().resolve("codex/data-management"); }

    @Bean DataSource dataSource() {
        String url = System.getenv("VLS_DATA_TEST_JDBC");
        if (url == null || !url.startsWith("jdbc:mysql://127.0.0.1:33316/vls_data_test")) throw new IllegalStateException("A dedicated vls_data_test database is required");
        return new DriverManagerDataSource(url, "root", "vls-data-test-only");
    }

    @Bean PlatformTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }

    @Bean ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule(); module.addSerializer(Long.class, ToStringSerializer.instance); mapper.registerModule(module);
        mapper.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")); return mapper;
    }

    @Bean SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean(); factory.setDataSource(source);
        MybatisConfiguration configuration = new MybatisConfiguration(); configuration.setMapUnderscoreToCamelCase(true);
        MybatisPlusInterceptor pagination = new MybatisPlusInterceptor(); pagination.addInnerInterceptor(new PaginationInnerInterceptor());
        configuration.addInterceptor(pagination); factory.setConfiguration(configuration);
        GlobalConfig global = new GlobalConfig(); global.setDbConfig(new GlobalConfig.DbConfig());
        global.setMetaObjectHandler(new MetaObjectHandler() {
            @Override public void insertFill(MetaObject meta) { fill(meta); if (meta.hasSetter("createTime")) meta.setValue("createTime", new Date()); }
            @Override public void updateFill(MetaObject meta) { fill(meta); }
            private void fill(MetaObject meta) {
                if (meta.hasSetter("tenantId")) meta.setValue("tenantId", TenantContextHolder.getTenantId());
                if (meta.hasSetter("updateTime")) meta.setValue("updateTime", new Date());
            }
        });
        factory.setGlobalConfig(global);
        SqlSessionFactory built = factory.getObject();
        for (Class<?> type : new Class<?>[]{VlsAlgorithmAnnotationMapper.class, DataSampleMapper.class, DatasetVersionMapper.class, VlsAnnotationLabelMapper.class, VlsAnnotationInstanceMapper.class}) built.getConfiguration().addMapper(type);
        return built;
    }

    @Bean SqlSessionTemplate template(SqlSessionFactory factory) { return new SqlSessionTemplate(factory); }
    @Bean DataManagementService service(SqlSessionTemplate template, ObjectMapper mapper, DataMediaStorage storage) {
        return new DataManagementService(template.getMapper(VlsAlgorithmAnnotationMapper.class), template.getMapper(DataSampleMapper.class),
            template.getMapper(VlsAnnotationLabelMapper.class), template.getMapper(VlsAnnotationInstanceMapper.class), template.getMapper(DatasetVersionMapper.class), mapper, storage);
    }
    @Bean SampleMediaInspector inspector() { return new SampleMediaInspector(); }
    @Bean DataTransferService transfer(DataManagementService service, DataMediaStorage storage, SampleMediaInspector inspector, ObjectMapper mapper) {
        DataTransferService transfer = new DataTransferService(service, storage, inspector, mapper);
        ReflectionTestUtils.setField(transfer, "tempDirectory", artifacts().resolve("temporary").toString()); return transfer;
    }

    @Bean DataMediaStorage storage() {
        return new DataMediaStorage() {
            private Path file(String key) {
                Path directory = artifacts().resolve("test-objects").toAbsolutePath().normalize();
                Path target = directory.resolve(key).normalize();
                if (!target.startsWith(directory)) throw new IllegalArgumentException("Invalid test object path"); return target;
            }
            @Override public void put(String key, byte[] bytes, String contentType) { try { Path target = file(key); Files.createDirectories(target.getParent()); Files.write(target, bytes); } catch (IOException e) { throw new UncheckedIOException(e); } }
            @Override public InputStream read(String key) { try { return Files.newInputStream(file(key)); } catch (IOException e) { throw new UncheckedIOException(e); } }
            @Override public String preview(String key) { return "http://127.0.0.1:38081/test-media/" + key; }
            @Override public void remove(String key) { try { Files.deleteIfExists(file(key)); } catch (IOException e) { throw new UncheckedIOException(e); } }
        };
    }

    static void initialize(DataSource source) throws Exception {
        Path backend = root().resolve("VLStream-Cloud-Backend-Server/vls-stream");
        String baseline = new String(Files.readAllBytes(backend.resolve("doc/sql/vls_stream.sql")), StandardCharsets.UTF_8);
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            for (String table : new String[]{"vls_dataset_version", "vls_annotation_instance", "vls_annotation_label", "vls_annotation_image", "vls_algorithm_annotation"}) statement.execute("DROP TABLE IF EXISTS " + table);
            for (String table : new String[]{"vls_algorithm_annotation", "vls_annotation_image", "vls_annotation_label", "vls_annotation_instance"}) {
                Matcher matcher = Pattern.compile("CREATE TABLE `" + table + "`[\\s\\S]*?;", Pattern.MULTILINE).matcher(baseline);
                if (!matcher.find()) throw new IllegalStateException("Missing baseline schema for " + table);
                statement.execute(matcher.group());
            }
            statement.execute("INSERT INTO vls_algorithm_annotation (id, tenant_id, annotation_name, annotation_type, annotation_rules) VALUES (10, 'tenant-a', 'legacy', 'object_detection', '{\"rule\":\"legacy\"}')");
            statement.execute("INSERT INTO vls_annotation_image (id, annotation_id, image_name, original_name, local_path) VALUES (15, 10, 'legacy.png', 'legacy.png', 'legacy.png')");
            String migration = new String(Files.readAllBytes(backend.resolve("ruoyi-admin/src/main/resources/db/migration/V1_2_0_011__data_sample_management.sql")), StandardCharsets.UTF_8);
            // Remove SQL line comments before splitting, because comments may contain semicolons.
            migration = migration.replaceAll("(?m)^--.*$", "");
            for (String sql : migration.split(";")) if (!sql.trim().isEmpty()) statement.execute(sql);
        }
    }
}
