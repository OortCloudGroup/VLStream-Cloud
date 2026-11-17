package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.impl.VlsAlgorithmServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Isolated contracts: no Spring context, database, remote model or camera access. */
@Tag("dev")
class AlgorithmLibraryAssociationTest {
    @BeforeAll static void initializeLambdaMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "association-test");
        TableInfoHelper.initTableInfo(assistant, Algorithm.class);
        TableInfoHelper.initTableInfo(assistant, AlgorithmLlmReviewConfig.class);
    }

    @Test void movingKeepsAlgorithmIdentityAndExistingVisualReviewPolicy() {
        VlsAlgorithmMapper algorithmMapper = mock(VlsAlgorithmMapper.class);
        IVlsAlgorithmRepositoryService repositories = mock(IVlsAlgorithmRepositoryService.class);
        VlsAlgorithmServiceImpl service = spy(new VlsAlgorithmServiceImpl(algorithmMapper, repositories));
        Algorithm stored = new Algorithm();
        stored.setId(2122225251566968834L); stored.setRepositoryId(1L); stored.setTenantId("tenant-a"); stored.setName("fire");
        doReturn(stored).when(service).getOne(any(Wrapper.class));
        doReturn(0L).when(service).count(any(Wrapper.class));
        when(repositories.list(any(Wrapper.class))).thenReturn(Collections.singletonList(category(2L, 0L, 1)));
        doAnswer(invocation -> {
            Algorithm updated = invocation.getArgument(0);
            assertEquals(stored.getId(), updated.getId());
            stored.setRepositoryId(updated.getRepositoryId());
            return true;
        }).when(service).updateById(any(Algorithm.class));
        AlgorithmLlmReviewConfig policy = new AlgorithmLlmReviewConfig();
        policy.setId(99L); policy.setAlgorithmId(stored.getId()); policy.setProviderId(88L);
        policy.setEnabled(true); policy.setPromptTemplate("existing visual policy");
        policy.setDecisionThreshold(new BigDecimal("0.8500"));
        AlgorithmLlmReviewConfigMapper configs = mock(AlgorithmLlmReviewConfigMapper.class);
        when(configs.selectOne(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<AlgorithmLlmReviewConfig> query = invocation.getArgument(0);
            assertTrue(query.getSqlSegment().contains("algorithm_id"));
            assertTrue(query.getParamNameValuePairs().containsValue(stored.getId()));
            return policy;
        });
        when(algorithmMapper.selectById(stored.getId())).thenReturn(stored);
        LlmReviewManagementService review = new LlmReviewManagementService(mock(LlmProviderMapper.class), configs,
            algorithmMapper, mock(LlmApiKeyCipher.class), mock(LlmSystemProviderService.class));
        assertSame(policy, review.getEventReviewConfig(stored.getId()));
        Algorithm update = new Algorithm(); update.setId(stored.getId()); update.setRepositoryId(2L);
        assertTrue(service.updateAlgorithm(update));
        assertEquals(2L, stored.getRepositoryId());
        assertSame(policy, review.getEventReviewConfig(stored.getId()));
        assertEquals(99L, policy.getId()); assertEquals(88L, policy.getProviderId());
        assertEquals("existing visual policy", policy.getPromptTemplate());
        assertEquals(new BigDecimal("0.8500"), policy.getDecisionThreshold());
        verify(configs, times(2)).selectOne(any());
        verifyNoMoreInteractions(configs);
        verify(algorithmMapper, never()).insert(any());
    }

    @Test void directoryFilterIncludesNestedCategoriesButNotOtherRoots() {
        assertDirectoryScope(1L, Arrays.asList(1L, 2L, 3L),
            Arrays.asList(category(1L,0L,1),category(2L,1L,1),category(3L,2L,1),category(4L,0L,1)));
    }

    @Test void childFilterExcludesItsParentAndSiblings() {
        assertDirectoryScope(2L, Arrays.asList(2L, 3L),
            Arrays.asList(category(1L,0L,1),category(2L,1L,1),category(3L,2L,1),category(4L,1L,1)));
    }

    @Test void allLibrariesExcludeDisabledAncestorsAndTheirDescendants() {
        assertDirectoryScope(null, Collections.singletonList(4L),
            Arrays.asList(category(1L,0L,0),category(2L,1L,1),category(3L,2L,1),category(4L,0L,1)));
    }

    @SuppressWarnings("unchecked")
    private void assertDirectoryScope(Long selected, List<Long> expected, List<AlgorithmRepository> rows) {
        VlsAlgorithmRepositoryMapper repositories = mock(VlsAlgorithmRepositoryMapper.class);
        VlsAlgorithmMapper algorithms = mock(VlsAlgorithmMapper.class);
        when(repositories.selectList(any())).thenReturn(rows);
        when(algorithms.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<Algorithm> query = invocation.getArgument(1);
            assertTrue(query.getSqlSegment().contains("repository_id IN"));
            assertEquals(new HashSet<>(expected), new HashSet<>(query.getParamNameValuePairs().values()));
            return invocation.getArgument(0);
        });
        AlgorithmCatalogService service = new AlgorithmCatalogService(repositories, algorithms,
            mock(AlgorithmCatalogPreferenceMapper.class), new ObjectMapper());
        service.algorithmPage(selected, null, null, 1, 24);
        verify(algorithms).selectPage(any(Page.class), any());
    }

    private static AlgorithmRepository category(Long id, Long parent, int status) {
        AlgorithmRepository row = new AlgorithmRepository(); row.setId(id); row.setParentId(parent);
        row.setStatus(status); row.setTenantId("tenant-a"); return row;
    }
}
