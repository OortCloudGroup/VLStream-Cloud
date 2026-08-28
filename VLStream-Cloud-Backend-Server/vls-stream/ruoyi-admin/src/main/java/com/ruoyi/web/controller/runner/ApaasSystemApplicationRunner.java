/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.runner;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.constant.CacheNames;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.constant.PlatformConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.LoginRsaUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.anyline.metadata.Table;
import org.anyline.proxy.ServiceProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * work order
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ApaasSystemApplicationRunner implements ApplicationRunner {

    private static final String[] TABLE_IGNORE = new String[]{"xxl_job_", "sys_user", "sys_dept", "onl_", "pmt_user"};

    private static final String CODE = "code";

    private static final String DATA = "data";

//    @Value(value = "${platform.loginCodeUrl}")
    private String loginCodeUrl;
//
//    @Value(value = "${platform.userTenantsUrl}")
    private String userTenantsUrl;
//
//    @Value(value = "${platform.loginUrl}")
    private String loginUrl;
//
//    @Value(value = "${platform.reportDataScopeUrl}")
    private String reportDataScopeUrl;
//
//    @Value(value = "${platform.verifyDataScopeUrl}")
    private String verifyDataScopeUrl;

    @Value("${vls.apaas-sync.enabled:false}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("APaaS外部权限上报已禁用，跳过启动同步。");
            return;
        }

        try {
            // need to data
            JSONObject data = new JSONObject();
            // idfrom /apaas-sso/sso/v1/getTenantIdByPhraseinterfaceGet
            data.set("loginId", "admin");
            data.set("password", "123456");
            // Convert to
            data.set("timestamp", System.currentTimeMillis() / 1000);
            data.set("client", "pcweb");

            JSONObject params = new JSONObject();
            params.set("userInfo", LoginRsaUtils.encrypt(data.toString()));
            String accessToken = null;
            String result1 = HttpRequest.post(loginCodeUrl)
                                        .header(PlatformConstants.HEADER_REQUEST_TYPE, PlatformConstants.APP)
                                        .header(PlatformConstants.HEADER_APP_ID, PlatformConstants.APP_ID)
                                        .header(PlatformConstants.HEADER_SERVER_KEY, PlatformConstants.SECRET_KEY)
                                        .body(params.toString()).timeout(5000)
                                        .execute().body();
            JSONObject loginCode = JSONUtil.parseObj(result1);
            if (HttpStatus.SUCCESS == loginCode.getInt(CODE)) {
                JSONObject tokenData = loginCode.getJSONObject(DATA);
                if (tokenData != null) {
                    String token = tokenData.getStr("token");
                    String result2 = HttpRequest.post(userTenantsUrl)
                                                .header(PlatformConstants.HEADER_REQUEST_TYPE, PlatformConstants.APP)
                                                .header(PlatformConstants.HEADER_APP_ID, PlatformConstants.APP_ID)
                                                .header(PlatformConstants.HEADER_SERVER_KEY,
                                                    PlatformConstants.SECRET_KEY)
                                                .body(new JSONObject().set(PlatformConstants.HEADER_ACCESS_TOKEN,
                                                    token).toString()).timeout(5000)
                                                .execute().body();
                    JSONObject userTenants = JSONUtil.parseObj(result2);
                    if (HttpStatus.SUCCESS == userTenants.getInt(CODE)) {
                        JSONArray jsonArray = userTenants.getJSONObject(DATA).getJSONArray("list");
                        if (CollUtil.isNotEmpty(jsonArray)) {
                            String tenantId = jsonArray.getJSONObject(0).getStr("tenant_id");

                            String result3 = HttpRequest.post(loginUrl)
                                                        .header(PlatformConstants.HEADER_REQUEST_TYPE,
                                                            PlatformConstants.APP)
                                                        .header(PlatformConstants.HEADER_APP_ID,
                                                            PlatformConstants.APP_ID)
                                                        .header(PlatformConstants.HEADER_SERVER_KEY,
                                                            PlatformConstants.SECRET_KEY)
                                                        .body(new JSONObject().set(PlatformConstants.HEADER_ACCESS_TOKEN, token).set("tenant_id", tenantId).toString()).timeout(5000)
                                                        .execute().body();
                            JSONObject bodyJson = JSONUtil.parseObj(result3);
                            if (HttpStatus.SUCCESS == loginCode.getInt(CODE)) {
                                if (HttpStatus.SUCCESS == bodyJson.getInt(CODE)) {
                                    try {
                                        accessToken = bodyJson.getJSONObject(DATA).getStr("accessToken");
                                    } catch (Exception e) {
                                        log.error("工单中心，网关登录失败：{}", result3);
                                        throw new ServiceException("工单中心，网关登录失败！");
                                    }
                                } else {
                                    log.error("工单中心，网关登录失败：{}", result3);
                                    throw new ServiceException("工单中心，网关登录失败！");
                                }
                            }
                        }
                    }
                }
            } else {
                log.error("工单中心，网关登录失败：{}", result1);
                throw new ServiceException("工单中心，网关登录失败！");
            }
            reportModel(accessToken);
        } catch (Exception e) {
            log.error("初始化上报失败：{}", e.getMessage());
        }
    }



    /**
     *
     *
     * @param token
     */
    private void reportModel(String token) {
// //data interface
//        JSONObject requestParam = new JSONObject();
//
//
// // 2. do_list object
//        JSONObject doList = new JSONObject();
// doList.put("query", new JSONObject().put("name", "Query "));
// doList.put("update", new JSONObject().put("name", "Update "));
// doList.put("add", new JSONObject().put("name", "Add "));
// doList.put("del", new JSONObject().put("name", "Delete "));
//
// // 3. act ( )node
//        JSONObject gongdanAct = new JSONObject();
// gongdanAct.put("name", "work order ");
//        gongdanAct.put("do_list",  doList);
//
// // 4. sub ( sub )node
//        JSONObject sub = new JSONObject();
//        sub.put("workOrder", gongdanAct);
//
// // 5. pact ( )node
//        JSONObject workflowforms = new JSONObject();
// workflowforms.put("name", "work order in ");
//        workflowforms.put("sub",  sub);
//
// // 6. auth node, pact
//        JSONObject auth = new JSONObject();
//        auth.put("workflowforms", workflowforms);


        JSONObject requestParam = new JSONObject();
        try {
            requestParam = buildAuthReportFromSource();
            log.info("开始上报权限：{}", requestParam);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        JSONObject dataParam = new JSONObject();
        LinkedHashMap<String, Table<?>> tablesMap = ServiceProxy.metadata().tables();
        List<Table<?>> tableList = tablesMap.values().stream()
                                            .filter(x -> !StringUtils.startWithAnyIgnoreCase(x.getName(),
                                                TABLE_IGNORE)).collect(Collectors.toList());
        tableList.forEach(tableData -> {
            JSONObject columnData = new JSONObject();

            Table<?> table = ServiceProxy.metadata().table(tableData.getName());
            // Get
            table.getColumns().forEach((columnName, column) -> {
                if (StringUtils.isNotBlank(column.getComment())) {
                    JSONObject fieldInfo = new JSONObject();
                    fieldInfo.set("name", column.getComment());
                    fieldInfo.set("type", column.getTypeName().toLowerCase());
                    columnData.set(column.getName().toLowerCase(), fieldInfo);
                }
            });
            JSONObject columnInfo = new JSONObject();
            columnInfo.set("columns", columnData);
            columnInfo.set("name", table.getComment());
            dataParam.set(tableData.getName().toLowerCase(), columnInfo);
        });
        requestParam.set("auth", requestParam.get("auth"));
        requestParam.set(DATA, dataParam);
        requestParam.set("service_id", PlatformConstants.APP_ID);
        requestParam.set("service", "apaas-workflowforms");
        requestParam.set("service_name", "统一工单服务");
        requestParam.set("timestamp", System.currentTimeMillis() / 1000);
        requestParam.set("ticket_alg ", "md5");
        //
        requestParam.set("ticket ",
            SecureUtil.md5(requestParam.toString().concat(PlatformConstants.DATA_REPORT_SECRET)));

        // POST
        String respBody = HttpRequest.post(reportDataScopeUrl)
                                     .header(PlatformConstants.HEADER_ACCESS_TOKEN, token)
                                     .header(PlatformConstants.HEADER_REQUEST_TYPE, PlatformConstants.APP)
                                     .header(PlatformConstants.HEADER_APP_ID, PlatformConstants.APP_ID)
                                     .header(PlatformConstants.HEADER_SERVER_KEY, PlatformConstants.SECRET_KEY)
                                     .body(requestParam.toString()).timeout(5000)
                                     .execute().body();
        log.info("初始化上报成功：{}", respBody);

        verifyDataScope(tableList, token);
    }
    /**
     * from configuration in need to Controller , .
     */
    @Value("${auth.report.excludeClasses}")
    private String excludeClasses;
    private static void collectJavaFiles(File dir, List<File> result) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                collectJavaFiles(file, result); // sub
            } else if (file.getName().endsWith(".java")) {
                result.add(file);
            }
        }
    }
    public JSONObject buildAuthReportFromSource() throws Exception {
        JSONObject auth = new JSONObject();

// 1.
        String projectRoot = System.getProperty("user.dir");
        File srcDir = new File(projectRoot, "ruoyi-admin/src/main/java/com/ruoyi/web/controller/workflow");
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            // log
            log.error("源码目录不存在，实际路径: {}", srcDir.getAbsolutePath());
            throw new IllegalStateException("源码目录不存在：" + srcDir.getAbsolutePath());
        }

