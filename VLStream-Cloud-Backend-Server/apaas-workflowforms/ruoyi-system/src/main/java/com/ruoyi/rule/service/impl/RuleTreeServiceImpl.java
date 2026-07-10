package com.ruoyi.rule.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.rule.bo.RuleTreeBo;
import com.ruoyi.rule.domain.RuleTree;
import com.ruoyi.rule.mapper.RuleTreeMapper;
import com.ruoyi.rule.service.IRuleTreeService;
import com.ruoyi.rule.vo.RuleTreeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 规则树Service业务层处理
 *
 * @author 雷超群
 * @date 2024-12-18
 */
@RequiredArgsConstructor
@Service
public class RuleTreeServiceImpl implements IRuleTreeService {

    private final RuleTreeMapper baseMapper;

    /**
     * 查询规则树
     */
    @Override
    public RuleTreeVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }


    /**
     * 查询规则树列表
     */
    @Override
    public List<RuleTreeVo> queryList(RuleTreeBo bo) {
        LambdaQueryWrapper<RuleTree> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<RuleTree> buildQueryWrapper(RuleTreeBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<RuleTree> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getUserId()), RuleTree::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getParentId()), RuleTree::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), RuleTree::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), RuleTree::getDescription, bo.getDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), RuleTree::getType, bo.getType());
        if (StringUtils.isBlank(bo.getId()) && StringUtils.isBlank(bo.getParentId())) {
            lqw.isNull(RuleTree::getParentId);  // 如果bo.getId()和bo.getParentId()都为空，查询parent_id为NULL
        }
        return lqw;
    }

    /**
     * 新增规则树
     */
    @Override
    public Boolean insertByBo(RuleTreeBo bo) {
        RuleTree add = BeanUtil.toBean(bo, RuleTree.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        return flag;
    }

    /**
     * 修改规则树
     */
    @Override
    public Boolean updateByBo(RuleTreeBo bo) {
        RuleTree update = BeanUtil.toBean(bo, RuleTree.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(RuleTree entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除规则树
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
