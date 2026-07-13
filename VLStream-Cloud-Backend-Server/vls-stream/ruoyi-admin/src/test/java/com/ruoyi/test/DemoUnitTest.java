/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.test;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.aviator.*;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.service.UserService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.workflow.domain.Job;
import com.ruoyi.workflow.handler.CustomTimerHandler;
import com.ruoyi.workflow.service.IWfTaskService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.repository.Model;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.asyncexecutor.JobManager;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 单元测试案例
 *
 * @author Lion Li
 */
@SpringBootTest // 此注解只能在 springboot 主包下使用 需包含 main 方法与 yml 配置文件
@DisplayName("单元测试案例")
public class DemoUnitTest extends FlowServiceFactory {

    @Autowired
    private RuoYiConfig ruoYiConfig;

    @DisplayName("测试 @SpringBootTest @Test @DisplayName 注解")
    @Test
    public void testTest() {
        System.out.println(ruoYiConfig);
    }

    @Disabled
    @DisplayName("测试 @Disabled 注解")
    @Test
    public void testDisabled() {
        System.out.println(ruoYiConfig);
    }

    @Timeout(value = 2L, unit = TimeUnit.SECONDS)
    @DisplayName("测试 @Timeout 注解")
    @Test
    public void testTimeout() throws InterruptedException {
        Thread.sleep(3000);
        System.out.println(ruoYiConfig);
    }


    @DisplayName("测试 @RepeatedTest 注解")
    @RepeatedTest(3)
    public void testRepeatedTest() {
        System.out.println(666);
    }

    @BeforeAll
    public static void testBeforeAll() {
        System.out.println("@BeforeAll ==================");
    }

    @BeforeEach
    public void testBeforeEach() {
        System.out.println("@BeforeEach ==================");
    }

    @AfterEach
    public void testAfterEach() {
        System.out.println("@AfterEach ==================");
    }

    @AfterAll
    public static void testAfterAll() {
        System.out.println("@AfterAll ==================");
    }


    @Test
    public void dhawo2() {
        byte[] bpmnBytes = repositoryService.getModelEditorSource("50935cb8-3d2e-11f0-bde7-8c688be18ef6");
        if (ArrayUtil.isEmpty(bpmnBytes)) {
            throw new RuntimeException("请先设计流程图！");
        }
    }

    @Resource
    private UserService sysUserService;

    @Value("${notification.slUrl2}")
    private String slUrl2;
    @Value("${notification.serviceID}")
    private String serviceID;
    @Value("${notification.secretKey}")
    private String secretKey;
    @Value("${notification.requestType}")
    private String requestType;
    @Resource
    IWfTaskService iWfTaskService;

    @Test
    public void dhawo() {
        // String idCardById = sysUserService.selectIdCardById("90180b98-a221-4574-ad12-9ebdee201113");
        // System.out.println(idCardById);
        System.out.println(requestType);
        System.out.println(serviceID);
        System.out.println(secretKey);
        //    iWfTaskService.sendMessage(true, "612324199105023174");
        List<String> list = new ArrayList<>();
        list.add("612324199105023174");
        //iWfTaskService.sendMessage(true, "90180b98-a221-4574-ad12-9ebdee201113");
    }

    @Resource
    private SysUserMapper baseMapper;

    @Test
    public void getDeptInfo() {
        SysUser sysUser = baseMapper.selectUserById("9c82f843-576e-4537-aeac-c730e598b585");
        System.out.println();
        System.out.println(sysUser.getDeptName());
        System.out.println(sysUser.getDeptInfo());
        System.out.println(sysUser.getDeptName());
        System.out.println(sysUser.getDeptInfo());
    }

    @Test
    public void Test2() {
        LocalDateTime now = LocalDateTime.now(); // 获取当前日期和时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // 设置格式化模式
        String formattedDate = now.format(formatter); // 格式化当前时间
        System.out.println(formattedDate);
    }

