package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.AlgorithmModel;
import com.ruoyi.vlstream.mapper.VlsAlgorithmModelMapper;
import com.ruoyi.vlstream.service.IVlsAlgorithmModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for the VLS algorithm model frontend compatibility surface.
 */
@Service
@RequiredArgsConstructor
public class VlsAlgorithmModelServiceImpl implements IVlsAlgorithmModelService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_TESTING = 1;
    private static final int STATUS_PUBLISHED = 2;

    private final VlsAlgorithmModelMapper algorithmModelMapper;

    @Override
    public BladePage<AlgorithmModel> getModelPage(Long current, Long size, String modelName, Long algorithmId,
                                                  Long trainingId, String status, String createdTimeBegin,
                                                  String createdTimeEnd) {
        Page<AlgorithmModel> page = new Page<AlgorithmModel>(normalizePage(current), normalizeSize(size));
        LambdaQueryWrapper<AlgorithmModel> wrapper = baseQuery();
        if (StringUtils.hasText(modelName)) {
            wrapper.like(AlgorithmModel::getModelName, modelName.trim());
        }
        if (algorithmId != null) {
            wrapper.eq(AlgorithmModel::getAlgorithmId, algorithmId);
        }
        if (trainingId != null) {
            wrapper.eq(AlgorithmModel::getTrainingId, trainingId);
        }
        Integer statusValue = toStatusValue(status);
        if (statusValue != null) {
            wrapper.eq(AlgorithmModel::getStatus, statusValue);
        }
        Date begin = parseDate(createdTimeBegin, false);
        if (begin != null) {
            wrapper.ge(AlgorithmModel::getCreateTime, begin);
        }
        Date end = parseDate(createdTimeEnd, true);
        if (end != null) {
            wrapper.le(AlgorithmModel::getCreateTime, end);
        }
        wrapper.orderByDesc(AlgorithmModel::getCreateTime).orderByDesc(AlgorithmModel::getId);

        Page<AlgorithmModel> result = algorithmModelMapper.selectPage(page, wrapper);
        return BladePage.of(fillDerived(result.getRecords()), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public AlgorithmModel getModelById(Long id) {
        return fillDerived(algorithmModelMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmModel createModel(AlgorithmModel model) {
        if (model == null || !StringUtils.hasText(model.getModelName())) {
            throw new IllegalArgumentException("Model name is required");
        }
        model.setModelName(model.getModelName().trim());
        normalizeDefaults(model, true);
        while (checkModelNameAndVersion(model.getModelName(), model.getVersion(), null)) {
            model.setVersion(model.getVersion() + 1);
        }
        algorithmModelMapper.insert(model);
        return fillDerived(model);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmModel updateModel(AlgorithmModel model) {
        if (model == null || model.getId() == null) {
            throw new IllegalArgumentException("Model ID is required");
        }
        AlgorithmModel existing = algorithmModelMapper.selectById(model.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Model does not exist");
        }
        if (!StringUtils.hasText(model.getModelName())) {
            model.setModelName(existing.getModelName());
        } else {
            model.setModelName(model.getModelName().trim());
        }
        if (model.getVersion() == null || model.getVersion() < 1) {
            model.setVersion(existing.getVersion());
        }
        if (checkModelNameAndVersion(model.getModelName(), model.getVersion(), model.getId())) {
            throw new IllegalArgumentException("Model name and version already exist");
        }
        mergeUnsetFields(model, existing);
        algorithmModelMapper.updateById(model);
        return getModelById(model.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteModel(Long id) {
        return id != null && algorithmModelMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteModels(List<Long> ids) {
        return ids != null && !ids.isEmpty() && algorithmModelMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public List<AlgorithmModel> getModelsByAlgorithmId(Long algorithmId) {
        if (algorithmId == null) {
            return java.util.Collections.emptyList();
        }
        LambdaQueryWrapper<AlgorithmModel> wrapper = baseQuery()
            .eq(AlgorithmModel::getAlgorithmId, algorithmId)
            .orderByDesc(AlgorithmModel::getVersion)
            .orderByDesc(AlgorithmModel::getCreateTime);
        return fillDerived(algorithmModelMapper.selectList(wrapper));
    }

    @Override
    public List<AlgorithmModel> getModelsByTrainingId(Long trainingId) {
        if (trainingId == null) {
            return java.util.Collections.emptyList();
        }
        LambdaQueryWrapper<AlgorithmModel> wrapper = baseQuery()
            .eq(AlgorithmModel::getTrainingId, trainingId)
            .orderByDesc(AlgorithmModel::getVersion)
            .orderByDesc(AlgorithmModel::getCreateTime);
        return fillDerived(algorithmModelMapper.selectList(wrapper));
    }

    @Override
    public List<AlgorithmModel> getModelsByStatus(String status) {
        Integer statusValue = toStatusValue(status);
        if (statusValue == null) {
            return java.util.Collections.emptyList();
        }
        LambdaQueryWrapper<AlgorithmModel> wrapper = baseQuery()
            .eq(AlgorithmModel::getStatus, statusValue)
            .orderByDesc(AlgorithmModel::getCreateTime)
            .orderByDesc(AlgorithmModel::getId);
        return fillDerived(algorithmModelMapper.selectList(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishModel(Long id) {
        AlgorithmModel model = algorithmModelMapper.selectById(id);
        if (model == null) {
            return false;
        }
        model.setStatus(STATUS_PUBLISHED);
        model.setPublishTime(new Date());
        model.setUpdateTime(new Date());
        return algorithmModelMapper.updateById(model) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unpublishModel(Long id) {
        AlgorithmModel model = algorithmModelMapper.selectById(id);
        if (model == null) {
            return false;
        }
        model.setStatus(STATUS_DRAFT);
        model.setPublishTime(null);
        model.setUpdateTime(new Date());
        return algorithmModelMapper.updateById(model) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishModels(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean updated = false;
        for (Long id : ids) {
            updated = publishModel(id) || updated;
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String downloadModel(Long id) {
        AlgorithmModel model = algorithmModelMapper.selectById(id);
        if (model == null) {
            return null;
        }
        model.setDownloadCount(nullToZero(model.getDownloadCount()) + 1);
        model.setUpdateTime(new Date());
        algorithmModelMapper.updateById(model);
        return firstNonBlank(model.getModelPath(), model.getOnnxModelPath(), model.getRknnModelPath(), model.getInt8RknnModelOutputPath());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deployModel(Long id) {
        AlgorithmModel model = algorithmModelMapper.selectById(id);
        if (model == null) {
            return false;
        }
        model.setDeployCount(nullToZero(model.getDeployCount()) + 1);
        model.setUpdateTime(new Date());
        return algorithmModelMapper.updateById(model) > 0;
    }

    @Override
    public Map<String, Object> getModelStatistics() {
        List<AlgorithmModel> models = algorithmModelMapper.selectList(baseQuery());
        Map<String, Object> statistics = new LinkedHashMap<String, Object>();
        long draft = 0L;
        long testing = 0L;
        long published = 0L;
        long downloads = 0L;
        long deployments = 0L;
        long totalSize = 0L;
        for (AlgorithmModel model : models) {
            if (Integer.valueOf(STATUS_PUBLISHED).equals(model.getStatus())) {
                published++;
            } else if (Integer.valueOf(STATUS_TESTING).equals(model.getStatus())) {
                testing++;
            } else {
                draft++;
            }
            downloads += nullToZero(model.getDownloadCount());
            deployments += nullToZero(model.getDeployCount());
            totalSize += parseModelSizeToBytes(model.getModelSize());
        }
        statistics.put("total", Long.valueOf(models.size()));
        statistics.put("draft", draft);
        statistics.put("testing", testing);
        statistics.put("published", published);
        statistics.put("downloadCount", downloads);
        statistics.put("deployCount", deployments);
        statistics.put("totalSize", totalSize);
        return statistics;
    }

    @Override
    public boolean checkModelNameAndVersion(String modelName, Integer version, Long excludeId) {
        if (!StringUtils.hasText(modelName) || version == null) {
            return false;
        }
        LambdaQueryWrapper<AlgorithmModel> wrapper = baseQuery()
            .eq(AlgorithmModel::getModelName, modelName.trim())
            .eq(AlgorithmModel::getVersion, version);
        if (excludeId != null) {
            wrapper.ne(AlgorithmModel::getId, excludeId);
        }
        return algorithmModelMapper.selectCount(wrapper) > 0L;
    }

    @Override
    public AlgorithmModel getModelByAlgorithmIdAndVersion(Long algorithmId, Integer version) {
        if (algorithmId == null || version == null) {
            return null;
        }
        LambdaQueryWrapper<AlgorithmModel> wrapper = baseQuery()
            .eq(AlgorithmModel::getAlgorithmId, algorithmId)
            .eq(AlgorithmModel::getVersion, version)
            .last("LIMIT 1");
        return fillDerived(algorithmModelMapper.selectOne(wrapper));
    }

    @Override
    public AlgorithmModel getLatestModelByAlgorithmId(Long algorithmId) {
        if (algorithmId == null) {
            return null;
        }
        LambdaQueryWrapper<AlgorithmModel> wrapper = baseQuery()
            .eq(AlgorithmModel::getAlgorithmId, algorithmId)
            .orderByDesc(AlgorithmModel::getVersion)
            .orderByDesc(AlgorithmModel::getCreateTime)
            .last("LIMIT 1");
        return fillDerived(algorithmModelMapper.selectOne(wrapper));
    }

    @Override
    public List<AlgorithmModel> getPopularModels(Integer limit) {
        LambdaQueryWrapper<AlgorithmModel> wrapper = baseQuery()
            .orderByDesc(AlgorithmModel::getDownloadCount)
            .orderByDesc(AlgorithmModel::getDeployCount)
            .orderByDesc(AlgorithmModel::getCreateTime);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return fillDerived(algorithmModelMapper.selectList(wrapper));
    }

    @Override
    public Long countModelsByCreatedBy(Long createdBy) {
        if (createdBy == null) {
            return 0L;
        }
        return algorithmModelMapper.selectCount(baseQuery().eq(AlgorithmModel::getCreateUser, createdBy));
    }

    @Override
    public Long getTotalModelSize() {
        long totalSize = 0L;
        List<AlgorithmModel> models = algorithmModelMapper.selectList(baseQuery());
        for (AlgorithmModel model : models) {
            totalSize += parseModelSizeToBytes(model.getModelSize());
        }
        return totalSize;
    }

    private LambdaQueryWrapper<AlgorithmModel> baseQuery() {
        return new LambdaQueryWrapper<AlgorithmModel>();
    }

    private void normalizeDefaults(AlgorithmModel model, boolean created) {
        if (!StringUtils.hasText(model.getTenantId())) {
            model.setTenantId("000000");
        }
        if (model.getAlgorithmId() == null) {
            model.setAlgorithmId(0L);
        }
        if (model.getVersion() == null || model.getVersion() < 1) {
            model.setVersion(1);
        }
        if (!StringUtils.hasText(model.getModelFormat())) {
            model.setModelFormat("pt");
        }
        if (!StringUtils.hasText(model.getModelPath())) {
            model.setModelPath(firstNonBlank(model.getOnnxModelPath(), model.getRknnModelPath(), model.getInt8RknnModelOutputPath(), ""));
        }
        if (model.getDownloadCount() == null) {
            model.setDownloadCount(0);
        }
        if (model.getDeployCount() == null) {
            model.setDeployCount(0);
        }
        if (model.getStatus() == null) {
            model.setStatus(STATUS_PUBLISHED);
        }
        if (created && model.getCreateTime() == null) {
            model.setCreateTime(new Date());
        }
        if (model.getStatus().equals(STATUS_PUBLISHED) && model.getPublishTime() == null) {
            model.setPublishTime(new Date());
        }
        if (model.getIsDeleted() == null) {
            model.setIsDeleted(0);
        }
    }

    private void mergeUnsetFields(AlgorithmModel target, AlgorithmModel existing) {
        if (!StringUtils.hasText(target.getTenantId())) {
            target.setTenantId(existing.getTenantId());
        }
        if (target.getAlgorithmId() == null) {
            target.setAlgorithmId(existing.getAlgorithmId());
        }
        if (target.getTrainingId() == null) {
            target.setTrainingId(existing.getTrainingId());
        }
        if (!StringUtils.hasText(target.getModelFormat())) {
            target.setModelFormat(existing.getModelFormat());
        }
        if (!StringUtils.hasText(target.getModelSize())) {
            target.setModelSize(existing.getModelSize());
        }
        if (!StringUtils.hasText(target.getModelPath())) {
            target.setModelPath(existing.getModelPath());
        }
        if (!StringUtils.hasText(target.getOnnxModelPath())) {
            target.setOnnxModelPath(existing.getOnnxModelPath());
        }
        if (!StringUtils.hasText(target.getRknnModelPath())) {
            target.setRknnModelPath(existing.getRknnModelPath());
        }
        if (!StringUtils.hasText(target.getInt8RknnModelOutputPath())) {
            target.setInt8RknnModelOutputPath(existing.getInt8RknnModelOutputPath());
        }
        if (target.getAccuracy() == null) {
            target.setAccuracy(existing.getAccuracy());
        }
        if (!StringUtils.hasText(target.getDescription())) {
            target.setDescription(existing.getDescription());
        }
        if (target.getDownloadCount() == null) {
            target.setDownloadCount(existing.getDownloadCount());
        }
        if (target.getDeployCount() == null) {
            target.setDeployCount(existing.getDeployCount());
        }
        if (target.getPublishTime() == null) {
            target.setPublishTime(existing.getPublishTime());
        }
        if (target.getCreateUser() == null) {
            target.setCreateUser(existing.getCreateUser());
        }
        if (!StringUtils.hasText(target.getCreateDept())) {
            target.setCreateDept(existing.getCreateDept());
        }
        if (target.getCreateTime() == null) {
            target.setCreateTime(existing.getCreateTime());
        }
        if (target.getStatus() == null) {
            target.setStatus(existing.getStatus());
        }
        if (target.getIsDeleted() == null) {
            target.setIsDeleted(existing.getIsDeleted());
        }
        target.setUpdateTime(new Date());
    }

    private AlgorithmModel fillDerived(AlgorithmModel model) {
        if (model == null) {
            return null;
        }
        model.setStatusName(statusName(model.getStatus()));
        model.setModelDownloadPath(firstNonBlank(model.getModelPath(), model.getOnnxModelPath(), model.getRknnModelPath(), model.getInt8RknnModelOutputPath()));
        return model;
    }

    private List<AlgorithmModel> fillDerived(List<AlgorithmModel> models) {
        for (AlgorithmModel model : models) {
            fillDerived(model);
        }
        return models;
    }

    private Integer toStatusValue(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String value = status.trim().toLowerCase();
        if ("published".equals(value) || "publish".equals(value) || "2".equals(value)) {
            return STATUS_PUBLISHED;
        }
        if ("testing".equals(value) || "test".equals(value) || "1".equals(value)) {
            return STATUS_TESTING;
        }
        if ("draft".equals(value) || "unpublished".equals(value) || "unpublish".equals(value) || "0".equals(value)) {
            return STATUS_DRAFT;
        }
        return null;
    }

    private String statusName(Integer status) {
        if (Integer.valueOf(STATUS_PUBLISHED).equals(status)) {
            return "published";
        }
        if (Integer.valueOf(STATUS_TESTING).equals(status)) {
            return "testing";
        }
        return "draft";
    }

    private long normalizePage(Long current) {
        return current == null || current < 1L ? 1L : current;
    }

    private long normalizeSize(Long size) {
        return size == null || size < 1L ? 10L : size;
    }

    private Date parseDate(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        String pattern = text.length() <= 10 ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss";
        try {
            Date date = new SimpleDateFormat(pattern).parse(text);
            if (endOfDay && text.length() <= 10) {
                return new Date(date.getTime() + 86399000L);
            }
            return date;
        } catch (ParseException ignored) {
            return null;
        }
    }

    private long parseModelSizeToBytes(String modelSize) {
        if (!StringUtils.hasText(modelSize)) {
            return 0L;
        }
        String normalized = modelSize.trim().toUpperCase();
        BigDecimal multiplier = BigDecimal.ONE;
        if (normalized.endsWith("GB")) {
            multiplier = BigDecimal.valueOf(1024L * 1024L * 1024L);
            normalized = normalized.substring(0, normalized.length() - 2).trim();
        } else if (normalized.endsWith("MB")) {
            multiplier = BigDecimal.valueOf(1024L * 1024L);
            normalized = normalized.substring(0, normalized.length() - 2).trim();
        } else if (normalized.endsWith("KB")) {
            multiplier = BigDecimal.valueOf(1024L);
            normalized = normalized.substring(0, normalized.length() - 2).trim();
        } else if (normalized.endsWith("B")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        try {
            return new BigDecimal(normalized).multiply(multiplier).longValue();
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
