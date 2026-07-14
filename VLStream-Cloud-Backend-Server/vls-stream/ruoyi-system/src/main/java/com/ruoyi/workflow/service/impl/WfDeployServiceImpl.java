/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.flowable.core.domain.ProcessQuery;
import com.ruoyi.workflow.domain.*;
import com.ruoyi.workflow.domain.vo.WfDeployVo;
import com.ruoyi.workflow.mapper.WfDeployFormMapper;
import com.ruoyi.workflow.service.*;
import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.NativeProcessDefinitionQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author KonBAI
 * @createTime 2022/6/30 9:04
 */
@RequiredArgsConstructor
@Service
public class WfDeployServiceImpl implements IWfDeployService {

    private final RepositoryService repositoryService;
    private final WfDeployFormMapper deployFormMapper;
    private final ProcessEngine processEngine;
    private final IWfAppService wfAppService;
    private final IWfSynthesisService wfSynthesisService;
    private final IWorkOrderAppService workOrderAppService;
    private final IWorkOrderSynthesisService workerSynthesisService;
//    @Override
//    public TableDataInfo<WfDeployVo> queryPageList(ProcessQuery processQuery, PageQuery pageQuery) {
//        // 流程定义列表数据查询
//        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery()
//            .latestVersion()
//            .orderByProcessDefinitionKey()
//            .asc();
//        // 构建搜索条件
//        ProcessUtils.buildProcessSearch(processDefinitionQuery, processQuery, processEngine);
//        long pageTotal = processDefinitionQuery.count();
//        if (pageTotal <= 0) {
//            return TableDataInfo.build();
//        }
//        int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
//        List<ProcessDefinition> definitionList = processDefinitionQuery.listPage(offset, pageQuery.getPageSize());
//
//        List<WfDeployVo> deployVoList = new ArrayList<>(definitionList.size());
//        for (ProcessDefinition processDefinition : definitionList) {
//            String deploymentId = processDefinition.getDeploymentId();
//            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
//            WfDeployVo vo = new WfDeployVo();
//            vo.setDefinitionId(processDefinition.getId());
//            vo.setProcessKey(processDefinition.getKey());
//            vo.setProcessName(processDefinition.getName());
//            vo.setVersion(processDefinition.getVersion());
//            vo.setCategory(processDefinition.getCategory());
//            vo.setDeploymentId(processDefinition.getDeploymentId());
//            vo.setSuspended(processDefinition.isSuspended());
//            // 流程部署信息
//            vo.setCategory(deployment.getCategory());
//            vo.setDeploymentTime(deployment.getDeploymentTime());
//            deployVoList.add(vo);
//        }
//        Page<WfDeployVo> page = new Page<>();
//        page.setRecords(deployVoList);
//        page.setTotal(pageTotal);
//        return TableDataInfo.build(page);
//    }


    //    /**
//     * 查询对应分类流程定义
//     * @param processQuery
//     * @param processDefinitionQuery
//     * @return
//     */
//    private ProcessDefinitionQuery processDefinitionClassification(ProcessQuery processQuery,ProcessDefinitionQuery processDefinitionQuery){
//        if (processQuery.getWfAppAll()) {
//            processDefinitionQuery.processDefinitionCategoryLike(ProcessConstants.WF_APP + "%");
//        } else if(processQuery.getWfSynthesisAll()){
//            processDefinitionQuery.processDefinitionCategoryLike(ProcessConstants.WF_SYNTHESIS + "%");
//        }else if (processQuery.getWorkOrderAppAll()) {
//            processDefinitionQuery.processDefinitionCategoryLike(ProcessConstants.WORK_ORDER_APP + "%");
//        } else if(processQuery.getWorkOrderSynthesisAll()){
//            processDefinitionQuery.processDefinitionCategoryLike(ProcessConstants.WORK_ORDER_SYNTHESIS + "%");
//        }
//        return processDefinitionQuery;
//    }
    @Override
    public TableDataInfo<WfDeployVo> queryPageList(ProcessQuery processQuery, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());

        // 构建计数查询 SQL
        String countSql = buildSql(processQuery, true);
        // 构建数据查询 SQL
        String dataSql = buildSql(processQuery, false);

        // 执行计数查询
        NativeProcessDefinitionQuery countQuery = repositoryService.createNativeProcessDefinitionQuery().sql(countSql).parameter("tenantId", sysUser.getTenantId());
        setQueryParameters(countQuery, processQuery);

