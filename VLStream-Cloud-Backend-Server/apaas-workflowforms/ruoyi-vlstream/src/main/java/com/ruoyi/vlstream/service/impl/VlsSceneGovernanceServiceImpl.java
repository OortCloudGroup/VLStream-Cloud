package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.SceneGovernance;
import com.ruoyi.vlstream.mapper.VlsSceneGovernanceMapper;
import com.ruoyi.vlstream.service.IVlsSceneGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service implementation for VLS scene governance compatibility routes.
 */
@Service
@RequiredArgsConstructor
public class VlsSceneGovernanceServiceImpl implements IVlsSceneGovernanceService {

    private static final String DEFAULT_TENANT_ID = "000000";

    private final VlsSceneGovernanceMapper sceneGovernanceMapper;

    @Override
    public BladePage<SceneGovernance> getSceneGovernanceList(Long current, Long size, String name, String startDate, String endDate) {
        Page<SceneGovernance> page = new Page<SceneGovernance>(normalizePage(current), normalizeSize(size));
        LambdaQueryWrapper<SceneGovernance> wrapper = baseQuery();
        if (StringUtils.hasText(name)) {
            wrapper.like(SceneGovernance::getName, name.trim());
        }
        Date begin = parseDate(startDate, false);
        if (begin != null) {
            wrapper.ge(SceneGovernance::getCreateTime, begin);
        }
        Date end = parseDate(endDate, true);
        if (end != null) {
            wrapper.le(SceneGovernance::getCreateTime, end);
        }
        wrapper.orderByDesc(SceneGovernance::getCreateTime).orderByDesc(SceneGovernance::getId);
        Page<SceneGovernance> result = sceneGovernanceMapper.selectPage(page, wrapper);
        return BladePage.of(fillAliases(result.getRecords()), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SceneGovernance submitSceneGovernance(SceneGovernance sceneGovernance) {
        if (sceneGovernance == null) {
            throw new IllegalArgumentException("Scene governance is required");
        }
        normalizeDefaults(sceneGovernance);
        if (sceneGovernance.getId() == null) {
            sceneGovernanceMapper.insert(sceneGovernance);
        } else {
            SceneGovernance existing = sceneGovernanceMapper.selectById(sceneGovernance.getId());
            if (existing == null) {
                throw new IllegalArgumentException("Scene governance does not exist");
            }
            mergeUnsetFields(sceneGovernance, existing);
            sceneGovernance.setUpdateTime(new Date());
            sceneGovernanceMapper.updateById(sceneGovernance);
        }
        SceneGovernance stored = sceneGovernanceMapper.selectById(sceneGovernance.getId());
        return fillAliases(stored == null ? sceneGovernance : stored);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeSceneGovernance(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return sceneGovernanceMapper.deleteBatchIds(ids) > 0;
    }

    private LambdaQueryWrapper<SceneGovernance> baseQuery() {
        return new LambdaQueryWrapper<SceneGovernance>().eq(SceneGovernance::getIsDeleted, 0);
    }

    private void normalizeDefaults(SceneGovernance sceneGovernance) {
        if (!StringUtils.hasText(sceneGovernance.getName())) {
            throw new IllegalArgumentException("Scene name is required");
        }
        sceneGovernance.setName(sceneGovernance.getName().trim());
        if (!StringUtils.hasText(sceneGovernance.getTenantId())) {
            sceneGovernance.setTenantId(DEFAULT_TENANT_ID);
        }
        if (sceneGovernance.getStatus() == null) {
            sceneGovernance.setStatus(Integer.valueOf(1));
        }
        if (sceneGovernance.getIsDeleted() == null) {
            sceneGovernance.setIsDeleted(0);
        }
        if (!StringUtils.hasText(sceneGovernance.getCameras()) && sceneGovernance.getCameraIds() != null) {
            sceneGovernance.setCameras(join(sceneGovernance.getCameraIds()));
        }
        Date now = new Date();
        if (sceneGovernance.getCreateTime() == null) {
            sceneGovernance.setCreateTime(now);
        }
        if (sceneGovernance.getUpdateTime() == null) {
            sceneGovernance.setUpdateTime(now);
        }
    }

    private void mergeUnsetFields(SceneGovernance target, SceneGovernance existing) {
        if (!StringUtils.hasText(target.getTenantId())) target.setTenantId(existing.getTenantId());
        if (!StringUtils.hasText(target.getName())) target.setName(existing.getName());
        if (!StringUtils.hasText(target.getDescription())) target.setDescription(existing.getDescription());
        if (!StringUtils.hasText(target.getCronExpression())) target.setCronExpression(existing.getCronExpression());
        if (!StringUtils.hasText(target.getLocation())) target.setLocation(existing.getLocation());
        if (!StringUtils.hasText(target.getCameras())) target.setCameras(existing.getCameras());
        if (target.getCreateUser() == null) target.setCreateUser(existing.getCreateUser());
        if (!StringUtils.hasText(target.getCreateDept())) target.setCreateDept(existing.getCreateDept());
        if (target.getCreateTime() == null) target.setCreateTime(existing.getCreateTime());
        if (target.getUpdateUser() == null) target.setUpdateUser(existing.getUpdateUser());
        if (target.getStatus() == null) target.setStatus(existing.getStatus());
        if (target.getIsDeleted() == null) target.setIsDeleted(existing.getIsDeleted());
    }

    private List<SceneGovernance> fillAliases(List<SceneGovernance> scenes) {
        List<SceneGovernance> result = new ArrayList<SceneGovernance>();
        if (scenes == null) {
            return result;
        }
        for (SceneGovernance scene : scenes) {
            result.add(fillAliases(scene));
        }
        return result;
    }

    private SceneGovernance fillAliases(SceneGovernance scene) {
        if (scene == null) {
            return null;
        }
        if (!StringUtils.hasText(scene.getDevices())) {
            scene.setDevices(firstNonBlank(scene.getCamerasName(), scene.getCameras(), "-"));
        }
        if (!StringUtils.hasText(scene.getCamerasName())) {
            scene.setCamerasName(firstNonBlank(scene.getDevices(), scene.getCameras(), "-"));
        }
        if (!StringUtils.hasText(scene.getRules())) {
            scene.setRules(firstNonBlank(scene.getAlgorithmName(), scene.getAlgorithm(), "-"));
        }
        if (!StringUtils.hasText(scene.getAlgorithmName())) {
            scene.setAlgorithmName(firstNonBlank(scene.getRules(), scene.getAlgorithm(), "-"));
        }
        return scene;
    }

    private Date parseDate(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        String[] patterns = new String[] {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            try {
                Date date = new SimpleDateFormat(pattern).parse(trimmed);
                if (endOfDay && "yyyy-MM-dd".equals(pattern)) {
                    return new Date(date.getTime() + 86399999L);
                }
                return date;
            } catch (ParseException ignored) {
                // Try the next frontend date format.
            }
        }
        return null;
    }

    private long normalizePage(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long normalizeSize(Long size) {
        return size == null || size < 1 ? 20L : size;
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        if (StringUtils.hasText(second)) {
            return second;
        }
        return fallback;
    }
}
