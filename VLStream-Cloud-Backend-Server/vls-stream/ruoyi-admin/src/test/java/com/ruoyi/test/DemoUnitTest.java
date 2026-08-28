/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
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
 *
 *
 * @author Lion Li
 */
@SpringBootTest // only can in springboot main main method and yml configuration
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
        LocalDateTime now = LocalDateTime.now(); // Get current and
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // Set Format
        String formattedDate = now.format(formatter); // Format current
        System.out.println(formattedDate);
    }

    @Test
    public void getExecutionI1d() {
        BpmnModel bpmnModel = repositoryService.getBpmnModel("Process_1724752895621:2:4c12d8d4-6a86-11ef-9011" +
            "-6cf6da435cd3");
        Task currentTask = taskService.createTaskQuery().processInstanceId("Process_1724752895621:2:4c12d8d4-6a86" +
            "-11ef-9011-6cf6da435cd3").active().singleResult();
        // Get workflow definition main workflow object
        Process process = bpmnModel.getMainProcess();
        // Get Customproperty(http://flowable.org/bpmn to null / empty userGet Customproperty)
        // String notifyAllSteps = process.getAttributeValue("http://flowable.org/bpmn", "notifyAllSteps");
        //runtimeService.setVariable(, "notifyAllSteps", notifyAllSteps);
        //  boolean notifyAllSteps = runtimeService.getVariable(task.getExecutionId(), "notifyAllSteps") != null &&
        //  (boolean) runtimeService.getVariable(task.getExecutionId(), "notifyAllSteps");
    }

    @Test
    public void test2() {
        BpmnModel bpmnModel = repositoryService.getBpmnModel("Process_1724983557619:6:c7044bc6-667c-11ef-b764" +
            "-6cf6da435cd3");
        // Get workflow definition main workflow object
        Process process = bpmnModel.getMainProcess();
        if (process == null) {
            System.err.println("Main process is null in BpmnModel!");
            return;
        }

        // Get Customproperty
        String notifyAllSteps = process.getAttributeValue("http://flowable.org/bpmn", "notifyAllSteps");
        System.out.println("notifyAllSteps 属性值: " + notifyAllSteps);  // property valuewhether correct

        // Get current tasknodeinfo
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
        String responseBody = response.body(); // Get

        JSONObject resultJson = JSONUtil.parseObj(responseBody);
        System.out.println("resultJson = " + resultJson.toString());
    }

    @Test
    public void test4() {
        //
        AviatorEvaluatorInstance engine = AviatorEvaluator.newInstance(EvalMode.INTERPRETER);
        // Execute
        engine.setOption(Options.TRACE_EVAL, true);
//        Expression exp = engine.compile("score < 80 ? 'true' : 'false'");
        // UUID ('cdbe6b06-f353-4dab-8f6a-e6c1bf253c95'=='cdbe6b06-f353-4dab-8f6a-e6c1bf253c95' && 99>=12)
        // || (77==77 && 12<=99 && == )
        Expression exp = engine.compile("('Process_1733280629879:5:570e4971-b924-11ef-a5a9-52a3bb44796a'=='d03295b4" +
            "-b732-47a9-abf6-821fa42ae297' && 'input57439'==12) || ('cdbe6b06-f353-4dab-8f6a-e6c1bf253c95'==77 && " +
            "'2024-12-17'==99 && '事假'=='d03295b4-b732-47a9-abf6-821fa42ae297')");
        System.out.println(exp.execute(exp.newEnv()));
    }

    // contains, contains method is , if need to Check whether , Convert to ( toLowerCase() toUpperCase())
    @Test
    public void test6() throws Exception {
        //
        AviatorEvaluatorInstance engine = AviatorEvaluator.newInstance(EvalMode.INTERPRETER);

//
        String expression = "string.contains(s1, s2)";

//
        Expression exp = engine.compile(expression);

// Set parameter
        Map<String, Object> env = new HashMap<>();
        env.put("s1", "Hello, Aviator!");
        env.put("s2", "6");

// Execute
        Object result = exp.execute(env);

//
        System.out.println(result);  // true false
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
        //
        AviatorEvaluatorInstance engine = AviatorEvaluator.newInstance(EvalMode.INTERPRETER);
        // Execute
        engine.setOption(Options.TRACE_EVAL, true);
        Expression exp = engine.compile("('测试'==2) || ('a4d6fb90-1270-4f3b-aaae-188759fcd87d'=='a4d6fb90-1270-4f3b" +
            "-aaae-188759fcd87d')");
        boolean execute = (Boolean) exp.execute(exp.newEnv());
        System.out.println(execute);
    }


    // JSONConvert to method
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


    // Convert to JSON method
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
        rootNode.put("rule_flag", "&&"); // layer , need to

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


    // Parse Replace workflow variable value method
    public static String resolveExpression(String expression, HistoricProcessInstance historicProcIns) {
        if (expression == null || expression.isEmpty() || historicProcIns == null) {
            return expression;
        }

        // , UUID
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

    // after method , UUID
    private static String[] splitExpressionSafely(String expression) {
        return expression.split("(?<![0-9a-fA-F\\-])(?<=\\W)(?=\\w)|(?<=\\w)(?=\\W)(?![0-9a-fA-F\\-])");
    }

    // Check whether is variable ( : processDefId, initiator, notifyAllSteps)
    private static boolean isVariableToken(String token) {
        return token.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")
            || isUUID(token);
    }

    // Process workflow variable value
    private static String processBooleanParameter(String param, HistoricProcessInstance historicProcIns) {
        Map<String, Object> processVariables = historicProcIns.getProcessVariables();
        Object o = processVariables.get(param);

        String valueStr = o != null ? o.toString() : param;

        if ("1".equals(valueStr)) {
            return "true";
        } else if ("0".equals(valueStr)) {
            return "false";
        } else if (isNumeric(valueStr)) {
            return valueStr; //
        } else {
            if (isUUID(valueStr)) {
                return "'" + valueStr + "'";
            }
            return "'" + valueStr.replaceAll("'", "\\'") + "'"; // 字符串加引号
        }
    }

    // Check whether to UUID
    private static boolean isUUID(String value) {
        return value.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    // Check whether to
    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }


    @Test
    public void initSchedule() {
        // Set
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY,18 );
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // workflow
        scheduleDailyProcess("Process_1740969345770:5:e279750e-087d-11f0-97e4-8c688be18ef6",
            calendar.getTime());
    }

        /**
     * task
     * @param processDefinitionKey workflow definitionKEY
     * @param firstTriggerTime
     */
    public void scheduleDailyProcess(String processDefinitionKey, Date firstTriggerTime) {
        ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) processEngineConfiguration;

        managementService.executeCommand(commandContext -> {
            // Get taskservice
            TimerJobService timerJobService = config.getJobServiceConfiguration().getTimerJobService();

            // task
            TimerJobEntity timerJob = timerJobService.createTimerJob();
            timerJob.setJobType(JobEntity.JOB_TYPE_TIMER);
            timerJob.setDuedate(firstTriggerTime);
            timerJob.setJobHandlerType(CustomTimerHandler.TYPE);
            timerJob.setJobHandlerConfiguration(processDefinitionKey);
            // task
            timerJobService.scheduleTimerJob(timerJob);
            return null;
        });
    }
    @Test
    public void executePendingJobs() {


        // Execute all Execute Job
//        managementService.moveTimerToExecutableJob("5e05441f-0a20-11f0-887b-8c688be18ef6");
//        managementService.executeJob("5e05441f-0a20-11f0-887b-8c688be18ef6");
    }

    @Test
    public void runTimeoutReminderJobDemo() throws Exception {
        // Custom
        Command customTimerJobCommand = (Command<Void>) commandContext -> {
            // Get JobServiceConfiguration
            JobServiceConfiguration jobServiceConfiguration =
                processEngineConfiguration.getAsyncExecutor().getJobServiceConfiguration();
            // Get TimerJob
            TimerJobEntityManager timerJobEntityManager = jobServiceConfiguration
                .getTimerJobEntityManager();
            // TimerJobobject
            TimerJobEntity timer = timerJobEntityManager.create();
            // Set TimerJob
            timer.setJobType(JobEntity.JOB_TYPE_TIMER);
            // Set Process
            timer.setJobHandlerType(CustomTimerHandler.TYPE);
            // Set Process parameter
            Map<String, Object> userInfo = new HashMap<>();
            timer.setJobHandlerConfiguration(com.alibaba.fastjson.JSONObject.toJSONString(userInfo));
            // Set taskExecute
            timer.setRepeat("R/PT2M");
            timer.setExclusive(true);
            //
            Date now = new Date();
            // delay to current , variable
            Date target = new Date(now.getTime() + 10 * 10);
            // Set current task
            timer.setDuedate(target);
            // task
            JobManager jobManager = jobServiceConfiguration.getJobManager();
            jobManager.scheduleTimerJob(timer);
            return null;
        };
        // Execute Custom
        managementService.executeCommand(customTimerJobCommand);
        // main
        Thread.sleep(1000 * 60 * 10);
    }
    /**
     * before Job parameterConvert to CRON .
     *
     * @param job before taskparameterobject
     * @return CRON
     */
    public String convertToCron(Job job) {
        String cron = "";
        int type = job.getTypes();

        switch (type) {
            case 1: // 每天
                // assuming run array in to "HH:mm:ss"
                String[] hmsDaily = job.getRun().get(0).split(":");
                cron = String.format("%s %s %s * * ?", hmsDaily[2], hmsDaily[1], hmsDaily[0]);
                break;
            case 2: // 隔天
                // start " " to , interval ( ) CRON
                String[] hmsInterval = job.getRun().get(0).split(":");
                // Parse job.start Get start ( Parse , )
                Calendar calendar = Calendar.getInstance();
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    calendar.setTime(sdf.parse(job.getStart()));
                } catch (ParseException e) {
                    throw new RuntimeException("日期解析失败：" + job.getStart(), e);
                }
                int startDay = calendar.get(Calendar.DAY_OF_MONTH);
                // "startDay/interval" from start interval
                // : method only in current , need to Process
                cron = String.format("%s %s %s %d/%d * ?", hmsInterval[2], hmsInterval[1], hmsInterval[0],
                    startDay, job.getInterval());
                break;
            case 3: // 每周
                // assuming job.run is ( "1" ), Quartz in to 1=SUN,2=MON,...,7=SAT
                // before ( : "1" ( )Convert to Quartz in "2")
                int inputWeekDay = Integer.parseInt(job.getRun().get(0));
                int quartzWeekDay = inputWeekDay % 7 + 1; // : 1 -> 2, 2 -> 3, …, 7 -> 1
                // trg_time to int , to HHmmss, 170633 17:06:33,
                // need to Format to HH:mm:ss
                String triggerTimeWeek = String.format("%06d", job.getTrgTime());
                String h_week = triggerTimeWeek.substring(0, 2);
                String m_week = triggerTimeWeek.substring(2, 4);
                String s_week = triggerTimeWeek.substring(4, 6);
                cron = String.format("%s %s %s ? * %d", s_week, m_week, h_week, quartzWeekDay);
                // if interval 0, Execute ,
                // CRON method , Process
                break;
            case 4: // 每月
                // trg_time to int , to HHmmss, 170633 17:06:33,
                // need to Format to HH:mm:ss
                String triggerTimeMonth = String.format("%06d", job.getTrgTime());
                String h_month = triggerTimeMonth.substring(0, 2);
                String m_month = triggerTimeMonth.substring(2, 4);
                String s_month = triggerTimeMonth.substring(4, 6);
                // , job.run in value
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
        // Case 1: task
        // : 17:06:33
        Job jobDaily = new Job();
        jobDaily.setStart("2025-03-11 10:23:00");
        jobDaily.setEnd("2025-11-29 18:46:18");
        jobDaily.setInterval(0);
        //
        jobDaily.setRun(java.util.Arrays.asList("17:06:33"));
        jobDaily.setTypes(1);
        // need to trg_time,
        String cronDaily = convertToCron(jobDaily);
        System.out.println("【每天】 CRON 表达式：" + cronDaily);
        // : 33 06 17 * * ?

        // ---------------------------------------
        // Case 2: task
        // : from start to , 3 Execute , "HH:mm:ss"
        Job jobInterval = new Job();
        jobInterval.setStart("2025-03-11 10:23:00");   // assuming 11
        jobInterval.setEnd("2025-11-29 18:46:18");
        jobInterval.setInterval(3); // 3 Execute
        // to "08:30:00"
        jobInterval.setRun(java.util.Arrays.asList("08:30:00"));
        jobInterval.setTypes(2);
        String cronInterval = convertToCron(jobInterval);
        System.out.println("【隔天】 CRON 表达式：" + cronInterval);
        // : 00 30 08 11/3 * ?
        // from 11 start, 3 ( in current )

        // ---------------------------------------
        // Case 3: task
        // : , in trg_time
        Job jobWeekly = new Job();
        jobWeekly.setStart("2025-03-11 10:23:00");
        jobWeekly.setEnd("2025-11-29 18:46:18");
        // only need to , "1"
        jobWeekly.setRun(java.util.Arrays.asList("1"));
        jobWeekly.setTypes(3);
        // trg_time to 170633 17:06:33
        jobWeekly.setTrgTime(170633);
        String cronWeekly = convertToCron(jobWeekly);
        System.out.println("【每周】 CRON 表达式：" + cronWeekly);
        // : 33 06 17 ? * 2 (Quartz in 1 2)

        // ---------------------------------------
        // Case 4: task
        // : in 10、15、2、5 , in trg_time
        Job jobMonthly = new Job();
        jobMonthly.setStart("2025-03-11 10:23:00");
        jobMonthly.setEnd("2025-11-29 18:46:18");
        // run array
        jobMonthly.setRun(java.util.Arrays.asList("10", "15", "2", "5"));
        jobMonthly.setTypes(4);
        // trg_time to 170633 17:06:33
        jobMonthly.setTrgTime(170633);
        String cronMonthly = convertToCron(jobMonthly);
        System.out.println("【每月】 CRON 表达式：" + cronMonthly);
        // : 33 06 17 10,15,2,5 * ?
    }
}
