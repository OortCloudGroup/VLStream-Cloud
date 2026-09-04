package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.AlgorithmCatalogPreferenceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmRepositoryMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AlgorithmCatalogServiceTest {
    @Mock private VlsAlgorithmRepositoryMapper repositories;
    @Mock private VlsAlgorithmMapper algorithms;
    @Mock private AlgorithmCatalogPreferenceMapper preferences;

    private AlgorithmCatalogService service() {
        return new AlgorithmCatalogService(repositories, algorithms, preferences, new ObjectMapper());
    }

    @Test
    void descendantsIncludesSelectedCategoryAndEveryDepth() {
        List<AlgorithmRepository> rows = Arrays.asList(category(1L, 0L, "A"), category(2L, 1L, "B"), category(3L, 2L, "C"), category(4L, 0L, "D"));
        Set<Long> result = service().descendants(rows, 1L);
        assertEquals(3, result.size());
        assertEquals(true, result.containsAll(Arrays.asList(1L, 2L, 3L)));
    }

    @Test
    void rejectsMovingCategoryUnderItsDescendant() {
        List<AlgorithmRepository> rows = Arrays.asList(category(1L, 0L, "A"), category(2L, 1L, "B"));
        when(repositories.selectList(any())).thenReturn(rows);
        AlgorithmRepository update = category(1L, 2L, "A");
        assertThrows(ServiceException.class, () -> service().saveCategory(update));
    }

    @Test
    void rejectsDeletingCategoryThatStillHasChildren() {
        List<AlgorithmRepository> rows = Arrays.asList(category(1L, 0L, "A"), category(2L, 1L, "B"));
        when(repositories.selectList(any())).thenReturn(rows);
        assertThrows(ServiceException.class, () -> service().deleteCategories(Collections.singletonList(1L)));
    }

    private AlgorithmRepository category(Long id, Long parentId, String name) {
        AlgorithmRepository row = new AlgorithmRepository();
        row.setId(id);
        row.setParentId(parentId);
        row.setName(name);
        row.setStatus(1);
        row.setIsDeleted(0);
        return row;
    }
}
