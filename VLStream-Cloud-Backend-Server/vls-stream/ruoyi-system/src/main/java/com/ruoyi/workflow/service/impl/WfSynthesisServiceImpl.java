package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.workflow.domain.WfSynthesis;
import com.ruoyi.workflow.domain.bo.WfSynthesisBo;
import com.ruoyi.workflow.domain.vo.WfSynthesisVo;
import com.ruoyi.workflow.mapper.WfSynthesisMapper;
import com.ruoyi.workflow.service.IWfSynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 综合通用流程Service业务层处理
 *
 * @author 雷超群
 * @date 2025-01-04
 */
@RequiredArgsConstructor
@Service
public class WfSynthesisServiceImpl extends ServiceImpl<WfSynthesisMapper, WfSynthesis> implements IWfSynthesisService {

    @Resource
    WfSynthesisMapper baseMapper;
    private final ValidateService validateService;


    /**
     * 查询综合通用流程
     */
    @Override
    public WfSynthesisVo queryById(String synthesisId) {
        return baseMapper.selectVoById(synthesisId);
    }


    /**
     * 查询综合通用流程列表
     */
    @Override
    public List<WfSynthesisVo> queryList(WfSynthesisBo bo) {
        LambdaQueryWrapper<WfSynthesis> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public List<WfSynthesisVo> queryListAll(String categoryName) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<WfSynthesis>().eq(StringUtils.isNotBlank(categoryName), WfSynthesis::getCategoryName, categoryName));
    }


    private LambdaQueryWrapper<WfSynthesis> buildQueryWrapper(WfSynthesisBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WfSynthesis> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getParentId()), WfSynthesis::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getCategoryName()), WfSynthesis::getCategoryName, bo.getCategoryName());
        if (StringUtils.isBlank(bo.getSynthesisId()) && StringUtils.isBlank(bo.getParentId())) {
            lqw.isNull(WfSynthesis::getParentId);  // 如果bo.getId()和bo.getParentId()都为空，查询parent_id为NULL
        }
        lqw.eq(WfSynthesis::getDelFlag,"0");
        return lqw;
    }

    /**
     * 新增综合通用流程
     */
    @Override
    public Boolean insertByBo(WfSynthesisBo bo) {
        WfSynthesis add = BeanUtil.toBean(bo, WfSynthesis.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setSynthesisId(add.getSynthesisId());
        }
        return flag;
    }

    /**
     * 修改综合通用流程
     */
    @Override
    public Boolean updateByBo(WfSynthesisBo bo) {
        WfSynthesis update = BeanUtil.toBean(bo, WfSynthesis.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WfSynthesis entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除综合通用流程
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            //效验这个分类下是否还有别的数据
            validateService.validateBeforeDeletion(ids);
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public List<String> selectChildById(String parentId) {
        return baseMapper.selectChildById(parentId);
    }
}