        long pageTotal = countQuery.count();
        if (pageTotal <= 0) {
            return TableDataInfo.build();
        }

        // 执行分页查询
        NativeProcessDefinitionQuery dataQuery = repositoryService.createNativeProcessDefinitionQuery().sql(dataSql).parameter("tenantId", sysUser.getTenantId());
        setQueryParameters(dataQuery, processQuery);

        List<ProcessDefinition> definitions = dataQuery.listPage((pageQuery.getPageNum() - 1) * pageQuery.getPageSize(), pageQuery.getPageSize());

        // 转换为 WfDeployVo
        List<WfDeployVo> deployVoList = definitions.stream().map(def -> {
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(def.getDeploymentId()).deploymentTenantId(sysUser.getTenantId()).singleResult();
            WfDeployVo vo = new WfDeployVo();
            vo.setDefinitionId(def.getId());
            vo.setProcessKey(def.getKey());
            vo.setProcessName(def.getName());
            vo.setVersion(def.getVersion());
            vo.setCategory(def.getCategory());
            vo.setDeploymentId(def.getDeploymentId());
            vo.setSuspended(def.isSuspended());
            vo.setDeploymentTime(deployment.getDeploymentTime()); // 添加部署时间
            return vo;
        }).collect(Collectors.toList());

        Page<WfDeployVo> page = new Page<>();
        page.setRecords(deployVoList);
        page.setTotal(pageTotal);

