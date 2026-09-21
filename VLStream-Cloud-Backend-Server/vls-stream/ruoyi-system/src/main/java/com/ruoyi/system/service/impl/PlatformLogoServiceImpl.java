/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SysPlatformLogo;
import com.ruoyi.system.domain.vo.PlatformLogoVo;
import com.ruoyi.system.domain.vo.SysOssVo;
import com.ruoyi.system.mapper.SysPlatformLogoMapper;
import com.ruoyi.system.service.IPlatformLogoService;
import com.ruoyi.system.service.ISysOssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformLogoServiceImpl implements IPlatformLogoService {

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final SysPlatformLogoMapper platformLogoMapper;
    private final ISysOssService ossService;

    @Override
    public List<PlatformLogoVo> list() {
        List<SysPlatformLogo> entities = platformLogoMapper.selectList(
            Wrappers.<SysPlatformLogo>lambdaQuery().orderByDesc(SysPlatformLogo::getCreateTime));
        boolean customActive = false;
        List<PlatformLogoVo> result = new ArrayList<PlatformLogoVo>(entities.size() + 1);
        for (SysPlatformLogo entity : entities) {
            customActive = customActive || Boolean.TRUE.equals(entity.getActive());
        }
        result.add(defaultLogo(!customActive));
        for (SysPlatformLogo entity : entities) {
            result.add(toVo(entity));
        }
        return result;
    }

    @Override
    public PlatformLogoVo current() {
        SysPlatformLogo entity = platformLogoMapper.selectOne(
            Wrappers.<SysPlatformLogo>lambdaQuery()
                .eq(SysPlatformLogo::getActive, true)
                .orderByDesc(SysPlatformLogo::getUpdateTime)
                .last("LIMIT 1"));
        return entity == null ? defaultLogo(true) : toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformLogoVo create(MultipartFile file, String description) {
        String safeDescription = normalizeDescription(description);
        SysOssVo uploaded = ossService.upload(file);
        try {
            SysPlatformLogo entity = new SysPlatformLogo();
            entity.setOssId(uploaded.getOssId());
            entity.setDescription(safeDescription);
            entity.setActive(false);
            entity.setIsDeleted(0);
            platformLogoMapper.insert(entity);
            return toVo(entity);
        } catch (RuntimeException exception) {
            deleteOssQuietly(uploaded.getOssId());
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformLogoVo update(Long id, MultipartFile file, String description) {
        SysPlatformLogo entity = requireCustom(id);
        Long previousOssId = entity.getOssId();
        SysOssVo uploaded = file == null ? null : ossService.upload(file);
        try {
            entity.setDescription(normalizeDescription(description));
            if (uploaded != null) {
                entity.setOssId(uploaded.getOssId());
            }
            platformLogoMapper.updateById(entity);
        } catch (RuntimeException exception) {
            if (uploaded != null) {
                deleteOssQuietly(uploaded.getOssId());
            }
            throw exception;
        }
        if (uploaded != null && previousOssId != null) {
            deleteOssQuietly(previousOssId);
        }
        return toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(Long id) {
        SysPlatformLogo target = requireCustom(id);
        clearActive();
        target.setActive(true);
        platformLogoMapper.updateById(target);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateDefault() {
        clearActive();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        SysPlatformLogo entity = requireCustom(id);
        platformLogoMapper.deleteById(id);
        deleteOssQuietly(entity.getOssId());
    }

    private void clearActive() {
        platformLogoMapper.update(null, Wrappers.<SysPlatformLogo>lambdaUpdate()
            .set(SysPlatformLogo::getActive, false)
            .eq(SysPlatformLogo::getActive, true));
    }

    private SysPlatformLogo requireCustom(Long id) {
        if (id == null) {
            throw new ServiceException("系统默认标识不能编辑或删除");
        }
        SysPlatformLogo entity = platformLogoMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("平台标识不存在或无权访问");
        }
        return entity;
    }

    private PlatformLogoVo toVo(SysPlatformLogo entity) {
        PlatformLogoVo vo = new PlatformLogoVo();
        vo.setId(entity.getId());
        vo.setDescription(entity.getDescription());
        vo.setActive(Boolean.TRUE.equals(entity.getActive()));
        vo.setSystemDefault(false);
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        SysOssVo oss = entity.getOssId() == null ? null : ossService.getById(entity.getOssId());
        vo.setLogoUrl(oss == null ? null : oss.getUrl());
        return vo;
    }

    private PlatformLogoVo defaultLogo(boolean active) {
        PlatformLogoVo vo = new PlatformLogoVo();
        vo.setDescription("VLStream Cloud 系统默认标识");
        vo.setActive(active);
        vo.setSystemDefault(true);
        vo.setCreateBy("system");
        return vo;
    }

    private String normalizeDescription(String description) {
        String value = description == null ? "" : description.trim();
        if (value.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ServiceException("描述不能超过500个字符");
        }
        return value;
    }

    private void deleteOssQuietly(Long ossId) {
        if (ossId == null) {
            return;
        }
        try {
            ossService.deleteWithValidByIds(Collections.singletonList(ossId), true);
        } catch (RuntimeException exception) {
            log.warn("清理平台标识 OSS 文件失败: ossId={}", ossId, exception);
        }
    }
}
