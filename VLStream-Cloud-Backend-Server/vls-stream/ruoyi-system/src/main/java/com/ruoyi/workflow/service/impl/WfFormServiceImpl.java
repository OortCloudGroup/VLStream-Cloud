/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workflow.domain.WfDeployForm;
import com.ruoyi.workflow.domain.WfForm;
import com.ruoyi.workflow.domain.bo.WfFormAppBo;
import com.ruoyi.workflow.domain.bo.WfFormBo;
import com.ruoyi.workflow.domain.bo.WfFormSynthesisBo;
import com.ruoyi.workflow.domain.bo.WfModelBo;
import com.ruoyi.workflow.domain.vo.WfFormAppVo;
import com.ruoyi.workflow.domain.vo.WfFormSynthesisVo;
import com.ruoyi.workflow.domain.vo.WfFormVo;
import com.ruoyi.workflow.mapper.WfDeployFormMapper;
import com.ruoyi.workflow.mapper.WfFormMapper;
import com.ruoyi.workflow.service.IWfFormAppService;
import com.ruoyi.workflow.service.IWfFormService;
import com.ruoyi.workflow.service.IWfFormSynthesisService;
import com.ruoyi.workflow.service.IWfModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程表单Service业务层处理
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
@RequiredArgsConstructor
@Service
public class WfFormServiceImpl extends ServiceImpl<WfFormMapper, WfForm> implements IWfFormService {

    private final WfFormMapper baseMapper;
    private final WfDeployFormMapper wfDeployFormMapper;
    private final IWfFormAppService wfFormAppService;
    private final IWfFormSynthesisService wfFormSynthesisService;
    @Lazy
    @Resource
    IWfModelService wfModelService;

    /**
     * 查询流程表单
     *
     * @param formId 流程表单ID
     * @return 流程表单
     */
    @Override
    public WfFormVo queryById(String formId) {
        return baseMapper.selectVoById(formId);
    }

    /**
     * 查询流程表单列表
     *
     * @param bo 流程表单
     * @return 流程表单
     */
    @Override
    public TableDataInfo<WfFormVo> queryPageList(WfFormBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WfForm> lqw = buildQueryWrapper(bo);
        Page<WfFormVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);

        return TableDataInfo.build(result);
    }

    /**
     * 查询流程表单列表（导出）
     *
     * @param bo 流程表单
     * @return 流程表单
     */
    @Override
    public List<WfFormVo> queryList(WfFormBo bo) {
        LambdaQueryWrapper<WfForm> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 新增流程表单
     *
     * @param bo 流程表单
     * @return 结果
     */
    @Override
    public WfForm insertForm(WfFormBo bo) {
        WfForm add = BeanUtil.toBean(bo, WfForm.class);
        if (bo.getFormId() != null) {
            add.setFormId(bo.getFormId());
        }
        if (StringUtils.isNotBlank(bo.getTenantId())) {
            add.setTenantId(bo.getTenantId());
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setCategoryId(add.getCategoryId());
        }
        if (StringUtils.isNotBlank(bo.getModelId())) {
            WfModelBo wfModelBo = new WfModelBo();
            wfModelBo.setModelId(bo.getModelId());
            wfModelBo.setFormId(add.getFormId());
            wfModelService.updateModel(wfModelBo);
        }
        return add;
    }

    /**
     * 修改流程表单
     *
     * @param bo 流程表单
     * @return 结果
     */
    @Override
    public int updateForm(WfFormBo bo) {
        return baseMapper.update(new WfForm(), new LambdaUpdateWrapper<WfForm>()
            .set(StrUtil.isNotBlank(bo.getFormName()), WfForm::getFormName, bo.getFormName())
            .set(StrUtil.isNotBlank(bo.getContent()), WfForm::getContent, bo.getContent())
            .set(StrUtil.isNotBlank(bo.getCategoryId()), WfForm::getCategoryId, bo.getCategoryId())
            .set(StrUtil.isNotBlank(bo.getRemark()), WfForm::getRemark, bo.getRemark())
            .eq(WfForm::getFormId, bo.getFormId()));
    }

    /**
     * 批量删除流程表单
     *
     * @param ids 需要删除的流程表单ID
     * @return 结果
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        for (Long id : ids) {
            List<WfDeployForm> wfDeployForms = wfDeployFormMapper.selectList(
                new LambdaQueryWrapper<WfDeployForm>().eq(WfDeployForm::getFormKey, "key_" + id)
            );
            if (wfDeployForms != null && !wfDeployForms.isEmpty()) {
                return false;
            }
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    private LambdaQueryWrapper<WfForm> buildQueryWrapper(WfFormBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WfForm> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getFormName()), WfForm::getFormName, bo.getFormName());
        lqw.eq(StringUtils.isNotBlank(bo.getIsFormComponents()), WfForm::getIsFormComponents, bo.getIsFormComponents());
        lqw.eq(StringUtils.isNotBlank(bo.getCategoryId()), WfForm::getCategoryId, bo.getCategoryId());
        lqw.eq(bo.getFormId() != null, WfForm::getFormId, bo.getFormId());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), WfForm::getType, bo.getType());
        if (bo.getWfFormApp()) {
            List<String> list = wfFormAppService.queryList(new WfFormAppBo())
                .map(item -> item.stream().map(WfFormAppVo::getCategoryId).collect(Collectors.toList()))
                .orElse(null);
            if (ObjectUtil.isEmpty(list)) {
                lqw.apply("1 = 0");
                return lqw;
            }
            lqw.in(ObjectUtil.isNotEmpty(list), WfForm::getCategoryId, list);
        } else if (bo.getWfFormSynthesis()) {
            List<String> list = wfFormSynthesisService.queryList(new WfFormSynthesisBo())
                .map(item -> item.stream().map(WfFormSynthesisVo::getCategoryId).collect(Collectors.toList()))
                .orElse(null);
            if (list == null) {
                lqw.apply("1 = 0");
                return lqw;
            }
            lqw.in(ObjectUtil.isNotEmpty(list), WfForm::getCategoryId, list);
        }
        return lqw;
    }
}