    @Test
    public void getExecutionI1d() {
        BpmnModel bpmnModel = repositoryService.getBpmnModel("Process_1724752895621:2:4c12d8d4-6a86-11ef-9011" +
            "-6cf6da435cd3");
        Task currentTask = taskService.createTaskQuery().processInstanceId("Process_1724752895621:2:4c12d8d4-6a86" +
            "-11ef-9011-6cf6da435cd3").active().singleResult();
        // 获取流程定义的主流程对象
        Process process = bpmnModel.getMainProcess();
        // 获取自定义属性(http://flowable.org/bpmn为命名空间专门用户获取自定义属性)
        // String notifyAllSteps = process.getAttributeValue("http://flowable.org/bpmn", "notifyAllSteps");
        //runtimeService.setVariable(, "notifyAllSteps", notifyAllSteps);
        //  boolean notifyAllSteps = runtimeService.getVariable(task.getExecutionId(), "notifyAllSteps") != null &&
        //  (boolean) runtimeService.getVariable(task.getExecutionId(), "notifyAllSteps");
    }

    @Test
    public void test2() {
        BpmnModel bpmnModel = repositoryService.getBpmnModel("Process_1724983557619:6:c7044bc6-667c-11ef-b764" +
            "-6cf6da435cd3");
        // 获取流程定义的主流程对象
        Process process = bpmnModel.getMainProcess();
        if (process == null) {
            System.err.println("Main process is null in BpmnModel!");
            return;
        }

        // 获取自定义属性
        String notifyAllSteps = process.getAttributeValue("http://flowable.org/bpmn", "notifyAllSteps");
        System.out.println("notifyAllSteps 属性值: " + notifyAllSteps);  // 确认属性值是否正确

        // 获取当前任务节点信息
        String currentTaskId = process.getId();
        System.out.println("当前任务ID: " + currentTaskId);
    }

    @Test
    public void test3() {
        System.out.println("token校验地址为：" + "http://192.168.60.75:32620/bus/apaas-sso/sso/v1/verifyToken");
        String requestBody = "{\"accessToken\": \"" + "4fc9b66609ff4e2bbc24e52b472df80f" + "\"}";
        HttpRequest request = HttpRequest.post("http://192.168.60.75:32620/bus/apaas-sso/sso/v1/verifyToken")
//            HttpRequest request = HttpRequest.post("http://192.168.60.75:32610/oort/oortcloud-cloud-classroom/user/v1/verifyToken")
                                         .header("Accept", "application/json")
                                         .header("AccessToken", "4fc9b66609ff4e2bbc24e52b472df80f").body(requestBody);

        HttpResponse response = request.execute();
        String responseBody = response.body(); // 获取响应体字符串

        JSONObject resultJson = JSONUtil.parseObj(responseBody);
        System.out.println("resultJson = " + resultJson.toString());
    }

    @Test
    public void test4() {
        // 创建解释器
        AviatorEvaluatorInstance engine = AviatorEvaluator.newInstance(EvalMode.INTERPRETER);
        // 打开跟踪执行
        engine.setOption(Options.TRACE_EVAL, true);
//        Expression exp = engine.compile("score < 80 ? 'true' : 'false'");
        // 将 UUID 字符串用引号括起来('cdbe6b06-f353-4dab-8f6a-e6c1bf253c95'=='cdbe6b06-f353-4dab-8f6a-e6c1bf253c95' && 99>=12)
        // || (77==77 && 12<=99 && 事假==事假)
        Expression exp = engine.compile("('Process_1733280629879:5:570e4971-b924-11ef-a5a9-52a3bb44796a'=='d03295b4" +
            "-b732-47a9-abf6-821fa42ae297' && 'input57439'==12) || ('cdbe6b06-f353-4dab-8f6a-e6c1bf253c95'==77 && " +
            "'2024-12-17'==99 && '事假'=='d03295b4-b732-47a9-abf6-821fa42ae297')");
        System.out.println(exp.execute(exp.newEnv()));
    }

