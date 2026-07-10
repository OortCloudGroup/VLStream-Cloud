package com.ruoyi.rule.mapper;


import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.rule.domain.RuleTree;
import com.ruoyi.rule.vo.RuleTreeVo;

import java.util.List;

/**
 * 规则树Mapper接口
 *
 * @author 雷超群
 * @date 2024-12-17
 */
public interface RuleTreeMapper extends BaseMapperPlus<RuleTreeMapper, RuleTree, RuleTreeVo> {
    /**
     * 查询规则树
     *
     * @param id 规则树主键
     * @return 规则树
     */
    public RuleTree selectRuleTreeById(String id);

    /**
     * 查询规则树列表
     *
     * @param ruleTree 规则树
     * @return 规则树集合
     */
    public List<RuleTree> selectRuleTreeList(RuleTree ruleTree);


    /**
     * 修改规则树
     *
     * @param ruleTree 规则树
     * @return 结果
     */
    public int updateRuleTree(RuleTree ruleTree);

    /**
     * 删除规则树
     *
     * @param id 规则树主键
     * @return 结果
     */
    public int deleteRuleTreeById(String id);

    /**
     * 批量删除规则树
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteRuleTreeByIds(String[] ids);
}
