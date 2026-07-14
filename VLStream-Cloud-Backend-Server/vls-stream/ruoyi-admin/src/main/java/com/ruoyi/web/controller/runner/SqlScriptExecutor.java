/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.runner;

import com.ruoyi.workflow.domain.bo.InitBo;
import com.ruoyi.workflow.service.IWfModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Resource;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class SqlScriptExecutor implements ApplicationRunner {

    @Value("${spring.datasource.dynamic.datasource.master.url}")
    private String url;

    @Value("${spring.datasource.dynamic.datasource.master.username}")
    private String user;

    @Value("${spring.datasource.dynamic.datasource.master.password}")
    private String password;

    @Resource
    private IWfModelService wfModelService;

    private final Logger logger = LoggerFactory.getLogger(SqlScriptExecutor.class);

    private static final String INIT_SQL_FILE = "oortcloud_workflowforms_initialize.sql";

    private static final String SEED_SQL_FILE = "oortcloud_workflowforms_seed.sql";

    private static final String LOCAL_USER_SYSTEM_SEED_SQL_FILE = "oortcloud_local_user_system_seed.sql";

    private static final String ROOT_TENANT_FIX_SQL_FILE = "oortcloud_workflowforms_seed_root_tenant_fix.sql";

    private static final String EVENT_MANAGEMENT_SEED_SQL_FILE = "oortcloud_event_management_seed.sql";

    private static final String ROOT_TENANT_ID = "0e391fd7-1033-4f09-88c0-187582fee462";

    private static final String SINGLE_TENANT_ID = "000000";

    private static final String EVENT_MANAGEMENT_APPLICATION_ID = "818301f0e77f4cd8a117414cbeb32d9e";

    private static final String EVENT_MANAGEMENT_APPLICATION_SECRET = "5f0de11687d744bc95e84e207d319493";

    private static final String SINGLE_TENANT_EVENT_MANAGEMENT_APP_ID = "000000-event-management-app";

    private static final String SINGLE_TENANT_EVENT_MANAGEMENT_FORM_CATEGORY_ID = "000000-event-management-form-app";

    private static final int REQUIRED_TEMPLATE_COUNT = 7;

    private static final int REQUIRED_SYSTEM_FORM_COUNT = 6;

    private static final List<String> necessaryTables = Arrays.asList(
        "ACT_RU_TASK", "wf_form", "sys_user","ACT_RE_DEPLOYMENT"
        // 添加所有其他必要的表名
    );

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 检查数据库是否已初始化
        if (!isDatabaseInitialized()) {
            executeSqlScript(INIT_SQL_FILE);
        } else {
            logger.info("数据库已初始化，跳过SQL脚本执行。");
        }

        if (!isWorkflowSeedInitialized()) {
            executeSqlScript(SEED_SQL_FILE);
        } else {
            logger.info("工作流模板种子数据已初始化，跳过种子SQL脚本执行。");
        }

        if (!isLocalUserSystemSeedInitialized()) {
            executeSqlScript(LOCAL_USER_SYSTEM_SEED_SQL_FILE);
        } else {
            logger.info("本地单租户用户权限种子数据已初始化，跳过本地用户权限SQL脚本执行。");
        }
        ensureSingleTenantEventManagementApplication();

        if (!isRootTenantSeedInitialized()) {
            executeSqlScript(ROOT_TENANT_FIX_SQL_FILE);
        } else {
            logger.info("顶级租户工作流模板种子数据已初始化，跳过顶级租户修正SQL脚本执行。");
        }

        if (isRootTenantSeedInitialized() && !isRootTenantWorkflowInitialized()) {
            initRootTenantWorkflow();
        } else {
            logger.info("顶级租户流程模板已初始化，跳过initStart自动初始化。");
        }

        boolean eventManagementSeedInitialized = isEventManagementSeedInitialized();
        if (!eventManagementSeedInitialized) {
            if (hasRootTenantEventManagementApplication()) {
                logger.warn("检测到顶级租户已存在事件管理应用，但事件管理 seed 数据未完整匹配，跳过固定 seed SQL，避免重复创建应用及流程数据。");
            } else {
                executeSqlScript(EVENT_MANAGEMENT_SEED_SQL_FILE);
                eventManagementSeedInitialized = isEventManagementSeedInitialized();
            }
        } else {
            logger.info("事件管理 seed 数据已初始化，跳过事件管理种子SQL脚本执行。");
        }

        if (eventManagementSeedInitialized) {
            if (!isRootTenantEventManagementDeployed()) {
                initRootTenantEventManagement();
            } else {
                logger.info("顶级租户事件管理流程已部署，跳过事件管理自动初始化。");
            }
        } else {
            logger.warn("事件管理 seed 数据未完整初始化，跳过事件管理自动部署，请检查顶级租户事件管理应用、表单和模型数据。");
        }

        repairDeployFormTenantIds();
        repairRootTenantEventManagementDeployForms();
    }

    private void ensureSingleTenantEventManagementApplication() {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            List<String> requiredTables = Arrays.asList("workorder_app", "wf_form_app").stream().collect(Collectors.toList());
            removeExistingTables(statement, requiredTables);
            if (!requiredTables.isEmpty()) {
                logger.warn("单租户事件管理工单应用及表单分类初始化依赖表不存在: {}", requiredTables);
                return;
            }

            int appCount = queryCount(statement,
                "SELECT COUNT(*) FROM workorder_app " +
                    "WHERE tenant_id = '" + SINGLE_TENANT_ID + "' " +
                    "  AND application_id = '" + EVENT_MANAGEMENT_APPLICATION_ID + "' " +
                    "  AND application_secret = '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "' " +
                    "  AND COALESCE(del_flag, '0') = '0'");
            if (appCount <= 0) {
                statement.executeUpdate(
                    "INSERT INTO workorder_app " +
                        "(app_id, application_name, application_id, application_secret, tenant_id, user_id, " +
                        " create_by, create_time, update_by, update_time, del_flag, app_flag, images, app_package) " +
                        "VALUES (" +
                        " '" + SINGLE_TENANT_EVENT_MANAGEMENT_APP_ID + "', '事件管理', '" + EVENT_MANAGEMENT_APPLICATION_ID + "', " +
                        " '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "', '" + SINGLE_TENANT_ID + "', NULL, " +
                        " 'system', NOW(), 'system', NOW(), '0', '0', NULL, 'vls-ui') " +
                        "ON DUPLICATE KEY UPDATE " +
                        " application_name = VALUES(application_name), " +
                        " application_id = VALUES(application_id), " +
                        " application_secret = VALUES(application_secret), " +
                        " tenant_id = VALUES(tenant_id), " +
                        " update_by = VALUES(update_by), " +
                        " update_time = VALUES(update_time), " +
                        " del_flag = '0', " +
                        " app_flag = VALUES(app_flag), " +
                        " app_package = VALUES(app_package)");
                logger.info("单租户事件管理工单应用初始化完成。");
            } else {
                logger.info("单租户事件管理工单应用已存在，跳过应用初始化。");
            }

            int formAppCount = queryCount(statement,
                "SELECT COUNT(*) FROM wf_form_app " +
                    "WHERE tenant_id = '" + SINGLE_TENANT_ID + "' " +
                    "  AND application_id = '" + EVENT_MANAGEMENT_APPLICATION_ID + "' " +
                    "  AND type = '1' " +
                    "  AND COALESCE(del_flag, '0') = '0'");
            if (formAppCount <= 0) {
                statement.executeUpdate(
                    "INSERT INTO wf_form_app " +
                        "(category_id, application_id, application_name, application_secret, tenant_id, user_id, " +
                        " parent_id, category_name, code, remark, create_by, create_time, update_by, update_time, " +
                        " del_flag, images, type, app_flag) " +
                        "VALUES (" +
                        " '" + SINGLE_TENANT_EVENT_MANAGEMENT_FORM_CATEGORY_ID + "', '" + EVENT_MANAGEMENT_APPLICATION_ID + "', " +
                        " '事件管理', '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "', '" + SINGLE_TENANT_ID + "', NULL, " +
                        " NULL, '事件管理', 'event_management', '', 'system', NOW(), 'system', NOW(), " +
                        " '0', NULL, '1', '0') " +
                        "ON DUPLICATE KEY UPDATE " +
                        " application_id = VALUES(application_id), " +
                        " application_name = VALUES(application_name), " +
                        " application_secret = VALUES(application_secret), " +
                        " tenant_id = VALUES(tenant_id), " +
                        " category_name = VALUES(category_name), " +
                        " code = VALUES(code), " +
                        " update_by = VALUES(update_by), " +
                        " update_time = VALUES(update_time), " +
                        " del_flag = '0', " +
                        " type = VALUES(type), " +
                        " app_flag = VALUES(app_flag)");
                logger.info("单租户事件管理工单表单分类初始化完成。");
            } else {
                logger.info("单租户事件管理工单表单分类已存在，跳过表单分类初始化。");
            }
        } catch (Exception e) {
            logger.error("初始化单租户事件管理工单应用及表单分类失败", e);
        }
    }

    private void executeSqlScript(String filePath) throws Exception {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date startdate = new Date();
        String startTime = sdf.format(startdate);

        logger.info("脚本执行开始: {}, 文件: {}", startTime, filePath);

        InputStream sqlInputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (sqlInputStream == null) {
            throw new IllegalStateException("SQL脚本不存在: " + filePath);
        }

        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(sqlInputStream, StandardCharsets.UTF_8))) {

            String line;
            AtomicInteger atomicInteger = new AtomicInteger(0);
            StringBuilder sqlAppender = new StringBuilder();
            boolean skip = false;

            while ((line = bufferedReader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                    continue; // 跳过空行
                }
                try {
                    // 跳过多行注释
                    if (trimmedLine.startsWith("/*")) {
                        skip = true;
                        sqlAppender.setLength(0);
                        continue;
                    }
                    if (trimmedLine.endsWith("*/")) {
                        skip = false;
                        sqlAppender.setLength(0);
                        continue;
                    }
                    if (skip) {
                        sqlAppender.setLength(0);
                        continue;
                    }

                    sqlAppender.append(line).append(System.lineSeparator());
                    if (trimmedLine.endsWith(";") || trimmedLine.startsWith("DROP")) {
                        String sql = sqlAppender.toString();
                        logger.info("正在执行SQL({} chars): {}", sql.length(), abbreviate(sql));
                        statement.execute(sql);
                        atomicInteger.incrementAndGet();
                        sqlAppender.setLength(0);
                    }
                } catch (Exception e) {
                    // 处理异常的SQL
                    logger.error("处理异常: " + sqlAppender.toString(), e);
                    sqlAppender.setLength(0);
                }
            }

            logger.info("总条数: " + atomicInteger.get());
        }

        Date endDate = new Date();
        logger.info("脚本执行完成: {}, 文件: {}", sdf.format(endDate), filePath);
        long milliseconds = endDate.getTime() - startdate.getTime();
        long minutes = milliseconds / (1000 * 60);

        logger.info("耗时毫秒: " + milliseconds);
        logger.info("耗时分钟: " + minutes);
    }

    private boolean isDatabaseInitialized() {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            // 获取所有表名并转换为小写
            List<String> lowerCaseNecessaryTables = necessaryTables.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

            removeExistingTables(statement, lowerCaseNecessaryTables);

            // 如果必要表的列表为空，表示所有必要的表都存在
            return lowerCaseNecessaryTables.isEmpty();
        } catch (Exception e) {
            logger.error("检查数据库初始化状态时发生错误", e);
            return false;
        }
    }

    private boolean isWorkflowSeedInitialized() {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            List<String> requiredSeedTables = Arrays.asList(
                "act_ge_bytearray", "act_re_model", "process_template", "re_model_json", "wf_form_synthesis", "wf_form"
            ).stream().collect(Collectors.toList());
            removeExistingTables(statement, requiredSeedTables);
            if (!requiredSeedTables.isEmpty()) {
                logger.warn("工作流模板种子数据依赖表不存在: {}", requiredSeedTables);
                return false;
            }

            int completeTemplateCount = queryCount(statement,
                "SELECT COUNT(*) FROM ( " +
                    "SELECT pt.id " +
                    "FROM process_template pt " +
                    "JOIN ACT_RE_MODEL m ON m.KEY_ = pt.model_key " +
                    "JOIN ( " +
                    "  SELECT pt2.model_key, MAX(m2.VERSION_) AS max_version " +
                    "  FROM process_template pt2 " +
                    "  JOIN ACT_RE_MODEL m2 ON m2.KEY_ = pt2.model_key " +
                    "  WHERE COALESCE(pt2.del_flag, '0') = '0' " +
                    "  GROUP BY pt2.model_key " +
                    ") latest ON latest.model_key = m.KEY_ AND latest.max_version = m.VERSION_ " +
                    "JOIN ACT_GE_BYTEARRAY ba ON ba.ID_ = m.EDITOR_SOURCE_VALUE_ID_ AND ba.BYTES_ IS NOT NULL " +
                    "JOIN re_model_json r ON r.model_id = m.ID_ AND COALESCE(r.del_flag, '0') = '0' " +
                    "WHERE COALESCE(pt.del_flag, '0') = '0' " +
                ") seed_templates");

            int completeSystemFormCategoryCount = queryCount(statement,
                "SELECT COUNT(*) FROM ( " +
                    "SELECT fs.category_id " +
                    "FROM wf_form_synthesis fs " +
                    "JOIN wf_form f ON f.category_id = fs.category_id " +
                    "  AND f.tenant_id = fs.tenant_id " +
                    "  AND COALESCE(f.del_flag, '0') = '0' " +
                    "WHERE fs.category_name = '系统审批' " +
                    "  AND COALESCE(fs.type, '') = '0' " +
                    "  AND COALESCE(fs.del_flag, '0') = '0' " +
                    "  AND COALESCE(f.type, '') = '0' " +
                    "  AND f.form_name IN ('租户审批', '企业主体审批', '部门审批', '用户审批', '用户权限提升申请审批', '租户认证审批') " +
                    "GROUP BY fs.category_id " +
                    "HAVING COUNT(DISTINCT f.form_name) >= " + REQUIRED_SYSTEM_FORM_COUNT +
                ") seed_form_categories");

            return completeTemplateCount >= REQUIRED_TEMPLATE_COUNT && completeSystemFormCategoryCount > 0;
        } catch (Exception e) {
            logger.error("检查工作流模板种子数据时发生错误", e);
            return false;
        }
    }

    private boolean isLocalUserSystemSeedInitialized() {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "SELECT COUNT(1) FROM sys_user WHERE user_name = 'admin' AND tenant_id = '" + SINGLE_TENANT_ID + "' AND del_flag = '0'")) {
            return resultSet.next() && resultSet.getInt(1) > 0;
        } catch (Exception e) {
            logger.warn("检查本地单租户用户权限种子数据失败，将尝试执行种子脚本: {}", e.getMessage());
            return false;
        }
    }

    private boolean isRootTenantSeedInitialized() {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            List<String> requiredSeedTables = Arrays.asList(
                "wf_form_synthesis", "wf_form"
            ).stream().collect(Collectors.toList());
            removeExistingTables(statement, requiredSeedTables);
            if (!requiredSeedTables.isEmpty()) {
                logger.warn("顶级租户工作流模板种子数据依赖表不存在: {}", requiredSeedTables);
                return false;
            }

            int rootSystemFormCategoryCount = queryCount(statement,
                "SELECT COUNT(*) FROM ( " +
                    "SELECT fs.category_id " +
                    "FROM wf_form_synthesis fs " +
                    "JOIN wf_form f ON f.category_id = fs.category_id " +
                    "  AND f.tenant_id = fs.tenant_id " +
                    "  AND COALESCE(f.del_flag, '0') = '0' " +
                    "WHERE fs.tenant_id = '" + ROOT_TENANT_ID + "' " +
                    "  AND fs.category_name = '系统审批' " +
                    "  AND COALESCE(fs.type, '') = '0' " +
                    "  AND COALESCE(fs.del_flag, '0') = '0' " +
                    "  AND COALESCE(f.type, '') = '0' " +
                    "  AND f.form_name IN ('租户审批', '企业主体审批', '部门审批', '用户审批', '用户权限提升申请审批', '租户认证审批') " +
                    "GROUP BY fs.category_id " +
                    "HAVING COUNT(DISTINCT f.form_name) >= " + REQUIRED_SYSTEM_FORM_COUNT +
                ") root_seed_form_categories");

            return rootSystemFormCategoryCount > 0;
        } catch (Exception e) {
            logger.error("检查顶级租户工作流模板种子数据时发生错误", e);
            return false;
        }
    }

    private boolean isRootTenantWorkflowInitialized() {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            List<String> requiredTables = Arrays.asList(
                "act_re_model", "process_template", "wf_synthesis"
            ).stream().collect(Collectors.toList());
            removeExistingTables(statement, requiredTables);
            if (!requiredTables.isEmpty()) {
                logger.warn("顶级租户流程模板初始化依赖表不存在: {}", requiredTables);
                return false;
            }

            int deployedModelCount = queryCount(statement,
                "SELECT COUNT(DISTINCT m.NAME_) " +
                    "FROM ACT_RE_MODEL m " +
                    "JOIN process_template pt ON pt.model_name = m.NAME_ " +
                    "WHERE m.TENANT_ID_ = '" + ROOT_TENANT_ID + "' " +
                    "  AND m.DEPLOYMENT_ID_ IS NOT NULL " +
                    "  AND COALESCE(pt.del_flag, '0') = '0'");

            int defaultCategoryCount = queryCount(statement,
                "SELECT COUNT(*) " +
                    "FROM wf_synthesis " +
                    "WHERE tenant_id = '" + ROOT_TENANT_ID + "' " +
                    "  AND category_name = '系统默认流程' " +
                    "  AND COALESCE(del_flag, '0') = '0'");

            return deployedModelCount >= REQUIRED_TEMPLATE_COUNT && defaultCategoryCount > 0;
        } catch (Exception e) {
            logger.error("检查顶级租户流程模板初始化状态时发生错误", e);
            return false;
        }
    }

    private void initRootTenantWorkflow() {
        try {
            InitBo initBo = new InitBo();
            initBo.setTenantId(ROOT_TENANT_ID);
            initBo.setToTenantId(ROOT_TENANT_ID);
            String result = wfModelService.initStart(initBo);
            logger.info("顶级租户流程模板自动初始化完成: {}", result);
        } catch (Exception e) {
            logger.error("顶级租户流程模板自动初始化失败，请检查工作流模板seed数据和Flowable部署状态", e);
        }
    }

    private boolean isEventManagementSeedInitialized() {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            List<String> requiredTables = Arrays.asList(
                "workorder_app", "wf_form_app", "wf_form", "act_ge_bytearray", "act_re_model", "re_model_json"
            ).stream().collect(Collectors.toList());
            removeExistingTables(statement, requiredTables);
            if (!requiredTables.isEmpty()) {
                logger.warn("事件管理 seed 数据依赖表不存在: {}", requiredTables);
                return false;
            }

            int appCount = queryCount(statement,
                "SELECT COUNT(*) FROM workorder_app " +
                    "WHERE tenant_id = '" + ROOT_TENANT_ID + "' " +
                    "  AND application_id = '" + EVENT_MANAGEMENT_APPLICATION_ID + "' " +
                    "  AND application_secret = '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "' " +
                    "  AND COALESCE(del_flag, '0') = '0'");

            int formAppCount = queryCount(statement,
                "SELECT COUNT(*) FROM wf_form_app " +
                    "WHERE tenant_id = '" + ROOT_TENANT_ID + "' " +
                    "  AND application_id = '" + EVENT_MANAGEMENT_APPLICATION_ID + "' " +
                    "  AND application_secret = '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "' " +
                    "  AND type = '1' " +
                    "  AND COALESCE(del_flag, '0') = '0'");

            int formCount = queryCount(statement,
                "SELECT COUNT(*) FROM wf_form f " +
                    "JOIN wf_form_app fa ON fa.category_id = f.category_id AND fa.tenant_id = f.tenant_id " +
                    "WHERE f.tenant_id = '" + ROOT_TENANT_ID + "' " +
                    "  AND fa.application_id = '" + EVENT_MANAGEMENT_APPLICATION_ID + "' " +
                    "  AND fa.application_secret = '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "' " +
                    "  AND fa.type = '1' " +
                    "  AND COALESCE(f.del_flag, '0') = '0' " +
                    "  AND COALESCE(fa.del_flag, '0') = '0'");

            int modelCount = queryCount(statement,
                "SELECT COUNT(*) FROM ACT_RE_MODEL m " +
                    "JOIN workorder_app a ON a.app_id = m.CATEGORY_ AND a.tenant_id = m.TENANT_ID_ " +
                    "JOIN ACT_GE_BYTEARRAY b ON b.ID_ = m.EDITOR_SOURCE_VALUE_ID_ AND b.BYTES_ IS NOT NULL " +
                    "JOIN re_model_json r ON r.model_id = m.ID_ AND r.tenant_id = m.TENANT_ID_ " +
                    "WHERE m.TENANT_ID_ = '" + ROOT_TENANT_ID + "' " +
                    "  AND a.application_id = '" + EVENT_MANAGEMENT_APPLICATION_ID + "' " +
                    "  AND a.application_secret = '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "' " +
                    "  AND COALESCE(a.del_flag, '0') = '0' " +
                    "  AND COALESCE(r.del_flag, '0') = '0'");

            return appCount > 0 && formAppCount > 0 && formCount > 0 && modelCount > 0;
        } catch (Exception e) {
            logger.error("检查事件管理 seed 数据时发生错误", e);
            return false;
        }
    }

    private boolean hasRootTenantEventManagementApplication() {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            List<String> requiredTables = Arrays.asList(
                "workorder_app", "wf_form_app"
            ).stream().collect(Collectors.toList());
            removeExistingTables(statement, requiredTables);
            if (!requiredTables.isEmpty()) {
                logger.warn("事件管理应用检查依赖表不存在: {}", requiredTables);
                return false;
            }

            int appCount = queryCount(statement,
                "SELECT COUNT(*) FROM workorder_app " +
                    "WHERE tenant_id = '" + ROOT_TENANT_ID + "' " +
                    "  AND application_id = '" + EVENT_MANAGEMENT_APPLICATION_ID + "' " +
                    "  AND application_secret = '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "' " +
                    "  AND COALESCE(del_flag, '0') = '0'");

            int formAppCount = queryCount(statement,
                "SELECT COUNT(*) FROM wf_form_app " +
                    "WHERE tenant_id = '" + ROOT_TENANT_ID + "' " +
                    "  AND application_id = '" + EVENT_MANAGEMENT_APPLICATION_ID + "' " +
                    "  AND application_secret = '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "' " +
                    "  AND type = '1' " +
                    "  AND COALESCE(del_flag, '0') = '0'");

            return appCount > 0 || formAppCount > 0;
        } catch (Exception e) {
            logger.error("检查顶级租户事件管理应用时发生错误", e);
            return false;
        }
    }

    private boolean isRootTenantEventManagementDeployed() {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            List<String> requiredTables = Arrays.asList(
                "workorder_app", "act_re_model", "act_re_procdef"
            ).stream().collect(Collectors.toList());
            removeExistingTables(statement, requiredTables);
            if (!requiredTables.isEmpty()) {
                logger.warn("事件管理部署状态依赖表不存在: {}", requiredTables);
                return false;
            }

            int deployedCount = queryCount(statement,
                "SELECT COUNT(*) FROM ACT_RE_PROCDEF p " +
                    "JOIN ACT_RE_MODEL m ON m.KEY_ = p.KEY_ AND m.TENANT_ID_ = p.TENANT_ID_ " +
                    "JOIN workorder_app a ON a.app_id = m.CATEGORY_ AND a.tenant_id = m.TENANT_ID_ " +
                    "WHERE m.TENANT_ID_ = '" + ROOT_TENANT_ID + "' " +
                    "  AND a.application_id = '" + EVENT_MANAGEMENT_APPLICATION_ID + "' " +
                    "  AND a.application_secret = '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "' " +
                    "  AND COALESCE(a.del_flag, '0') = '0'");
            return deployedCount > 0;
        } catch (Exception e) {
            logger.error("检查顶级租户事件管理部署状态时发生错误", e);
            return false;
        }
    }

    private void initRootTenantEventManagement() {
        try {
            InitBo initBo = new InitBo();
            initBo.setTenantId(ROOT_TENANT_ID);
            initBo.setToTenantId(ROOT_TENANT_ID);
            wfModelService.eventManagementInitStart(initBo);
            logger.info("顶级租户事件管理流程自动初始化完成。");
        } catch (Exception e) {
            logger.error("顶级租户事件管理流程自动初始化失败，请检查事件管理 seed 数据和 Flowable 部署状态", e);
        }
    }

    private void repairDeployFormTenantIds() {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            List<String> requiredTables = Arrays.asList(
                "wf_deploy_form", "act_re_deployment", "wf_form"
            ).stream().collect(Collectors.toList());
            removeExistingTables(statement, requiredTables);
            if (!requiredTables.isEmpty()) {
                logger.warn("部署表单租户数据修复依赖表不存在: {}", requiredTables);
                return;
            }

            int tenantCount = statement.executeUpdate(
                "UPDATE wf_deploy_form df " +
                    "JOIN ACT_RE_DEPLOYMENT d ON d.ID_ = df.deploy_id " +
                    "SET df.tenant_id = d.TENANT_ID_ " +
                    "WHERE (df.tenant_id IS NULL OR df.tenant_id = '') " +
                    "  AND d.TENANT_ID_ IS NOT NULL " +
                    "  AND d.TENANT_ID_ <> ''");

            int userCount = statement.executeUpdate(
                "UPDATE wf_deploy_form df " +
                    "JOIN wf_form f ON CONCAT('key_', f.form_id) = df.form_key " +
                    "  AND f.tenant_id = df.tenant_id " +
                    "SET df.user_id = f.user_id " +
                    "WHERE (df.user_id IS NULL OR df.user_id = '') " +
                    "  AND f.user_id IS NOT NULL " +
                    "  AND f.user_id <> ''");

            if (tenantCount > 0 || userCount > 0) {
                logger.info("部署表单租户数据修复完成，tenantId修复{}条，userId修复{}条。", tenantCount, userCount);
            }
        } catch (Exception e) {
            logger.error("部署表单租户数据修复失败", e);
        }
    }

    private void repairRootTenantEventManagementDeployForms() {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            List<String> requiredTables = Arrays.asList(
                "wf_deploy_form", "act_re_model", "workorder_app", "wf_form", "wf_form_app"
            ).stream().collect(Collectors.toList());
            removeExistingTables(statement, requiredTables);
            if (!requiredTables.isEmpty()) {
                logger.warn("事件管理部署表单修复依赖表不存在: {}", requiredTables);
                return;
            }

            int count = statement.executeUpdate(
                "UPDATE wf_deploy_form df " +
                    "JOIN ACT_RE_MODEL m ON m.DEPLOYMENT_ID_ = df.deploy_id AND m.TENANT_ID_ = df.tenant_id " +
                    "JOIN workorder_app a ON a.app_id = m.CATEGORY_ AND a.tenant_id = m.TENANT_ID_ " +
                    "JOIN wf_form f ON df.form_key = CONCAT('key_', f.form_id) AND f.tenant_id = df.tenant_id " +
                    "JOIN wf_form_app fa ON fa.category_id = f.category_id AND fa.tenant_id = f.tenant_id " +
                    "SET df.form_name = f.form_name, df.content = f.content " +
                    "WHERE df.tenant_id = '" + ROOT_TENANT_ID + "' " +
                    "  AND a.application_id = '" + EVENT_MANAGEMENT_APPLICATION_ID + "' " +
                    "  AND a.application_secret = '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "' " +
                    "  AND COALESCE(a.del_flag, '0') = '0' " +
                    "  AND fa.application_id = '" + EVENT_MANAGEMENT_APPLICATION_ID + "' " +
                    "  AND fa.application_secret = '" + EVENT_MANAGEMENT_APPLICATION_SECRET + "' " +
                    "  AND fa.type = '1' " +
                    "  AND COALESCE(f.del_flag, '0') = '0' " +
                    "  AND COALESCE(fa.del_flag, '0') = '0' " +
                    "  AND (COALESCE(df.form_name, '') <> COALESCE(f.form_name, '') " +
                    "    OR COALESCE(df.content, '') <> COALESCE(f.content, ''))");
            if (count > 0) {
                logger.info("事件管理部署表单快照已刷新，更新 {} 条。", count);
            }
        } catch (Exception e) {
            logger.error("事件管理部署表单快照修复失败", e);
        }
    }

    private void removeExistingTables(Statement statement, List<String> missingTables) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("SHOW TABLES")) {
            while (resultSet.next()) {
                missingTables.remove(resultSet.getString(1).toLowerCase());
            }
        }
    }

    private int queryCount(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }

    private String abbreviate(String sql) {
        String normalizedSql = sql.replaceAll("\\s+", " ").trim();
        if (normalizedSql.length() <= 500) {
            return normalizedSql;
        }
        return normalizedSql.substring(0, 500) + "...";
    }
}
