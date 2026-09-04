package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmRepositoryTypeEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.AlgorithmCatalogPreferenceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmRepositoryMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmCatalogPreference;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Category hierarchy is the existing repository identity, not a second algorithm directory. */
@Service
@RequiredArgsConstructor
public class AlgorithmCatalogService {
    private final VlsAlgorithmRepositoryMapper repositories;
    private final VlsAlgorithmMapper algorithms;
    private final AlgorithmCatalogPreferenceMapper preferences;
    private final ObjectMapper json;

    public List<AlgorithmRepository> categories() {
        return repositories.selectList(new LambdaQueryWrapper<AlgorithmRepository>()
            .orderByAsc(AlgorithmRepository::getSortOrder, AlgorithmRepository::getId));
    }

    private List<AlgorithmRepository> lockCategories() {
        // Serialize hierarchy mutations within the SQL-isolated tenant, including parent deletion.
        return repositories.selectList(new LambdaQueryWrapper<AlgorithmRepository>()
            .orderByAsc(AlgorithmRepository::getId).last("FOR UPDATE"));
    }

    public AlgorithmRepository requireCategory(List<AlgorithmRepository> rows, Long id) {
        return rows.stream().filter(row -> Objects.equals(row.getId(), id)).findFirst()
            .orElseThrow(() -> new ServiceException("分类不存在或无权访问"));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean saveCategory(AlgorithmRepository input) {
        List<AlgorithmRepository> rows = lockCategories();
        AlgorithmRepository existing = input.getId() == null ? null : requireCategory(rows, input.getId());
        Long parentId = input.getParentId() == null ? (existing == null ? 0L : existing.getParentId()) : input.getParentId();
        if (parentId == null) parentId = 0L;
        String name = input.getName() == null ? (existing == null ? "" : existing.getName()) : input.getName().trim();
        if (name.isEmpty() || name.length() > 50) throw new ServiceException("分类名称须为 1 至 50 个字符");
        if (input.getRemark() != null && input.getRemark().length() > 500) throw new ServiceException("备注不能超过 500 个字符");
        if (input.getSortOrder() != null && input.getSortOrder() < 0) throw new ServiceException("排序不能小于 0");
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != 0L) {
            if (Objects.equals(cursor, input.getId()) || !visited.add(cursor)) throw new ServiceException("不能将分类放入自身或下级分类");
            AlgorithmRepository parent = requireCategory(rows, cursor);
            cursor = parent.getParentId() == null ? 0L : parent.getParentId();
        }
        for (AlgorithmRepository row : rows) {
            if (!Objects.equals(row.getId(), input.getId()) && Objects.equals(parentId, row.getParentId() == null ? 0L : row.getParentId())
                && name.equalsIgnoreCase(row.getName())) throw new ServiceException("同一级分类名称已存在");
        }
        // Whitelist writable fields; counters, ownership and navigation preferences are server-owned.
        AlgorithmRepository row = existing == null ? new AlgorithmRepository() : existing;
        row.setName(name);
        row.setParentId(parentId);
        if (input.getRemark() != null) row.setRemark(input.getRemark());
        if (input.getSortOrder() != null) row.setSortOrder(input.getSortOrder());
        if (input.getStatus() != null) {
            if (input.getStatus() != 0 && input.getStatus() != 1) throw new ServiceException("分类状态不合法");
            row.setStatus(input.getStatus());
        }
        if (existing == null) {
            row.setRepositoryType(AlgorithmRepositoryTypeEnum.extended);
            row.setAlgorithmCount(0);
            row.setSortOrder(row.getSortOrder() == null ? 0 : row.getSortOrder());
            row.setStatus(row.getStatus() == null ? 1 : row.getStatus());
            row.setIsDeleted(0);
            return repositories.insert(row) == 1;
        }
        return repositories.updateById(row) == 1;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategories(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new ServiceException("请选择要删除的分类");
        List<AlgorithmRepository> rows = lockCategories();
        Set<Long> selected = new HashSet<>(ids);
        for (Long id : selected) {
            AlgorithmRepository row = requireCategory(rows, id);
            if (row.getRepositoryType() == AlgorithmRepositoryTypeEnum.basic) throw new ServiceException("系统预置分类不能删除");
            if (rows.stream().anyMatch(child -> Objects.equals(child.getParentId(), id))) throw new ServiceException("分类拥有下级，请先删除下级分类");
        }
        if (algorithms.selectCount(new LambdaQueryWrapper<Algorithm>().in(Algorithm::getRepositoryId, selected)) > 0) {
            throw new ServiceException("分类下存在算法，请先移动或删除算法");
        }
        return repositories.deleteBatchIds(selected) == selected.size();
    }

    public Page<Algorithm> algorithmPage(Long categoryId, String keyword, String type, long current, long size) {
        List<AlgorithmRepository> rows = categories();
        Set<Long> visible = rows.stream().filter(row -> Integer.valueOf(1).equals(row.getStatus()))
            .map(AlgorithmRepository::getId).collect(Collectors.toSet());
        // Disabled ancestors also hide their descendants from the library, but not category management.
        boolean changed;
        do {
            changed = false;
            for (AlgorithmRepository row : rows) {
                if (row.getParentId() != null && row.getParentId() != 0L && !visible.contains(row.getParentId())) changed |= visible.remove(row.getId());
            }
        } while (changed);
        if (categoryId != null && categoryId != 0L) {
            requireCategory(rows, categoryId);
            Set<Long> descendants = descendants(rows, categoryId);
            visible.retainAll(descendants);
        }
        Page<Algorithm> page = new Page<>(Math.max(1, current), Math.max(1, Math.min(100, size)));
        if (visible.isEmpty()) return page;
        return algorithms.selectPage(page, new LambdaQueryWrapper<Algorithm>()
            .in(Algorithm::getRepositoryId, visible)
            .like(keyword != null && !keyword.trim().isEmpty(), Algorithm::getName, keyword == null ? null : keyword.trim())
            .eq(type != null && !type.isEmpty(), Algorithm::getCategory, type)
            .orderByDesc(Algorithm::getCreateTime, Algorithm::getId));
    }

    public Set<Long> descendants(List<AlgorithmRepository> rows, Long id) {
        Set<Long> result = new HashSet<>(Collections.singleton(id));
        boolean changed;
        do {
            changed = false;
            for (AlgorithmRepository row : rows) if (result.contains(row.getParentId())) changed |= result.add(row.getId());
        } while (changed);
        return result;
    }

    public String viewMode() {
        AlgorithmCatalogPreference preference = preferences.selectOne(new LambdaQueryWrapper<AlgorithmCatalogPreference>()
            .eq(AlgorithmCatalogPreference::getSettingKey, "catalog"));
        return preference == null ? "flat" : preference.getViewMode();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveSettings(Settings settings) {
        if (!"tree".equals(settings.getViewMode()) && !"flat".equals(settings.getViewMode())) throw new ServiceException("展示模式不合法");
        List<AlgorithmRepository> rows = lockCategories();
        if (settings.getRepositoryId() != null && settings.getRepositoryId() != 0L) {
            AlgorithmRepository row = requireCategory(rows, settings.getRepositoryId());
            List<String> ids = settings.getPeerIds() == null ? new ArrayList<>() : settings.getPeerIds();
            if (ids.size() > 500) throw new ServiceException("同级展示分类过多");
            for (String id : ids) {
                AlgorithmRepository child;
                try { child = requireCategory(rows, Long.valueOf(id)); }
                catch (NumberFormatException ex) { throw new ServiceException("分类标识不合法"); }
                if (!Objects.equals(child.getParentId(), row.getId())) throw new ServiceException("只能选择当前上级分类的直接下级作为同级展示项");
            }
            try { row.setFlatCategoryIds(json.writeValueAsString(ids)); }
            catch (JsonProcessingException ex) { throw new ServiceException("分类设置保存失败"); }
            repositories.updateById(row);
        }
        AlgorithmCatalogPreference preference = preferences.selectOne(new LambdaQueryWrapper<AlgorithmCatalogPreference>()
            .eq(AlgorithmCatalogPreference::getSettingKey, "catalog"));
        if (preference == null) {
            preference = new AlgorithmCatalogPreference();
            preference.setSettingKey("catalog");
            preference.setViewMode(settings.getViewMode());
            preference.setStatus(1);
            preference.setIsDeleted(0);
            preferences.insert(preference);
        } else {
            preference.setViewMode(settings.getViewMode());
            preferences.updateById(preference);
        }
    }

    @Data
    public static class Settings {
        private String viewMode;
        private Long repositoryId;
        private List<String> peerIds;
    }
}