// 2. Initialize Parse
        JavaProjectBuilder builder = new JavaProjectBuilder();
        builder.addSourceTree(srcDir);

// 3. Build pact node
        JSONObject pact = new JSONObject();
        pact.put("name", "统一工单服务");

        JSONObject sub = new JSONObject();

// Update : Get all sub Java
        List<File> javaFiles = new ArrayList<>();
        collectJavaFiles(srcDir, javaFiles); // Add method

// 4. Process each Controller
        for (File file : javaFiles) {
            String className = file.getName().replace(".java", "");

            // Update : Generate ------------------------------------
            String relativePackage = srcDir.toPath()
                                           .relativize(file.getParentFile().toPath())
                                           .toString()
                                           .replace(File.separatorChar, '.');

            String fqn = "com.ruoyi.web.controller.workflow." +
                (relativePackage.isEmpty() ? "" : relativePackage + ".") +
                className;

            JavaClass jc = builder.getClassByName(fqn);
            Class<?> clazz;
            try {
                clazz = Class.forName(fqn);
            } catch (ClassNotFoundException e) {
                log.warn("类加载失败，跳过: {}", fqn);
                continue;
            }

            // 4.1 non-RestController
            if (!clazz.isAnnotationPresent(RestController.class)) continue;
            if (excludeClasses.contains(clazz.getSimpleName())) continue;

            // 4.2 Get (act key)
            String classPath = "";
            RequestMapping rm = clazz.getAnnotation(RequestMapping.class);
            if (rm != null && rm.value().length > 0) {
                classPath = rm.value()[0].replace("/", "");
            }

            // 4.3 Get (act name)
            String classComment = jc.getComment() != null ?
                                  jc.getComment().trim() : className;

            // 4.4 Build do_list
            JSONObject doList = new JSONObject();
            for (Method method : clazz.getDeclaredMethods()) {
                if (!hasMappingAnnotation(method)) continue;

                String methodComment = getMethodComment(jc, method);
                SaCheckPermission perm = method.getAnnotation(SaCheckPermission.class);
                if (perm == null) continue;

                String permissionKey = perm.value().length > 0 ?
                                       perm.value()[0] : method.getName();
                doList.put(
                    StringUtils.substringAfterLast(permissionKey, ":"),
                    new JSONObject().put("name", methodComment)
                );
            }

            // 4.5 Build act node
            JSONObject act = new JSONObject();
            act.put("name", classComment);
            act.put("do_list", doList);
            sub.put(classPath, act);  // classPath to act key
        }

        // 5.
        pact.put("sub", sub);
        auth.put("workflowforms", pact);  // pact key

        JSONObject result = new JSONObject();
        result.put("auth", auth);

        return result;
    }

    // Check method whether
    private boolean hasMappingAnnotation(Method method) {
        return method.isAnnotationPresent(SaCheckPermission.class)
            ;
    }

    // Get method ( QDox, method )
    private String getMethodComment(JavaClass jc, Method method) {
        return jc.getMethods().stream()
                 .filter(m -> m.getName().equals(method.getName()) &&
                     m.getParameters().size() == method.getParameterCount())
                 .findFirst()
                 .map(m -> m.getComment() != null ? m.getComment().trim() : method.getName())
                 .orElse(method.getName());
    }

    /**
     * Validate data
     *
     * @param tableList
     */
    private void verifyDataScope(List<Table<?>> tableList, String token) {
        // Validate
        JSONObject verifyRequestParam = new JSONObject();
        verifyRequestParam.set(PlatformConstants.HEADER_ACCESS_TOKEN, token);
        verifyRequestParam.set("service", "apaas-workflowforms");
        verifyRequestParam.set("pauth", "workflowforms");
        verifyRequestParam.set("auth", "model");
        verifyRequestParam.set("do", "downloadXml");
        verifyRequestParam.set("table", tableList.stream().map(Table::getName).collect(Collectors.toList()));
        Console.log(verifyRequestParam.toString());
        // POST
        String verifyRespBody = HttpRequest.post(verifyDataScopeUrl)
                                           .header(PlatformConstants.HEADER_ACCESS_TOKEN, token)
                                           .header(PlatformConstants.HEADER_REQUEST_TYPE, PlatformConstants.APP)
                                           .header(PlatformConstants.HEADER_APP_ID, PlatformConstants.APP_ID)
                                           .header(PlatformConstants.HEADER_SERVER_KEY, PlatformConstants.SECRET_KEY)
                                           .body(verifyRequestParam.toString()).timeout(5000)
                                           .execute().body();
        Console.log(verifyRespBody);
        JSONObject verifyJson = JSONUtil.parseObj(verifyRespBody);
        if (HttpStatus.SUCCESS == verifyJson.getInt(CODE)) {
            if (Objects.nonNull(verifyJson.getJSONObject(DATA)) && Objects.nonNull(verifyJson.getJSONObject(DATA).getJSONArray(DATA))) {
                JSONArray tableData = verifyJson.getJSONObject(DATA).getJSONArray(DATA);
                for (Object object : tableData) {
                    JSONObject table = (JSONObject) object;
                    RedisUtils.setCacheObject(CacheNames.DATA_SCOPE_AUTH_CODE_KEY.concat(table.getStr("table")), table);
                }
            }
        }
    }

}
