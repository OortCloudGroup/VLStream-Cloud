/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * workflow object
 *
 * @author KonBAI
 * @createTime 2022/8/7 15:01
 */
@Data
public class WfDetailVo {

    /**
     * taskforminfo
     */
    private List<Object> taskFormData;

    /**
     * history workflownodeinfo
     */
    private List<WfProcNodeVo> historyProcNodeList;

    /**
     * workflowform
     */
    private List<Object> processFormList;

    /**
     * workflowXML
     */
    private String bpmnXml;
    /**
     * workflowXML
     */
    private String bpmnJson;

    private WfViewerVo flowViewer;
    /**
     * workflow info
     */
    private WfBasicInfoVo wfBasicInfoVo;
    /**
     * nodeextension properties
     */
    private Map<String, String> extensionMap;
    /**
     * nodebuttoncontrol
     */
    private Map<String, String> buttonsMap;
    /**
     * all approverID
     */
    private List<String> approverIds;

    /**
     * whether in taskforminfo
     *
     * @return true: in ; false: in
     */
    public Boolean isExistTaskForm() {
        return ObjectUtil.isNotEmpty(this.taskFormData);
    }


}
