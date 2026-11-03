package com.ruoyi.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.ruoyi.workflow.domain.WorkflowStartRecord;
import java.util.List;

/** All queries explicitly bind the trusted tenant; the table is not user-editable. */
public interface WorkflowOwnershipMapper {
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT business_type FROM vls_workflow_ownership WHERE tenant_id = #{tenant} AND proc_ins_id = #{instance}")
    String findType(@Param("tenant") String tenant, @Param("instance") String instance);

    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT IGNORE INTO vls_workflow_ownership (tenant_id, proc_ins_id, business_type) VALUES (#{tenant}, #{instance}, 'WORK_ORDER')")
    int recordWorkOrder(@Param("tenant") String tenant, @Param("instance") String instance);

    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT IGNORE INTO vls_workflow_ownership (tenant_id, proc_ins_id, business_type) VALUES (#{tenant}, #{instance}, 'PERIODIC_WORK_ORDER')")
    int recordPeriodicWorkOrder(@Param("tenant") String tenant, @Param("instance") String instance);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT CASE WHEN EXISTS (SELECT 1 FROM workorder_app a WHERE BINARY a.tenant_id = BINARY #{tenant} "
        + "AND BINARY a.app_id = BINARY #{category} AND a.del_flag = '0') "
        + "OR EXISTS (SELECT 1 FROM workorder_synthesis s WHERE BINARY s.tenant_id = BINARY #{tenant} "
        + "AND BINARY s.synthesis_id = BINARY #{category} AND s.del_flag = '0') THEN 1 ELSE 0 END")
    int isWorkOrderCategory(@Param("tenant") String tenant, @Param("category") String category);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM work_order w WHERE BINARY w.tenant_id = BINARY #{tenant} "
        + "AND BINARY w.proc_ins_id = BINARY #{instance} AND BINARY w.id = BINARY #{id} "
        + "AND BINARY w.workorder_id = BINARY #{category} AND w.del_flag = '0'")
    int isPersistedWorkOrder(@Param("tenant") String tenant, @Param("instance") String instance,
                             @Param("id") String id, @Param("category") String category);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM work_order w WHERE BINARY w.tenant_id = BINARY #{tenant} "
        + "AND BINARY w.proc_ins_id = BINARY #{instance} AND BINARY w.id = BINARY #{id} AND w.del_flag = '0'")
    int isPersistedInstanceOrder(@Param("tenant") String tenant, @Param("instance") String instance,
                                 @Param("id") String id);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT e.id FROM oort_task_event e WHERE BINARY e.tenant_id = BINARY #{tenant} "
        + "AND BINARY e.id = BINARY #{id} AND e.no = #{number} AND e.deleted_at = 0 FOR UPDATE")
    String lockLocalEvent(@Param("tenant") String tenant, @Param("id") String id, @Param("number") long number);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT CAST(proc_ins_id AS CHAR CHARACTER SET utf8mb4) FROM vls_workflow_ownership "
        + "WHERE tenant_id = CAST(#{tenant} AS BINARY) AND (business_type IN ('OTHER_WORKFLOW','PERIODIC_WORK_ORDER') "
        + "OR (business_type = 'WORK_ORDER' AND #{allowWorkOrders} = 1)) LIMIT 10001")
    List<String> selectAllowedInstances(@Param("tenant") String tenant, @Param("allowWorkOrders") boolean allowWorkOrders);

    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT INTO vls_workflow_start_request (tenant_id,user_id,request_id,request_hash) "
        + "VALUES (#{tenant},#{user},#{request},#{hash})")
    int claimStart(@Param("tenant") String tenant, @Param("user") String user, @Param("request") String request,
                   @Param("hash") String hash);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT request_hash AS requestHash, CAST(proc_ins_id AS CHAR CHARACTER SET utf8mb4) AS procInsId, "
        + "CAST(work_order_id AS CHAR CHARACTER SET utf8mb4) AS workOrderId FROM vls_workflow_start_request "
        + "WHERE tenant_id=CAST(#{tenant} AS BINARY) AND user_id=CAST(#{user} AS BINARY) "
        + "AND request_id=CAST(#{request} AS BINARY) FOR UPDATE")
    WorkflowStartRecord lockStart(@Param("tenant") String tenant, @Param("user") String user,
                                  @Param("request") String request);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE vls_workflow_start_request SET proc_ins_id=#{instance},work_order_id=#{order} "
        + "WHERE tenant_id=CAST(#{tenant} AS BINARY) AND user_id=CAST(#{user} AS BINARY) "
        + "AND request_id=CAST(#{request} AS BINARY) AND proc_ins_id IS NULL")
    int completeStart(@Param("tenant") String tenant, @Param("user") String user, @Param("request") String request,
                      @Param("instance") String instance, @Param("order") String order);

}