        return TableDataInfo.build(page);
    }

    /**
     * 构建动态 SQL
     *
     * @param processQuery 查询条件
     * @param isCount      是否是计数查询
     * @return 构建的 SQL
     */
    private String buildSql(ProcessQuery processQuery, boolean isCount) {
        StringBuilder sqlBuilder = new StringBuilder();
        if (isCount) {
            sqlBuilder.append("SELECT COUNT(1) ");
        } else {
            sqlBuilder.append("SELECT RES.* ");
        }
        sqlBuilder.append("FROM ACT_RE_PROCDEF RES ")
            .append("WHERE RES.TENANT_ID_ = #{tenantId} ")
            .append("AND RES.VERSION_ = (")
            .append("    SELECT MAX(SUB.VERSION_) ")
            .append("    FROM ACT_RE_PROCDEF SUB ")
            .append("    WHERE SUB.TENANT_ID_ = #{tenantId} AND SUB.KEY_ = RES.KEY_")
            .append(")");

        //根据参数分类
        if (processQuery.getWorkOrderAppAll()) {
            processQuery.setCategoryList(mergeAllCategories(processQuery.getCategoryList(), workOrderAppService.list(), category -> ((WorkOrderApp) category).getAppId()));
        } else if (processQuery.getWorkOrderSynthesisAll()) {
            processQuery.setCategoryList(mergeAllCategories(processQuery.getCategoryList(), workerSynthesisService.list(), category -> ((WorkOrderSynthesis) category).getSynthesisId()));
        } else if (processQuery.getWfAppAll()) {
            processQuery.setCategoryList(mergeAllCategories(processQuery.getCategoryList(), wfAppService.list(), category -> ((WfApp) category).getAppId()));
        } else if (processQuery.getWfSynthesisAll()) {
            processQuery.setCategoryList(mergeAllCategories(processQuery.getCategoryList(), wfSynthesisService.list(), category -> ((WfSynthesis) category).getSynthesisId()));
        }

        //没有分类时直接返回null
        if(ObjectUtil.isEmpty(processQuery.getCategoryList())){
            sqlBuilder.append(" and 1=2");
        }

        // 动态拼接分类条件
        if (processQuery.getWorkOrderAppAll() || processQuery.getWorkOrderSynthesisAll() || processQuery.getWfAppAll() || processQuery.getWfSynthesisAll()|| processQuery.getCategory()!=null) {
            List<String> categoryList = processQuery.getCategoryList();
            if (ObjectUtil.isNotEmpty(categoryList)) {
                String inClause = categoryList.stream()
                    .map(category -> "'" + category.replace("'", "''") + "'")
                    .collect(Collectors.joining(", "));
                sqlBuilder.append(" AND RES.CATEGORY_ IN (").append(inClause).append(")");
            }
            if(processQuery.getCategory()!=null){
                sqlBuilder.append(" AND RES.CATEGORY_ = #{Category}");
            }
        }

        if (StringUtils.isNotBlank(processQuery.getProcessKey())) {
            sqlBuilder.append(" AND RES.KEY_ LIKE #{key}");
        }
        if (StringUtils.isNotBlank(processQuery.getProcessName())) {
            sqlBuilder.append(" AND RES.NAME_ LIKE #{name}");
        }
        if (StringUtils.isNotBlank(processQuery.getState())) {
            sqlBuilder.append(" AND RES.SUSPENSION_STATE_ = #{state}");
        }

        return sqlBuilder.toString();
    }


    /**
     * 将查询参数占位符的值做替换
     *
     * @param query        NativeQuery
     * @param processQuery 查询条件
     */
    private void setQueryParameters(NativeProcessDefinitionQuery query, ProcessQuery processQuery) {
        if (StringUtils.isNotBlank(processQuery.getCategory())) {
            query.parameter("Category", processQuery.getCategory());
        }
        if (StringUtils.isNotBlank(processQuery.getProcessKey())) {
            query.parameter("key", "%" + processQuery.getProcessKey() + "%");
        }
        if (StringUtils.isNotBlank(processQuery.getProcessName())) {
            query.parameter("name", "%" + processQuery.getProcessName() + "%");
        }
        if (StringUtils.isNotBlank(processQuery.getState())) {
            int state = SuspensionState.ACTIVE.toString().equals(processQuery.getState()) ? 1 : 2;
            query.parameter("state", state);
        }
    }


    //将分类id列表合并到已有的分类列表中，并去重
    private List<String> mergeAllCategories(List<String> existingList, List<?> categoryList, Function<Object, String> mapper) {
        if (categoryList != null && !categoryList.isEmpty()) {
            Set<String> resultSet = new HashSet<>(existingList);
            categoryList.stream().map(mapper).forEach(resultSet::add);
            return new ArrayList<>(resultSet);
        }
        return existingList;
    }

    @Override
    public TableDataInfo<WfDeployVo> queryPublishList(String processKey, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        // 创建查询条件
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processKey).processDefinitionTenantId(sysUser.getTenantId()).orderByProcessDefinitionVersion().desc();
        long pageTotal = processDefinitionQuery.count();
        if (pageTotal <= 0) {
            return TableDataInfo.build();
        }
        // 根据查询条件，查询所有版本
        int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
        List<ProcessDefinition> processDefinitionList = processDefinitionQuery.processDefinitionTenantId(sysUser.getTenantId()).listPage(offset, pageQuery.getPageSize());
        List<WfDeployVo> deployVoList = processDefinitionList.stream().map(item -> {
            WfDeployVo vo = new WfDeployVo();
            vo.setDefinitionId(item.getId());
            vo.setProcessKey(item.getKey());
            vo.setProcessName(item.getName());
            vo.setVersion(item.getVersion());
            vo.setCategory(item.getCategory());
            vo.setDeploymentId(item.getDeploymentId());
            vo.setSuspended(item.isSuspended());
            return vo;
        }).collect(Collectors.toList());
        Page<WfDeployVo> page = new Page<>();
        page.setRecords(deployVoList);
        page.setTotal(pageTotal);
        return TableDataInfo.build(page);
    }

    /**
     * 激活或挂起流程
     *
     * @param state        状态
     * @param definitionId 流程定义ID
     */
    @Override
    public void updateState(String definitionId, String state) {
        if (SuspensionState.ACTIVE.toString().equals(state)) {
            // 激活
            repositoryService.activateProcessDefinitionById(definitionId, true, null);
        } else if (SuspensionState.SUSPENDED.toString().equals(state)) {
            // 挂起
            repositoryService.suspendProcessDefinitionById(definitionId, true, null);
        }
    }

    @Override
    public String queryBpmnXmlById(String definitionId) {
        InputStream inputStream = repositoryService.getProcessModel(definitionId);
        try {
            return IoUtil.readUtf8(inputStream);
        } catch (IORuntimeException exception) {
            throw new RuntimeException("加载xml文件异常");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<String> deployIds) {
        for (String deployId : deployIds) {
            repositoryService.deleteDeployment(deployId, true);
            deployFormMapper.delete(new LambdaQueryWrapper<WfDeployForm>().eq(WfDeployForm::getDeployId, deployId));
        }
    }
}
