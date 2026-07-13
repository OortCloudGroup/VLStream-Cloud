/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.workflow.domain.WfApp;
import com.ruoyi.workflow.domain.bo.WfAppBo;
import com.ruoyi.workflow.domain.vo.WfAppVo;
import com.ruoyi.workflow.mapper.WfAppMapper;
import com.ruoyi.workflow.service.IWfAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 应用通用流程Service业务层处理
 *
 * @author 雷超群
 * @date 2025-01-04
 */
@RequiredArgsConstructor
@Service
public class WfAppServiceImpl extends ServiceImpl<WfAppMapper, WfApp> implements IWfAppService {

    private final WfAppMapper baseMapper;
    private final ValidateService validateService;


    /**
     * 查询应用通用流程
     */
    @Override
    public WfAppVo queryById(String appId) {
        return baseMapper.selectVoById(appId);
    }

    /**
     * 查询应用通用流程列表
     */
    @Override
    public List<WfAppVo> queryPageList(WfAppBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WfApp> lqw = buildQueryWrapper(bo);
        List<WfAppVo> wfAppVos = baseMapper.selectVoList(lqw);
        return wfAppVos;
    }

    /**
     * 查询应用通用流程列表
     */
    @Override
    public List<WfAppVo> queryList(WfAppBo bo) {
        LambdaQueryWrapper<WfApp> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WfApp> buildQueryWrapper(WfAppBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WfApp> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getApplicationName()), WfApp::getApplicationName, bo.getApplicationName());
        lqw.eq(StringUtils.isNotBlank(bo.getApplicationId()), WfApp::getApplicationId, bo.getApplicationId());
        lqw.eq(StringUtils.isNotBlank(bo.getApplicationSecret()), WfApp::getApplicationSecret, bo.getApplicationSecret());
        return lqw;
    }

    /**
     * 新增应用通用流程
     */
    @Override
    public WfApp insertByBo(WfAppBo bo) {
        LambdaQueryWrapper<WfApp> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfApp::getApplicationId, bo.getApplicationId());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("应用已存在");
        }
        WfApp add = BeanUtil.toBean(bo, WfApp.class);
        if ("1".equals(bo.getAppFlag())) {
            if (!StringUtils.isNotBlank(bo.getApplicationId())) {
                add.setApplicationId(generateUuid());
            }
            if (!StringUtils.isNotBlank(bo.getApplicationSecret())) {
                add.setApplicationSecret(generateUuid());
            }
        }
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setAppId(add.getAppId());
        }
        return add;
    }
    public static String generateUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    /**
     * 修改应用通用流程
     */
    @Override
    public Boolean updateByBo(WfAppBo bo) {
        WfApp update = BeanUtil.toBean(bo, WfApp.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WfApp entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除应用通用流程
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            //效验这个分类下是否还有别的数据
            validateService.validateBeforeDeletion(ids);
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
