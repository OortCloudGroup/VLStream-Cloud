/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.common.utils.ApiHeaderUtil;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.flowable.common.constant.ProcessConstants;
import com.ruoyi.flowable.common.constant.TaskConstants;
import com.ruoyi.flowable.common.enums.FormType;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.flowable.utils.ModelUtils;
import com.ruoyi.workflow.convert.ProcessModel;
import com.ruoyi.workflow.convert.node.EndNode;
import com.ruoyi.workflow.convert.node.InitiatorNode;
import com.ruoyi.workflow.convert.node.StartNode;
import com.ruoyi.workflow.domain.*;
import com.ruoyi.workflow.domain.bo.*;
import com.ruoyi.workflow.domain.dto.WfMetaInfoDto;
import com.ruoyi.workflow.domain.vo.ReModelJsonVo;
import com.ruoyi.workflow.domain.vo.WfFormVo;
import com.ruoyi.workflow.domain.vo.WfModelVo;
import com.ruoyi.workflow.mapper.ProcessTemplateMapper;
import com.ruoyi.workflow.mapper.ReModelJsonMapper;
import com.ruoyi.workflow.mapper.WfModelMapper;
import com.ruoyi.workflow.service.*;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.service.IWorkOrderService;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author KonBAI
 * @createTime 2022/6/21 9:11
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class WfModelServiceImpl extends FlowServiceFactory implements IWfModelService {

    private static final List<String> REQUIRED_INIT_FORM_NAMES = Arrays.asList(
            "租户审批",
            "企业主体审批",
            "部门审批",
            "用户审批",
            "用户权限提升申请审批",
            "租户认证审批");

    private static final String ROOT_TENANT_ID = "0e391fd7-1033-4f09-88c0-187582fee462";

    private static final String INIT_TO_TENANT_ID_CACHE_KEY = "to_tenant_id";

    private static final String EVENT_MANAGEMENT_APPLICATION_NAME = "事件管理";

    private static final String EVENT_MANAGEMENT_APPLICATION_ID = "818301f0e77f4cd8a117414cbeb32d9e";

    private static final String EVENT_MANAGEMENT_APPLICATION_SECRET = "5f0de11687d744bc95e84e207d319493";

    private final IWfFormService formService;
    private final IWfDeployFormService deployFormService;
    private final IReModeJsonService remodeJsonService;
    private final WfModelMapper wfModelMapper;
    private final ProcessTemplateMapper processTemplateMapper;
    private final ReModelJsonMapper reModelJsonMapper;
    private final IWfAppService wfAppService;
    private final IWfSynthesisService wfSynthesisService;
    private final IWorkOrderAppService workOrderAppService;
    private final IWfFormAppService wfFormAppService;
    private final IWorkOrderSynthesisService workerSynthesisService;
    private final IWorkOrderService workOrderService;
    private final IReModeJsonService reModeJsonService;
    private final IWfFormSynthesisService iWfFormSynthesisService;
    private final IWfDeployService deployService;
//    @Value("${http.get-tenant-admin}")
    private String getTenantAdmin;

    /**
     * Get current user; Initialize etc. null, null / empty token Redis.
     */
    private SysUser getCurrentSysUser() {
        String token = AuthorizationInterceptor.getToken();
        if (StringUtils.isBlank(token)) {
            return null;
        }
        return RedisUtils.getCacheObject(token);
    }

    /**
     * Get in current user; interface .
     */
    private SysUser getCurrentSysUserRequired() {
        SysUser sysUser = getCurrentSysUser();
        if (sysUser == null) {
            throw new ServiceException("当前登录用户信息不存在");
        }
        return sysUser;
    }

    /**
     * model ID Delete model、workflow info、all model 、 instance and history data
     *
     * @param modelId need to Delete model ID
     * @param isWorkOrder
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelCascade(String modelId, boolean isWorkOrder) {
        // 1. Query current model, Get key
        Model currentModel = repositoryService.getModel(modelId);
        if (currentModel == null) {
            throw new FlowableObjectNotFoundException("无法找到模型，id=" + modelId);
        }
        String modelKey = currentModel.getKey();

        // 2. Query key all model
        List<Model> allVersions = repositoryService.createModelQuery()
                .modelKey(modelKey)
                .list();

        // 3. each , , model
        for (Model versionModel : allVersions) {
            String versionModelId = versionModel.getId();
            String deploymentId = versionModel.getDeploymentId();

            if (isWorkOrder) {
                LambdaQueryWrapper<WorkOrder> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(WorkOrder::getProcessKey, versionModel.getKey());
                workOrderService.remove(queryWrapper);
            }
            // if already , Delete all workflow instance and history data
            if (deploymentId != null) {
                repositoryService.deleteDeployment(deploymentId, true);
            }
            reModeJsonService.removeById(versionModelId);

            // Delete model ( will Delete editor bytearrays)
            repositoryService.deleteModel(versionModelId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean eventManagementInitStart(InitBo initBo) {
        validateEventManagementInitBo(initBo);
        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
        try {
            String sourceTenantId = resolveEventManagementSourceTenantId(initBo.getTenantId());
            String targetTenantId = initBo.getToTenantId();
            WorkOrderApp sourceApp = getEventManagementWorkOrderApp(sourceTenantId);
            if (Objects.equals(sourceTenantId, targetTenantId)) {
                deployEventManagementModelsIfNeeded(sourceTenantId, sourceApp);
                return true;
            }
            int sourceModelCount = getSourceEventManagementModelCount(sourceTenantId, sourceApp);
            if (isEventManagementTenantReady(targetTenantId, sourceModelCount)) {
                return true;
            }

            WorkOrderApp targetApp = getOrCreateEventManagementWorkOrderApp(targetTenantId, sourceApp);
            WfFormApp targetFormApp = getOrCreateEventManagementFormApp(targetTenantId);
            List<Model> sourceModels = getSourceEventManagementModels(sourceTenantId, sourceApp);
            if (sourceModels.isEmpty()) {
                throw new RuntimeException("事件管理初始化失败：源租户流程模型为空");
            }
            for (Model sourceModel : sourceModels) {
                cloneAndDeployEventManagementModel(sourceTenantId, targetTenantId, targetApp, targetFormApp, sourceModel);
            }
            return true;
        } finally {
            RedisUtils.deleteObject(INIT_TO_TENANT_ID_CACHE_KEY);
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    /**
     * Validate event Initialize parameter.
     */
    private void validateEventManagementInitBo(InitBo initBo) {
        if (initBo == null || StringUtils.isBlank(initBo.getTenantId()) || StringUtils.isBlank(initBo.getToTenantId())) {
            throw new RuntimeException("事件管理初始化失败：tenant_id 和 to_tenant_id 不能为空");
        }
    }

    /**
     * event , .
     */
    private String resolveEventManagementSourceTenantId(String sourceTenantId) {
        WorkOrderApp sourceApp = getEventManagementWorkOrderApp(sourceTenantId);
        if (sourceApp != null && getSourceEventManagementModelCount(sourceTenantId, sourceApp) > 0) {
            return sourceTenantId;
        }
        WorkOrderApp rootApp = getEventManagementWorkOrderApp(ROOT_TENANT_ID);
        if (rootApp != null && getSourceEventManagementModelCount(ROOT_TENANT_ID, rootApp) > 0) {
            return ROOT_TENANT_ID;
        }
        throw new RuntimeException("事件管理初始化失败：未找到完整的顶级租户模板数据");
    }

    /**
     * Get event work order .
     */
    private WorkOrderApp getEventManagementWorkOrderApp(String tenantId) {
        return workOrderAppService.getOne(new LambdaQueryWrapper<WorkOrderApp>()
                .eq(WorkOrderApp::getApplicationId, EVENT_MANAGEMENT_APPLICATION_ID)
                .eq(WorkOrderApp::getApplicationSecret, EVENT_MANAGEMENT_APPLICATION_SECRET)
                .eq(WorkOrderApp::getTenantId, tenantId)
                .eq(WorkOrderApp::getDelFlag, "0")
                .last("limit 1"));
    }

    /**
     * Get event work order .
     */
    private WorkOrderApp getOrCreateEventManagementWorkOrderApp(String tenantId, WorkOrderApp sourceApp) {
        WorkOrderApp exists = getEventManagementWorkOrderApp(tenantId);
        if (exists != null) {
            return exists;
        }
        WorkOrderApp add = new WorkOrderApp();
        add.setApplicationName(EVENT_MANAGEMENT_APPLICATION_NAME);
        add.setApplicationId(EVENT_MANAGEMENT_APPLICATION_ID);
        add.setApplicationSecret(EVENT_MANAGEMENT_APPLICATION_SECRET);
        add.setTenantId(tenantId);
        add.setAppFlag("0");
        add.setImages(sourceApp == null ? null : sourceApp.getImages());
        workOrderAppService.save(add);
        return add;
    }

    /**
     * Get event form .
     */
    private WfFormApp getOrCreateEventManagementFormApp(String tenantId) {
        WfFormApp exists = wfFormAppService.getOne(new LambdaQueryWrapper<WfFormApp>()
                .eq(WfFormApp::getApplicationId, EVENT_MANAGEMENT_APPLICATION_ID)
                .eq(WfFormApp::getApplicationSecret, EVENT_MANAGEMENT_APPLICATION_SECRET)
                .eq(WfFormApp::getTenantId, tenantId)
                .eq(WfFormApp::getType, "1")
                .eq(WfFormApp::getDelFlag, "0")
                .last("limit 1"));
        if (exists != null) {
            return exists;
        }
        WfFormApp add = new WfFormApp();
        add.setApplicationName(EVENT_MANAGEMENT_APPLICATION_NAME);
        add.setApplicationId(EVENT_MANAGEMENT_APPLICATION_ID);
        add.setApplicationSecret(EVENT_MANAGEMENT_APPLICATION_SECRET);
        add.setTenantId(tenantId);
        add.setType("1");
        add.setAppFlag("0");
        wfFormAppService.save(add);
        return add;
    }

    /**
     * Query event new workflowmodel.
     */
    private List<Model> getSourceEventManagementModels(String tenantId, WorkOrderApp app) {
        if (app == null) {
            return Collections.emptyList();
        }
        return repositoryService.createModelQuery()
                .modelTenantId(tenantId)
                .modelCategory(app.getAppId())
                .latestVersion()
                .list();
    }

    /**
     * event model、form and JSON, .
     */
    private void cloneAndDeployEventManagementModel(String sourceTenantId, String targetTenantId,
                                                    WorkOrderApp targetApp, WfFormApp targetFormApp, Model sourceModel) {
        if (hasTargetEventManagementModel(targetTenantId, targetApp.getAppId(), sourceModel.getName())) {
            return;
        }
        WfMetaInfoDto metaInfo = JsonUtils.parseObject(sourceModel.getMetaInfo(), WfMetaInfoDto.class);
        if (metaInfo == null || StringUtils.isBlank(metaInfo.getFormId())) {
            throw new RuntimeException("事件管理初始化失败：源模型未绑定表单，modelId=" + sourceModel.getId());
        }
        WfForm sourceForm = getEventManagementSourceForm(sourceTenantId, metaInfo.getFormId());
        WfForm targetForm = getOrCreateEventManagementTargetForm(targetTenantId, targetFormApp.getCategoryId(), sourceForm);
        metaInfo.setFormId(String.valueOf(targetForm.getFormId()));

        Model newModel = repositoryService.newModel();
        newModel.setName(sourceModel.getName());
        newModel.setKey("Process_" + System.currentTimeMillis() + RandomUtil.randomNumbers(4));
        newModel.setCategory(targetApp.getAppId());
        newModel.setMetaInfo(JsonUtils.toJsonString(metaInfo));
        newModel.setTenantId(targetTenantId);
        repositoryService.saveModel(newModel);

        byte[] bpmnBytes = repositoryService.getModelEditorSource(sourceModel.getId());
        if (ArrayUtil.isEmpty(bpmnBytes)) {
            throw new RuntimeException("事件管理初始化失败：源模型 BPMN 为空，modelId=" + sourceModel.getId());
        }
        BpmnModel bpmnModel = ModelUtils.getBpmnModel(StringUtils.toEncodedString(bpmnBytes, StandardCharsets.UTF_8));
        BpmnModel targetBpmnModel = updateBpmnFormBinding(bpmnModel, String.valueOf(targetForm.getFormId()), newModel.getKey());
        byte[] xmlBytes = new BpmnXMLConverter().convertToXML(targetBpmnModel);
        repositoryService.addModelEditorSource(newModel.getId(), xmlBytes);

        WfModelBo saveBo = new WfModelBo();
        saveBo.setBpmnXml(new String(xmlBytes, StandardCharsets.UTF_8));
        saveBo.setModelId(newModel.getId());
        saveBo.setModelName(sourceModel.getName());
        Model finalModel = saveModel(saveBo, targetTenantId, null);
        copyEventManagementModelJson(sourceModel.getId(), finalModel.getId(), finalModel.getKey(),
                String.valueOf(targetForm.getFormId()), targetTenantId);
        deployEventManagementModel(finalModel.getId(), targetTenantId);
    }

    /**
     * Check whether already event model.
     */
    private boolean hasTargetEventManagementModel(String tenantId, String appId, String modelName) {
        return repositoryService.createModelQuery()
                .modelTenantId(tenantId)
                .modelCategory(appId)
                .modelName(modelName)
                .list()
                .stream()
                .anyMatch(model -> StringUtils.isNotBlank(model.getDeploymentId()));
    }

    /**
     * Query model form.
     */
    private WfForm getEventManagementSourceForm(String sourceTenantId, String formId) {
        WfForm wfForm = formService.getOne(new LambdaQueryWrapper<WfForm>()
                .eq(WfForm::getFormId, formId)
                .eq(WfForm::getTenantId, sourceTenantId)
                .last("AND del_flag = '0' LIMIT 1"));
        if (wfForm == null) {
            throw new RuntimeException("事件管理初始化失败：源表单不存在，formId=" + formId);
        }
        return wfForm;
    }

    /**
     * form in , form.
     */
    private WfForm getOrCreateEventManagementTargetForm(String tenantId, String targetFormCategoryId, WfForm sourceForm) {
        WfForm exists = formService.getOne(new LambdaQueryWrapper<WfForm>()
                .eq(WfForm::getTenantId, tenantId)
                .eq(WfForm::getCategoryId, targetFormCategoryId)
                .eq(WfForm::getFormName, sourceForm.getFormName())
                .last("AND del_flag = '0' LIMIT 1"));
        if (exists != null) {
            return exists;
        }
        WfFormBo formBo = new WfFormBo();
        formBo.setFormName(sourceForm.getFormName());
        formBo.setContent(sourceForm.getContent());
        formBo.setRemark(sourceForm.getRemark());
        formBo.setIsFormComponents("0");
        formBo.setFormType(sourceForm.getFormType());
        formBo.setCategoryId(targetFormCategoryId);
        formBo.setTenantId(tenantId);
        formBo.setType(sourceForm.getType());
        return formService.insertForm(formBo);
    }

    /**
     * model JSON, new model key and form key.
     */
    private void copyEventManagementModelJson(String sourceModelId, String targetModelId, String targetModelKey,
                                              String targetFormId, String targetTenantId) {
        ReModelJsonVo reModelJsonVo = reModeJsonService.queryById(sourceModelId);
        if (reModelJsonVo == null || StringUtils.isBlank(reModelJsonVo.getJsonContent())) {
            throw new RuntimeException("事件管理初始化失败：源模型 JSON 不存在，modelId=" + sourceModelId);
        }
        JSONObject jsonObject = JSON.parseObject(reModelJsonVo.getJsonContent());
        jsonObject.put("code", targetModelKey);
        updateFormKey(jsonObject.getJSONObject("process"), targetFormId);
        ReModeJsonBo remodeJsonBo = new ReModeJsonBo();
        remodeJsonBo.setJsonContent(jsonObject.toJSONString());
        remodeJsonBo.setModelId(targetModelId);
        remodeJsonBo.setTenantId(targetTenantId);
        reModeJsonService.insertByBo(remodeJsonBo);
    }

    /**
     * in Initialize in model.
     */
    private void deployEventManagementModel(String modelId, String tenantId) {
        normalizeEventManagementModelBpmnFormKey(modelId);
        RedisUtils.setCacheObject(INIT_TO_TENANT_ID_CACHE_KEY, tenantId, Duration.ofMinutes(1));
        try {
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            deployModel(modelId);
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
            RedisUtils.deleteObject(INIT_TO_TENANT_ID_CACHE_KEY);
        }
    }

    /**
     * seed in is JSON form id, BPMN need to Convert key_formid.
     */
    private void normalizeEventManagementModelBpmnFormKey(String modelId) {
        Model model = repositoryService.getModel(modelId);
        if (model == null) {
            throw new RuntimeException("事件管理初始化失败：模型不存在，modelId=" + modelId);
        }
        WfMetaInfoDto metaInfo = JsonUtils.parseObject(model.getMetaInfo(), WfMetaInfoDto.class);
        if (metaInfo == null || StringUtils.isBlank(metaInfo.getFormId())) {
            throw new RuntimeException("事件管理初始化失败：模型未绑定表单，modelId=" + modelId);
        }
        byte[] bpmnBytes = repositoryService.getModelEditorSource(modelId);
        if (bpmnBytes == null || bpmnBytes.length == 0) {
            throw new RuntimeException("事件管理初始化失败：模型 BPMN 不存在，modelId=" + modelId);
        }
        BpmnModel bpmnModel = ModelUtils.getBpmnModel(StringUtils.toEncodedString(bpmnBytes, StandardCharsets.UTF_8));
        BpmnModel normalizedBpmnModel = updateBpmnFormBinding(bpmnModel, metaInfo.getFormId(), model.getKey());
        repositoryService.addModelEditorSource(modelId, new BpmnXMLConverter().convertToXML(normalizedBpmnModel));
    }

    /**
     * seed already not , event model.
     */
    private void deployEventManagementModelsIfNeeded(String tenantId, WorkOrderApp app) {
        for (Model model : getSourceEventManagementModels(tenantId, app)) {
            if (repositoryService.createProcessDefinitionQuery()
                    .processDefinitionTenantId(tenantId)
                    .processDefinitionKey(model.getKey())
                    .count() == 0) {
                deployEventManagementModel(model.getId(), tenantId);
            }
        }
    }

    /**
     * event model .
     */
    private int getSourceEventManagementModelCount(String tenantId, WorkOrderApp app) {
        return getSourceEventManagementModels(tenantId, app).size();
    }

    /**
     * Check event whether already workflow.
     */
    private boolean isEventManagementTenantReady(String tenantId, int sourceModelCount) {
        WorkOrderApp app = getEventManagementWorkOrderApp(tenantId);
        if (app == null || sourceModelCount <= 0) {
            return false;
        }
        long deployedCount = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(tenantId)
                .processDefinitionCategory(app.getAppId())
                .count();
        return deployedCount >= sourceModelCount;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String initStart(InitBo initBo) {
        try {
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            initFormData(initBo);
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }

        String flag = null;
        try {
            flag = "同步完成";
            WfSynthesisBo wfSynthesisBO = new WfSynthesisBo();
            //
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            WfSynthesis wfSynthesisOne = wfSynthesisService
                    .getOne(new LambdaQueryWrapper<WfSynthesis>().eq(WfSynthesis::getCategoryName,
                            "系统默认流程").eq(WfSynthesis::getTenantId, initBo.getToTenantId()));
            if (wfSynthesisOne == null) {
                wfSynthesisBO.setCategoryName("系统默认流程");
                wfSynthesisBO.setTenantId(initBo.getToTenantId());
                wfSynthesisService.insertByBo(wfSynthesisBO);
            }
            InterceptorIgnoreHelper.clearIgnoreStrategy();
            //
            List<ProcessTemplate> processTemplateVos = processTemplateMapper.selectList(new LambdaQueryWrapper<>());
            for (ProcessTemplate processTemplateVo : processTemplateVos) {
                List<Model> list = repositoryService.createModelQuery().modelTenantId(initBo.getToTenantId())
                        .modelName(processTemplateVo.getModelName()).latestVersion().list();
                boolean deployedModelExists = Optional.ofNullable(list).orElse(Collections.emptyList()).stream()
                        .anyMatch(model -> StringUtils.isNotBlank(model.getDeploymentId())
                                && !Objects.equals(model.getKey(), processTemplateVo.getModelKey()));
                if (deployedModelExists) {
                    return "数据已存在";
                }
            }
            for (ProcessTemplate processTemplateVo : processTemplateVos) {
                log.info("开始初始化流程模板，tenantId={}, toTenantId={}, templateName={}, templateKey={}",
                        initBo.getTenantId(), initBo.getToTenantId(), processTemplateVo.getModelName(),
                        processTemplateVo.getModelKey());
                Model newModel = repositoryService.newModel();
                newModel.setName(processTemplateVo.getModelName());
                newModel.setKey("Process_" + System.currentTimeMillis());
                if (wfSynthesisOne == null) {
                    newModel.setCategory(wfSynthesisBO.getSynthesisId());
                } else {
                    newModel.setCategory(wfSynthesisOne.getSynthesisId());
                }
                InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
                LambdaQueryWrapper<WfForm> wfFormLambdaQueryWrapper = new LambdaQueryWrapper<>();

                if(newModel.getName().equals("用户自行申请租户审批")){
                    wfFormLambdaQueryWrapper.eq(WfForm::getFormName, "租户审批");
                }else{
                    wfFormLambdaQueryWrapper.eq(WfForm::getFormName, newModel.getName());
                }
                wfFormLambdaQueryWrapper.eq(WfForm::getTenantId, initBo.getToTenantId());
                WfForm wfFormOne = formService.getOne(wfFormLambdaQueryWrapper, false);
                InterceptorIgnoreHelper.clearIgnoreStrategy();
                if (wfFormOne == null) {
                    throw new ServiceException("未找到流程模板表单: " + newModel.getName());
                }
                String metaInfo = buildMetaInfo(new WfMetaInfoDto(), processTemplateVo.getDescription(), null,
                        processTemplateVo.getShowMobile(), wfFormOne.getFormId());
                newModel.setMetaInfo(metaInfo);
                newModel.setTenantId(initBo.getToTenantId());
                // workflowmodel
                repositoryService.saveModel(newModel);
                Model model = repositoryService.createModelQuery().modelKey(processTemplateVo.getModelKey())
                        .latestVersion().singleResult();
                if (model == null) {
                    throw new ServiceException("未找到公共流程模板模型: " + processTemplateVo.getModelKey());
                }
                byte[] bpmnBytes = repositoryService.getModelEditorSource(model.getId());
                if (ArrayUtil.isEmpty(bpmnBytes)) {
                    throw new ServiceException("公共流程模板BPMN为空: " + processTemplateVo.getModelKey());
                }
                String bpmnXml = StringUtils.toEncodedString(bpmnBytes, StandardCharsets.UTF_8);
                BpmnModel bpmnModel = ModelUtils.getBpmnModel(bpmnXml);
                // Get adminuser
                // String userId = getTenantAdmin(initBo);
                // new formKey
                BpmnModel bpmnModel1 = updateBpmnFormBinding(bpmnModel, String.valueOf(wfFormOne.getFormId()),
                        newModel.getKey());
                byte[] xmlBytes = new BpmnXMLConverter().convertToXML(bpmnModel1);
                repositoryService.addModelEditorSource(newModel.getId(), xmlBytes);
                WfModelBo wfModelBo = new WfModelBo();
                wfModelBo.setBpmnXml(new String(xmlBytes, StandardCharsets.UTF_8));
                wfModelBo.setModelId(newModel.getId());
                wfModelBo.setModelName(processTemplateVo.getModelName());
                // workflow after model
                InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
                Model finalModel = saveModel(wfModelBo, initBo.getToTenantId(), null);
                ReModelJson reModelJson = reModelJsonMapper.selectById(model.getId());
                if (reModelJson == null || StringUtils.isBlank(reModelJson.getJsonContent())) {
                    throw new ServiceException("公共流程模板JSON为空: " + processTemplateVo.getModelKey());
                }
                ReModeJsonBo remodeJsonBo = new ReModeJsonBo();
                JSONObject jsonObject1 = JSON.parseObject(reModelJson.getJsonContent());
                jsonObject1.put("code", finalModel.getKey());
                // Update formKey
                updateFormKey(jsonObject1.getJSONObject("process"), String.valueOf(wfFormOne.getFormId()));
                remodeJsonBo.setJsonContent(jsonObject1.toJSONString()); // JSON Set field in
                remodeJsonBo.setModelId(finalModel.getId());
                remodeJsonBo.setTenantId(initBo.getToTenantId());
                reModeJsonService.insertByBo(remodeJsonBo);
                InterceptorIgnoreHelper.clearIgnoreStrategy();
                // model
                RedisUtils.setCacheObject("to_tenant_id", initBo.getToTenantId(), Duration.ofMinutes(1));
                try {
                    InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
                    deployModel(finalModel.getId());
                } finally {
                    InterceptorIgnoreHelper.clearIgnoreStrategy();
                }
            }
        } catch (Exception e) {
            flag = "同步失败";
            log.error("流程模板初始化失败，tenantId={}, toTenantId={}", initBo.getTenantId(), initBo.getToTenantId(), e);
            throw new RuntimeException("请检查当前租户模型是否完整且正确: " + e.getMessage(), e);
        } finally {
            RedisUtils.deleteObject("to_tenant_id");
        }
        return flag;
    }

    /**
     * new JSON in formKey.
     * only formKey is empty node, will new to new value newFormKey.
     *
     * @param node current nodeobject(process)
     * @param newFormKey new formKey value
     */
    public JSONObject updateFormKey(JSONObject node, String newFormKey) {
        if (node == null) {
            return null;
        }

        // Get current node formKey
        String formKey = node.getString("formKey");
        // only formKey non- null / empty new
        if (StringUtils.isNotBlank(formKey)) {
            node.put("formKey", newFormKey);
        }

        node.put("approvalType", 3);
        JSONArray users = node.getJSONArray("users");
        if (ObjectUtil.isNotEmpty(users)) {
            users = new JSONArray();
            users.set(0, TaskConstants.PROCESS_INITIATOR);
            node.put("users", users);
        }

        // if in sub node, new
        JSONObject childNode = node.getJSONObject("childNode");
        if (childNode != null) {
            updateFormKey(childNode, newFormKey);
        }
        return node;
    }

    /**
     * Update formKey Update approver to new admin
     *
     * @param bpmnModel
     * @param newFormKey
     * @return
     */
    public BpmnModel updateBpmnFormBinding(BpmnModel bpmnModel, String newFormKey, String processId) {
        StartEvent startEvent = ModelUtils.getStartEvent(bpmnModel);
        if (startEvent != null) {
            startEvent.setFormKey("key_" + newFormKey);
        }
        Collection<UserTask> userTasks = ModelUtils.getAllUserTaskEvent(bpmnModel);
        for (UserTask userTask : userTasks) {
            userTask.setFormKey("key_" + newFormKey);
            // user_id Set userTask assignee property in
            userTask.setAssignee(String.format("${%s}", TaskConstants.PROCESS_INITIATOR));
        }
        // Update workflow
        bpmnModel.getMainProcess().setId(processId);
        return bpmnModel;
    }

    /**
     * idGet admininfo
     *
     * @param initBo
     * @return
     */
    public String getTenantAdmin(InitBo initBo) {
        String userId = null;
        // Get current parameter
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30,
                TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();
        Request.Builder requestBuilder = new Request.Builder().url(String.format("%s?tenant_id=%s", getTenantAdmin,
                initBo.getToTenantId())).get();
        // current all info, new in
        ApiHeaderUtil.transferHeaders(requestBuilder);
        // Build Execute
        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // Get Parse to
                String responseBody = response.body().string();

                // FastJSON Parse data
                JSONObject jsonResponse = JSON.parseObject(responseBody);

                // whether to 200
                int code = jsonResponse.getIntValue("code");
                if (code == 200) {
                    // user_id
                    userId = jsonResponse.getJSONObject("data").getString("user_id");

                } else {
                    // if is 200, recordlog
                    String msg = jsonResponse.getString("msg");
                    throw new RuntimeException("接口调用失败，错误信息：" + msg);
                }
            } else {
                // if successfully,
                throw new RuntimeException("接口调用失败，HTTP 状态码：" + response.code());
            }
        } catch (IOException e) {
            // IO Custom
            throw new RuntimeException("接口调用过程中发生异常", e);
        }
        return userId;
    }

    /**
     * Initialize formdata
     *
     * @param initBo
     */
    public void initFormData(InitBo initBo) {
        if (StringUtils.isBlank(initBo.getToTenantId())) {
            throw new ServiceException("目标租户不能为空");
        }

        WfFormSynthesis targetCategory = getSystemApprovalCategory(initBo.getToTenantId());
        if (targetCategory == null) {
            WfFormSynthesisBo targetCategoryBo = new WfFormSynthesisBo();
            targetCategoryBo.setCategoryName("系统审批");
            targetCategoryBo.setTenantId(initBo.getToTenantId());
            targetCategoryBo.setType("0");
            iWfFormSynthesisService.insertByBo(targetCategoryBo);
            targetCategory = getSystemApprovalCategory(initBo.getToTenantId());
        }
        if (targetCategory == null || StringUtils.isBlank(targetCategory.getCategoryId())) {
            throw new ServiceException("目标租户系统审批表单分类初始化失败");
        }

        WfFormSynthesis sourceCategory = getSystemApprovalCategory(initBo.getTenantId());
        if (!hasRequiredTemplateForms(sourceCategory)) {
            sourceCategory = findSeedSystemApprovalCategory();
        }
        if (!hasRequiredTemplateForms(sourceCategory)) {
            throw new ServiceException("公共流程模板表单未初始化，请检查工作流模板种子数据");
        }

        LambdaQueryWrapper<WfForm> sourceFormWrapper = new LambdaQueryWrapper<>();
        sourceFormWrapper.eq(WfForm::getCategoryId, sourceCategory.getCategoryId());
        sourceFormWrapper.eq(WfForm::getTenantId, sourceCategory.getTenantId());
        sourceFormWrapper.eq(WfForm::getType, "0");
        sourceFormWrapper.in(WfForm::getFormName, REQUIRED_INIT_FORM_NAMES);
        List<WfForm> sourceForms = formService.list(sourceFormWrapper);
        if (ObjectUtil.isEmpty(sourceForms)) {
            throw new ServiceException("公共流程模板表单为空，请检查工作流模板种子数据");
        }

        for (WfForm sourceForm : sourceForms) {
            if (!REQUIRED_INIT_FORM_NAMES.contains(sourceForm.getFormName())) {
                continue;
            }
            if (existsTenantForm(initBo.getToTenantId(), sourceForm.getFormName())) {
                continue;
            }
            WfFormBo targetFormBo = new WfFormBo();
            targetFormBo.setFormName(sourceForm.getFormName());
            targetFormBo.setContent(sourceForm.getContent());
            targetFormBo.setRemark(sourceForm.getRemark());
            targetFormBo.setIsFormComponents("0");
            targetFormBo.setFormType(sourceForm.getFormType());
            targetFormBo.setType("0");
            targetFormBo.setCategoryId(targetCategory.getCategoryId());
            targetFormBo.setTenantId(initBo.getToTenantId());
            formService.insertForm(targetFormBo);
        }
    }

    private WfFormSynthesis getSystemApprovalCategory(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return null;
        }
        LambdaQueryWrapper<WfFormSynthesis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFormSynthesis::getCategoryName, "系统审批");
        queryWrapper.eq(WfFormSynthesis::getTenantId, tenantId);
        queryWrapper.eq(WfFormSynthesis::getType, "0");
        return iWfFormSynthesisService.getOne(queryWrapper, false);
    }

    private WfFormSynthesis findSeedSystemApprovalCategory() {
        WfFormSynthesis rootCategory = getSystemApprovalCategory(ROOT_TENANT_ID);
        if (hasRequiredTemplateForms(rootCategory)) {
            return rootCategory;
        }

        LambdaQueryWrapper<WfFormSynthesis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFormSynthesis::getCategoryName, "系统审批");
        queryWrapper.eq(WfFormSynthesis::getType, "0");
        queryWrapper.eq(WfFormSynthesis::getDelFlag, "0");
        List<WfFormSynthesis> categories = iWfFormSynthesisService.list(queryWrapper);
        if (ObjectUtil.isEmpty(categories)) {
            return null;
        }
        for (WfFormSynthesis category : categories) {
            if (hasRequiredTemplateForms(category)) {
                return category;
            }
        }
        return null;
    }

    private boolean hasRequiredTemplateForms(WfFormSynthesis category) {
        if (category == null || StringUtils.isBlank(category.getCategoryId())
                || StringUtils.isBlank(category.getTenantId())) {
            return false;
        }
        LambdaQueryWrapper<WfForm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForm::getCategoryId, category.getCategoryId());
        queryWrapper.eq(WfForm::getTenantId, category.getTenantId());
        queryWrapper.eq(WfForm::getType, "0");
        queryWrapper.in(WfForm::getFormName, REQUIRED_INIT_FORM_NAMES);
        List<WfForm> forms = formService.list(queryWrapper);
        if (ObjectUtil.isEmpty(forms)) {
            return false;
        }
        Set<String> formNames = forms.stream().map(WfForm::getFormName).collect(Collectors.toSet());
        return formNames.containsAll(REQUIRED_INIT_FORM_NAMES);
    }

    private boolean existsTenantForm(String tenantId, String formName) {
        LambdaQueryWrapper<WfForm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForm::getFormName, formName);
        queryWrapper.eq(WfForm::getTenantId, tenantId);
        queryWrapper.eq(WfForm::getType, "0");
        return formService.count(queryWrapper) > 0;
    }

    @Override
    public List<List<String>> initShow() {
        // Query interfaceGet Initialize data
        List<ProcessTemplate> processTemplateVos = processTemplateMapper.selectList(new LambdaQueryWrapper<>()); // Query data

        // data, to "modelid", "modelKey", "model "
        List<List<String>> data = new ArrayList<>();
        data.add(Arrays.asList("模型id", "模型Key", "模型名称")); //

        // Query , fill data
        for (int index = 0; index < processTemplateVos.size(); index++) {
            ProcessTemplate vo = processTemplateVos.get(index);
            data.add(Arrays.asList(vo.getModelId(), // modelid
                    vo.getModelKey(), // modelKey
                    vo.getModelName() // model
            ));
        }

        // data
        return data;
    }

    // id already in ,
    private List<String> mergeAllCategories(List<String> existingList, List<?> categoryList,
            Function<Object, String> mapper) {
        if (categoryList != null && !categoryList.isEmpty()) {
            Set<String> resultSet = new HashSet<>(existingList);
            categoryList.stream().map(mapper).forEach(resultSet::add);
            return new ArrayList<>(resultSet);
        }
        return existingList;
    }

    /**
     * before Check , Query
     *
     * @param modelBo
     * @return
     */
    public Map<String, List<String>> getMergedCategories(WfModelBo modelBo) {
        List<String> workOrderSyntheses = new ArrayList<>();
        List<String> wfSyntheses = new ArrayList<>();

        // Query sub ID
        if (modelBo.getWfCategory() != null) {
            wfSyntheses = wfSynthesisService.selectChildById(modelBo.getWfCategory());
        }
        // Query sub ID
        if (modelBo.getWorkOrderCategory() != null) {
            workOrderSyntheses = workerSynthesisService.selectChildById(modelBo.getWorkOrderCategory());
        }

        if (Boolean.TRUE.equals(modelBo.getWorkOrderAppAll())) {
            // Query all
            workOrderSyntheses = mergeAllCategories(wfSyntheses, workOrderAppService.list(),
                    category -> ((WorkOrderApp) category).getAppId());
        }
        // if (Boolean.TRUE.equals(modelBo.getWorkOrderSynthesisAll())) {
        // workOrderSyntheses = mergeAllCategories(workOrderSyntheses,
        // workerSynthesisService.list(),
        // category -> ((WorkOrderSynthesis) category).getSynthesisId());
        // }
        if (Boolean.TRUE.equals(modelBo.getWfAppAll())) {
            wfSyntheses = mergeAllCategories(workOrderSyntheses, wfAppService.list(),
                    category -> ((WfApp) category).getAppId());
        }
        // if (Boolean.TRUE.equals(modelBo.getWfSynthesisAll())) {
        // // Query all
        // wfSyntheses = mergeAllCategories(wfSyntheses, wfSynthesisService.list(),
        // category -> ((WfSynthesis) category).getSynthesisId());
        // }

        //
        Map<String, List<String>> result = new HashMap<>();
        result.put("workOrderSyntheses", workOrderSyntheses);
        result.put("wfSyntheses", wfSyntheses);
        return result;
    }

    /**
     * data
     *
     * @param modelBo
     * @param page
     * @param wfSyntheses
     * @param workOrderSyntheses
     * @param tenantId
     * @return
     */
    public TableDataInfo<WfModelVo> getModelPageData(WfModelBo modelBo, Page<Model> page, List<String> wfSyntheses,
            List<String> workOrderSyntheses, String tenantId,
            Boolean history) {
        // Query model list
        List<Model> modelList = wfModelMapper.selectModelList(modelBo, page, wfSyntheses, workOrderSyntheses,
                tenantId, history);

        // Convert to VO
        List<WfModelVo> modelVoList = convertModelToVoList(modelList, modelBo);

        // Build data
        Page<WfModelVo> pageResult = new Page<>(page.getCurrent(), page.getSize());
        pageResult.setRecords(modelVoList);
        pageResult.setTotal(page.getTotal()); // from object in Get record

        return TableDataInfo.build(pageResult);
    }

    @Override
    public TableDataInfo<WfModelVo> list(WfModelBo modelBo, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        Map<String, List<String>> mergedCategories = getMergedCategories(modelBo);
        String appId = null;
        if (StringUtils.isNotBlank(modelBo.getApplicationId())) {
            LambdaQueryWrapper<WfApp> objectLambdaQueryWrapper = new LambdaQueryWrapper<>();
            objectLambdaQueryWrapper.eq(WfApp::getApplicationId, modelBo.getApplicationId());
            appId = wfAppService.getOne(objectLambdaQueryWrapper).getAppId();
            modelBo.setWfCategory(appId);
        }
        // Query
        Long pageTotal = wfModelMapper.selectModelCount(modelBo, mergedCategories.get("wfSyntheses"),
                mergedCategories.get("workOrderSyntheses"), sysUser.getTenantId(), false);
        if (pageTotal <= 0 || mergedCategories.get("wfSyntheses").isEmpty() && mergedCategories.get(
                "workOrderSyntheses").isEmpty() && StringUtil.isBlank(modelBo.getWfCategory())
                && StringUtil.isBlank(modelBo.getWorkOrderCategory())) {
            return TableDataInfo.build();
        }
        IPage<Model> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        return getModelPageData(modelBo, (Page<Model>) page, mergedCategories.get("wfSyntheses"),
                mergedCategories.get("workOrderSyntheses"), sysUser.getTenantId(), false);
    }

    private List<WfModelVo> convertModelToVoList(List<Model> modelList, WfModelBo modelBo) {
        List<WfModelVo> modelVoList = new ArrayList<>(modelList.size());
        modelList.forEach(model -> {
            WfModelVo modelVo = getWfModelVo(model);
            WfMetaInfoDto metaInfo = JsonUtils.parseObject(model.getMetaInfo(), WfMetaInfoDto.class);
            if (metaInfo != null) {
                modelVo.setDescription(metaInfo.getDescription());
                modelVo.setFormType(metaInfo.getFormType());
                modelVo.setFormId(metaInfo.getFormId());
                modelVo.setIconId(metaInfo.getIconId());
                modelVo.setShowMobile(Integer.valueOf(metaInfo.getShowMobile()));
                if (StringUtils.isNotBlank(model.getDeploymentId())) {
                    modelVo.setDeploymentStatus(true);
                    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                            .processDefinitionTenantId(model.getTenantId()).deploymentId(model.getDeploymentId())
                            .singleResult();
                    modelVo.setSuspended(processDefinition.isSuspended());
                    modelVo.setDefinitionId(processDefinition.getId());
                } else {
                    modelVo.setDeploymentStatus(false);
                    modelVo.setSuspended(true);
                }

                // modelBo in parameterSet Query
                if (Boolean.TRUE.equals(modelBo.getWfAppAll())) {
                    modelVo.setWfAppAll(true);
                }
                if (Boolean.TRUE.equals(modelBo.getWfSynthesisAll())) {
                    modelVo.setWfSynthesisAll(true);
                }
                if (Boolean.TRUE.equals(modelBo.getWorkOrderAppAll())) {
                    modelVo.setWorkOrderAppAll(true);
                }
                if (Boolean.TRUE.equals(modelBo.getWorkOrderSynthesisAll())) {
                    modelVo.setWorkOrderSynthesisAll(true);
                }
                if (StringUtils.isNotBlank(modelBo.getWfCategory())) {
                    modelVo.setWfAppAll(true);
                }
                if (StringUtils.isNotBlank(modelBo.getWorkOrderCategory())) {
                    modelVo.setWorkOrderAppAll(true);
                }


                modelVoList.add(modelVo);
            }
        });
        return modelVoList;
    }

    @Override
    public List<WfModelVo> list(WfModelBo modelBo) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        if (StringUtils.isNotBlank(modelBo.getApplicationId())) {
            LambdaQueryWrapper<WfApp> objectLambdaQueryWrapper = new LambdaQueryWrapper<>();
            objectLambdaQueryWrapper.eq(WfApp::getApplicationId, modelBo.getApplicationId());
            modelBo.setWfCategory(wfAppService.getOne(objectLambdaQueryWrapper).getAppId());
        }
        Map<String, List<String>> mergedCategories = getMergedCategories(modelBo);
        List<Model> modelList = wfModelMapper.selectModelList(modelBo, null, mergedCategories.get("wfSyntheses"),
                mergedCategories.get("workOrderSyntheses"), sysUser.getTenantId(), false);
        // Convert to VO
        return convertModelToVoList(modelList, modelBo);
    }

    @Override
    public TableDataInfo<WfModelVo> historyList(WfModelBo modelBo, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        Map<String, List<String>> mergedCategories = getMergedCategories(modelBo);
        // Query
        Long pageTotal = wfModelMapper.selectModelCount(modelBo, mergedCategories.get("wfSyntheses"),
                mergedCategories.get("workOrderSyntheses"), sysUser.getTenantId(), true);
        if (pageTotal <= 0) {
            return TableDataInfo.build();
        }
        // offset+1, new
        int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
        IPage<Model> page = new Page<>(offset, pageQuery.getPageSize());
        return getModelPageData(modelBo, (Page<Model>) page, mergedCategories.get("wfSyntheses"),
                mergedCategories.get("workOrderSyntheses"), sysUser.getTenantId(), true);
    }

    @Override
    public WfModelVo getModel(String modelId, String applicationId) {
        String appId = null;
        if (StringUtils.isNotBlank(applicationId)) {
            LambdaQueryWrapper<WfApp> objectLambdaQueryWrapper = new LambdaQueryWrapper<>();
            objectLambdaQueryWrapper.eq(WfApp::getApplicationId, applicationId);
            appId = wfAppService.getOne(objectLambdaQueryWrapper).getAppId();
        }
        ModelQuery modelQuery = repositoryService.createModelQuery().modelId(modelId);
        Model model = null;
        if (StringUtils.isNotBlank(appId)) {
            model = modelQuery.modelCategory(appId).singleResult();
        } else {
            model = modelQuery.singleResult();
        }
        if (ObjectUtil.isNull(model)) {
            throw new RuntimeException("流程模型不存在！");
        }
        // Get workflow
        String bpmnXml = queryBpmnXmlById(modelId);
        WfModelVo modelVo = getWfModelVo(model);
        modelVo.setBpmnXml(bpmnXml);
        WfMetaInfoDto metaInfo = JsonUtils.parseObject(model.getMetaInfo(), WfMetaInfoDto.class);
        if (metaInfo != null) {
            modelVo.setDescription(metaInfo.getDescription());
            modelVo.setFormType(metaInfo.getFormType());
            modelVo.setFormId(metaInfo.getFormId());
            modelVo.setIconId(metaInfo.getIconId());
            modelVo.setShowMobile(Integer.valueOf(metaInfo.getShowMobile()));
            if (FormType.PROCESS.getType().equals(metaInfo.getFormType())) {
                WfFormVo wfFormVo = formService.queryById(metaInfo.getFormId());
                modelVo.setContent(wfFormVo.getContent());
            }
        }
        return modelVo;
    }

    private WfModelVo getWfModelVo(Model model) {
        WfModelVo modelVo = new WfModelVo();
        modelVo.setModelId(model.getId());
        modelVo.setModelName(model.getName());
        modelVo.setModelKey(model.getKey());
        modelVo.setCategory(model.getCategory());
        modelVo.setCreateTime(model.getCreateTime());
        modelVo.setVersion(model.getVersion());
        return modelVo;
    }

    @Override
    public String queryBpmnXmlById(String modelId) {
        byte[] bpmnBytes = repositoryService.getModelEditorSource(modelId);
        return StrUtil.utf8Str(bpmnBytes);
    }

    @Override
    public String insertModel(WfModelBo modelBo) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        Model model = repositoryService.newModel();
        model.setName(modelBo.getModelName());
        model.setKey(modelBo.getModelKey());
        if (StringUtils.isNotBlank(modelBo.getWfCategory())) {
            model.setCategory(modelBo.getWfCategory());
        } else if (StringUtils.isNotBlank(modelBo.getWorkOrderCategory())) {
            model.setCategory(modelBo.getWorkOrderCategory());
        }
        String metaInfo = buildMetaInfo(new WfMetaInfoDto(), modelBo.getDescription(), modelBo.getIconId(),
                modelBo.getShowMobile(), modelBo.getFormId());
        model.setMetaInfo(metaInfo);
        model.setTenantId(sysUser.getTenantId());
        // workflowmodel
        repositoryService.saveModel(model);
        if (StringUtils.isNotBlank(modelBo.getCategoryId())) {
            WfFormBo bo = new WfFormBo();
            bo.setFormName(modelBo.getModelName());
            bo.setIsFormComponents("0");
            bo.setFormType(modelBo.getFormType() == null ? 0 : modelBo.getFormType());
            bo.setType(modelBo.getType());
            bo.setCategoryId(modelBo.getCategoryId());
            bo.setModelId(model.getId());
            modelBo.setModelId(model.getId());
            WfForm wfForm = formService.insertForm(bo);
            ProcessModel processModel = new ProcessModel();
            processModel.setCode(model.getKey());
            processModel.setName(model.getName());
            processModel.setId(1l);
            processModel.setNotifyAllSteps(true);

            StartNode startNode = new StartNode();
            startNode.setNodeName("开始");
            startNode.setNodeType("start");
            startNode.setId("root");
            startNode.setType(0);
            startNode.setFormKey(wfForm.getFormId());

            InitiatorNode initiatorNode = new InitiatorNode();
            initiatorNode.setNodeName("发起人");
            initiatorNode.setNodeType(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR);
            initiatorNode.setPid(startNode.getId());
            initiatorNode.setType(0);
            initiatorNode.setFormKey(wfForm.getFormId());
            initiatorNode.setId("node_" + RandomUtil.randomString(5));
            startNode.setChildNode(initiatorNode);

            EndNode endNode = new EndNode();
            endNode.setId("end");
            endNode.setNodeType("end");
            endNode.setPid("root");
            endNode.setNodeName("结束");
            endNode.setType(999);
            initiatorNode.setChildNode(endNode);
            processModel.setProcess(startNode);
            saveModel(modelBo, null, processModel);
        }
        return model.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModel(WfModelBo modelBo) {
        // modelKeyQuery modelinfo
        Model model = repositoryService.getModel(modelBo.getModelId());
        if (ObjectUtil.isNull(model)) {
            throw new RuntimeException("流程模型不存在！");
        }
        if (StringUtils.isNotBlank(modelBo.getWfCategory())) {
            model.setCategory(modelBo.getWfCategory());
        } else if (StringUtils.isNotBlank(modelBo.getWorkOrderCategory())) {
            model.setCategory(modelBo.getWorkOrderCategory());
        }
        WfMetaInfoDto metaInfoDto = JsonUtils.parseObject(model.getMetaInfo(), WfMetaInfoDto.class);
        String metaInfo = buildMetaInfo(metaInfoDto, modelBo.getDescription(), modelBo.getIconId(),
                modelBo.getShowMobile(), modelBo.getFormId());
        model.setMetaInfo(metaInfo);
        if (StringUtils.isNotBlank(modelBo.getDeploymentId())) {
            model.setDeploymentId(modelBo.getDeploymentId());
        }
        if (StringUtils.isEmpty(modelBo.getTenantId())) {
            SysUser sysUser = getCurrentSysUserRequired();
            model.setTenantId(sysUser.getTenantId());
        } else {
            model.setTenantId(modelBo.getTenantId());
        }
        if (StringUtils.isNotBlank(modelBo.getModelName()) && !modelBo.getModelName().equals(model.getName())) {
            // Set workflow to new
            model.setName(modelBo.getModelName());
            byte[] bpmnBytes = repositoryService.getModelEditorSource(model.getId());
            if (bpmnBytes != null) {
                byte[] bytes = updateProcessName(bpmnBytes, modelBo);
                repositoryService.addModelEditorSource(model.getId(), bytes);
            }
        }
        // workflowmodel
        repositoryService.saveModel(model);
    }

    private byte[] updateProcessName(byte[] bpmnBytes, WfModelBo modelBo) {
        byte[] updatedBpmnBytes = null; // Initialize new after BPMN array
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new ByteArrayInputStream(bpmnBytes)));
            // XPath workflow
            XPath xpath = XPathFactory.newInstance().newXPath();
            // Set null / empty before and URI
            NamespaceContext context = new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    if ("bpmn2".equals(prefix)) {
                        return "http://www.omg.org/spec/BPMN/20100524/MODEL";
                    }
                    return null;
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    throw new UnsupportedOperationException();
                }
            };
            xpath.setNamespaceContext(context);
            String expression = "/*/process/@name";
            NodeList nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            System.out.println("Found " + nodeList.getLength() + " process names:");
            for (int i = 0; i < nodeList.getLength(); i++) {
                org.w3c.dom.Node node = nodeList.item(i);
                node.setTextContent(modelBo.getModelName());
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(source, result);
            updatedBpmnBytes = outputStream.toByteArray();
        } catch (ParserConfigurationException | TransformerException | XPathExpressionException | SAXException
                | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("请检查当前流程模型是否已设计完成！");
        }
        return updatedBpmnBytes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Model saveModel(WfModelBo modelBo, String ToTenantId, ProcessModel processModel) {
        // Query modelinfo
        Model model = repositoryService.getModel(modelBo.getModelId());
        if (ObjectUtil.isNull(model)) {
            throw new RuntimeException("流程模型不存在！");
        }
        ReModeJsonBo remodeJsonBo = new ReModeJsonBo();
        if (StringUtils.isBlank(ToTenantId)) {
            // ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            // processModel Convert to JSON
            String jsonContent = null;
            try {
                jsonContent = objectMapper.writeValueAsString(processModel);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e + "processModel 转换为 JSON 字符串错误");
            }
            remodeJsonBo.setJsonContent(jsonContent); // JSON Set field in
            SysUser sysUser = getCurrentSysUserRequired();
            remodeJsonBo.setUserId(sysUser.getUserId());
            BpmnModel bpmnModelXml = processModel.toBpmnModel();
            byte[] xmlBytes = new BpmnXMLConverter().convertToXML(bpmnModelXml);
            modelBo.setBpmnXml(new String(xmlBytes));
            log.info("==================bpmnXml================== " + new String(xmlBytes));
        }

        BpmnModel bpmnModel = ModelUtils.getBpmnModel(modelBo.getBpmnXml());
        if (ObjectUtil.isEmpty(bpmnModel)) {
            throw new RuntimeException("获取模型设计失败！");
        }
        String processName = model.getName();
        // Get startnode
        StartEvent startEvent = ModelUtils.getStartEvent(bpmnModel);
        if (ObjectUtil.isNull(startEvent)) {
            throw new RuntimeException("开始节点不存在，请检查流程设计是否有误！");
        }
        // Get startnodeconfiguration formKey
        if (StrUtil.isBlank(startEvent.getFormKey())) {
            throw new RuntimeException("请配置流程表单");
        }

        Model newModel;
        if (Boolean.TRUE.equals(modelBo.getNewVersion())) {
            newModel = repositoryService.newModel();
            newModel.setName(processName);
            newModel.setKey(model.getKey());
            newModel.setCategory(model.getCategory());
            newModel.setMetaInfo(model.getMetaInfo());
            newModel.setVersion(model.getVersion() + 1);
            if (StringUtil.isNotBlank(ToTenantId)) {
                newModel.setTenantId(ToTenantId);
            } else {
                SysUser sysUser = getCurrentSysUserRequired();
                newModel.setTenantId(sysUser.getTenantId());
            }
        } else {
            newModel = model;
            // Set workflow
            newModel.setName(processName);
        }

        if (StringUtils.isNotBlank(modelBo.getFormId())) {
            WfMetaInfoDto metaInfo = JsonUtils.parseObject(model.getMetaInfo(), WfMetaInfoDto.class);
            if (metaInfo != null) {
                metaInfo.setFormId(modelBo.getFormId());
                newModel.setMetaInfo(JsonUtils.toJsonString(metaInfo));
            }
        }
        // workflowmodel
        repositoryService.saveModel(newModel);
        if (StringUtils.isBlank(ToTenantId)) {
            remodeJsonBo.setModelId(newModel.getId());
            reModeJsonService.insertByBo(remodeJsonBo);
        }
        // BPMN XML
        byte[] bpmnXmlBytes = StringUtils.getBytes(modelBo.getBpmnXml(), StandardCharsets.UTF_8);
        repositoryService.addModelEditorSource(newModel.getId(), bpmnXmlBytes);
        return newModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void latestModel(String modelId) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        // Get workflowmodel
        Model model = repositoryService.getModel(modelId);
        if (ObjectUtil.isNull(model)) {
            throw new RuntimeException("流程模型不存在！");
        }
        Integer latestVersion = repositoryService.createModelQuery().modelTenantId(sysUser.getTenantId())
                .modelKey(model.getKey()).latestVersion().singleResult().getVersion();
        if (model.getVersion().equals(latestVersion)) {
            throw new RuntimeException("当前版本已是最新版！");
        }
        // Get BPMN XML
        byte[] bpmnBytes = repositoryService.getModelEditorSource(modelId);
        Model newModel = repositoryService.newModel();
        newModel.setName(model.getName());
        newModel.setKey(model.getKey());
        newModel.setCategory(model.getCategory());
        newModel.setMetaInfo(model.getMetaInfo());
        newModel.setVersion(latestVersion + 1);
        newModel.setTenantId(sysUser.getTenantId());
        // workflowmodel
        repositoryService.saveModel(newModel);
        // BPMN XML
        repositoryService.addModelEditorSource(newModel.getId(), bpmnBytes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(Collection<String> ids) {
        ids.forEach(id -> {
            Model model = repositoryService.getModel(id);
            if (ObjectUtil.isNull(model)) {
                throw new RuntimeException("流程模型不存在！");
            } else if (ObjectUtil
                    .isNotEmpty(repositoryService.createProcessDefinitionQuery()
                            .processDefinitionTenantId(model.getTenantId()).processDefinitionKey(model.getKey()).list())
                    || ObjectUtil.isNotEmpty(repositoryService.createDeploymentQuery()
                            .deploymentTenantId(model.getTenantId()).deploymentKey(model.getKey()).list())) {
                throw new RuntimeException("该流程模型存在未删除实例版本，请检查");
            }
            repositoryService.deleteModel(id);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deployModel(String modelId) {
        // workflowdataInitialize need to
        String toTenantId = RedisUtils.getCacheObject("to_tenant_id");
        SysUser sysUser = getCurrentSysUser();
        if (sysUser == null) {
            sysUser = new SysUser();
        }
        // workflowdataInitialize need to
        if (StringUtil.isNotBlank(toTenantId)) {
            sysUser.setTenantId(toTenantId);
        }
        // Get workflowmodel
        Model model = repositoryService.getModel(modelId);
        if (ObjectUtil.isNull(model)) {
            throw new RuntimeException("流程模型不存在！");
        }
        if (StringUtils.isBlank(sysUser.getTenantId())) {
            sysUser.setTenantId(model.getTenantId());
        }
        // Get workflow
        byte[] bpmnBytes = repositoryService.getModelEditorSource(modelId);
        if (ArrayUtil.isEmpty(bpmnBytes)) {
            throw new RuntimeException("请先设计流程图！");
        }
        String bpmnXml = StringUtils.toEncodedString(bpmnBytes, StandardCharsets.UTF_8);
        BpmnModel bpmnModel = ModelUtils.getBpmnModel(bpmnXml);
        String processName = model.getName() + ProcessConstants.SUFFIX;
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(model.getKey()) //
                // workflow
                .processDefinitionTenantId(sysUser.getTenantId()) // current
                .orderByProcessDefinitionVersion().desc() //
                .list();
        if (definitions.size() > 1 && !definitions.get(0).isSuspended()) {
            ProcessDefinition previous = definitions.get(0);
            ProcessDefinition processDefinition2 = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(previous.getDeploymentId()).processDefinitionTenantId(sysUser.getTenantId())
                    .singleResult();
            deployService.updateState(processDefinition2.getId(), SuspensionState.SUSPENDED.toString());
        }
        // workflow
        Deployment deployment = repositoryService.createDeployment().tenantId(sysUser.getTenantId())
                .name(model.getName()).key(model.getKey()).category(model.getCategory())
                .addBytes(processName, bpmnBytes).deploy();
        ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(sysUser.getTenantId()).deploymentId(deployment.getId()).singleResult();

        WfModelBo wfModelBo = new WfModelBo();
        wfModelBo.setDeploymentId(deployment.getId());
        wfModelBo.setModelId(modelId);
        wfModelBo.setTenantId(sysUser.getTenantId());
        // JAVA value
        List<String> wfSyntheses = wfSynthesisService.selectChildById(model.getCategory());
        if (!wfSyntheses.isEmpty()) {
            wfModelBo.setWfCategory(model.getCategory());
        } else {
            wfModelBo.setWorkOrderCategory(model.getCategory());
        }
        updateModel(wfModelBo);
        // Update workflow definition , workflow
        repositoryService.setProcessDefinitionCategory(procDef.getId(), model.getCategory());
        // form
        return deployFormService.saveInternalDeployForm(deployment.getId(), bpmnModel);
    }

    /**
     * workflowmodel
     *
     * @param modelBo
     */
    @Override
    public void copyModel(WfModelBo modelBo) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        String modelKey = modelBo.getModelKey();
        // Check modelKeywhether already in
        Model model = repositoryService.createModelQuery().modelTenantId(sysUser.getTenantId()).modelKey(modelKey)
                .singleResult();
        if (model != null) {
            throw new RuntimeException("模型标识已存在");
        }
        // Get workflowmodelid
        String copyModelId = modelBo.getCopyModelId();
        // modelidGet workflowmodelinfo
        Model originalModel = repositoryService.createModelQuery().modelTenantId(sysUser.getTenantId())
                .modelId(copyModelId).singleResult();
        if (originalModel == null) {
            throw new IllegalArgumentException("原始模型ID不存在: " + copyModelId);
        }

        // Get model BPMN XML
        byte[] bpmnBytes = repositoryService.getModelEditorSource(originalModel.getId());
        if (bpmnBytes == null || bpmnBytes.length == 0) {
            throw new IllegalStateException("无法获取原始模型的BPMN XML");
        }
        // new model
        Model newModel = repositoryService.newModel();

        newModel.setKey(modelBo.getModelKey()); // to new model new
        newModel.setName(modelBo.getModelName()); // Set model , ( )
        if (!StringUtils.isBlank(modelBo.getWfCategory())) {
            newModel.setCategory(modelBo.getWfCategory());
        } else {
            newModel.setCategory(modelBo.getWorkOrderCategory());
        }
        newModel.setVersion(1); // new model
        newModel.setDeploymentId(null); // new model not
        String metaInfo = buildMetaInfo(new WfMetaInfoDto(), modelBo.getDescription(), modelBo.getIconId(),
                modelBo.getShowMobile(), modelBo.getFormId()); // Set and
        newModel.setMetaInfo(metaInfo);
        newModel.setTenantId(originalModel.getTenantId()); // tenant ID

        ReModelJsonVo reModelJsonVo = remodeJsonService.queryById(modelBo.getCopyModelId());

        // processModel Convert to JSON
        ReModeJsonBo remodeJsonBo = new ReModeJsonBo();

        remodeJsonBo.setJsonContent(reModelJsonVo.getJsonContent()); // JSON Set field in
        remodeJsonBo.setTenantId(sysUser.getTenantId());
        remodeJsonBo.setUserId(sysUser.getUserId());

        // new model
        repositoryService.saveModel(newModel);

        remodeJsonBo.setModelId(newModel.getId());
        remodeJsonService.insertByBo(remodeJsonBo);
        try {
            // Parse BPMN XML
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new ByteArrayInputStream(bpmnBytes));
            // null / empty all flowable:formKeyproperty
            // NodeList elementsWithFormKey = doc.getElementsByTagName("*");
            // for (int i = 0; i < elementsWithFormKey.getLength(); i++) {
            // Node node = elementsWithFormKey.item(i);
            // NamedNodeMap attributes = node.getAttributes();
            // if (attributes != null) {
            // Node formKeyAttr = attributes.getNamedItem("flowable:formKey");
            // if (formKeyAttr != null) {
            // formKeyAttr.setTextContent(""); // null / empty formKeyproperty value
            // }
            // }
            // }

            // Set workflow
            NodeList processes = doc.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "process");
            if (processes.getLength() > 0) {
                org.w3c.dom.Node processNode = processes.item(0); // assuming only workflow definition, if , need to
                NamedNodeMap attributes = processNode.getAttributes();
                Node nameAttr = attributes.getNamedItem("name");
                if (nameAttr != null) {
                    // Update workflow definition nameproperty value
                    nameAttr.setTextContent(modelBo.getModelName());
                }
            }

            // // all startEventelement Update startEvent flowable:formKey
            // // Get all startEvent element
            // NodeList startEvents =
            // doc.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL",
            // "startEvent");
            // // NodeList, startEvent element
            // for (int i = 0; i < startEvents.getLength(); i++) {
            // Node node = startEvents.item(i);
            // if (node instanceof Element) {
            // Element startEventElement = (Element) node;
            // // in Check propertyProcess
            // String elementName = startEventElement.getNodeName(); // Get element ,
            // "startEvent"
            // String namespaceURI = startEventElement.getNamespaceURI(); // Get null / empty URI
            //
            // // : whether flowable:formKey property, if Set
            // if (!startEventElement.hasAttributeNS("http://flowable.org/bpmn", "formKey"))
            // {
            // startEventElement.setAttributeNS("http://flowable.org/bpmn",
            // "flowable:formKey", String
            // .valueOf(modelBo.getFormId()));
            // }
            //
            // // Process startEvent element afterexit loop
            // break;
            // }
            // }

            // Update after Convert array
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(baos);
            transformer.transform(source, result);
            byte[] modifiedBpmnBytes = baos.toByteArray();
            // Update after BPMN XML to new model
            repositoryService.addModelEditorSource(newModel.getId(), modifiedBpmnBytes);
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Long selectModelCount(WfModelBo modelBo, List<String> wfSyntheses, List<String> workOrderSyntheses,
            String tenantId, Boolean history) {
        return wfModelMapper.selectModelCount(modelBo, wfSyntheses, workOrderSyntheses, tenantId, false);
    }

    @Override
    public List<Model> selectModelList(WfModelBo modelBo, IPage<Model> page, List<String> wfSyntheses,
            List<String> workOrderSyntheses, String tenantId, Boolean history) {
        return wfModelMapper.selectModelList(modelBo, page, wfSyntheses, workOrderSyntheses, tenantId, false);
    }

    @Override
    public void batchRemove(String modelKey) {
        List<String> list = repositoryService.createModelQuery().modelKey(modelKey).list().stream().map(model -> {
            return model.getId();
        }).collect(Collectors.toList());
        deleteByIds(list);
    }

    /**
     * Build model info
     *
     * @return
     */
    private String buildMetaInfo(WfMetaInfoDto metaInfo, String description, String iconId, String showMobile,
            String formId) {
        // only non- null / empty , Set , new
        if (StringUtils.isNotEmpty(description)) {
            metaInfo.setDescription(description);
        }
        if (StringUtils.isNotEmpty(metaInfo.getCreateUser())) {
            metaInfo.setCreateUser(LoginHelper.getUsername());
        }
        if (StringUtils.isNotEmpty(iconId)) {
            metaInfo.setIconId(iconId);
        }
        if (StringUtils.isNotEmpty(showMobile)) {
            metaInfo.setShowMobile(showMobile);
        }
        if (StringUtils.isNotEmpty(formId)) {
            metaInfo.setFormId(formId);
        }
        return JsonUtils.toJsonString(metaInfo);
    }
}