    //使用函数contains，contains 方法是区分大小写的，如果你需要不区分大小写地判断是否包含，可以考虑先将字符串转换为统一的大小写格式（比如 toLowerCase() 或 toUpperCase()）
    @Test
    public void test6() throws Exception {
        // 创建解释器
        AviatorEvaluatorInstance engine = AviatorEvaluator.newInstance(EvalMode.INTERPRETER);

// 定义表达式
        String expression = "string.contains(s1, s2)";

// 编译表达式
        Expression exp = engine.compile(expression);

// 设置参数
        Map<String, Object> env = new HashMap<>();
        env.put("s1", "Hello, Aviator!");
        env.put("s2", "6");

// 执行表达式
        Object result = exp.execute(env);

// 打印结果
        System.out.println(result);  // 输出 true 或 false
    }

    @Test
    public void test7() throws Exception {
        Model model2 =
            repositoryService.createModelQuery().modelKey("Process_1736938270455").latestVersion().singleResult();
        byte[] bpmnBytes = repositoryService.getModelEditorSource(model2.getId());
        String bpmnXml = StringUtils.toEncodedString(bpmnBytes, StandardCharsets.UTF_8);
        System.out.println(bpmnXml);
    }

    @Test
    public void test8() throws Exception {
        OkHttpClient client = new OkHttpClient().newBuilder()
                                                .build();
        Request request = new Request.Builder()
            .url("http://127.0.0.1:32620/bus/apaas-user/tenant/v1/getTenantAdmin?tenant_id=e6a76c7b-6ade-4b75-b917" +
                "-59f76e25472b")
            .get()
            .addHeader("AccessToken", "7a989b6f3a0141bfa141252d8886235a")
            .addHeader("appID", "7c170a697c0447f8a45bbac4d96dfef6")
            .addHeader("requestType", "app")
            .addHeader("secretKey", "9cc161f366e24ab98e1362f0e21a721d")
            .addHeader("tenantid", "0e391fd7-1033-4f09-88c0-187582fee462")
            .addHeader("Cookie", "oort-task-token-key=7a989b6f3a0141bfa141252d8886235a")
            .addHeader("Content-Type", "application/json")
            .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.body().string());
    }


    @Test
    public void test5() throws Exception {
//        String jsonString = "{\n" +
//            "    \"rule_flag\": \"||\",\n" +
//            "    \"rule\": {\n" +
//            "        \"r1\": {\n" +
//            "            \"rule_flag\": \"&&\",\n" +
//            "            \"a\": \"creator==a4d6fb90-1270-4f3b-aaae-188759fcd87d\",\n" +
//            "            \"b\": \"input57439==12\"\n" +
//            "        },\n" +
//            "        \"r2\": {\n" +
//            "            \"rule_flag\": \"&&\",\n" +
//            "            \"a\": \"input57439==77\",\n" +
//            "            \"b\": \"input57439==99\",\n" +
//            "            \"c\": \"creator==a4d6fb90-1270-4f3b-aaae-188759fcd87d\"\n" +
//            "        }\n" +
//            "    }\n" +
//            "}";
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        HistoricProcessInstance historicProcIns = historyService.createHistoricProcessInstanceQuery()
//            .processInstanceId("03265bed-bc47-11ef-a5a9-52a3bb44796a")
//            .includeProcessVariables()
//            .singleResult();
//
//        try {
//            JsonNode jsonNode = objectMapper.readTree(jsonString);
//            System.out.println(jsonToExpression(jsonNode));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        HistoricProcessInstance historicProcIns = historyService.createHistoricProcessInstanceQuery()
                                                                .processInstanceId("0247b58b-f328-11ef-b3b9" +
                                                                    "-6cf6da435cd3")
                                                                .includeProcessVariables()
                                                                .singleResult();
        System.out.println("=============================================================================");
        System.out.println(resolveExpression("(initiator==cb4f4374-de55-4890-bc25-1f29886a625f) or (input13811==2)",
            historicProcIns));
    }

    @Test
    public void test9() throws Exception {
        HistoricProcessInstance historicProcIns = historyService.createHistoricProcessInstanceQuery()
                                                                .processInstanceId("0247b58b-f328-11ef-b3b9" +
                                                                    "-6cf6da435cd3")
                                                                .includeProcessVariables()
                                                                .singleResult();
        // 创建解释器
        AviatorEvaluatorInstance engine = AviatorEvaluator.newInstance(EvalMode.INTERPRETER);
        // 打开跟踪执行
        engine.setOption(Options.TRACE_EVAL, true);
        Expression exp = engine.compile("('测试'==2) || ('a4d6fb90-1270-4f3b-aaae-188759fcd87d'=='a4d6fb90-1270-4f3b" +
            "-aaae-188759fcd87d')");
        boolean execute = (Boolean) exp.execute(exp.newEnv());
        System.out.println(execute);
    }


    // 将JSON转换为表达式的方法
    public static String jsonToExpression(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return "";
        }

        StringBuilder expression = new StringBuilder();
        JsonNode ruleNode = jsonNode.get("rule");
        if (ruleNode != null) {
            Iterator<String> ruleKeys = ruleNode.fieldNames();
            while (ruleKeys.hasNext()) {
                String key = ruleKeys.next();
                JsonNode subRule = ruleNode.get(key);

                if (key.startsWith("r") && subRule.isObject()) {
                    if (expression.length() > 0) {
                        expression.append(" ").append(jsonNode.get("rule_flag").asText()).append(" ");
                    }
                    expression.append(buildSubExpression(subRule));
                }
            }
        }

        return expression.toString().trim();
    }

    private static String buildSubExpression(JsonNode subRule) {
        StringBuilder subExpression = new StringBuilder("(");
        Iterator<String> fieldNames = subRule.fieldNames();
        String ruleFlag = "";

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (fieldName.equals("rule_flag")) {
                ruleFlag = subRule.get(fieldName).asText();
            } else {
                subExpression.append(subRule.get(fieldName).asText()).append(" ").append(ruleFlag).append(" ");
            }
        }

        // Remove the trailing operator and close the parenthesis
        if (subExpression.length() > 1 && ruleFlag.length() > 0) {
            subExpression.setLength(subExpression.length() - ruleFlag.length() - 2);
        }
        subExpression.append(")");

        return subExpression.toString();
    }


    // 将表达式转换为JSON的方法
    public static JsonNode expressionToJson(String expression) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        ObjectNode ruleNode = mapper.createObjectNode();

        String[] subExpressions = splitExpression(expression);
        for (int i = 0; i < subExpressions.length; i++) {
            String subExpression = subExpressions[i];
            ObjectNode subRuleNode = buildRuleFromExpression(subExpression, mapper);
            ruleNode.set("r" + (i + 1), subRuleNode);

            if (i < subExpressions.length - 1) {
                ruleNode.put("rule_r" + (i + 1) + (i + 2) + "_flag", cleanConnector(extractConnector(expression,
                    subExpressions[i], subExpressions[i + 1])));
            }
        }

        rootNode.set("rule", ruleNode);
        rootNode.put("rule_flag", "&&"); // 顶层规则标志，根据需要调整

        return rootNode;
    }

    private static String[] splitExpression(String expression) {
        return expression.split("\\)\\s*(\\|\\||&&)\\s*\\(");
    }

    private static ObjectNode buildRuleFromExpression(String subExpression, ObjectMapper mapper) {
        ObjectNode subRuleNode = mapper.createObjectNode();
        String[] conditions = subExpression.replace("(", "").replace(")", "").split("\\s*(&&|\\|\\|)\\s*");
        String[] connectors = subExpression.split("[^&|]+");

        for (int i = 0; i < conditions.length; i++) {
            subRuleNode.put(Character.toString((char) ('a' + i)), conditions[i].trim());
            if (i < connectors.length - 1) {
                subRuleNode.put("rule_" + Character.toString((char) ('a' + i)) + Character.toString((char) ('a' + i + 1)) + "_flag", cleanConnector(connectors[i + 1].trim()));
            }
        }

        return subRuleNode;
    }

    private static String extractConnector(String expression, String current, String next) {
        int start = expression.indexOf(current) + current.length();
        int end = expression.indexOf(next);
        return expression.substring(start, end).trim();
    }

    private static String cleanConnector(String connector) {
        return connector.replace(")", "").replace("(", "").trim();
    }


    // 解析表达式并替换流程变量值的方法
    public static String resolveExpression(String expression, HistoricProcessInstance historicProcIns) {
        if (expression == null || expression.isEmpty() || historicProcIns == null) {
            return expression;
        }

        // 改进的分割逻辑，保护 UUID
        String[] tokens = splitExpressionSafely(expression);

        StringBuilder resolvedExpression = new StringBuilder();

        for (String token : tokens) {
            if (isVariableToken(token)) {
                String resolvedValue = processBooleanParameter(token, historicProcIns);
                resolvedExpression.append(resolvedValue);
            } else {
                resolvedExpression.append(token);
            }
        }
        return resolvedExpression.toString();
    }

    // 改进后的分割方法，保护 UUID
    private static String[] splitExpressionSafely(String expression) {
        return expression.split("(?<![0-9a-fA-F\\-])(?<=\\W)(?=\\w)|(?<=\\w)(?=\\W)(?![0-9a-fA-F\\-])");
    }

    // 判断是否是变量名（例如：processDefId, initiator, notifyAllSteps）
    private static boolean isVariableToken(String token) {
        return token.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")
            || isUUID(token);
    }

    // 处理流程变量的值
    private static String processBooleanParameter(String param, HistoricProcessInstance historicProcIns) {
        Map<String, Object> processVariables = historicProcIns.getProcessVariables();
        Object o = processVariables.get(param);

        String valueStr = o != null ? o.toString() : param;

        if ("1".equals(valueStr)) {
            return "true";
        } else if ("0".equals(valueStr)) {
            return "false";
        } else if (isNumeric(valueStr)) {
            return valueStr; // 数字不加引号
        } else {
            if (isUUID(valueStr)) {
                return "'" + valueStr + "'";
            }
            return "'" + valueStr.replaceAll("'", "\\'") + "'"; // 字符串加引号
        }
    }

    // 判断是否为UUID格式
    private static boolean isUUID(String value) {
        return value.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    // 判断是否为数字
    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }


    @Test
    public void initSchedule() {
        // 设置首次触发时间
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY,18 );
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // 调度每日巡检流程
        scheduleDailyProcess("Process_1740969345770:5:e279750e-087d-11f0-97e4-8c688be18ef6",
            calendar.getTime());
    }

        /**
     * 创建初始定时任务
     * @param processDefinitionKey 流程定义KEY
     * @param firstTriggerTime 首次触发时间
     */
    public void scheduleDailyProcess(String processDefinitionKey, Date firstTriggerTime) {
        ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) processEngineConfiguration;

        managementService.executeCommand(commandContext -> {
            // 获取定时任务服务
            TimerJobService timerJobService = config.getJobServiceConfiguration().getTimerJobService();

            // 创建定时任务
            TimerJobEntity timerJob = timerJobService.createTimerJob();
            timerJob.setJobType(JobEntity.JOB_TYPE_TIMER);
            timerJob.setDuedate(firstTriggerTime);
            timerJob.setJobHandlerType(CustomTimerHandler.TYPE);
            timerJob.setJobHandlerConfiguration(processDefinitionKey);
            // 持久化任务
            timerJobService.scheduleTimerJob(timerJob);
            return null;
        });
    }
    @Test
    public void executePendingJobs() {


        // 手动触发执行所有待执行的Job
//        managementService.moveTimerToExecutableJob("5e05441f-0a20-11f0-887b-8c688be18ef6");
//        managementService.executeJob("5e05441f-0a20-11f0-887b-8c688be18ef6");
    }

    @Test
    public void runTimeoutReminderJobDemo() throws Exception {
        //自定义命令
        Command customTimerJobCommand = (Command<Void>) commandContext -> {
            //获取JobServiceConfiguration
            JobServiceConfiguration jobServiceConfiguration =
                processEngineConfiguration.getAsyncExecutor().getJobServiceConfiguration();
            //获取TimerJob实体管理器
            TimerJobEntityManager timerJobEntityManager = jobServiceConfiguration
                .getTimerJobEntityManager();
            //创建TimerJob对象
            TimerJobEntity timer = timerJobEntityManager.create();
            //设置TimerJob类型
            timer.setJobType(JobEntity.JOB_TYPE_TIMER);
            //设置作业处理器
            timer.setJobHandlerType(CustomTimerHandler.TYPE);
            //设置传递给作业处理器的参数
            Map<String, Object> userInfo = new HashMap<>();
            timer.setJobHandlerConfiguration(com.alibaba.fastjson.JSONObject.toJSONString(userInfo));
            //设置定时器任务执行周期
            timer.setRepeat("R/PT2M");
            timer.setExclusive(true);
            //时间计算
            Date now = new Date();
            //delay为相较当前时间，延时的时间变量
            Date target = new Date(now.getTime() + 10 * 10);
            //设置当前定时器任务的触发时间
            timer.setDuedate(target);
            //保存并触发定时器任务
            JobManager jobManager = jobServiceConfiguration.getJobManager();
            jobManager.scheduleTimerJob(timer);
            return null;
        };
        //执行自定义命令
        managementService.executeCommand(customTimerJobCommand);
        //主线程暂停
        Thread.sleep(1000 * 60 * 10);
    }
    /**
     * 将前端传递的 Job 参数转换为 CRON 表达式。
     *
     * @param job 前端传入的定时任务参数对象
     * @return 对应的 CRON 表达式字符串
     */
    public String convertToCron(Job job) {
        String cron = "";
        int type = job.getTypes();

        switch (type) {
            case 1: // 每天
                // 假设 run 数组中存放的格式为 "HH:mm:ss"
                String[] hmsDaily = job.getRun().get(0).split(":");
                cron = String.format("%s %s %s * * ?", hmsDaily[2], hmsDaily[1], hmsDaily[0]);
                break;
            case 2: // 隔天
                // 隔天使用 start 日期的“日”作为起始日，并结合 interval（间隔天数）构造CRON
                String[] hmsInterval = job.getRun().get(0).split(":");
                // 解析 job.start 获取开始日（仅解析到日，不考虑月之间的衔接问题）
                Calendar calendar = Calendar.getInstance();
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    calendar.setTime(sdf.parse(job.getStart()));
                } catch (ParseException e) {
                    throw new RuntimeException("日期解析失败：" + job.getStart(), e);
                }
                int startDay = calendar.get(Calendar.DAY_OF_MONTH);
                // 这里用 "startDay/interval" 表示从指定日开始每隔 interval 天
                // 注意：这种写法只在当前月内有效，如需支持跨月需要更复杂的处理逻辑
                cron = String.format("%s %s %s %d/%d * ?", hmsInterval[2], hmsInterval[1], hmsInterval[0],
                    startDay, job.getInterval());
                break;
            case 3: // 每周
                // 假设 job.run 传入的是星期几（如 "1" 表示星期一），而 Quartz 中星期定义为 1=SUN,2=MON,...,7=SAT
                // 因此我们将前端的星期数字做一个映射（如：传入“1”（星期一）转换为 Quartz 中的“2”）
                int inputWeekDay = Integer.parseInt(job.getRun().get(0));
                int quartzWeekDay = inputWeekDay % 7 + 1; // 如：1 -> 2, 2 -> 3, …, 7 -> 1
                // trg_time 为 int 类型，假定格式为 HHmmss，例如 170633 表示 17:06:33，
                // 需要将其格式化为 HH:mm:ss
                String triggerTimeWeek = String.format("%06d", job.getTrgTime());
                String h_week = triggerTimeWeek.substring(0, 2);
                String m_week = triggerTimeWeek.substring(2, 4);
                String s_week = triggerTimeWeek.substring(4, 6);
                cron = String.format("%s %s %s ? * %d", s_week, m_week, h_week, quartzWeekDay);
                // 如果 interval 大于 0，则说明希望每隔几周执行，
                // 原生 CRON 表达式无法直接支持多周间隔，此处暂不处理
                break;
            case 4: // 每月
                // trg_time 为 int 类型，假定格式为 HHmmss，例如 170633 表示 17:06:33，
                // 需要将其格式化为 HH:mm:ss
                String triggerTimeMonth = String.format("%06d", job.getTrgTime());
                String h_month = triggerTimeMonth.substring(0, 2);
                String m_month = triggerTimeMonth.substring(2, 4);
                String s_month = triggerTimeMonth.substring(4, 6);
                // 当传入多个日期时，将 job.run 中的多个值用逗号分隔
                String daysOfMonth = String.join(",", job.getRun());
                cron = String.format("%s %s %s %s * ?", s_month, m_month, h_month, daysOfMonth);
                break;
            default:
                throw new IllegalArgumentException("不支持的定时类型：" + type);
        }
        return cron;
    }
    @Test
    void test20(){

        // ---------------------------------------
        // Case 1: 每天定时任务测试
        // 示例：每天 17:06:33 触发
        Job jobDaily = new Job();
        jobDaily.setStart("2025-03-11 10:23:00");
        jobDaily.setEnd("2025-11-29 18:46:18");
        jobDaily.setInterval(0);
        // 每天的触发时间采用完整时间格式字符串传递
        jobDaily.setRun(java.util.Arrays.asList("17:06:33"));
        jobDaily.setTypes(1);
        // 每天不需要 trg_time，此处忽略
        String cronDaily = convertToCron(jobDaily);
        System.out.println("【每天】 CRON 表达式：" + cronDaily);
        // 输出预期：33 06 17 * * ?

        // ---------------------------------------
        // Case 2: 隔天定时任务测试
        // 示例：从 start 的日期为起点，每隔3天执行一次，触发时间采用 "HH:mm:ss"
        Job jobInterval = new Job();
        jobInterval.setStart("2025-03-11 10:23:00");   // 假设起始于 11 号
        jobInterval.setEnd("2025-11-29 18:46:18");
        jobInterval.setInterval(3); // 每隔 3 天执行一次
        // 触发时间为 "08:30:00"
        jobInterval.setRun(java.util.Arrays.asList("08:30:00"));
        jobInterval.setTypes(2);
        String cronInterval = convertToCron(jobInterval);
        System.out.println("【隔天】 CRON 表达式：" + cronInterval);
        // 输出类似：00 30 08 11/3 * ?
        // 表示从 11 号开始，每隔 3 天触发一次（仅在当前月有效）

        // ---------------------------------------
        // Case 3: 每周定时任务测试
        // 示例：每周星期一，在 trg_time 指定的时间触发
        Job jobWeekly = new Job();
        jobWeekly.setStart("2025-03-11 10:23:00");
        jobWeekly.setEnd("2025-11-29 18:46:18");
        // 每周只需要传入星期几，例如 "1" 表示星期一
        jobWeekly.setRun(java.util.Arrays.asList("1"));
        jobWeekly.setTypes(3);
        // trg_time 为 170633 表示 17:06:33
        jobWeekly.setTrgTime(170633);
        String cronWeekly = convertToCron(jobWeekly);
        System.out.println("【每周】 CRON 表达式：" + cronWeekly);
        // 输出预期：33 06 17 ? * 2  (Quartz中星期1表示2)

        // ---------------------------------------
        // Case 4: 每月定时任务测试
        // 示例：每月在 10、15、2、5 号，在 trg_time 指定时间触发
        Job jobMonthly = new Job();
        jobMonthly.setStart("2025-03-11 10:23:00");
        jobMonthly.setEnd("2025-11-29 18:46:18");
        // 多个日期通过 run 数组传入
        jobMonthly.setRun(java.util.Arrays.asList("10", "15", "2", "5"));
        jobMonthly.setTypes(4);
        // trg_time 为 170633 表示 17:06:33
        jobMonthly.setTrgTime(170633);
        String cronMonthly = convertToCron(jobMonthly);
        System.out.println("【每月】 CRON 表达式：" + cronMonthly);
        // 输出预期：33 06 17 10,15,2,5 * ?
    }
}
