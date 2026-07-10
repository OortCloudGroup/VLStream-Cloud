package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.workflow.domain.WfFormSynthesis;
import com.ruoyi.workflow.domain.bo.WfFormSynthesisBo;
import com.ruoyi.workflow.domain.vo.WfFormSynthesisVo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;



/**
 * 表单分类Service接口
 *
 * @author 雷超群
 * @date 2024-12-25
 */
public interface IWfFormSynthesisService extends IService<WfFormSynthesis> {

    /**
     * 查询表单分类
     */
    WfFormSynthesisVo queryById(String categoryId);


    /**
     * 查询表单分类列表
     */
    Optional<List<WfFormSynthesisVo>> queryList(WfFormSynthesisBo bo);

    /**
     * 新增表单分类
     */
    Boolean insertByBo(WfFormSynthesisBo bo);

    /**
     * 修改表单分类
     */
    Boolean updateByBo(WfFormSynthesisBo bo);

    /**
     * 校验并批量删除表单分类信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
