package com.ruoyi.rule.service;


import com.ruoyi.rule.bo.RuleTreeBo;
import com.ruoyi.rule.vo.RuleTreeVo;

import java.util.Collection;
import java.util.List;

/**
 * 规则树Service接口
 *
 * @author 雷超群
 * @date 2024-12-17
 */
public interface IRuleTreeService
{
    /**
     * 查询规则树
     */
    RuleTreeVo queryById(String id);


    /**
     * 查询规则树列表
     */
    List<RuleTreeVo> queryList(RuleTreeBo bo);

    /**
     * 新增规则树
     */
    Boolean insertByBo(RuleTreeBo bo);

    /**
     * 修改规则树
     */
    Boolean updateByBo(RuleTreeBo bo);

    /**
     * 校验并批量删除规则树信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
