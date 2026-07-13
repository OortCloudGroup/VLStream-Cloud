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
import com.ruoyi.workflow.domain.vo.ProcessTemplateVo;
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
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${http.get-tenant-admin}")
    private String getTenantAdmin;

    /**
     * 获取当前请求登录用户；启动初始化等无请求上下文场景返回 null，避免使用空 token 访问 Redis。
     */
    private SysUser getCurrentSysUser() {
        String token = AuthorizationInterceptor.getToken();
        if (StringUtils.isBlank(token)) {
            return null;
        }
        return RedisUtils.getCacheObject(token);
    }

    /**
     * 获取必须存在的当前请求登录用户；普通接口缺少登录上下文时抛出业务异常。
     */
    private SysUser getCurrentSysUserRequired() {
        SysUser sysUser = getCurrentSysUser();
        if (sysUser == null) {
            throw new ServiceException("当前登录用户信息不存在");
        }
        return sysUser;
    }

    /**
     * 根据模型 ID 级联删除模型、流程图信息、所有模型版本、部署及其运行实例与历史数据
     *
     * @param modelId     要删除的模型 ID
     * @param isWorkOrder
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelCascade(String modelId, boolean isWorkOrder) {
        // 1. 查询当前模型，获取 key
        Model currentModel = repositoryService.getModel(modelId);
        if (currentModel == null) {
            throw new FlowableObjectNotFoundException("无法找到模型，id=" + modelId);
        }
        String modelKey = currentModel.getKey();

        // 2. 查询该 key 下的所有模型版本
        List<Model> allVersions = repositoryService.createModelQuery()
                .modelKey(modelKey)
                .list();

        // 3. 遍历每个版本，先删部署，再删模型
        for (Model versionModel : allVersions) {
            String versionModelId = versionModel.getId();
            String deploymentId = versionModel.getDeploymentId();

            if (isWorkOrder) {
                LambdaQueryWrapper<WorkOrder> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(WorkOrder::getProcessKey, versionModel.getKey());
                workOrderService.remove(queryWrapper);
            }
            // 如果已部署，级联删除部署及所有流程实例和历史数据
            if (deploymentId != null) {
                repositoryService.deleteDeployment(deploymentId, true);
            }
            reModeJsonService.removeById(versionModelId);

            // 删除模型（会一并删除 editor bytearrays）
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
     * 校验事件管理初始化的租户参数。
     */
    private void validateEventManagementInitBo(InitBo initBo) {
        if (initBo == null || StringUtils.isBlank(initBo.getTenantId()) || StringUtils.isBlank(initBo.getToTenantId())) {
            throw new RuntimeException("事件管理初始化失败：tenant_id 和 to_tenant_id 不能为空");
        }
    }

    /**
     * 源租户没有事件管理模板时，回退到固定顶级租户模板。
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
     * 获取指定租户的事件管理工单应用。
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
     * 获取或创建目标租户的事件管理工单应用。
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
     * 获取或创建目标租户的事件管理表单应用。
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
     * 查询事件管理源租户下最新流程模型。
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
     * 复制事件管理模型、表单和设计 JSON，并部署到目标租户。
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
     * 判断目标租户是否已有同名事件管理模型。
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
     * 查询源模型绑定的表单。
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
     * 目标租户同分类同名表单存在时复用，否则复制源表单。
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
     * 复制模型设计 JSON，并同步新模型 key 和表单 key。
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
     * 在初始化链路中临时切换租户上下文并部署模型。
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
     * seed 中保留的是设计器 JSON 的表单 id，部署用 BPMN 需要转换成现有部署逻辑识别的 key_表单id。
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
     * 顶级租户 seed 已写入但未部署时，部署现有事件管理模型。
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
     * 统计源租户可复制的事件管理模型数。
     */
    private int getSourceEventManagementModelCount(String tenantId, WorkOrderApp app) {
        return getSourceEventManagementModels(tenantId, app).size();
    }

    /**
     * 判断目标租户事件管理是否已经有部署好的流程。
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
            // 关闭多租户插件
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
            // 公用模版列表
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
                // 保存流程模型
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
                // 获取租户admin用户
                // String userId = getTenantAdmin(initBo);
                // 更新formKey
                BpmnModel bpmnModel1 = updateBpmnFormBinding(bpmnModel, String.valueOf(wfFormOne.getFormId()),
                        newModel.getKey());
                byte[] xmlBytes = new BpmnXMLConverter().convertToXML(bpmnModel1);
                repositoryService.addModelEditorSource(newModel.getId(), xmlBytes);
                WfModelBo wfModelBo = new WfModelBo();
                wfModelBo.setBpmnXml(new String(xmlBytes, StandardCharsets.UTF_8));
                wfModelBo.setModelId(newModel.getId());
                wfModelBo.setModelName(processTemplateVo.getModelName());
                // 流程图保存后返回的model
                InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
                Model finalModel = saveModel(wfModelBo, initBo.getToTenantId(), null);
                ReModelJson reModelJson = reModelJsonMapper.selectById(model.getId());
                if (reModelJson == null || StringUtils.isBlank(reModelJson.getJsonContent())) {
                    throw new ServiceException("公共流程模板JSON为空: " + processTemplateVo.getModelKey());
                }
                ReModeJsonBo remodeJsonBo = new ReModeJsonBo();
                JSONObject jsonObject1 = JSON.parseObject(reModelJson.getJsonContent());
                jsonObject1.put("code", finalModel.getKey());
                // 递归修改 formKey
                updateFormKey(jsonObject1.getJSONObject("process"), String.valueOf(wfFormOne.getFormId()));
                remodeJsonBo.setJsonContent(jsonObject1.toJSONString()); // 将 JSON 字符串设置到字段中
                remodeJsonBo.setModelId(finalModel.getId());
                remodeJsonBo.setTenantId(initBo.getToTenantId());
                reModeJsonService.insertByBo(remodeJsonBo);
                InterceptorIgnoreHelper.clearIgnoreStrategy();
                // 部署模型
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
     * 递归更新 JSON 中的 formKey。
     * 只有原本 formKey 不为空的节点，才会将其更新为传入的新值 newFormKey。
     *
     * @param node       当前节点对象(process)
     * @param newFormKey 新的 formKey 值
     */
    public JSONObject updateFormKey(JSONObject node, String newFormKey) {
        if (node == null) {
            return null;
        }

        // 获取当前节点的 formKey
        String formKey = node.getString("formKey");
        // 只有 formKey 非空时才更新
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

        // 如果存在子节点，则递归更新
        JSONObject childNode = node.getJSONObject("childNode");
        if (childNode != null) {
            updateFormKey(childNode, newFormKey);
        }
        return node;
    }

    /**
     * 修改formKey以及修改审批人为新租户的admin
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
            // 将 user_id 设置到 userTask 的 assignee 属性中
            userTask.setAssignee(String.format("${%s}", TaskConstants.PROCESS_INITIATOR));
        }
        // 修改流程图标识
        bpmnModel.getMainProcess().setId(processId);
        return bpmnModel;
    }

    /**
     * 通过租户id获取租户admin信息
     *
     * @param initBo
     * @return
     */
    public String getTenantAdmin(InitBo initBo) {
        String userId = null;
        // 获取当前请求的请求参数
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30,
                TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();
        Request.Builder requestBuilder = new Request.Builder().url(String.format("%s?tenant_id=%s", getTenantAdmin,
                initBo.getToTenantId())).get();
        // 遍历当前请求的所有头信息，并添加到新的请求中
        ApiHeaderUtil.transferHeaders(requestBuilder);
        // 构建并执行请求
        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // 获取响应体并解析为字符串
                String responseBody = response.body().string();

                // 使用 FastJSON 解析响应数据
                JSONObject jsonResponse = JSON.parseObject(responseBody);

                // 检查返回的状态码是否为 200
                int code = jsonResponse.getIntValue("code");
                if (code == 200) {
                    // 提取 user_id
                    userId = jsonResponse.getJSONObject("data").getString("user_id");

                } else {
                    // 如果状态码不是 200，抛出异常或记录日志
                    String msg = jsonResponse.getString("msg");
                    throw new RuntimeException("接口调用失败，错误信息：" + msg);
                }
            } else {
                // 如果响应不成功，抛出异常
                throw new RuntimeException("接口调用失败，HTTP 状态码：" + response.code());
            }
        } catch (IOException e) {
            // 捕获 IO 异常并抛出自定义运行时异常
            throw new RuntimeException("接口调用过程中发生异常", e);
        }
        return userId;
    }

    /**
     * 初始化表单数据
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
        // 调用查询接口获取初始化数据
        List<ProcessTemplate> processTemplateVos = processTemplateMapper.selectList(new LambdaQueryWrapper<>()); // 查询模板数据

        // 准备返回的数据，表头为“模型id”，“模型Key”，“模型名称”
        List<List<String>> data = new ArrayList<>();
        data.add(Arrays.asList("模型id", "模型Key", "模型名称")); // 添加表头

        // 遍历查询结果，填充数据
        for (int index = 0; index < processTemplateVos.size(); index++) {
            ProcessTemplate vo = processTemplateVos.get(index);
            data.add(Arrays.asList(vo.getModelId(), // 模型id
                    vo.getModelKey(), // 模型Key
                    vo.getModelName() // 模型名称
            ));
        }

        // 返回构造的结果数据
        return data;
    }

    // 将分类id列表合并到已有的分类列表中，并去重
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
     * 根据前端传的条件判断，查询哪个分类
     *
     * @param modelBo
     * @return
     */
    public Map<String, List<String>> getMergedCategories(WfModelBo modelBo) {
        List<String> workOrderSyntheses = new ArrayList<>();
        List<String> wfSyntheses = new ArrayList<>();

        // 查询子分类ID
        if (modelBo.getWfCategory() != null) {
            wfSyntheses = wfSynthesisService.selectChildById(modelBo.getWfCategory());
        }
        // 查询子分类ID
        if (modelBo.getWorkOrderCategory() != null) {
            workOrderSyntheses = workerSynthesisService.selectChildById(modelBo.getWorkOrderCategory());
        }

        if (Boolean.TRUE.equals(modelBo.getWorkOrderAppAll())) {
            // 查询所有分类
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
        // // 查询所有分类
        // wfSyntheses = mergeAllCategories(wfSyntheses, wfSynthesisService.list(),
        // category -> ((WfSynthesis) category).getSynthesisId());
        // }

        // 返回结果
        Map<String, List<String>> result = new HashMap<>();
        result.put("workOrderSyntheses", workOrderSyntheses);
        result.put("wfSyntheses", wfSyntheses);
        return result;
    }

    /**
     * 返回分页数据
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
        // 查询模型列表
        List<Model> modelList = wfModelMapper.selectModelList(modelBo, page, wfSyntheses, workOrderSyntheses,
                tenantId, history);

        // 转换为 VO 列表
        List<WfModelVo> modelVoList = convertModelToVoList(modelList, modelBo);

        // 构建分页数据
        Page<WfModelVo> pageResult = new Page<>(page.getCurrent(), page.getSize());
        pageResult.setRecords(modelVoList);
        pageResult.setTotal(page.getTotal()); // 从分页对象中获取总记录数

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
        // 查询总数
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

                // 根据 modelBo 中的参数设置查询类别标识
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
        // 转换为 VO 列表
        return convertModelToVoList(modelList, modelBo);
    }

    @Override
    public TableDataInfo<WfModelVo> historyList(WfModelBo modelBo, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        Map<String, List<String>> mergedCategories = getMergedCategories(modelBo);
        // 查询总数
        Long pageTotal = wfModelMapper.selectModelCount(modelBo, mergedCategories.get("wfSyntheses"),
                mergedCategories.get("workOrderSyntheses"), sysUser.getTenantId(), true);
        if (pageTotal <= 0) {
            return TableDataInfo.build();
        }
        // offset+1，去掉最新版
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
        // 获取流程图
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
        // 保存流程模型
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
        // 根据模型Key查询模型信息
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
            // 设置流程名称为新名称
            model.setName(modelBo.getModelName());
            byte[] bpmnBytes = repositoryService.getModelEditorSource(model.getId());
            if (bpmnBytes != null) {
                byte[] bytes = updateProcessName(bpmnBytes, modelBo);
                repositoryService.addModelEditorSource(model.getId(), bytes);
            }
        }
        // 保存流程模型
        repositoryService.saveModel(model);
    }

    private byte[] updateProcessName(byte[] bpmnBytes, WfModelBo modelBo) {
        byte[] updatedBpmnBytes = null; // 声明并初始化更新后的 BPMN 字节数组
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new ByteArrayInputStream(bpmnBytes)));
            // 使用 XPath 定位流程名称
            XPath xpath = XPathFactory.newInstance().newXPath();
            // 设置命名空间前缀和 URI 上下文
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
        // 查询模型信息
        Model model = repositoryService.getModel(modelBo.getModelId());
        if (ObjectUtil.isNull(model)) {
            throw new RuntimeException("流程模型不存在！");
        }
        ReModeJsonBo remodeJsonBo = new ReModeJsonBo();
        if (StringUtils.isBlank(ToTenantId)) {
            // 创建 ObjectMapper 实例
            ObjectMapper objectMapper = new ObjectMapper();
            // 将 processModel 转换为 JSON 字符串
            String jsonContent = null;
            try {
                jsonContent = objectMapper.writeValueAsString(processModel);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e + "processModel 转换为 JSON 字符串错误");
            }
            remodeJsonBo.setJsonContent(jsonContent); // 将 JSON 字符串设置到字段中
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
        // 获取开始节点
        StartEvent startEvent = ModelUtils.getStartEvent(bpmnModel);
        if (ObjectUtil.isNull(startEvent)) {
            throw new RuntimeException("开始节点不存在，请检查流程设计是否有误！");
        }
        // 获取开始节点配置的表单Key
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
            // 设置流程名称
            newModel.setName(processName);
        }

        if (StringUtils.isNotBlank(modelBo.getFormId())) {
            WfMetaInfoDto metaInfo = JsonUtils.parseObject(model.getMetaInfo(), WfMetaInfoDto.class);
            if (metaInfo != null) {
                metaInfo.setFormId(modelBo.getFormId());
                newModel.setMetaInfo(JsonUtils.toJsonString(metaInfo));
            }
        }
        // 保存流程模型
        repositoryService.saveModel(newModel);
        if (StringUtils.isBlank(ToTenantId)) {
            remodeJsonBo.setModelId(newModel.getId());
            reModeJsonService.insertByBo(remodeJsonBo);
        }
        // 保存 BPMN XML
        byte[] bpmnXmlBytes = StringUtils.getBytes(modelBo.getBpmnXml(), StandardCharsets.UTF_8);
        repositoryService.addModelEditorSource(newModel.getId(), bpmnXmlBytes);
        return newModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void latestModel(String modelId) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        // 获取流程模型
        Model model = repositoryService.getModel(modelId);
        if (ObjectUtil.isNull(model)) {
            throw new RuntimeException("流程模型不存在！");
        }
        Integer latestVersion = repositoryService.createModelQuery().modelTenantId(sysUser.getTenantId())
                .modelKey(model.getKey()).latestVersion().singleResult().getVersion();
        if (model.getVersion().equals(latestVersion)) {
            throw new RuntimeException("当前版本已是最新版！");
        }
        // 获取 BPMN XML
        byte[] bpmnBytes = repositoryService.getModelEditorSource(modelId);
        Model newModel = repositoryService.newModel();
        newModel.setName(model.getName());
        newModel.setKey(model.getKey());
        newModel.setCategory(model.getCategory());
        newModel.setMetaInfo(model.getMetaInfo());
        newModel.setVersion(latestVersion + 1);
        newModel.setTenantId(sysUser.getTenantId());
        // 保存流程模型
        repositoryService.saveModel(newModel);
        // 保存 BPMN XML
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
        // 流程数据初始化需要
        String toTenantId = RedisUtils.getCacheObject("to_tenant_id");
        SysUser sysUser = getCurrentSysUser();
        if (sysUser == null) {
            sysUser = new SysUser();
        }
        // 流程数据初始化需要
        if (StringUtil.isNotBlank(toTenantId)) {
            sysUser.setTenantId(toTenantId);
        }
        // 获取流程模型
        Model model = repositoryService.getModel(modelId);
        if (ObjectUtil.isNull(model)) {
            throw new RuntimeException("流程模型不存在！");
        }
        if (StringUtils.isBlank(sysUser.getTenantId())) {
            sysUser.setTenantId(model.getTenantId());
        }
        // 获取流程图
        byte[] bpmnBytes = repositoryService.getModelEditorSource(modelId);
        if (ArrayUtil.isEmpty(bpmnBytes)) {
            throw new RuntimeException("请先设计流程图！");
        }
        String bpmnXml = StringUtils.toEncodedString(bpmnBytes, StandardCharsets.UTF_8);
        BpmnModel bpmnModel = ModelUtils.getBpmnModel(bpmnXml);
        String processName = model.getName() + ProcessConstants.SUFFIX;
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(model.getKey()) //
                // 同一流程标识
                .processDefinitionTenantId(sysUser.getTenantId()) // 当前租户过滤
                .orderByProcessDefinitionVersion().desc() // 按版本号倒序
                .list();
        if (definitions.size() > 1 && !definitions.get(0).isSuspended()) {
            ProcessDefinition previous = definitions.get(0);
            ProcessDefinition processDefinition2 = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(previous.getDeploymentId()).processDefinitionTenantId(sysUser.getTenantId())
                    .singleResult();
            deployService.updateState(processDefinition2.getId(), SuspensionState.SUSPENDED.toString());
        }
        // 部署流程
        Deployment deployment = repositoryService.createDeployment().tenantId(sysUser.getTenantId())
                .name(model.getName()).key(model.getKey()).category(model.getCategory())
                .addBytes(processName, bpmnBytes).deploy();
        ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(sysUser.getTenantId()).deploymentId(deployment.getId()).singleResult();

        WfModelBo wfModelBo = new WfModelBo();
        wfModelBo.setDeploymentId(deployment.getId());
        wfModelBo.setModelId(modelId);
        wfModelBo.setTenantId(sysUser.getTenantId());
        // 类似JAVA的短路求值
        List<String> wfSyntheses = wfSynthesisService.selectChildById(model.getCategory());
        if (!wfSyntheses.isEmpty()) {
            wfModelBo.setWfCategory(model.getCategory());
        } else {
            wfModelBo.setWorkOrderCategory(model.getCategory());
        }
        updateModel(wfModelBo);
        // 修改流程定义的分类，便于搜索流程
        repositoryService.setProcessDefinitionCategory(procDef.getId(), model.getCategory());
        // 保存部署表单
        return deployFormService.saveInternalDeployForm(deployment.getId(), bpmnModel);
    }

    /**
     * 复制流程模型
     *
     * @param modelBo
     */
    @Override
    public void copyModel(WfModelBo modelBo) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        String modelKey = modelBo.getModelKey();
        // 判断该modelKey是否已存在
        Model model = repositoryService.createModelQuery().modelTenantId(sysUser.getTenantId()).modelKey(modelKey)
                .singleResult();
        if (model != null) {
            throw new RuntimeException("模型标识已存在");
        }
        // 获取被复制的流程模型id
        String copyModelId = modelBo.getCopyModelId();
        // 根据模型id获取原始流程模型信息
        Model originalModel = repositoryService.createModelQuery().modelTenantId(sysUser.getTenantId())
                .modelId(copyModelId).singleResult();
        if (originalModel == null) {
            throw new IllegalArgumentException("原始模型ID不存在: " + copyModelId);
        }

        // 获取原始模型的BPMN XML
        byte[] bpmnBytes = repositoryService.getModelEditorSource(originalModel.getId());
        if (bpmnBytes == null || bpmnBytes.length == 0) {
            throw new IllegalStateException("无法获取原始模型的BPMN XML");
        }
        // 创建新的模型实体
        Model newModel = repositoryService.newModel();

        newModel.setKey(modelBo.getModelKey()); // 为新模型设定一个新的唯一键
        newModel.setName(modelBo.getModelName()); // 设置模型名称，可以加上（副本）之类的标识
        if (!StringUtils.isBlank(modelBo.getWfCategory())) {
            newModel.setCategory(modelBo.getWfCategory());
        } else {
            newModel.setCategory(modelBo.getWorkOrderCategory());
        }
        newModel.setVersion(1); // 新模型的初始版本
        newModel.setDeploymentId(null); // 新模型尚未部署
        String metaInfo = buildMetaInfo(new WfMetaInfoDto(), modelBo.getDescription(), modelBo.getIconId(),
                modelBo.getShowMobile(), modelBo.getFormId()); // 设置图标和描述
        newModel.setMetaInfo(metaInfo);
        newModel.setTenantId(originalModel.getTenantId()); // 继承租户ID

        ReModelJsonVo reModelJsonVo = remodeJsonService.queryById(modelBo.getCopyModelId());

        // 将 processModel 转换为 JSON 字符串
        ReModeJsonBo remodeJsonBo = new ReModeJsonBo();

        remodeJsonBo.setJsonContent(reModelJsonVo.getJsonContent()); // 将 JSON 字符串设置到字段中
        remodeJsonBo.setTenantId(sysUser.getTenantId());
        remodeJsonBo.setUserId(sysUser.getUserId());

        // 保存新模型
        repositoryService.saveModel(newModel);

        remodeJsonBo.setModelId(newModel.getId());
        remodeJsonService.insertByBo(remodeJsonBo);
        try {
            // 解析BPMN XML
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new ByteArrayInputStream(bpmnBytes));
            // 清空所有flowable:formKey属性
            // NodeList elementsWithFormKey = doc.getElementsByTagName("*");
            // for (int i = 0; i < elementsWithFormKey.getLength(); i++) {
            // Node node = elementsWithFormKey.item(i);
            // NamedNodeMap attributes = node.getAttributes();
            // if (attributes != null) {
            // Node formKeyAttr = attributes.getNamedItem("flowable:formKey");
            // if (formKeyAttr != null) {
            // formKeyAttr.setTextContent(""); // 清空formKey属性值
            // }
            // }
            // }

            // 设置流程名称
            NodeList processes = doc.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "process");
            if (processes.getLength() > 0) {
                org.w3c.dom.Node processNode = processes.item(0); // 假设只有一个流程定义，如果有多个，你需要根据实际情况选择
                NamedNodeMap attributes = processNode.getAttributes();
                Node nameAttr = attributes.getNamedItem("name");
                if (nameAttr != null) {
                    // 修改流程定义的name属性值
                    nameAttr.setTextContent(modelBo.getModelName());
                }
            }

            // // 定位到所有的startEvent元素并修改第一个startEvent的flowable:formKey
            // // 获取所有的 startEvent 元素
            // NodeList startEvents =
            // doc.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL",
            // "startEvent");
            // // 遍历 NodeList，找到第一个符合条件的 startEvent 元素
            // for (int i = 0; i < startEvents.getLength(); i++) {
            // Node node = startEvents.item(i);
            // if (node instanceof Element) {
            // Element startEventElement = (Element) node;
            // // 在这里进行你的条件判断或属性处理
            // String elementName = startEventElement.getNodeName(); // 获取元素名，例如
            // "startEvent"
            // String namespaceURI = startEventElement.getNamespaceURI(); // 获取命名空间 URI
            //
            // // 例：检查是否有 flowable:formKey 属性，如果没有则设置
            // if (!startEventElement.hasAttributeNS("http://flowable.org/bpmn", "formKey"))
            // {
            // startEventElement.setAttributeNS("http://flowable.org/bpmn",
            // "flowable:formKey", String
            // .valueOf(modelBo.getFormId()));
            // }
            //
            // // 处理完第一个符合条件的 startEvent 元素后退出循环
            // break;
            // }
            // }

            // 将修改后的文档转换回字节数组
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(baos);
            transformer.transform(source, result);
            byte[] modifiedBpmnBytes = baos.toByteArray();
            // 保存修改后的BPMN XML至新模型
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
     * 构建模型扩展信息
     *
     * @return
     */
    private String buildMetaInfo(WfMetaInfoDto metaInfo, String description, String iconId, String showMobile,
            String formId) {
        // 只有非空，才进行设置，避免更新时的覆盖
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
