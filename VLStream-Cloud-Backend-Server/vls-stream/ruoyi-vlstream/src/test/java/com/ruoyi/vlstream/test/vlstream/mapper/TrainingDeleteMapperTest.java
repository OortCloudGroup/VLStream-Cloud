package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class TrainingDeleteMapperTest {
    private final MybatisConfiguration config = configuration();
    private static MybatisConfiguration configuration() {
        MybatisConfiguration config = new MybatisConfiguration();
        String resource = VlsAlgorithmTrainingMapper.class.getName().replace('.', '/') + ".xml";
        try (InputStream input = TrainingDeleteMapperTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
        } catch (Exception e) { throw new IllegalStateException(e); }
        return config;
    }
    private BoundSql sql(String method, Class<?> type, Object argument) throws Exception {
        Object params = new ParamNameResolver(config, VlsAlgorithmTrainingMapper.class.getMethod(method, type))
            .getNamedParams(new Object[]{argument});
        org.apache.ibatis.mapping.MappedStatement statement = config.getMappedStatement(VlsAlgorithmTrainingMapper.class.getName() + "." + method);
        BoundSql sql = statement.getBoundSql(params);
        assertTrue(sql.getSql().contains("SET is_deleted = 1"));
        assertFalse(sql.getSql().matches("(?s).*\\bSET deleted\\b.*"));
        PreparedStatement prepared = mock(PreparedStatement.class);
        new DefaultParameterHandler(statement, params, sql).setParameters(prepared);
        if (argument instanceof Long) verify(prepared).setLong(1, (Long) argument);
        if (argument instanceof Long[] && ((Long[]) argument).length > 0) {
            verify(prepared).setLong(1, ((Long[]) argument)[0]);
            verify(prepared).setLong(2, ((Long[]) argument)[1]);
        }
        return sql;
    }
    @Test void singleUsesExistingColumnAndExactId() throws Exception {
        assertEquals(1, sql("deleteAlgorithmTrainingById", Long.class, 2098371387210203137L).getParameterMappings().size());
    }
    @Test void batchBindsNamedIdsWithoutPrecisionLoss() throws Exception {
        assertEquals(2, sql("deleteAlgorithmTrainingByIds", Long[].class, new Long[]{2098371387210203137L,2098366221929021442L}).getParameterMappings().size());
    }
    @Test void emptyArrayCannotDeleteAllRows() throws Exception {
        assertTrue(sql("deleteAlgorithmTrainingByIds", Long[].class, new Long[0]).getSql().contains("1 = 0"));
    }
    @Test void nullArrayCannotDeleteAllRows() throws Exception {
        assertTrue(sql("deleteAlgorithmTrainingByIds", Long[].class, null).getSql().contains("1 = 0"));
    }
    @Test void annotationBatchUsesSameLogicalDeleteColumn() throws Exception {
        assertEquals(2, sql("deleteBatch", java.util.List.class, Arrays.asList(1L,2L)).getParameterMappings().size());
    }
    @Test void emptyListCannotDeleteAllRows() throws Exception {
        assertTrue(sql("deleteBatch", java.util.List.class, Collections.emptyList()).getSql().contains("1 = 0"));
    }
    @Test void containerBatchAlsoUsesExistingLogicalDeleteColumn() throws Exception {
        config.addMapper(VlsContainerInstanceMapper.class);
        Object params = new ParamNameResolver(config, VlsContainerInstanceMapper.class.getMethod("deleteBatch", java.util.List.class))
            .getNamedParams(new Object[]{Arrays.asList(1L, 2L)});
        BoundSql bound = config.getMappedStatement(VlsContainerInstanceMapper.class.getName() + ".deleteBatch").getBoundSql(params);
        assertTrue(bound.getSql().contains("SET is_deleted = 1"));
        assertEquals(2, bound.getParameterMappings().size());
    }
}
