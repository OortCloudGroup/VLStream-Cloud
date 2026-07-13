package com.ruoyi.workflow.domain.vo;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程详情视图对象
 *
 * @author KonBAI
 * @createTime 2022/8/7 15:01
 */
@Data
public class WfDetailVo {

    /**
     * 任务表单信息
     */
    private List<Object> taskFormData;

    /**
     * 历史流程节点信息
     */
    private List<WfProcNodeVo> historyProcNodeList;

    /**
     * 流程表单列表
     */
    private List<Object> processFormList;

    /**
     * 流程XML
     */
    private String bpmnXml;
    /**
     * 流程XML
     */
    private String bpmnJson;

    private WfViewerVo flowViewer;
    /**
     * 流程基本信息
     */
    private WfBasicInfoVo wfBasicInfoVo;
    /**
     * 节点扩展属性
     */
    private Map<String, String> extensionMap;
    /**
     * 节点按钮控制
     */
    private Map<String, String> buttonsMap;
    /**
     * 所有审批人ID列表
     */
    private List<String> approverIds;

    /**
     * 是否存在任务表单信息
     * 
     * @return true:存在；false:不存在
     */
    public Boolean isExistTaskForm() {
        return ObjectUtil.isNotEmpty(this.taskFormData);
    }


}
