/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workflow.domain.WfFormApp;
import com.ruoyi.workflow.domain.bo.WfFormAppBo;
import com.ruoyi.workflow.domain.vo.WfFormAppVo;
import com.ruoyi.workflow.mapper.WfFormAppMapper;
import com.ruoyi.workflow.service.IWfFormAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ruoyi.workflow.service.impl.WfAppServiceImpl.generateUuid;

/**
 * 表单应用分类Service业务层处理
 *
 * @author 雷超群
 * @date 2025-04-26
 */
@RequiredArgsConstructor
@Service
public class WfFormAppServiceImpl  extends ServiceImpl<WfFormAppMapper, WfFormApp> implements IWfFormAppService{

    private final WfFormAppMapper baseMapper;

    /**
     * 查询表单应用分类
     */
    @Override
    public WfFormAppVo queryById(String categoryId) {
        return baseMapper.selectVoById(categoryId);
    }

    /**
     * 查询表单应用分类列表
     */
    @Override
    public TableDataInfo<WfFormAppVo> queryPageList(WfFormAppBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WfFormApp> lqw = buildQueryWrapper(bo);
        Page<WfFormAppVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询表单应用分类列表
     */
    @Override
    public Optional<List<WfFormAppVo>> queryList(WfFormAppBo bo) {
        LambdaQueryWrapper<WfFormApp> lqw = buildQueryWrapper(bo);
        return Optional.ofNullable(baseMapper.selectVoList(lqw));
    }

    private LambdaQueryWrapper<WfFormApp> buildQueryWrapper(WfFormAppBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WfFormApp> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getCategoryId()), WfFormApp::getCategoryId, bo.getCategoryId());
        lqw.eq(StringUtils.isNotBlank(bo.getApplicationId()), WfFormApp::getApplicationId, bo.getApplicationId());
        lqw.like(StringUtils.isNotBlank(bo.getApplicationName()), WfFormApp::getApplicationName,
            bo.getApplicationName());
        lqw.eq(StringUtils.isNotBlank(bo.getApplicationSecret()), WfFormApp::getApplicationSecret,
            bo.getApplicationSecret());
        lqw.eq(StringUtils.isNotBlank(bo.getParentId()), WfFormApp::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getCategoryName()), WfFormApp::getCategoryName, bo.getCategoryName());
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), WfFormApp::getCode, bo.getCode());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), WfFormApp::getType, bo.getType());
//        lqw.eq(WfFormApp::getDelFlag,"0");
        return lqw;
    }

    /**
     * 新增表单应用分类
     */
    @Override
    public WfFormApp insertByBo(WfFormAppBo bo) {
        LambdaQueryWrapper<WfFormApp> queryWrapper = new LambdaQueryWrapper<>();
        if(bo.getType().equals("0")){
            //流程
            queryWrapper.eq(WfFormApp::getType, "0");
        }else{
            //工单
            queryWrapper.eq(WfFormApp::getType, "1");
        }
        queryWrapper.eq(WfFormApp::getApplicationId, bo.getApplicationId());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("应用已存在");
        }
        WfFormApp add = BeanUtil.toBean(bo, WfFormApp.class);
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
            bo.setCategoryId(add.getCategoryId());
        }
        return add;
    }

    /**
     * 修改表单应用分类
     */
    @Override
    public Boolean updateByBo(WfFormAppBo bo) {
        WfFormApp update = BeanUtil.toBean(bo, WfFormApp.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WfFormApp entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除表单应用分类
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
