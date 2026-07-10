package com.ruoyi.workflow.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.workflow.domain.bo.WfModelBo;
import com.ruoyi.workflow.domain.vo.WfModelVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.flowable.engine.repository.Model;

import java.util.List;
@Mapper
public interface WfModelMapper extends BaseMapperPlus<WfModelMapper, Model, WfModelVo> {

    /**
     * 查询模型总数
     * @param modelBo
     * @param wfSyntheses
     * @param workOrderSyntheses
     * @param tenantId
     * @return
     */
    Long selectModelCount(@Param("modelBo") WfModelBo modelBo, @Param("wfSyntheses") List<String> wfSyntheses, @Param("workOrderSyntheses") List<String> workOrderSyntheses,@Param("tenantId")String tenantId,@Param("history")Boolean history);
    /**
     * 查询模型列表
     */
    List<Model> selectModelList(@Param("modelBo") WfModelBo modelBo, @Param("page") IPage<Model> page, @Param("wfSyntheses") List<String> wfSyntheses, @Param("workOrderSyntheses") List<String> workOrderSyntheses, @Param("tenantId")String tenantId,@Param("history")Boolean history);
}
