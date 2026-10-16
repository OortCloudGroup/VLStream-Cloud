package com.ruoyi.vlstream.test.vlstream.compute;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

/** Guards public and newly SSO-owned private compute jobs; legacy records retain known local authors. */
@Service
@RequiredArgsConstructor
public class ComputePublicTaskAccess {
    private final JdbcTemplate jdbc;
    private final ComputePublicInventoryService inventory;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputeSharedQueueService sharedQueue;
    /** Explicit fallback for manually constructed consumers; Spring consumers share the injected guard. */
    public static ComputePublicTaskAccess forCompute(JdbcTemplate jdbc,ComputePublicInventoryService inventory,ComputeSharedQueueService sharedQueue) {
        ComputePublicTaskAccess access=new ComputePublicTaskAccess(jdbc,inventory);
        access.sharedQueue=sharedQueue;
        return access;
    }
    public String viewer() { String value=inventory.viewer();return value==null?"":value; }
    String sharedViewer() { return sharedQueue==null?"":sharedQueue.viewer(); }
    static String sharedMode(String alias) {
        return "CAST(COALESCE(CASE WHEN JSON_VALID("+alias+".options_json) THEN JSON_UNQUOTE(JSON_EXTRACT("+alias+".options_json,'$.sharedQueue.mode')) END,'') AS BINARY)=CAST('SHARED_GPU_QUEUE' AS BINARY)";
    }
    private static String subjectCondition() {
        return "(CAST(COALESCE(pn.scheduling_mode,'INSTANCE_EXCLUSIVE') AS BINARY)=CAST('SHARED_GPU_QUEUE' AS BINARY) AND NOT ("+sharedMode("pj")+"))"
            +" OR pj.tenant_id IS NULL OR CAST(pj.tenant_id AS BINARY) <> CAST({2} AS BINARY) OR pj.public_owner_subject IS NULL OR pj.public_owner_subject=''"
            +" OR CAST(pj.public_owner_subject AS BINARY) <> CAST((CASE WHEN "+sharedMode("pj")+" THEN {1} ELSE {0} END) AS BINARY)";
    }
    private static String exclusiveSubjectCondition() {
        return "pj.public_owner_subject IS NULL OR pj.public_owner_subject='' OR CAST(pj.public_owner_subject AS BINARY) <> CAST({0} AS BINARY)";
    }
    private static String protectedJob() {
        return "(pj.public_instance_key IS NOT NULL OR pn.public_instance_key IS NOT NULL OR pn.resource_scope='PLATFORM' OR pj.public_owner_subject IS NOT NULL"
            +" OR CAST(COALESCE(pn.scheduling_mode,'INSTANCE_EXCLUSIVE') AS BINARY)=CAST('SHARED_GPU_QUEUE' AS BINARY) OR ("+sharedMode("pj")+"))";
    }
    static String personalPredicate(String trustedTrainingReference) {
        return predicate(trustedTrainingReference).replace(exclusiveSubjectCondition(),subjectCondition());
    }
    static String jdbcPredicate(String reference,List<Object> parameters,String sso,String shared,String tenant) {
        Object[] values={sso,shared,tenant};
        java.util.regex.Matcher placeholders=java.util.regex.Pattern.compile("\\{([012])\\}").matcher(personalPredicate(reference));
        StringBuffer sql=new StringBuffer();
        while(placeholders.find()) {
            parameters.add(values[Integer.parseInt(placeholders.group(1))]);
            placeholders.appendReplacement(sql,"?");
        }
        placeholders.appendTail(sql);return sql.toString();
    }
    public static String predicate(String trustedTrainingReference) {
        return "NOT EXISTS (SELECT 1 FROM vls_cloud_training_job pj LEFT JOIN vls_compute_node pn ON pn.id=pj.node_id WHERE pj.training_id="
            +trustedTrainingReference+" AND "+protectedJob()
            +" AND ("+exclusiveSubjectCondition()+"))";
    }
    public <T> QueryWrapper<T> filter(QueryWrapper<T> query,String trustedTrainingReference) {
        return query.apply(personalPredicate(trustedTrainingReference),viewer(),sharedViewer(),ComputeNodeService.tenant()).apply(legacyPredicate(trustedTrainingReference),localViewer())
            .apply("(EXISTS (SELECT 1 FROM vls_cloud_training_job bj WHERE bj.training_id="+trustedTrainingReference+")"
                +" OR EXISTS (SELECT 1 FROM vls_algorithm_training bt WHERE bt.id="+trustedTrainingReference
                +" AND CAST(bt.tenant_id AS BINARY)=CAST({0} AS BINARY) AND bt.create_user IS NOT NULL AND bt.create_user<>''"
                +" AND CAST(bt.create_user AS BINARY)=CAST({1} AS BINARY)))",ComputeNodeService.tenant(),localViewer());
    }
    private static String legacyPredicate(String trustedTrainingReference) {
        return "NOT EXISTS (SELECT 1 FROM vls_cloud_training_job lj LEFT JOIN vls_compute_node ln ON ln.id=lj.node_id"
            +" LEFT JOIN vls_algorithm_training lt ON lt.id=lj.training_id AND CAST(lt.tenant_id AS BINARY)=CAST(lj.tenant_id AS BINARY) WHERE lj.training_id="
            +trustedTrainingReference+" AND lj.public_owner_subject IS NULL AND lj.public_instance_key IS NULL AND ln.public_instance_key IS NULL"
            +" AND COALESCE(ln.resource_scope,'PRIVATE') <> 'PLATFORM' AND (lt.create_user IS NULL OR lt.create_user='' OR CAST(lt.create_user AS BINARY) <> CAST({0} AS BINARY)))";
    }
    private String localViewer(){try{String user=LoginHelper.getUserId();return user==null?"":user;}catch(Exception e){return "";}}
    public void training(Long id) {
        if(id==null)return;
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT pj.tenant_id,pj.public_owner_subject,pj.options_json,pn.scheduling_mode AS node_scheduling_mode FROM vls_cloud_training_job pj LEFT JOIN vls_compute_node pn ON pn.id=pj.node_id WHERE pj.training_id=? AND "+protectedJob(),id);
        String owner=viewer(), sharedOwner=sharedViewer();
        if(rows.stream().anyMatch(r->!allowed(r,owner,sharedOwner)))
            throw new ServiceException("无权读取或操作其他用户的训练记录与产物");
        List<Map<String,Object>> legacy=jdbc.queryForList("SELECT lj.tenant_id,lt.create_user FROM vls_cloud_training_job lj LEFT JOIN vls_compute_node ln ON ln.id=lj.node_id LEFT JOIN vls_algorithm_training lt ON lt.id=lj.training_id AND CAST(lt.tenant_id AS BINARY)=CAST(lj.tenant_id AS BINARY) WHERE lj.training_id=? AND lj.public_owner_subject IS NULL AND lj.public_instance_key IS NULL AND ln.public_instance_key IS NULL AND COALESCE(ln.resource_scope,'PRIVATE') <> 'PLATFORM'",id);
        String local=localViewer();
        if(!legacy.isEmpty() && (local.isEmpty() || legacy.stream().anyMatch(r->!Objects.equals(local,databaseCreator(r.get("create_user"))) || !Objects.equals(ComputeNodeService.tenant(),r.get("tenant_id")))))
            throw new ServiceException("历史私人训练归属未确认或不属于当前用户");
        if(rows.isEmpty() && legacy.isEmpty()) {
            List<Map<String,Object>> training=jdbc.queryForList("SELECT tenant_id,create_user FROM vls_algorithm_training WHERE id=?",id);
            requireLegacyCreator(training,"历史训练不存在、归属未确认或不属于当前用户");
        }
    }
    private void requireLegacyCreator(List<Map<String,Object>> records,String message) {
        String local=localViewer();
        if(records.size()!=1 || local.isEmpty()
            || !Objects.equals(ComputeNodeService.tenant(),records.get(0).get("tenant_id"))
            || !Objects.equals(local,databaseCreator(records.get(0).get("create_user"))))
            throw new ServiceException(message, 403);
    }
    /** JDBC BIGINT is integral; normalize its exact decimal value without lossy numeric coercion. */
    static String databaseCreator(Object value) {
        if(value instanceof String) return (String)value;
        if(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
            || value instanceof java.math.BigInteger) return value.toString();
        return null;
    }
    private static boolean allowed(Map<String,Object> row,String owner,String sharedOwner) {
        String selected=owner;
        boolean shared=false;
        try {
            shared=ComputeSharedQueueService.MODE.equals(new com.fasterxml.jackson.databind.ObjectMapper().readTree(String.valueOf(row.get("options_json"))).path("sharedQueue").path("mode").asText());
        } catch(Exception ignored) { }
        if(ComputeSharedQueueService.MODE.equals(row.get("node_scheduling_mode")) && !shared)return false;
        if(shared)selected=sharedOwner;
        return !selected.isEmpty() && Objects.equals(selected,row.get("public_owner_subject")) && Objects.equals(ComputeNodeService.tenant(),row.get("tenant_id"));
    }
    public void model(Long id) {
        if(id==null)return;
        List<Long> rows=jdbc.queryForList("SELECT training_id FROM vls_algorithm_model WHERE id=?",Long.class,id);
        for(Long training:rows)training(training);
    }
    public static String containerPredicate(String trustedId,String trustedTrainingReference) {
        return "NOT EXISTS (SELECT 1 FROM vls_cloud_training_job pj LEFT JOIN vls_compute_node pn ON pn.id=pj.node_id WHERE (pj.container_record_id="
            +trustedId+" OR pj.training_id="+trustedTrainingReference+") AND "+protectedJob()
            +" AND ("+exclusiveSubjectCondition()+"))";
    }
    public <T> QueryWrapper<T> filterContainers(QueryWrapper<T> query) {
        String annotation="CAST(COALESCE(vls_container_instance.instance_type,'') AS BINARY)=CAST('smart_annotation' AS BINARY)";
        String trainingReference="CASE WHEN "+annotation+" THEN NULL ELSE vls_container_instance.training_task_id END";
        return query.apply(containerPredicate("vls_container_instance.id",trainingReference).replace(exclusiveSubjectCondition(),subjectCondition()),viewer(),sharedViewer(),ComputeNodeService.tenant())
            .apply("CAST(vls_container_instance.tenant_id AS BINARY)=CAST({0} AS BINARY)",ComputeNodeService.tenant())
            .apply("(vls_container_instance.training_task_id IS NULL OR vls_container_instance.instance_type IS NULL"
                +" OR CAST(vls_container_instance.instance_type AS BINARY) IN (CAST('training' AS BINARY),CAST('cloud_training' AS BINARY),CAST('smart_annotation' AS BINARY)))")
            .apply("(vls_container_instance.training_task_id IS NOT NULL OR EXISTS (SELECT 1 FROM vls_cloud_training_job bc WHERE bc.container_record_id=vls_container_instance.id)"
                +" OR (vls_container_instance.create_user IS NOT NULL AND vls_container_instance.create_user<>'' AND CAST(vls_container_instance.create_user AS BINARY)=CAST({0} AS BINARY)))",localViewer())
            .apply("(("+annotation+" AND vls_container_instance.create_user IS NOT NULL AND CAST(vls_container_instance.create_user AS BINARY)=CAST({1} AS BINARY)"
                +" AND NOT EXISTS (SELECT 1 FROM vls_cloud_training_job ac WHERE ac.container_record_id=vls_container_instance.id)"
                +" AND EXISTS (SELECT 1 FROM vls_smart_annotation_round ar JOIN vls_smart_annotation_task at ON at.id=ar.task_id"
                +" WHERE ar.id=vls_container_instance.training_task_id AND CAST(ar.tenant_id AS BINARY)=CAST({0} AS BINARY)"
                +" AND CAST(at.tenant_id AS BINARY)=CAST({0} AS BINARY) AND at.create_user IS NOT NULL AND at.create_user<>''"
                +" AND CAST(at.create_user AS BINARY)=CAST({1} AS BINARY) AND COALESCE(ar.is_deleted,0)=0 AND COALESCE(at.is_deleted,0)=0))"
                +" OR (NOT ("+annotation+") AND (vls_container_instance.training_task_id IS NULL OR EXISTS (SELECT 1 FROM vls_cloud_training_job bl WHERE bl.training_id=vls_container_instance.training_task_id)"
                +" OR EXISTS (SELECT 1 FROM vls_algorithm_training bt WHERE bt.id=vls_container_instance.training_task_id"
                +" AND CAST(bt.tenant_id AS BINARY)=CAST({0} AS BINARY) AND bt.create_user IS NOT NULL AND bt.create_user<>'' AND CAST(bt.create_user AS BINARY)=CAST({1} AS BINARY)))))",ComputeNodeService.tenant(),localViewer())
            .apply(legacyPredicate(trainingReference),localViewer())
            .apply("NOT EXISTS (SELECT 1 FROM vls_cloud_training_job lc WHERE lc.container_record_id=vls_container_instance.id AND NOT ("
                +legacyPredicate("lc.training_id")+"))",localViewer());
    }
    public void container(Long id) {
        if(id==null)return;
        List<Map<String,Object>> records=jdbc.queryForList("SELECT tenant_id,create_user,instance_type,training_task_id FROM vls_container_instance WHERE id=?",id);
        if(records.size()!=1 || !Objects.equals(ComputeNodeService.tenant(),records.get(0).get("tenant_id")))
            throw new ServiceException("容器记录不存在或不属于当前租户");
        Object type=records.get(0).get("instance_type");
        if(records.get(0).get("training_task_id")!=null && type!=null
            && !java.util.Arrays.asList("training","cloud_training","smart_annotation").contains(type))
            throw new ServiceException("容器的持久任务关联类型未确认");
        if("smart_annotation".equals(records.get(0).get("instance_type"))) {
            requireLegacyCreator(records,"智能标注容器归属未确认或不属于当前用户");
            if(!jdbc.queryForList("SELECT training_id FROM vls_cloud_training_job WHERE container_record_id=?",Long.class,id).isEmpty())
                throw new ServiceException("智能标注容器关联类型不一致");
            Object round=records.get(0).get("training_task_id");
            if(!(round instanceof Long || round instanceof Integer || round instanceof Short || round instanceof Byte))throw new ServiceException("智能标注容器缺少可信轮次关联");
            annotationRound(((Number)round).longValue());return;
        }
        List<Long> jobs=jdbc.queryForList("SELECT training_id FROM vls_cloud_training_job WHERE container_record_id=? UNION SELECT training_task_id FROM vls_container_instance WHERE id=?",Long.class,id,id);
        if(jobs.stream().noneMatch(Objects::nonNull)) {
            requireLegacyCreator(records,"历史容器归属未确认或不属于当前用户");
            return;
        }
        for(Long training:jobs)if(training!=null)training(training);
    }
    public void containerFromHttp(Long id) { if(RequestContextHolder.getRequestAttributes()!=null)container(id); }
    private List<Map<String,Object>> annotationRoundRecords(Long id) {
        return jdbc.queryForList("SELECT t.tenant_id,t.create_user,r.tenant_id AS round_tenant_id FROM vls_smart_annotation_round r"
            +" JOIN vls_smart_annotation_task t ON t.id=r.task_id AND CAST(t.tenant_id AS BINARY)=CAST(r.tenant_id AS BINARY)"
            +" WHERE r.id=? AND COALESCE(r.is_deleted,0)=0 AND COALESCE(t.is_deleted,0)=0",id);
    }
    public void annotationRound(Long id) {
        if(id==null)throw new ServiceException("缺少智能标注轮次身份");
        requireLegacyCreator(annotationRoundRecords(id),"智能标注轮次归属未确认或不属于当前用户");
    }
    /** Only scheduler-owned composition uses this path; public controllers retain manual creation. */
    public void prepareTaskRecord(com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance input) {
        if(input.getId()!=null || input.getTrainingTaskId()==null
            || !java.util.Arrays.asList("training","cloud_training","smart_annotation").contains(input.getInstanceType()))
            throw new ServiceException("无效的后台任务记录身份或类型");
        boolean annotation="smart_annotation".equals(input.getInstanceType());
        List<Map<String,Object>> rows=annotation?annotationRoundRecords(input.getTrainingTaskId())
            :jdbc.queryForList("SELECT tenant_id,create_user FROM vls_algorithm_training WHERE id=?",input.getTrainingTaskId());
        if(rows.size()!=1 || !Objects.equals(ComputeNodeService.tenant(),rows.get(0).get("tenant_id"))
            || databaseCreator(rows.get(0).get("create_user"))==null || databaseCreator(rows.get(0).get("create_user")).isEmpty()
            || (input.getTenantId()!=null && !Objects.equals(input.getTenantId(),rows.get(0).get("tenant_id"))))
            throw new ServiceException("后台任务归属不存在、未确认或租户不匹配");
        if(RequestContextHolder.getRequestAttributes()!=null) {
            requireLegacyCreator(rows,"只能为本人任务创建执行记录");
            if(!annotation)training(input.getTrainingTaskId());
        }
        org.springframework.beans.BeanWrapper bean=new org.springframework.beans.BeanWrapperImpl(input);
        bean.setPropertyValue("tenantId",rows.get(0).get("tenant_id"));
        try { bean.setPropertyValue("createUser",databaseCreator(rows.get(0).get("create_user"))); }
        catch(RuntimeException error) { throw new ServiceException("无法确认后台任务创建身份"); }
    }
    public String queuedTrainingLog(Long trainingId,Long containerId) {
        if(trainingId==null || containerId==null)throw new ServiceException("缺少持久排队身份");
        trainingFromHttp(trainingId);containerFromHttp(containerId);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT c.logs_path FROM vls_container_instance c JOIN vls_algorithm_training t"
            +" ON t.id=c.training_task_id AND CAST(t.tenant_id AS BINARY)=CAST(c.tenant_id AS BINARY)"
            +" WHERE c.id=? AND c.training_task_id=? AND CAST(c.tenant_id AS BINARY)=CAST(? AS BINARY)"
            +" AND CAST(c.instance_type AS BINARY)=CAST('training' AS BINARY)"
            +" AND t.create_user IS NOT NULL AND t.create_user<>'' AND CAST(c.create_user AS BINARY)=CAST(t.create_user AS BINARY)",containerId,trainingId,ComputeNodeService.tenant());
        if(rows.size()!=1 || !(rows.get(0).get("logs_path") instanceof String) || ((String)rows.get(0).get("logs_path")).isEmpty())
            throw new ServiceException("无法确认排队记录的训练日志身份");
        return (String)rows.get(0).get("logs_path");
    }
    /** Execution identity and ownership come from persisted records, never update DTOs. */
    public static void protectTrainingUpdate(Object existing,Object input) {
        protectFields(existing,input,"tenantId","createUser","logPath");
    }
    public static void protectContainerUpdate(Object existing,Object input) {
        protectContainerExecution(existing,input);
        protectFields(existing,input,"instanceStatus","healthStatus","queueTime","startTime","stopTime","restartCount");
    }
    public static void protectContainerExecution(Object existing,Object input) {
        protectFields(existing,input,"tenantId","createUser","trainingTaskId","instanceType",
            "containerId","instanceName","serverId","serverIp","gpuIndex","gpuUuid","logsPath",
            "imageName","imageType","imageTag","resourceTypeId","resourceSpecId","instanceCount","algorithmId",
            "cpuLimit","memoryLimit","gpuLimit","portConfig","volumeConfig");
        org.springframework.beans.BeanWrapper stored=new org.springframework.beans.BeanWrapperImpl(existing);
        org.springframework.beans.BeanWrapper incoming=new org.springframework.beans.BeanWrapperImpl(input);
        if(stored.getPropertyValue("trainingTaskId")==null
            && !java.util.Arrays.asList("training","cloud_training","smart_annotation").contains(stored.getPropertyValue("instanceType")))
            validateManualVariables((String)incoming.getPropertyValue("envConfig"));
        else protectFields(existing,input,"envConfig");
    }
    private static void validateManualVariables(String value) {
        if(value==null || value.trim().isEmpty())return;
        try {
            com.fasterxml.jackson.databind.JsonNode node=new com.fasterxml.jackson.databind.ObjectMapper().readTree(value);
            if(!node.isObject() || node.size()>64)throw new IllegalArgumentException();
            java.util.Iterator<Map.Entry<String,com.fasterxml.jackson.databind.JsonNode>> fields=node.fields();
            while(fields.hasNext()) {
                Map.Entry<String,com.fasterxml.jackson.databind.JsonNode> field=fields.next();
                if(!field.getKey().matches("[A-Za-z_][A-Za-z0-9_]{0,63}") || "launchCommand".equalsIgnoreCase(field.getKey())
                    || "launchDefinition".equalsIgnoreCase(field.getKey()) || !field.getValue().isTextual() || field.getValue().asText().length()>4096)
                    throw new IllegalArgumentException();
            }
        } catch(Exception error) { throw new ServiceException("手工容器仅接受有限的字符串环境变量，不能提供执行定义或命令"); }
    }
    /** Worker-side persisted association proof, independent of HTTP and company-pool blocking. */
    public void requireTaskExecution(com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance row) {
        if(row==null || row.getId()==null || row.getTrainingTaskId()==null || row.getCreateUser()==null
            || !Objects.equals(ComputeNodeService.tenant(),row.getTenantId())
            || !java.util.Arrays.asList("training","smart_annotation").contains(row.getInstanceType()))
            throw new ServiceException("缺少可信调度任务关联");
        List<Map<String,Object>> tasks="smart_annotation".equals(row.getInstanceType())?annotationRoundRecords(row.getTrainingTaskId())
            :jdbc.queryForList("SELECT tenant_id,create_user FROM vls_algorithm_training WHERE id=?",row.getTrainingTaskId());
        if(tasks.size()!=1 || !Objects.equals(row.getTenantId(),tasks.get(0).get("tenant_id"))
            || databaseCreator(tasks.get(0).get("create_user"))==null || databaseCreator(tasks.get(0).get("create_user")).isEmpty()
            || !Objects.equals(String.valueOf(row.getCreateUser()),databaseCreator(tasks.get(0).get("create_user"))))
            throw new ServiceException("调度容器与持久任务归属不一致");
        containerFromHttp(row.getId());
    }
    public void prepareContainerCreate(com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance input) {
        if(input.getId()!=null || input.getContainerId()!=null || input.getTrainingTaskId()!=null
            || input.getServerId()!=null || input.getServerIp()!=null || input.getGpuIndex()!=null
            || input.getGpuUuid()!=null || input.getLogsPath()!=null)
            throw new ServiceException("执行身份及训练关联由后台维护，不能手动创建");
        String type=input.getInstanceType()==null?"":input.getInstanceType().trim().toLowerCase(java.util.Locale.ROOT);
        if(java.util.Arrays.asList("training","cloud_training","smart_annotation").contains(type)
            || (input.getInstanceStatus()!=null && !"stopped".equals(input.getInstanceStatus()))
            || (input.getHealthStatus()!=null && !"unknown".equals(input.getHealthStatus())) || input.getQueueTime()!=null || input.getStartTime()!=null
            || input.getStopTime()!=null || input.getRestartCount()!=null)
            throw new ServiceException("训练类型及调度生命周期由后台维护，不能手动创建或入队");
        validateManualVariables(input.getEnvConfig());
        if(input.getInstanceType()==null)input.setInstanceType("custom");
        input.setInstanceStatus("stopped");input.setHealthStatus("unknown");
        String user=localViewer();
        if(user.isEmpty())throw new ServiceException("缺少可信容器创建身份");
        org.springframework.beans.BeanWrapper bean=new org.springframework.beans.BeanWrapperImpl(input);
        bean.setPropertyValue("tenantId",ComputeNodeService.tenant());
        try { bean.setPropertyValue("createUser",user); }
        catch(RuntimeException error) { throw new ServiceException("无法确认容器创建身份"); }
    }
    private static void protectFields(Object existing,Object input,String... fields) {
        if(existing==null)throw new ServiceException("原记录不存在，无法确认任务身份");
        org.springframework.beans.BeanWrapper stored=new org.springframework.beans.BeanWrapperImpl(existing);
        org.springframework.beans.BeanWrapper incoming=new org.springframework.beans.BeanWrapperImpl(input);
        for(String field:fields) {
            Object value=incoming.getPropertyValue(field);
            if(value!=null && !Objects.equals(value,stored.getPropertyValue(field)))
                throw new ServiceException("任务归属、关联及执行身份字段由后台维护，不能修改: "+field);
        }
    }
    /** Existing workers use explicit tenant context and persisted jobs, without an HTTP user session. */
    public void trainingFromHttp(Long id) { if(RequestContextHolder.getRequestAttributes()!=null)training(id); }
    public void modelFromHttp(Long id) { if(RequestContextHolder.getRequestAttributes()!=null)model(id); }
}
