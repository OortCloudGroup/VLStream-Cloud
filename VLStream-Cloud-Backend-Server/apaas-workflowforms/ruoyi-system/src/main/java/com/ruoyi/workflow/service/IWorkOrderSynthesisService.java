package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.workflow.domain.WorkOrderSynthesis;
import com.ruoyi.workflow.domain.bo.WorkOrderSynthesisBo;
import com.ruoyi.workflow.domain.vo.WorkOrderSynthesisVo;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 综合工单流程Service接口
 *
 * @author Lei Chao Qun
 * @date 2025-01-04
 */
public interface IWorkOrderSynthesisService extends IService<WorkOrderSynthesis> {

    /**
     * 查询综合工单流程
     */
    WorkOrderSynthesisVo queryById(String synthesisId);

    /**
     * 查询综合工单流程列表
     */
    List<WorkOrderSynthesisVo> queryList(WorkOrderSynthesisBo bo);

    /**
     * 新增综合工单流程
     */
    Boolean insertByBo(WorkOrderSynthesisBo bo);

    /**
     * 修改综合工单流程
     */
    Boolean updateByBo(WorkOrderSynthesisBo bo);

    /**
     * 校验并批量删除综合工单流程信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);

    /**
     * 根据父id递归查询子节点
     */
    List<String> selectChildById(@Param("parentId") String parentId);

    List<WorkOrderSynthesisVo> queryListAll(String categoryName);

}
