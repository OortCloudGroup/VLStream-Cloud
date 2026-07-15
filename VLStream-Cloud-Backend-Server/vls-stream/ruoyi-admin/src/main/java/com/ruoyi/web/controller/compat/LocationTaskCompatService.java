/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.workflow.domain.bo.ProcessStartBo;
import com.ruoyi.workflow.service.IWfProcessService;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.bo.WorkOrderBo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Java implementation of the task APIs previously served by apaas-location-service.
 */
@Service
public class LocationTaskCompatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationTaskCompatService.class);

    private static final int PARAM_ERROR = 4101;
    private static final int NOT_FOUND = 40001;
    private static final int TASK_STATUS_FINISHED = 40002;
    private static final int GROUP_NOT_FOUND = 40201;
    private static final int EVENT_GROUP_NOT_FOUND = 40301;
    private static final int EVENT_ITEM_NOT_FOUND = 40401;
    private static final int EVENT_ITEM_EXISTS = 40402;
    private static final int GROUP_APP_NOT_AUTHORIZED = 40503;

    private static final String EVENT = "`oort_task_event`";
    private static final String EVENT_USER = "`oort_task_event_user`";
    private static final String EVENT_BACK = "`oort_task_event_back`";
    private static final String EVENT_ITEM = "`oort_task_event_item`";
    private static final String EVENT_GROUP = "`oort_task_event_group`";
    private static final String TABLE_GROUP_V2 = "`oort_definition_table_group_v2`";
    private static final String TABLE_GROUP_APP_V2 = "`ap_definition_table_group_app_v2`";
    private static final String TABLE_GROUP_CONFIG_V2 = "`ap_definition_app_group_config_v2`";
    private static final String APP = "`wf_form_app`";
    private static final String USER = "`sys_user`";
    private static final String DEPT = "`sys_dept`";
    private static final String SETTINGS = "`oort_task_setting`";
    private static final String OLD_APP_GROUP_DEVICE = "`oort_definition_app_group_device`";
    private static final String OLD_APP_GROUP = "`oort_definition_app_group`";

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
        new TypeReference<LinkedHashMap<String, Object>>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final IWfProcessService processService;

    /**
     * Build the service on the application's primary dynamic data source.
     */
    public LocationTaskCompatService(DataSource dataSource, ObjectMapper objectMapper,
                                     IWfProcessService processService) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
        this.processService = processService;
    }

    /**
     * Create an event using the original mytask_updata/event_report_photo behavior.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> addEvent(Map<String, Object> body, UserContext user) {
        if (!(body.get("point") instanceof Map)) {
            return parameterError("参数错误 point不能为空");
        }
        String describe = stringValue(body, "describe");
        String item = stringValue(body, "item");
        if (item.isEmpty()) {
            item = describe;
            if (codePointLength(item) > 10) {
                return parameterError("参数错误 describe 不能超过10个字符串");
            }
        }
        if (codePointLength(item) > 10 || codePointLength(stringValue(body, "name")) > 10) {
            return parameterError("参数错误 字符串长度超出限制");
        }

        EventKey key = nextEventKey(user.getUserName());
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("describe", describe);
        List<String> pictures = distinctStrings(body.get("pics"), true);
        Map<String, Object> point = pointValue(body.get("point"));
        data.put("point", point);
        data.put("pics", pictures);
        data.put("send_pics", new ArrayList<String>());

        jdbc.update("INSERT INTO " + EVENT
                + " (`id`,`no`,`tenant_id`,`uuid`,`name`,`data`,`status`,`item`,`client`,`mod_type`,"
                + "`mod_status`,`device_id`,`device_name`,`device_tag`,`work_order_status`,"
                + "`created_at`,`updated_at`,`deleted_at`) "
                + "VALUES (?,?,?,?,?,CAST(? AS JSON),2,?,?,1,0,'','','',0,NOW(),NOW(),0)",
            key.getId(), key.getNo(), user.getTenantId(), user.getUserId(), stringValue(body, "name"),
            toJson(data), item, user.getClient());

        scheduleAutomaticWorkOrder(new EventSnapshot(key, user.getTenantId(), user.getUserId(),
            stringValue(body, "name"), item, describe, point, pictures, 1, ""));
        Map<String, Object> event = findEvent(key.externalId());
        return LocationTaskResult.success(event);
    }

    /**
     * Validate, persist, and return one camera-reported active-safety event.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> reportCameraEvent(Map<String, Object> body) {
        LocationTaskResult<?> validation = validateCameraEvent(body);
        if (validation != null) {
            return validation;
        }
        EventKey key = insertCameraEvent(body);
        return LocationTaskResult.success(findEvent(key.externalId()));
    }

    /**
     * Validate an entire camera batch, then persist every valid member independently as Go did.
     */
    public LocationTaskResult<?> reportCameraEvents(Map<String, Object> body) {
        Object rawEvents = body.get("event_report");
        if (!(rawEvents instanceof List) || ((List<?>) rawEvents).isEmpty()) {
            return parameterError("参数错误 event_report不能为空");
        }
        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
        for (Object rawEvent : (List<?>) rawEvents) {
            if (!(rawEvent instanceof Map)) {
                return parameterError("参数错误 event_report格式不正确");
            }
            Map<String, Object> event = castMap(rawEvent);
            LocationTaskResult<?> validation = validateCameraEvent(event);
            if (validation != null) {
                return validation;
            }
            events.add(event);
        }
        for (Map<String, Object> event : events) {
            try {
                insertCameraEvent(event);
            } catch (DataAccessException exception) {
                LOGGER.warn("Camera batch member could not be persisted for device {}",
                    stringValue(event, "device_id"), exception);
            }
        }
        return LocationTaskResult.success();
    }

    /**
     * Return a Go-compatible validation error for an invalid camera event, or null when valid.
     */
    private LocationTaskResult<?> validateCameraEvent(Map<String, Object> body) {
        if (!(body.get("point") instanceof Map)) {
            return parameterError("参数错误 point不能为空");
        }
        if (stringValue(body, "device_id").isEmpty()
            || stringValue(body, "device_name").isEmpty()
            || stringValue(body, "device_tenant_id").isEmpty()) {
            return parameterError("参数错误 device_id、device_name和device_tenant_id不能为空");
        }
        String item = cameraEventItem(body);
        if (stringValue(body, "item").isEmpty() && codePointLength(item) > 10) {
            return parameterError("参数错误 describe 不能超过10个字符串");
        }
        if (codePointLength(item) > 10 || codePointLength(stringValue(body, "name")) > 10) {
            return parameterError("参数错误 字符串长度超出限制");
        }
        return null;
    }

    /**
     * Persist one active-safety event and schedule its configuration-controlled work-order hook.
     */
    private EventKey insertCameraEvent(Map<String, Object> body) {
        String tenantId = stringValue(body, "device_tenant_id");
        String deviceId = stringValue(body, "device_id");
        String deviceName = stringValue(body, "device_name");
        String item = cameraEventItem(body);
        String describe = stringValue(body, "describe");
        String name = stringValue(body, "name");
        Map<String, Object> point = pointValue(body.get("point"));
        List<String> pictures = distinctStrings(body.get("pics"), true);
        List<String> videos = distinctStrings(body.get("video"), true);
        EventKey key = nextEventKey(deviceName);

        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("describe", describe);
        data.put("point", point);
        data.put("pics", pictures);
        data.put("send_pics", new ArrayList<String>());
        data.put("video", videos);
        jdbc.update("INSERT INTO " + EVENT
                + " (`id`,`no`,`tenant_id`,`uuid`,`name`,`data`,`status`,`item`,`client`,`mod_type`,"
                + "`mod_status`,`device_id`,`device_name`,`device_tag`,`work_order_status`,"
                + "`created_at`,`updated_at`,`deleted_at`) "
                + "VALUES (?,?,?,?,?,CAST(? AS JSON),2,?,'camera',2,0,?,?,?,0,NOW(),NOW(),0)",
            key.getId(), key.getNo(), tenantId, deviceId, name, toJson(data), item,
            deviceId, deviceName, stringValue(body, "device_tag"));
        scheduleAutomaticWorkOrder(new EventSnapshot(key, tenantId, deviceId, name, item,
            describe, point, pictures, 2, deviceId));
        return key;
    }

    /**
     * Resolve the event item, falling back to describe exactly as the camera Go endpoint did.
     */
    private static String cameraEventItem(Map<String, Object> body) {
        String item = stringValue(body, "item");
        return item.isEmpty() ? stringValue(body, "describe") : item;
    }

    /**
     * Return all event-item definitions for the current tenant.
     */
    public LocationTaskResult<?> eventItemList(Map<String, Object> body, UserContext user) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM " + EVENT_ITEM + " WHERE tenant_id = ?", user.getTenantId());
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            list.add(toEventItem(row));
        }
        return LocationTaskResult.success(singleListPayload(list));
    }

    /**
     * Create or update an event-item definition.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> eventItemSave(Map<String, Object> body, UserContext user) {
        String item = stringValue(body, "item");
        if (item.isEmpty() || codePointLength(item) > 50) {
            return parameterError("参数错误 item不能为空且不能超过50个字符串");
        }
        String remark = stringValue(body, "remark");
        if (codePointLength(remark) > 255) {
            return parameterError("参数错误 remark不能超过255个字符串");
        }

        String uid = stringValue(body, "uid");
        if (!uid.isEmpty()) {
            Map<String, Object> existing = firstRow(
                "SELECT * FROM " + EVENT_ITEM + " WHERE uid = ? LIMIT 1", uid);
            if (existing == null) {
                return error(EVENT_ITEM_NOT_FOUND, "没找到事件类型记录");
            }
            if (!item.equals(string(existing.get("item")))) {
                Map<String, Object> duplicate = firstRow(
                    "SELECT uid FROM " + EVENT_ITEM + " WHERE tenant_id = ? AND item = ? LIMIT 1",
                    user.getTenantId(), item);
                if (duplicate != null) {
                    return error(EVENT_ITEM_EXISTS, "事件类型已存在");
                }
            }
            if (!remark.equals(string(existing.get("remark")))) {
                jdbc.update("UPDATE " + EVENT_ITEM + " SET remark = ?, updated_at = NOW() WHERE uid = ?",
                    remark, uid);
            }
            return LocationTaskResult.success();
        }

        if (firstRow("SELECT uid FROM " + EVENT_ITEM + " WHERE tenant_id = ? AND item = ? LIMIT 1",
            user.getTenantId(), item) != null) {
            return error(EVENT_ITEM_EXISTS, "事件类型已存在");
        }
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        LinkedHashMap<String, Object> configData = new LinkedHashMap<String, Object>();
        configData.put("tenant_id", "");
        configData.put("user_id", "");
        data.put("config", configData);
        jdbc.update("INSERT INTO " + EVENT_ITEM
                + " (`uid`,`item`,`mod_type`,`tenant_id`,`data`,`remark`,`created_at`,`updated_at`) "
                + "VALUES (?,?,1,?,CAST(? AS JSON),?,NOW(),NOW())",
            IdUtil.getSnowflakeNextIdStr(), item, user.getTenantId(), toJson(data), remark);
        return LocationTaskResult.success();
    }

    /**
     * Delete an event-item definition.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> eventItemDelete(Map<String, Object> body, UserContext user) {
        String uid = stringValue(body, "uid");
        if (uid.isEmpty()) {
            return parameterError("参数错误 uid不能为空");
        }
        if (firstRow("SELECT uid FROM " + EVENT_ITEM + " WHERE uid = ? LIMIT 1", uid) == null) {
            return error(EVENT_ITEM_NOT_FOUND, "没找到事件类型记录");
        }
        jdbc.update("DELETE FROM " + EVENT_ITEM + " WHERE uid = ?", uid);
        return LocationTaskResult.success();
    }

    /**
     * Return the filtered, paged event list.
     */
    public LocationTaskResult<?> eventList(Map<String, Object> body, UserContext user) {
        int page = positiveInt(body, "page", 1);
        int pageSize = Math.min(positiveInt(body, "pagesize", 10), 100);
        StringBuilder from = new StringBuilder(" FROM ").append(EVENT).append(" te ");
        List<String> where = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        where.add("te.deleted_at = 0");
        where.add("te.tenant_id = ?");
        args.add(user.getTenantId());

        int modType = intValue(body, "mod_type", 0);
        if (modType > 0) {
            where.add("te.mod_type = ?");
            args.add(modType);
            int modStatus = intValue(body, "mod_status", 0);
            if (modType == 2 && modStatus > 0) {
                where.add(modStatus == 1 ? "te.mod_status = 0" : "te.mod_status > 0");
            }
        } else {
            where.add("te.mod_type = 1");
        }
        appendEquals(where, args, body, "item", "te.item");
        appendEquals(where, args, body, "task_id", "te.task_id");
        appendPositiveEquals(where, args, body, "status", "te.status");
        appendPictureFilter(where, body, "te.pic_len");

        String uuid = stringValue(body, "uuid");
        String deptId = stringValue(body, "dept_id");
        String groupUid = stringValue(body, "group_uid");
        if (!uuid.isEmpty()) {
            where.add("te.uuid = ?");
            args.add(uuid);
        } else if (!deptId.isEmpty()) {
            from.append(" LEFT JOIN ").append(USER)
                .append(" u ON u.user_id = te.uuid AND u.tenant_id = te.tenant_id ")
                .append(" LEFT JOIN ").append(DEPT)
                .append(" d ON d.dept_id = u.dept_id AND d.tenant_id = u.tenant_id ")
                .append(" LEFT JOIN ").append(DEPT)
                .append(" d2 ON d2.tenant_id = d.tenant_id AND (d.dept_id = d2.dept_id ")
                .append("OR FIND_IN_SET(d2.dept_id,d.ancestors) > 0) ");
            where.add(deptKey("d2") + " = ?");
            args.add(deptId);
        } else if (!groupUid.isEmpty()) {
            from.append(" LEFT JOIN ").append(OLD_APP_GROUP_DEVICE).append(" gd ON gd.device_id = te.device_id ")
                .append(" LEFT JOIN ").append(OLD_APP_GROUP).append(" ag ON ag.uid = gd.uid ")
                .append(" LEFT JOIN ").append(OLD_APP_GROUP)
                .append(" ag2 ON ag.uid_path LIKE CONCAT(ag2.uid_path,'%') ");
            where.add("ag2.tenant_id = ? AND ag2.uid = ?");
            args.add(user.getTenantId());
            args.add(groupUid);
        }

        LocationTaskResult<?> dateError = appendDateRange(where, args, body, "start_at", "end_at", "te.created_at");
        if (dateError != null) {
            return dateError;
        }
        String keyword = stringValue(body, "keyword");
        if (!keyword.isEmpty()) {
            where.add("te.item LIKE ?");
            args.add("%" + keyword + "%");
        }
        return LocationTaskResult.success(pagedEvents(from.toString(), where, args, page, pageSize));
    }

    /**
     * Add feedback to an event and update its status fields transactionally.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> eventBackAdd(Map<String, Object> body, UserContext user) {
        String externalId = stringValue(body, "id");
        if (externalId.isEmpty()) {
            return parameterError("参数错误 id不能为空");
        }
        Map<String, Object> event = findEventRaw(externalId);
        if (event == null || !user.getTenantId().equals(string(event.get("tenant_id")))) {
            return error(NOT_FOUND, "没找到记录");
        }
        int modStatus = intValue(body, "mod_status", 0);
        if (number(event.get("mod_type")).intValue() == 2
            && number(event.get("mod_status")).intValue() == 0 && modStatus == 0) {
            return parameterError("参数错误 mod_status 主动安全反馈时状态必须确定状态");
        }

        EventKey key = EventKey.parse(externalId);
        LinkedHashMap<String, Object> feedbackData = new LinkedHashMap<String, Object>();
        feedbackData.put("describe", stringValue(body, "describe"));
        feedbackData.put("point", pointValue(body.get("point")));
        feedbackData.put("pics", body.containsKey("pics") ? stringList(body.get("pics")) : null);
        jdbc.update("INSERT INTO " + EVENT_BACK
                + " (`id`,`tenant_id`,`task_event_id`,`uuid`,`data`,`created_at`,`updated_at`) "
                + "VALUES (?,?,?,?,CAST(? AS JSON),NOW(),NOW())",
            IdUtil.fastSimpleUUID(), user.getTenantId(), key.externalId(), user.getUserId(), toJson(feedbackData));

        int oldStatus = number(event.get("status")).intValue();
        int status = intValue(body, "status", 0);
        int oldModStatus = number(event.get("mod_status")).intValue();
        int workOrderStatus = intValue(body, "work_order_status", 0);
        Map<String, Object> eventData = jsonMap(event.get("data"));
        StringBuilder update = new StringBuilder("UPDATE ").append(EVENT).append(" SET updated_at = NOW()");
        List<Object> args = new ArrayList<Object>();
        if (number(event.get("mod_type")).intValue() == 2 && modStatus > 0 && modStatus != oldModStatus) {
            update.append(", mod_status = ?");
            args.add(modStatus);
        }
        if (status != oldStatus) {
            update.append(", status = ?");
            args.add(status);
            if (status == 1) {
                eventData.put("finish_at", DATE_TIME_FORMAT.format(LocalDateTime.now()));
            }
        }
        if (workOrderStatus == 1) {
            update.append(", work_order_status = 1");
            Object workOrderData = body.get("work_order_data");
            if (workOrderData instanceof Map && ((Map<?, ?>) workOrderData).size() > 1) {
                eventData.put("work_order_data", workOrderData);
            }
        }
        update.append(", data = CAST(? AS JSON) WHERE id = ? AND `no` = ?");
        args.add(toJson(eventData));
        args.add(key.getId());
        args.add(key.getNo());
        jdbc.update(update.toString(), args.toArray());
        return LocationTaskResult.success();
    }

    /**
     * Return one event with the same composite-id and JSON expansion as Go.
     */
    public LocationTaskResult<?> eventInfo(Map<String, Object> body, UserContext user) {
        String id = stringValue(body, "id");
        if (id.isEmpty()) {
            return parameterError("参数错误 id不能为空");
        }
        Map<String, Object> event = findEvent(id);
        if (event == null) {
            return error(NOT_FOUND, "没找到记录");
        }
        return LocationTaskResult.success(event);
    }

    /**
     * Return the paged feedback list for one event.
     */
    public LocationTaskResult<?> eventBackList(Map<String, Object> body, UserContext user) {
        String eventId = stringValue(body, "task_event_id");
        if (eventId.isEmpty()) {
            return parameterError("参数错误 task_event_id不能为空");
        }
        if (findEventRaw(eventId) == null) {
            return error(NOT_FOUND, "没找到记录");
        }
        int page = positiveInt(body, "page", 1);
        int pageSize = positiveInt(body, "pagesize", 10);
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + EVENT_BACK + " WHERE tenant_id = ? AND task_event_id = ?",
            Long.class, user.getTenantId(), eventId);
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM " + EVENT_BACK
                + " WHERE tenant_id = ? AND task_event_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
            user.getTenantId(), eventId, pageSize, (page - 1) * pageSize);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            list.add(toEventBack(row));
        }
        return LocationTaskResult.success(pagePayload(page, pageSize, count == null ? 0L : count, list));
    }

    /**
     * Associate event executors or vehicles with an event.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> eventAddUser(Map<String, Object> body, UserContext user) {
        String eventId = stringValue(body, "id");
        Object values = body.get("uuids");
        if (eventId.isEmpty() || !(values instanceof List) || ((List<?>) values).isEmpty()) {
            return parameterError("参数错误 id和uuids不能为空");
        }
        Map<String, Object> event = findEventRaw(eventId);
        if (event == null || !user.getTenantId().equals(string(event.get("tenant_id")))) {
            return error(NOT_FOUND, "没找到记录");
        }
        if (number(event.get("status")).intValue() != 2) {
            return error(TASK_STATUS_FINISHED, "任务状态已完成");
        }
        for (Object value : (List<?>) values) {
            if (!(value instanceof Map)) {
                return parameterError("参数错误 uuids格式不正确");
            }
            Map<?, ?> executor = (Map<?, ?>) value;
            String uuid = string(executor.get("uuid"));
            if (uuid.isEmpty()) {
                return parameterError("参数错误 uuid不能为空");
            }
            int userType = integer(executor.get("u_type"), 0);
            if (userType == 0) {
                userType = 1;
            }
            Long exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + EVENT_USER + " WHERE task_event_id = ? AND uuid = ?",
                Long.class, eventId, uuid);
            if (exists == null || exists == 0L) {
                jdbc.update("INSERT INTO " + EVENT_USER
                        + " (`task_event_id`,`tenant_id`,`uuid`,`u_type`,`created_at`,`updated_at`) "
                        + "VALUES (?,?,?,?,NOW(),NOW())",
                    eventId, user.getTenantId(), uuid, userType);
            }
        }
        return LocationTaskResult.success();
    }

    /**
     * Soft-delete an event and physically remove its executor and feedback rows.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> eventDelete(Map<String, Object> body, UserContext user) {
        String eventId = stringValue(body, "id");
        if (eventId.isEmpty()) {
            return parameterError("参数错误 id不能为空");
        }
        Map<String, Object> event = findEventRaw(eventId);
        if (event == null) {
            return error(NOT_FOUND, "没找到记录");
        }
        EventKey key = EventKey.parse(eventId);
        jdbc.update("UPDATE " + EVENT + " SET deleted_at = ?, updated_at = NOW() WHERE id = ? AND `no` = ?",
            System.currentTimeMillis(), key.getId(), key.getNo());
        jdbc.update("DELETE FROM " + EVENT_USER + " WHERE task_event_id = ?", eventId);
        jdbc.update("DELETE FROM " + EVENT_BACK + " WHERE task_event_id = ?", eventId);
        return LocationTaskResult.success();
    }

    /**
     * Return the current user's event list for the requested execution relationship.
     */
    public LocationTaskResult<?> myEventList(Map<String, Object> body, UserContext user) {
        int page = positiveInt(body, "page", 1);
        int pageSize = positiveInt(body, "pagesize", 10);
        int executionStatus = intValue(body, "exec_status", 0);
        if (executionStatus <= 1) {
            executionStatus = 1;
        }

        StringBuilder from = new StringBuilder(" FROM ").append(EVENT).append(" te ");
        List<String> where = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        where.add("te.deleted_at = 0");
        where.add("te.tenant_id = ?");
        args.add(user.getTenantId());
        if (executionStatus == 4 || executionStatus == 1) {
            from.append(" LEFT JOIN ").append(EVENT_USER)
                .append(" eu ON eu.task_event_id = CONCAT(te.id,'-',te.`no`) ");
        }
        if (executionStatus == 4) {
            where.add("((eu.uuid = ? AND eu.u_type = 1) OR te.uuid = ? OR "
                + "CONCAT(te.id,'-',te.`no`) IN (SELECT task_event_id FROM " + EVENT_BACK + " WHERE uuid = ?))");
            args.add(user.getUserId());
            args.add(user.getUserId());
            args.add(user.getUserId());
        } else if (executionStatus == 1) {
            where.add("eu.uuid = ? AND eu.u_type = 1");
            args.add(user.getUserId());
        } else if (executionStatus == 2) {
            where.add("te.uuid = ?");
            args.add(user.getUserId());
        } else if (executionStatus == 3) {
            where.add("CONCAT(te.id,'-',te.`no`) IN (SELECT task_event_id FROM " + EVENT_BACK + " WHERE uuid = ?)");
            args.add(user.getUserId());
        }

        int modType = intValue(body, "mod_type", 0);
        if (modType > 0) {
            where.add("te.mod_type = ?");
            args.add(modType);
            int modStatus = intValue(body, "mod_status", 0);
            if (modType == 2 && modStatus > 0) {
                where.add(modStatus == 1 ? "te.mod_status = 0" : "te.mod_status > 0");
            }
        } else {
            where.add("te.mod_type = 1");
        }
        appendPositiveEquals(where, args, body, "status", "te.status");
        appendEquals(where, args, body, "item", "te.item");
        appendPictureFilter(where, body, "te.pic_len");
        return LocationTaskResult.success(pagedEvents(from.toString(), where, args, page, pageSize));
    }

    /**
     * Return the V2 groups authorized to the requested application.
     */
    public LocationTaskResult<?> eventGroupListV2(Map<String, Object> body, UserContext user) {
        String appId = stringValue(body, "app_id");
        int groupType = intValue(body, "group_type", 0);
        if (appId.isEmpty() || groupType < 1 || groupType > 3) {
            return parameterError("参数错误 app_id和group_type不能为空");
        }
        if (!appExists(user.getTenantId(), appId)) {
            return error(NOT_FOUND, "应用不存在");
        }

        String parentUid = stringValue(body, "puid");
        Map<String, Object> parent = null;
        if (!parentUid.isEmpty()) {
            parent = findGroupV2(parentUid);
            if (parent == null) {
                return error(GROUP_NOT_FOUND, "没找到分组记录");
            }
            groupType = number(parent.get("group_type")).intValue();
        }

        List<Map<String, Object>> grants = appGroupGrants(user.getTenantId(), appId);
        if (grants.isEmpty()) {
            return LocationTaskResult.success(singleListPayload(new ArrayList<Object>()));
        }
        boolean parentAuthorized = parent == null;
        LinkedHashSet<String> authorizedRootUids = new LinkedHashSet<String>();
        for (Map<String, Object> grant : grants) {
            String grantPath = string(grant.get("uid_path"));
            if (parent != null && string(parent.get("uid_path")).startsWith(grantPath)) {
                parentAuthorized = true;
            }
            if (number(grant.get("group_type")).intValue() == groupType) {
                authorizedRootUids.add(string(grant.get("group_uid")));
            }
        }
        if (!parentAuthorized) {
            return LocationTaskResult.success(singleListPayload(new ArrayList<Object>()));
        }

        List<Map<String, Object>> rows;
        if (parent != null) {
            rows = jdbc.queryForList("SELECT g.*, c.data AS conf_data FROM " + TABLE_GROUP_V2 + " g "
                    + "LEFT JOIN " + TABLE_GROUP_CONFIG_V2
                    + " c ON c.group_uid = g.uid AND c.app_id = ? WHERE g.puid = ? ORDER BY g.sort ASC",
                appId, parentUid);
        } else if (authorizedRootUids.isEmpty()) {
            rows = new ArrayList<Map<String, Object>>();
        } else {
            List<Object> args = new ArrayList<Object>();
            args.add(appId);
            args.addAll(authorizedRootUids);
            rows = jdbc.queryForList("SELECT g.*, c.data AS conf_data FROM " + TABLE_GROUP_V2 + " g "
                    + "LEFT JOIN " + TABLE_GROUP_CONFIG_V2
                    + " c ON c.group_uid = g.uid AND c.app_id = ? WHERE g.uid IN ("
                    + placeholders(authorizedRootUids.size()) + ")",
                args.toArray());
        }
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            list.add(toGroupV2(row));
        }
        return LocationTaskResult.success(singleListPayload(list));
    }

    /**
     * Preserve the original deprecated V1 event-group save endpoint.
     */
    public LocationTaskResult<?> eventGroupSaveV1(Map<String, Object> body, UserContext user) {
        return LocationTaskResult.deprecated();
    }

    /**
     * Preserve the original deprecated V1 event-group delete endpoint.
     */
    public LocationTaskResult<?> eventGroupDeleteV1(Map<String, Object> body, UserContext user) {
        return LocationTaskResult.deprecated();
    }

    /**
     * Replace an event item's workflow configuration.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> eventItemSettingSave(Map<String, Object> body, UserContext user) {
        String uid = stringValue(body, "uid");
        if (uid.isEmpty() || !(body.get("config") instanceof Map)) {
            return parameterError("参数错误 uid和config不能为空");
        }
        Map<String, Object> item = firstRow("SELECT * FROM " + EVENT_ITEM + " WHERE uid = ? LIMIT 1", uid);
        if (item == null) {
            return error(EVENT_ITEM_NOT_FOUND, "没找到事件类型记录");
        }
        Map<String, Object> data = jsonMap(item.get("data"));
        data.put("config", workflowConfigData(castMap(body.get("config")), user));
        jdbc.update("UPDATE " + EVENT_ITEM + " SET data = CAST(? AS JSON), updated_at = NOW() WHERE uid = ?",
            toJson(data), uid);
        return LocationTaskResult.success();
    }

    /**
     * Update an event item's automatic-work-order status.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> eventItemStatus(Map<String, Object> body, UserContext user) {
        String uid = stringValue(body, "uid");
        if (uid.isEmpty()) {
            return parameterError("参数错误 uid不能为空");
        }
        Map<String, Object> item = firstRow("SELECT * FROM " + EVENT_ITEM + " WHERE uid = ? LIMIT 1", uid);
        if (item == null) {
            return error(EVENT_ITEM_NOT_FOUND, "没找到事件类型记录");
        }
        Map<String, Object> data = jsonMap(item.get("data"));
        Map<String, Object> config = nestedMap(data, "config");
        boolean autoToWork = booleanValue(body, "auto_to_work", false);
        if (booleanValue(config, "auto_to_work", false) != autoToWork) {
            config.put("auto_to_work", autoToWork);
            data.put("config", config);
            jdbc.update("UPDATE " + EVENT_ITEM + " SET data = CAST(? AS JSON), updated_at = NOW() WHERE uid = ?",
                toJson(data), uid);
        }
        return LocationTaskResult.success();
    }

    /**
     * Return one V2 event group after application-authorization checks.
     */
    public LocationTaskResult<?> eventGroupInfoV2(Map<String, Object> body, UserContext user) {
        String uid = stringValue(body, "uid");
        String appId = stringValue(body, "app_id");
        if (uid.isEmpty() || appId.isEmpty()) {
            return parameterError("参数错误 uid和app_id不能为空");
        }
        if (!appExists(user.getTenantId(), appId)) {
            return error(NOT_FOUND, "没找到记录");
        }
        List<Map<String, Object>> grants = appGroupGrants(user.getTenantId(), appId);
        if (grants.isEmpty()) {
            return error(GROUP_APP_NOT_AUTHORIZED, "应用分组没有权限");
        }
        Map<String, Object> group = findGroupV2(uid);
        if (group == null) {
            return error(GROUP_NOT_FOUND, "没找到分组记录");
        }
        if (!isGroupAuthorized(group, grants)) {
            return error(GROUP_APP_NOT_AUTHORIZED, "应用分组没有权限");
        }
        Map<String, Object> row = firstRow(
            "SELECT g.*, c.data AS conf_data FROM " + TABLE_GROUP_V2 + " g LEFT JOIN "
                + TABLE_GROUP_CONFIG_V2
                + " c ON c.group_uid = g.uid AND c.app_id = ? WHERE g.uid = ? LIMIT 1",
            appId, uid);
        if (row == null || !user.getTenantId().equals(string(row.get("tenant_id")))) {
            return error(EVENT_GROUP_NOT_FOUND, "没找到事件分组记录");
        }
        return LocationTaskResult.success(toGroupV2(row));
    }

    /**
     * Return the global workflow configuration identified by tenant, group type, and event type.
     */
    public LocationTaskResult<?> workflowConfigGet(Map<String, Object> body, UserContext user) {
        int modType = intValue(body, "mod_type", 0);
        int groupType = intValue(body, "group_type", 0);
        if (!validOptionalType(modType, 2) || !validOptionalType(groupType, 2)) {
            return parameterError("参数错误 mod_type或group_type不正确");
        }
        String key = workflowSettingKey(user.getTenantId(), modType, groupType);
        Map<String, Object> setting = firstRow("SELECT `val` FROM " + SETTINGS + " WHERE `key` = ? LIMIT 1", key);
        Map<String, Object> config;
        if (setting == null) {
            config = new LinkedHashMap<String, Object>();
            config.put("tenant_id", "");
            config.put("user_id", "");
        } else {
            config = jsonMap(setting.get("val"));
            config.put("tenant_id", user.getTenantId());
        }
        applyWorkflowDefaults(config);
        return LocationTaskResult.success(config);
    }

    /**
     * Upsert the global workflow configuration in the original settings table.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> workflowConfigSet(Map<String, Object> body, UserContext user) {
        int modType = intValue(body, "mod_type", 0);
        int groupType = intValue(body, "group_type", 0);
        if (!validOptionalType(modType, 2) || !validOptionalType(groupType, 2)) {
            return parameterError("参数错误 mod_type或group_type不正确");
        }
        LinkedHashMap<String, Object> config = workflowConfig(body);
        config.put("tenant_id", user.getTenantId());
        config.put("user_id", user.getUserId());
        upsertSetting(workflowSettingKey(user.getTenantId(), modType, groupType), config);
        return LocationTaskResult.success();
    }

    /**
     * Save a department- or user-specific workflow configuration.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> eventGroupDeptUserSave(Map<String, Object> body, UserContext user) {
        String userId = stringValue(body, "user_id");
        String deptId = stringValue(body, "dept_id");
        if (userId.isEmpty() && deptId.isEmpty()) {
            return parameterError("参数错误 用户ID或部门ID不能都为空");
        }
        Map<String, Object> config = body.get("config") instanceof Map
            ? castMap(body.get("config")) : new LinkedHashMap<String, Object>();
        String name = !userId.isEmpty() ? userId : deptId;
        String parent = !userId.isEmpty() ? "user" : "dept";
        Map<String, Object> existing = firstRow(
            "SELECT * FROM " + EVENT_GROUP
                + " WHERE tenant_id = ? AND name = ? AND puid = ? LIMIT 1",
            user.getTenantId(), name, parent);
        LinkedHashMap<String, Object> data = existing == null
            ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(jsonMap(existing.get("data")));
        data.put("config", workflowConfigData(config, user));
        if (existing == null) {
            jdbc.update("INSERT INTO " + EVENT_GROUP
                    + " (`uid`,`tenant_id`,`group_type`,`mod_type`,`name`,`puid`,`uid_path`,`data`,"
                    + "`created_at`,`updated_at`) VALUES (?,?,1,1,?,?,?,CAST(? AS JSON),NOW(),NOW())",
                IdUtil.getSnowflakeNextIdStr(), user.getTenantId(), name, parent, "", toJson(data));
        } else {
            jdbc.update("UPDATE " + EVENT_GROUP + " SET data = CAST(? AS JSON), updated_at = NOW() WHERE uid = ?",
                toJson(data), string(existing.get("uid")));
        }
        return LocationTaskResult.success();
    }

    /**
     * Return child departments and department users with their workflow configurations.
     */
    public LocationTaskResult<?> eventGroupDeptUserList(Map<String, Object> body, UserContext user) {
        String deptId = stringValue(body, "dept_id");
        List<Map<String, Object>> deptRows = jdbc.queryForList(
            "SELECT " + deptKey("d") + " AS dept_id," + deptCode("d") + " AS dept_code,"
                + "d.dept_name," + deptKey("p") + " AS parent_dept_id,g.data FROM " + DEPT + " d "
                + "LEFT JOIN " + DEPT + " p ON p.dept_id = d.parent_id AND p.tenant_id = d.tenant_id "
                + "LEFT JOIN " + EVENT_GROUP + " g ON g.name = " + deptKey("d")
                + " AND g.puid = 'dept' AND g.tenant_id = d.tenant_id "
                + "WHERE d.tenant_id = ? AND COALESCE(d.del_flag,'0') = '0' "
                + "AND ((? = '' AND COALESCE(d.parent_id,0) = 0) OR " + deptKey("p") + " = ?)",
            user.getTenantId(), deptId, deptId);
        List<Map<String, Object>> depts = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : deptRows) {
            LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
            value.put("dept_id", string(row.get("dept_id")));
            value.put("dept_code", string(row.get("dept_code")));
            value.put("dept_name", string(row.get("dept_name")));
            value.put("parent_dept_id", string(row.get("parent_dept_id")));
            Map<String, Object> config = nestedMap(jsonMap(row.get("data")), "config");
            if (!config.isEmpty()) {
                value.put("config", workflowConfigOnly(config));
            }
            depts.add(value);
        }

        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        if (!deptId.isEmpty()) {
            List<Map<String, Object>> userRows = jdbc.queryForList(
                "SELECT u.user_id,COALESCE(NULLIF(u.nick_name,''),u.user_name) AS user_name,"
                    + "u.avatar AS photo,g.data FROM " + USER + " u "
                    + "JOIN " + DEPT + " d ON d.dept_id = u.dept_id AND d.tenant_id = u.tenant_id "
                    + "LEFT JOIN " + EVENT_GROUP
                    + " g ON g.name = u.user_id AND g.puid = 'user' AND g.tenant_id = u.tenant_id "
                    + "WHERE " + deptKey("d") + " = ? AND u.tenant_id = ? "
                    + "AND COALESCE(u.del_flag,'0') = '0'",
                deptId, user.getTenantId());
            for (Map<String, Object> row : userRows) {
                LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
                value.put("user_id", string(row.get("user_id")));
                value.put("user_name", string(row.get("user_name")));
                value.put("user_photo", string(row.get("photo")));
                Map<String, Object> config = nestedMap(jsonMap(row.get("data")), "config");
                if (!config.isEmpty()) {
                    value.put("config", workflowConfigOnly(config));
                }
                users.add(value);
            }
        }
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("dept", depts);
        payload.put("user", users);
        return LocationTaskResult.success(payload);
    }

    /**
     * Update a department- or user-specific automatic-work-order flag.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> eventGroupDeptUserStatus(Map<String, Object> body, UserContext user) {
        String userId = stringValue(body, "user_id");
        String deptId = stringValue(body, "dept_id");
        if (userId.isEmpty() && deptId.isEmpty()) {
            return parameterError("参数错误 用户ID或部门ID不能都为空");
        }
        String name = !userId.isEmpty() ? userId : deptId;
        String parent = !userId.isEmpty() ? "user" : "dept";
        Map<String, Object> group = firstRow(
            "SELECT * FROM " + EVENT_GROUP + " WHERE tenant_id = ? AND name = ? AND puid = ? LIMIT 1",
            user.getTenantId(), name, parent);
        if (group == null) {
            return error(EVENT_GROUP_NOT_FOUND, "没找到事件分组记录");
        }
        updateAutoToWork(EVENT_GROUP, "uid", string(group.get("uid")), group.get("data"),
            booleanValue(body, "auto_to_work", false));
        return LocationTaskResult.success();
    }

    /**
     * Upsert a V2 application-group workflow configuration.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> eventGroupSettingSaveV2(Map<String, Object> body, UserContext user) {
        AuthorizationResult authorization = authorizeV2Group(body, user);
        if (authorization.getError() != null) {
            return authorization.getError();
        }
        Map<String, Object> config = body.get("config") instanceof Map
            ? castMap(body.get("config")) : new LinkedHashMap<String, Object>();
        Map<String, Object> existing = firstRow(
            "SELECT * FROM " + TABLE_GROUP_CONFIG_V2 + " WHERE group_uid = ? AND app_id = ? LIMIT 1",
            string(authorization.getGroup().get("uid")), authorization.getAppId());
        LinkedHashMap<String, Object> data = existing == null
            ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(jsonMap(existing.get("data")));
        data.put("config", workflowConfigData(config, user));
        if (existing == null) {
            jdbc.update("INSERT INTO " + TABLE_GROUP_CONFIG_V2
                    + " (`id`,`app_id`,`group_uid`,`uid_path`,`data`,`created_at`,`updated_at`) "
                    + "VALUES (?,?,?,?,CAST(? AS JSON),NOW(),NOW())",
                IdUtil.getSnowflakeNextIdStr(), authorization.getAppId(),
                string(authorization.getGroup().get("uid")), string(authorization.getGroup().get("uid_path")),
                toJson(data));
        } else {
            jdbc.update("UPDATE " + TABLE_GROUP_CONFIG_V2
                    + " SET data = CAST(? AS JSON), updated_at = NOW() WHERE id = ?",
                toJson(data), string(existing.get("id")));
        }
        return LocationTaskResult.success();
    }

    /**
     * Update a V2 application-group automatic-work-order flag.
     */
    @Transactional(rollbackFor = Exception.class)
    public LocationTaskResult<?> eventGroupStatusV2(Map<String, Object> body, UserContext user) {
        AuthorizationResult authorization = authorizeV2Group(body, user);
        if (authorization.getError() != null) {
            return authorization.getError();
        }
        Map<String, Object> config = firstRow(
            "SELECT * FROM " + TABLE_GROUP_CONFIG_V2 + " WHERE group_uid = ? AND app_id = ? LIMIT 1",
            string(authorization.getGroup().get("uid")), authorization.getAppId());
        if (config == null) {
            return error(EVENT_GROUP_NOT_FOUND, "没有配置记录,请先配置");
        }
        updateAutoToWork(TABLE_GROUP_CONFIG_V2, "id", string(config.get("id")), config.get("data"),
            booleanValue(body, "auto_to_work", false));
        return LocationTaskResult.success();
    }

    /**
     * Calculate event statistics using the original view, grouping, and summary rules.
     */
    public LocationTaskResult<?> eventStatistics(Map<String, Object> body, UserContext user) {
        String userType = stringValue(body, "user_type");
        if (!userType.isEmpty() && !"dept".equals(userType) && !"user".equals(userType)) {
            return parameterError("参数错误 user_type不正确");
        }
        String granularity = stringValue(body, "time_granularity");
        if (granularity.isEmpty()) {
            granularity = "day";
        }
        if (!Arrays.asList("year", "month", "day").contains(granularity)) {
            return parameterError("参数错误 time_granularity不正确");
        }
        LocationTaskResult<?> timeError = validateStatisticsTime(body);
        if (timeError != null) {
            return timeError;
        }

        String timeExpression = "DATE_FORMAT(te.created_at,'%Y-%m-%d')";
        if ("year".equals(granularity)) {
            timeExpression = "DATE_FORMAT(te.created_at,'%Y')";
        } else if ("month".equals(granularity)) {
            timeExpression = "DATE_FORMAT(te.created_at,'%Y-%m')";
        }
        boolean byUser = "user".equals(userType);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
            .append(byUser ? "u.user_id" : deptKey("d")).append(" AS id,")
            .append(byUser ? "COALESCE(NULLIF(u.nick_name,''),u.user_name)" : "d.dept_name")
            .append(" AS name,")
            .append(timeExpression).append(" AS time_key,")
            .append("COUNT(te.id) AS total_count,")
            .append("SUM(CASE WHEN te.status = 1 THEN 1 ELSE 0 END) AS completed_count,")
            .append("SUM(CASE WHEN te.status = 2 THEN 1 ELSE 0 END) AS uncompleted_count,")
            .append("ROUND(IF(COUNT(te.id) > 0, SUM(CASE WHEN te.status = 1 THEN 1 ELSE 0 END) "
                + "* 100.0 / COUNT(te.id), 0), 2) AS completion_rate,")
            .append("ROUND(IF(SUM(CASE WHEN te.status = 1 THEN 1 ELSE 0 END) > 0, "
                + "TIMESTAMPDIFF(SECOND, ANY_VALUE(te.created_at), "
                + "ANY_VALUE(STR_TO_DATE(JSON_UNQUOTE(JSON_EXTRACT(te.data,'$.finish_at')),"
                + "'%Y-%m-%d %H:%i:%s'))) / "
                + "SUM(CASE WHEN te.status = 1 THEN 1 ELSE 0 END), 0), 2) AS avg_completion_time ")
            .append("FROM ").append(byUser ? USER + " u " : DEPT + " d ");
        if (byUser) {
            sql.append("LEFT JOIN ").append(DEPT)
                .append(" d ON d.dept_id = u.dept_id AND d.tenant_id = u.tenant_id ");
        } else {
            sql.append("LEFT JOIN ").append(USER)
                .append(" u ON u.dept_id = d.dept_id AND u.tenant_id = d.tenant_id ");
        }
        sql.append("LEFT JOIN ").append(DEPT)
            .append(" p ON p.dept_id = d.parent_id AND p.tenant_id = d.tenant_id ");

        List<Object> joinArgs = new ArrayList<Object>();
        List<String> eventFilters = new ArrayList<String>();
        int modType = intValue(body, "mod_type", 0);
        if (modType > 0) {
            eventFilters.add("te.mod_type = ?");
            joinArgs.add(modType);
        }
        String startTime = stringValue(body, "start_time");
        String endTime = stringValue(body, "end_time");
        if (!startTime.isEmpty()) {
            eventFilters.add("te.created_at >= ?");
            joinArgs.add(startTime);
        }
        if (!endTime.isEmpty()) {
            eventFilters.add("te.created_at <= ?");
            joinArgs.add(endTime);
        }
        String eventJoinConditions = eventFilters.isEmpty() ? "" : " AND " + join(eventFilters, " AND ");
        if (intValue(body, "stat_type", 0) == 2) {
            sql.append("LEFT JOIN ").append(EVENT)
                .append(" te ON te.uuid = u.user_id AND te.tenant_id = d.tenant_id")
                .append(eventJoinConditions).append(' ');
        } else {
            sql.append("LEFT JOIN ").append(EVENT_USER)
                .append(" teu ON teu.uuid = u.user_id AND teu.tenant_id = d.tenant_id ")
                .append("LEFT JOIN ").append(EVENT)
                .append(" te ON te.id = teu.task_event_id AND teu.tenant_id = te.tenant_id")
                .append(eventJoinConditions).append(' ');
        }
        sql.append("WHERE d.tenant_id = ? AND COALESCE(d.del_flag,'0') = '0' ")
            .append("AND ((? = '' AND COALESCE(d.parent_id,0) = 0) OR ")
            .append(deptKey("p")).append(" = ?) GROUP BY ")
            .append(byUser
                ? "u.user_id,COALESCE(NULLIF(u.nick_name,''),u.user_name),"
                : deptKey("d") + ",d.dept_name,")
            .append(timeExpression);
        joinArgs.add(user.getTenantId());
        String statisticsDeptId = stringValue(body, "dept_id");
        joinArgs.add(statisticsDeptId);
        joinArgs.add(statisticsDeptId);

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), joinArgs.toArray());
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        long total = 0L;
        long completed = 0L;
        long uncompleted = 0L;
        double weightedAverage = 0D;
        long completedForAverage = 0L;
        for (Map<String, Object> row : rows) {
            LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
            long rowTotal = number(row.get("total_count")).longValue();
            long rowCompleted = number(row.get("completed_count")).longValue();
            long rowUncompleted = number(row.get("uncompleted_count")).longValue();
            double average = number(row.get("avg_completion_time")).doubleValue();
            value.put("id", string(row.get("id")));
            value.put("name", string(row.get("name")));
            value.put("time_key", string(row.get("time_key")));
            value.put("total_count", rowTotal);
            value.put("completed_count", rowCompleted);
            value.put("uncompleted_count", rowUncompleted);
            value.put("completion_rate", number(row.get("completion_rate")).doubleValue());
            value.put("avg_completion_time", average);
            list.add(value);
            total += rowTotal;
            completed += rowCompleted;
            uncompleted += rowUncompleted;
            if (average > 0D && rowCompleted > 0L) {
                weightedAverage += average * rowCompleted;
                completedForAverage += rowCompleted;
            }
        }
        LinkedHashMap<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total_count", total);
        summary.put("completed_count", completed);
        summary.put("uncompleted_count", uncompleted);
        summary.put("completion_rate", total > 0L ? completed * 100D / total : 0D);
        summary.put("avg_completion_time",
            completedForAverage > 0L ? weightedAverage / completedForAverage : 0D);
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("list", list);
        result.put("summary", summary);
        return LocationTaskResult.success(result);
    }

    /**
     * Schedule the Go AfterCreate automatic-work-order hook after the event commits.
     */
    private void scheduleAutomaticWorkOrder(final EventSnapshot event) {
        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                /** Launch the fire-and-forget hook only after the event row is committed. */
                @Override
                public void afterCommit() {
                    launchAutomaticWorkOrder(event, requestAttributes);
                }
            });
            return;
        }
        launchAutomaticWorkOrder(event, requestAttributes);
    }

    /**
     * Launch the original fire-and-forget work-order hook on a worker thread.
     */
    private void launchAutomaticWorkOrder(final EventSnapshot event,
                                          final RequestAttributes requestAttributes) {
        CompletableFuture.runAsync(new Runnable() {
            /** Execute the asynchronous hook while retaining the originating request headers. */
            @Override
            public void run() {
                runAutomaticWorkOrder(event, requestAttributes);
            }
        });
    }

    /**
     * Find every matching configuration, start its workflow, and save the first success.
     */
    private void runAutomaticWorkOrder(EventSnapshot event, RequestAttributes requestAttributes) {
        RequestAttributes previous = RequestContextHolder.getRequestAttributes();
        try {
            if (requestAttributes != null) {
                RequestContextHolder.setRequestAttributes(requestAttributes);
            }
            if (event.pictures.isEmpty()) {
                return;
            }
            List<Map<String, Object>> configurations = automaticWorkOrderConfigurations(event);
            boolean saved = false;
            for (Map<String, Object> configuration : configurations) {
                try {
                    WorkOrder workflowResult = startWorkflow(event, configuration);
                    if (!saved) {
                        Map<String, Object> workOrderData = workflowResult == null
                            ? new LinkedHashMap<String, Object>()
                            : objectMapper.convertValue(workflowResult, MAP_TYPE);
                        jdbc.update("UPDATE " + EVENT
                                + " SET data = JSON_SET(data,'$.work_order_data',CAST(? AS JSON)),"
                                + " mod_status = 1, work_order_status = 1, updated_at = NOW()"
                                + " WHERE id = ? AND `no` = ?",
                            toJson(workOrderData), event.key.getId(), event.key.getNo());
                        saved = true;
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("Automatic work-order start failed for event {}",
                        event.key.externalId(), exception);
                }
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Automatic work-order lookup failed for event {}", event.key.externalId(), exception);
        } finally {
            if (previous == null) {
                RequestContextHolder.resetRequestAttributes();
            } else {
                RequestContextHolder.setRequestAttributes(previous);
            }
        }
    }

    /**
     * Resolve event-item first, then the event-category-specific configuration hierarchy.
     */
    private List<Map<String, Object>> automaticWorkOrderConfigurations(EventSnapshot event) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Map<String, Object> item = firstRow("SELECT data FROM " + EVENT_ITEM
                + " WHERE tenant_id = ? AND mod_type = ? AND item = ? LIMIT 1",
            event.tenantId, event.modType, event.item);
        Map<String, Object> configuration = enabledWorkflowConfiguration(item == null ? null : item.get("data"));
        if (configuration != null) {
            result.add(configuration);
            return result;
        }

        if (event.modType == 2) {
            result.addAll(activeSafetyWorkOrderConfigurations(event));
            if (!result.isEmpty()) {
                return result;
            }
            addEnabledGlobalWorkflowConfiguration(result, event.tenantId, 2, 1);
            addEnabledGlobalWorkflowConfiguration(result, event.tenantId, 2, 2);
            return result;
        }

        Map<String, Object> userGroup = firstRow("SELECT data FROM " + EVENT_GROUP
                + " WHERE tenant_id = ? AND name = ? AND puid = 'user' LIMIT 1",
            event.tenantId, event.userId);
        configuration = enabledWorkflowConfiguration(userGroup == null ? null : userGroup.get("data"));
        if (configuration != null) {
            result.add(configuration);
            return result;
        }

        List<Map<String, Object>> departments = jdbc.queryForList(
            "SELECT d.dept_id,d.ancestors FROM " + USER + " u JOIN " + DEPT
                + " d ON d.dept_id = u.dept_id AND d.tenant_id = u.tenant_id WHERE u.user_id = ?"
                + " AND u.tenant_id = ? AND COALESCE(u.del_flag,'0') = '0'",
            event.userId, event.tenantId);
        for (Map<String, Object> department : departments) {
            Map<String, Object> departmentConfiguration = nearestDepartmentConfiguration(
                event.tenantId, string(department.get("dept_id")), string(department.get("ancestors")));
            if (departmentConfiguration != null) {
                result.add(departmentConfiguration);
            }
        }
        return result;
    }

    /**
     * Resolve direct device groups first and nearest configured ancestors second.
     */
    private List<Map<String, Object>> activeSafetyWorkOrderConfigurations(EventSnapshot event) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (event.deviceId.isEmpty()) {
            return result;
        }
        List<Map<String, Object>> groups = jdbc.queryForList(
            "SELECT DISTINCT g.uid,g.uid_path FROM " + TABLE_GROUP_V2 + " g JOIN "
                + OLD_APP_GROUP_DEVICE + " gd ON gd.uid = g.uid WHERE g.tenant_id = ? AND gd.device_id = ?",
            event.tenantId, event.deviceId);
        if (groups.isEmpty()) {
            return result;
        }

        List<String> groupIds = new ArrayList<String>();
        for (Map<String, Object> group : groups) {
            groupIds.add(string(group.get("uid")));
        }
        result.addAll(enabledGroupWorkflowConfigurations(groupIds));
        if (!result.isEmpty()) {
            return result;
        }

        for (Map<String, Object> group : groups) {
            Map<String, Object> parentConfiguration = nearestGroupWorkflowConfiguration(
                string(group.get("uid")), string(group.get("uid_path")));
            if (parentConfiguration != null) {
                result.add(parentConfiguration);
            }
        }
        return result;
    }

    /**
     * Return all enabled application workflow configurations attached to the supplied groups.
     */
    private List<Map<String, Object>> enabledGroupWorkflowConfigurations(List<String> groupIds) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (groupIds.isEmpty()) {
            return result;
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT data FROM " + TABLE_GROUP_CONFIG_V2 + " WHERE group_uid IN ("
                + placeholders(groupIds.size()) + ")",
            groupIds.toArray());
        for (Map<String, Object> row : rows) {
            Map<String, Object> configuration = enabledWorkflowConfiguration(row.get("data"));
            if (configuration != null) {
                result.add(configuration);
            }
        }
        return result;
    }

    /**
     * Walk one group UID path from the nearest parent upward and return the first enabled configuration.
     */
    private Map<String, Object> nearestGroupWorkflowConfiguration(String groupId, String uidPath) {
        List<String> path = pathSegments(uidPath);
        if (!path.isEmpty() && groupId.equals(path.get(path.size() - 1))) {
            path.remove(path.size() - 1);
        }
        if (path.isEmpty()) {
            return null;
        }
        List<Object> args = new ArrayList<Object>(path);
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT group_uid,data FROM " + TABLE_GROUP_CONFIG_V2 + " WHERE group_uid IN ("
                + placeholders(path.size()) + ")",
            args.toArray());
        for (int index = path.size() - 1; index >= 0; index--) {
            for (Map<String, Object> row : rows) {
                if (path.get(index).equals(string(row.get("group_uid")))) {
                    Map<String, Object> configuration = enabledWorkflowConfiguration(row.get("data"));
                    if (configuration != null) {
                        return configuration;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Append one enabled global workflow configuration stored by the settings page.
     */
    private void addEnabledGlobalWorkflowConfiguration(List<Map<String, Object>> target, String tenantId,
                                                       int modType, int groupType) {
        Map<String, Object> setting = firstRow("SELECT `val` FROM " + SETTINGS + " WHERE `key` = ? LIMIT 1",
            workflowSettingKey(tenantId, modType, groupType));
        Map<String, Object> configuration = enabledWorkflowConfiguration(
            setting == null ? null : setting.get("val"));
        if (configuration != null) {
            target.add(configuration);
        }
    }

    /**
     * Return the closest enabled department configuration along one department path.
     */
    private Map<String, Object> nearestDepartmentConfiguration(String tenantId, String departmentId,
                                                               String ancestorIds) {
        List<String> departmentIds = commaSegments(ancestorIds);
        if (!departmentId.isEmpty()) {
            departmentIds.add(departmentId);
        }
        if (departmentIds.isEmpty()) {
            return null;
        }
        List<Object> args = new ArrayList<Object>();
        args.add(tenantId);
        args.addAll(departmentIds);
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT eg.data,d.dept_id FROM " + EVENT_GROUP + " eg JOIN " + DEPT
                + " d ON eg.name = " + deptKey("d")
                + " AND eg.puid = 'dept' AND eg.tenant_id = d.tenant_id WHERE d.tenant_id = ?"
                + " AND d.dept_id IN (" + placeholders(departmentIds.size()) + ")",
            args.toArray());
        for (int index = departmentIds.size() - 1; index >= 0; index--) {
            for (Map<String, Object> row : rows) {
                if (departmentIds.get(index).equals(string(row.get("dept_id")))) {
                    Map<String, Object> configuration = enabledWorkflowConfiguration(row.get("data"));
                    if (configuration != null) {
                        return configuration;
                    }
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Extract an enabled ConfigData object and apply the Go navigation defaults.
     */
    private Map<String, Object> enabledWorkflowConfiguration(Object rawData) {
        Map<String, Object> data = jsonMap(rawData);
        Map<String, Object> configuration = nestedMap(data, "config");
        if (configuration.isEmpty() && hasWorkflowConfig(data)) {
            configuration = data;
        }
        if (!booleanValue(configuration, "auto_to_work", false)) {
            return null;
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>(configuration);
        applyWorkflowDefaults(copy);
        return copy;
    }

    /**
     * Start one configured workflow using the same variables sent by the Go service.
     */
    private WorkOrder startWorkflow(EventSnapshot event, Map<String, Object> configuration) {
        String tenantId = string(configuration.get("tenant_id"));
        String userId = string(configuration.get("user_id"));
        String processId = string(configuration.get("process_id"));
        String appId = string(configuration.get("app_id"));
        if (tenantId.isEmpty() || userId.isEmpty() || processId.isEmpty() || appId.isEmpty()) {
            throw new IllegalArgumentException("没有获取到流程中心的配置参数");
        }

        LinkedHashMap<String, Object> variables = new LinkedHashMap<String, Object>();
        variables.put("no", event.key.externalId());
        variables.put("item", event.item);
        variables.put("name", event.name);
        variables.put("time", DATE_TIME_FORMAT.format(LocalDateTime.now()));
        variables.put("address", string(event.point.get("address")));
        variables.put("describe", event.describe);
        variables.put("image", workflowImages(event.pictures));
        variables.put("app_package", string(configuration.get("app_package")));
        variables.put("jump_path", string(configuration.get("jump_path")));
        variables.put("jump_params", string(configuration.get("jump_params"))
            .replace("{{event_id}}", event.key.externalId()));

        WorkOrderBo workOrder = new WorkOrderBo();
        workOrder.setEventNumber(event.key.externalId());
        ProcessStartBo request = new ProcessStartBo();
        request.setProcessDefId(processId);
        request.setAutoGetFormFlag(true);
        request.setAppId(appId);
        request.setEventName(event.name);
        request.setWorkOrderBo(workOrder);
        request.setFrontFlag(true);
        request.setVariables(variables);

        SysUser workflowUser = new SysUser();
        workflowUser.setTenantId(tenantId);
        workflowUser.setUserId(userId);
        workflowUser.setUserName(userId);
        workflowUser.setLoginId(userId);
        processService.startProcessByDefId(request, workflowUser);
        return request.getWorkOrder();
    }

    /**
     * Convert event picture URLs to the workflow image objects built by Go.
     */
    private static List<Map<String, Object>> workflowImages(List<String> pictures) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (String picture : pictures) {
            String normalized = picture.replace('\\', '/');
            int separator = normalized.lastIndexOf('/');
            LinkedHashMap<String, Object> image = new LinkedHashMap<String, Object>();
            image.put("name", separator < 0 ? normalized : normalized.substring(separator + 1));
            image.put("url", picture);
            result.add(image);
        }
        return result;
    }

    /**
     * Build the public department identifier used by legacy payloads from the canonical sys_dept row.
     */
    private static String deptKey(String alias) {
        return "COALESCE(NULLIF(" + alias + ".oort_udid,''),CAST(" + alias
            + ".dept_id AS CHAR)) COLLATE utf8mb4_general_ci";
    }

    /**
     * Build the legacy department-code value while supporting departments created by a clean open-source install.
     */
    private static String deptCode(String alias) {
        return "COALESCE(NULLIF(" + alias + ".oort_dcode,''),CAST(" + alias
            + ".dept_id AS CHAR)) COLLATE utf8mb4_general_ci";
    }

    /**
     * Split the numeric comma-delimited sys_dept ancestor chain and discard the RuoYi root sentinel.
     */
    private static List<String> commaSegments(String value) {
        List<String> result = new ArrayList<String>();
        if (value == null) {
            return result;
        }
        for (String segment : value.split(",")) {
            String normalized = segment.trim();
            if (!normalized.isEmpty() && !"0".equals(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    /**
     * Split a slash-delimited Go uid or department path into non-empty segments.
     */
    private static List<String> pathSegments(String path) {
        List<String> result = new ArrayList<String>();
        if (path == null) {
            return result;
        }
        for (String segment : path.split("/")) {
            if (!segment.isEmpty()) {
                result.add(segment);
            }
        }
        return result;
    }

    /**
     * Validate application and group authorization for a V2 write operation.
     */
    private AuthorizationResult authorizeV2Group(Map<String, Object> body, UserContext user) {
        String uid = stringValue(body, "uid");
        String appId = stringValue(body, "app_id");
        if (uid.isEmpty() || appId.isEmpty()) {
            return AuthorizationResult.error(parameterError("参数错误 uid和app_id不能为空"));
        }
        if (!appExists(user.getTenantId(), appId)) {
            return AuthorizationResult.error(error(NOT_FOUND, "没找到记录"));
        }
        List<Map<String, Object>> grants = appGroupGrants(user.getTenantId(), appId);
        if (grants.isEmpty()) {
            return AuthorizationResult.error(error(GROUP_APP_NOT_AUTHORIZED, "应用分组没有权限"));
        }
        Map<String, Object> group = findGroupV2(uid);
        if (group == null) {
            return AuthorizationResult.error(error(GROUP_NOT_FOUND, "没找到分组记录"));
        }
        if (!isGroupAuthorized(group, grants)) {
            return AuthorizationResult.error(error(GROUP_APP_NOT_AUTHORIZED, "应用分组没有权限"));
        }
        return AuthorizationResult.success(appId, group);
    }

    /**
     * Determine whether an application exists in the current tenant.
     */
    private boolean appExists(String tenantId, String appId) {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + APP
                + " WHERE tenant_id = ? AND application_id = ? AND COALESCE(del_flag,'0') = '0'",
            Long.class, tenantId, appId);
        return count != null && count > 0L;
    }

    /**
     * Load all group roots authorized to an application.
     */
    private List<Map<String, Object>> appGroupGrants(String tenantId, String appId) {
        return jdbc.queryForList(
            "SELECT * FROM " + TABLE_GROUP_APP_V2 + " WHERE tenant_id = ? AND app_id = ?",
            tenantId, appId);
    }

    /**
     * Load one V2 group by UID.
     */
    private Map<String, Object> findGroupV2(String uid) {
        return firstRow("SELECT * FROM " + TABLE_GROUP_V2 + " WHERE uid = ? LIMIT 1", uid);
    }

    /**
     * Check whether a group path is within one of the application's authorized roots.
     */
    private boolean isGroupAuthorized(Map<String, Object> group, List<Map<String, Object>> grants) {
        String path = string(group.get("uid_path"));
        for (Map<String, Object> grant : grants) {
            if (path.startsWith(string(grant.get("uid_path")))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update data.config.auto_to_work while preserving every other JSON member.
     */
    private void updateAutoToWork(String table, String keyColumn, String key, Object rawData, boolean value) {
        Map<String, Object> data = jsonMap(rawData);
        Map<String, Object> config = nestedMap(data, "config");
        if (booleanValue(config, "auto_to_work", false) != value) {
            config.put("auto_to_work", value);
            data.put("config", config);
            jdbc.update("UPDATE " + table + " SET data = CAST(? AS JSON), updated_at = NOW() WHERE `"
                    + keyColumn + "` = ?",
                toJson(data), key);
        }
    }

    /**
     * Save a setting by its primary key using MySQL's original JSON value column.
     */
    private void upsertSetting(String key, Map<String, Object> value) {
        String json = toJson(value);
        int updated = jdbc.update("UPDATE " + SETTINGS + " SET `val` = CAST(? AS JSON) WHERE `key` = ?", json, key);
        if (updated == 0) {
            jdbc.update("INSERT INTO " + SETTINGS + " (`key`,`val`) VALUES (?,CAST(? AS JSON))", key, json);
        }
    }

    /**
     * Build the settings key, including the parameter-order behavior of the Go implementation.
     */
    private static String workflowSettingKey(String tenantId, int modType, int groupType) {
        String key = "workflow_v2_config:" + tenantId;
        if (modType > 0 || groupType > 0) {
            key = key + ":" + groupType + ":" + modType;
        }
        return key;
    }

    /**
     * Copy the six workflow configuration fields from a request object.
     */
    private static LinkedHashMap<String, Object> workflowConfig(Map<String, Object> source) {
        LinkedHashMap<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("process_id", string(source.get("process_id")));
        config.put("app_id", string(source.get("app_id")));
        config.put("app_package", string(source.get("app_package")));
        config.put("jump_path", string(source.get("jump_path")));
        config.put("jump_params", string(source.get("jump_params")));
        config.put("auto_to_work", booleanValue(source, "auto_to_work", false));
        return config;
    }

    /**
     * Build a stored ConfigData object with tenant and user identity.
     */
    private static LinkedHashMap<String, Object> workflowConfigData(Map<String, Object> source, UserContext user) {
        LinkedHashMap<String, Object> config = workflowConfig(source);
        config.put("tenant_id", user.getTenantId());
        config.put("user_id", user.getUserId());
        return config;
    }

    /**
     * Return only the public workflow Config fields from stored ConfigData.
     */
    private static LinkedHashMap<String, Object> workflowConfigOnly(Map<String, Object> source) {
        LinkedHashMap<String, Object> config = workflowConfig(source);
        applyWorkflowDefaults(config);
        return config;
    }

    /**
     * Apply the same default navigation values as workflow.Config.GetDefault.
     */
    private static void applyWorkflowDefaults(Map<String, Object> config) {
        putIfMissing(config, "process_id", "");
        putIfMissing(config, "app_id", "");
        putIfMissing(config, "auto_to_work", Boolean.FALSE);
        if (string(config.get("jump_params")).isEmpty()) {
            config.put("jump_params", "task={\"id\":\"{{event_id}}\"}");
        }
        if (string(config.get("jump_path")).isEmpty()) {
            config.put("jump_path", "/event-detail");
        }
        if (string(config.get("app_package")).isEmpty()) {
            config.put("app_package", "com.oort-event.demo");
        }
    }

    /**
     * Put a default only when a map lacks a key or contains null.
     */
    private static void putIfMissing(Map<String, Object> target, String key, Object value) {
        if (!target.containsKey(key) || target.get(key) == null) {
            target.put(key, value);
        }
    }

    /**
     * Determine whether stored ConfigData contains an allocated workflow Config.
     */
    private static boolean hasWorkflowConfig(Map<String, Object> config) {
        return config.containsKey("process_id") || config.containsKey("app_id")
            || config.containsKey("app_package") || config.containsKey("jump_path")
            || config.containsKey("jump_params") || config.containsKey("auto_to_work");
    }

    /**
     * Convert one V2 group database row to its Go JSON representation.
     */
    private Map<String, Object> toGroupV2(Map<String, Object> row) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("uid", string(row.get("uid")));
        value.put("tenant_id", string(row.get("tenant_id")));
        value.put("group_type", number(row.get("group_type")).intValue());
        value.put("name", string(row.get("name")));
        value.put("puid", string(row.get("puid")));
        value.put("uid_path", string(row.get("uid_path")));
        value.put("sort", number(row.get("sort")).intValue());
        value.put("remark", string(row.get("remark")));
        appendModelTimes(value, row);
        Map<String, Object> configData = nestedMap(jsonMap(row.get("conf_data")), "config");
        if (hasWorkflowConfig(configData)) {
            value.put("config", workflowConfigOnly(configData));
        }
        return value;
    }

    /**
     * Convert one event-item database row to its Go JSON representation.
     */
    private Map<String, Object> toEventItem(Map<String, Object> row) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("uid", string(row.get("uid")));
        value.put("item", string(row.get("item")));
        Map<String, Object> configData = nestedMap(jsonMap(row.get("data")), "config");
        if (hasWorkflowConfig(configData)) {
            value.put("config", workflowConfigOnly(configData));
        }
        value.put("remark", string(row.get("remark")));
        appendModelTimes(value, row);
        return value;
    }

    /**
     * Convert one event feedback database row to its Go JSON representation.
     */
    private Map<String, Object> toEventBack(Map<String, Object> row) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("id", string(row.get("id")));
        value.put("task_event_id", string(row.get("task_event_id")));
        value.put("uuid", string(row.get("uuid")));
        Map<String, Object> data = jsonMap(row.get("data"));
        value.put("describe", string(data.get("describe")));
        value.put("point", pointValue(data.get("point")));
        value.put("pics", data.containsKey("pics") ? data.get("pics") : null);
        appendModelTimes(value, row);
        return value;
    }

    /**
     * Convert one event database row and expand its JSON and executor relations.
     */
    private Map<String, Object> toEvent(Map<String, Object> row) {
        String rawId = string(row.get("id"));
        long no = number(row.get("no")).longValue();
        String externalId = no == 0L ? rawId : rawId + "-" + no;
        LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("id", externalId);
        value.put("tenant_id", string(row.get("tenant_id")));
        value.put("uuid", string(row.get("uuid")));
        String item = string(row.get("item"));
        String name = string(row.get("name"));
        value.put("name", name.isEmpty() ? item : name);
        Map<String, Object> data = jsonMap(row.get("data"));
        value.put("describe", string(data.get("describe")));
        value.put("point", pointValue(data.get("point")));
        value.put("pics", distinctStrings(data.get("pics"), true));
        value.put("send_pics", distinctStrings(data.get("send_pics"), false));
        value.put("video", data.containsKey("video") ? data.get("video") : null);
        if (data.get("work_order_data") instanceof Map && !((Map<?, ?>) data.get("work_order_data")).isEmpty()) {
            value.put("work_order_data", data.get("work_order_data"));
        }
        if (data.get("work_order_call_back") instanceof Map
            && !((Map<?, ?>) data.get("work_order_call_back")).isEmpty()) {
            value.put("work_order_call_back", data.get("work_order_call_back"));
        }
        String updatedAt = formatDate(row.get("updated_at"));
        String finishAt = string(data.get("finish_at"));
        value.put("finish_at", finishAt.isEmpty() ? updatedAt : finishAt);
        value.put("status", number(row.get("status")).intValue());
        value.put("item", item);
        value.put("client", string(row.get("client")));
        value.put("uuids", eventUsers(externalId));
        value.put("mod_type", number(row.get("mod_type")).intValue());
        value.put("mod_status", number(row.get("mod_status")).intValue());
        appendIfNotEmpty(value, "device_id", row.get("device_id"));
        appendIfNotEmpty(value, "device_name", row.get("device_name"));
        appendIfNotEmpty(value, "device_tag", row.get("device_tag"));
        value.put("work_order_status", number(row.get("work_order_status")).intValue());
        value.put("created_at", formatDate(row.get("created_at")));
        value.put("updated_at", updatedAt);
        value.put("pic_len", number(row.get("pic_len")).longValue());
        return value;
    }

    /**
     * Append a non-empty string field to an output map.
     */
    private static void appendIfNotEmpty(Map<String, Object> target, String key, Object value) {
        String text = string(value);
        if (!text.isEmpty()) {
            target.put(key, text);
        }
    }

    /**
     * Load the executor array returned inside each event.
     */
    private List<Map<String, Object>> eventUsers(String eventId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT uuid,u_type FROM " + EVENT_USER + " WHERE task_event_id = ? ORDER BY created_at ASC",
            eventId);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            LinkedHashMap<String, Object> executor = new LinkedHashMap<String, Object>();
            executor.put("uuid", string(row.get("uuid")));
            executor.put("u_type", number(row.get("u_type")).intValue());
            result.add(executor);
        }
        return result;
    }

    /**
     * Append sqlbase.Model timestamps to a response object.
     */
    private static void appendModelTimes(Map<String, Object> target, Map<String, Object> row) {
        target.put("created_at", formatDate(row.get("created_at")));
        target.put("updated_at", formatDate(row.get("updated_at")));
    }

    /**
     * Load and expand one active event by its external composite ID.
     */
    private Map<String, Object> findEvent(String externalId) {
        Map<String, Object> raw = findEventRaw(externalId);
        return raw == null ? null : toEvent(raw);
    }

    /**
     * Load one active event row by its external composite ID.
     */
    private Map<String, Object> findEventRaw(String externalId) {
        EventKey key = EventKey.parse(externalId);
        return firstRow("SELECT * FROM " + EVENT
                + " WHERE id = ? AND `no` = ? AND deleted_at = 0 LIMIT 1",
            key.getId(), key.getNo());
    }

    /**
     * Build a page payload and expand every event row.
     */
    private Map<String, Object> pagedEvents(String from, List<String> where, List<Object> args,
                                             int page, int pageSize) {
        String condition = where.isEmpty() ? "" : " WHERE " + join(where, " AND ");
        Long count = jdbc.queryForObject("SELECT COUNT(*)" + from + condition, Long.class, args.toArray());
        List<Object> queryArgs = new ArrayList<Object>(args);
        queryArgs.add(pageSize);
        queryArgs.add((page - 1) * pageSize);
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT te.*" + from + condition + " ORDER BY te.created_at DESC LIMIT ? OFFSET ?",
            queryArgs.toArray());
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            list.add(toEvent(row));
        }
        return pagePayload(page, pageSize, count == null ? 0L : count, list);
    }

    /**
     * Build the legacy page object returned by the Go page helper.
     */
    private static Map<String, Object> pagePayload(int page, int pageSize, long count, List<?> list) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("page", page);
        payload.put("pages", pageSize == 0 ? 0L : (count + pageSize - 1L) / pageSize);
        payload.put("pagesize", pageSize);
        payload.put("count", count);
        payload.put("list", list);
        return payload;
    }

    /**
     * Build a data object containing only a list member.
     */
    private static Map<String, Object> singleListPayload(List<?> list) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("list", list);
        return payload;
    }

    /**
     * Return the first row of a query or null when no row exists.
     */
    private Map<String, Object> firstRow(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * Build the next event composite key using the original daily per-user sequence.
     */
    private EventKey nextEventKey(String userName) {
        String prefix = eventIdPrefix(userName) + "-" + new SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(new Date());
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + EVENT + " WHERE id = ?", Long.class, prefix);
        return new EventKey(prefix, (count == null ? 0L : count) + 1L);
    }

    /**
     * Create a stable identifier prefix for the user's display name.
     */
    private static String eventIdPrefix(String userName) {
        String source = userName == null ? "" : userName.trim().toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < source.length();) {
            int codePoint = source.codePointAt(offset);
            if (Character.isLetterOrDigit(codePoint)) {
                result.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        return result.length() == 0 ? "event" : result.toString();
    }

    /**
     * Append an equality condition when a request string is not empty.
     */
    private static void appendEquals(List<String> where, List<Object> args, Map<String, Object> body,
                                     String requestKey, String column) {
        String value = stringValue(body, requestKey);
        if (!value.isEmpty()) {
            where.add(column + " = ?");
            args.add(value);
        }
    }

    /**
     * Append an equality condition when a request integer is positive.
     */
    private static void appendPositiveEquals(List<String> where, List<Object> args,
                                             Map<String, Object> body, String requestKey, String column) {
        int value = intValue(body, requestKey, 0);
        if (value > 0) {
            where.add(column + " = ?");
            args.add(value);
        }
    }

    /**
     * Append the original three-state picture-count filter.
     */
    private static void appendPictureFilter(List<String> where, Map<String, Object> body, String column) {
        int value = intValue(body, "had_pic", 0);
        if (value == 1) {
            where.add(column + " > 0");
        } else if (value > 1) {
            where.add(column + " = 0");
        }
    }

    /**
     * Validate and append a request date range.
     */
    private static LocationTaskResult<?> appendDateRange(List<String> where, List<Object> args,
                                                         Map<String, Object> body, String startKey,
                                                         String endKey, String column) {
        String start = stringValue(body, startKey);
        String end = stringValue(body, endKey);
        if (!start.isEmpty() && !end.isEmpty()) {
            LocalDateTime startTime;
            LocalDateTime endTime;
            try {
                startTime = LocalDateTime.parse(start, DATE_TIME_FORMAT);
                endTime = LocalDateTime.parse(end, DATE_TIME_FORMAT);
            } catch (DateTimeParseException ex) {
                return parameterError("参数错误 时间格式不正确");
            }
            if (startTime.isAfter(endTime)) {
                return parameterError("参数错误 开始时间不能大于结束时间");
            }
            where.add(column + " BETWEEN ? AND ?");
            args.add(start);
            args.add(end);
        }
        return null;
    }

    /**
     * Validate the statistics request's independently optional time bounds.
     */
    private static LocationTaskResult<?> validateStatisticsTime(Map<String, Object> body) {
        String start = stringValue(body, "start_time");
        String end = stringValue(body, "end_time");
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (!start.isEmpty()) {
            try {
                startTime = LocalDateTime.parse(start, DATE_TIME_FORMAT);
            } catch (DateTimeParseException ex) {
                return parameterError("参数错误 开始时间格式不正确");
            }
        }
        if (!end.isEmpty()) {
            try {
                endTime = LocalDateTime.parse(end, DATE_TIME_FORMAT);
            } catch (DateTimeParseException ex) {
                return parameterError("参数错误 结束时间格式不正确");
            }
        }
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            return parameterError("参数错误 开始时间不能大于结束时间");
        }
        return null;
    }

    /**
     * Normalize a PointNew-compatible value and apply its zero-value fields.
     */
    private static Map<String, Object> pointValue(Object raw) {
        Map<?, ?> source = raw instanceof Map ? (Map<?, ?>) raw : Collections.emptyMap();
        LinkedHashMap<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("lng", number(source.get("lng")).doubleValue());
        point.put("lat", number(source.get("lat")).doubleValue());
        point.put("address", string(source.get("address")));
        point.put("coord_system_type", number(source.get("coord_system_type")).intValue());
        double lngChange = number(source.get("lng_change")).doubleValue();
        double latChange = number(source.get("lat_change")).doubleValue();
        int coordChange = number(source.get("coord_system_type_change")).intValue();
        if (lngChange != 0D) {
            point.put("lng_change", lngChange);
        }
        if (latChange != 0D) {
            point.put("lat_change", latChange);
        }
        if (coordChange != 0) {
            point.put("coord_system_type_change", coordChange);
        }
        return point;
    }

    /**
     * Parse a JSON database value into a mutable linked map.
     */
    private Map<String, Object> jsonMap(Object raw) {
        if (raw == null) {
            return new LinkedHashMap<String, Object>();
        }
        if (raw instanceof Map) {
            return new LinkedHashMap<String, Object>(castMap(raw));
        }
        String json;
        try {
            if (raw instanceof byte[]) {
                json = new String((byte[]) raw, StandardCharsets.UTF_8);
            } else if (raw instanceof Blob) {
                Blob blob = (Blob) raw;
                json = new String(blob.getBytes(1L, (int) blob.length()), StandardCharsets.UTF_8);
            } else {
                json = String.valueOf(raw);
            }
            if (json.trim().isEmpty() || "null".equals(json.trim())) {
                return new LinkedHashMap<String, Object>();
            }
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("数据库JSON解析失败", ex);
        } catch (SQLException ex) {
            throw new IllegalArgumentException("数据库JSON读取失败", ex);
        }
    }

    /**
     * Serialize a value for a MySQL JSON column.
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("JSON序列化失败", ex);
        }
    }

    /**
     * Read a nested map, returning a mutable empty map when absent.
     */
    private static Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Map
            ? new LinkedHashMap<String, Object>(castMap(value)) : new LinkedHashMap<String, Object>();
    }

    /**
     * Cast a string-keyed JSON object to its Java map representation.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    /**
     * Convert a request value to a string list without deduplication.
     */
    private static List<String> stringList(Object raw) {
        if (!(raw instanceof List)) {
            return new ArrayList<String>();
        }
        List<String> values = new ArrayList<String>();
        for (Object item : (List<?>) raw) {
            values.add(string(item));
        }
        return values;
    }

    /**
     * Deduplicate a request string list and optionally remove its first empty member.
     */
    private static List<String> distinctStrings(Object raw, boolean removeEmpty) {
        Set<String> values = new LinkedHashSet<String>(stringList(raw));
        if (removeEmpty) {
            values.remove("");
        }
        return new ArrayList<String>(values);
    }

    /**
     * Return a request string value or an empty string.
     */
    private static String stringValue(Map<String, Object> body, String key) {
        return body == null ? "" : string(body.get(key));
    }

    /**
     * Convert any value to a non-null string.
     */
    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * Return a request integer value with a default.
     */
    private static int intValue(Map<String, Object> body, String key, int defaultValue) {
        return body == null ? defaultValue : integer(body.get(key), defaultValue);
    }

    /**
     * Convert an arbitrary numeric value to an integer with a default.
     */
    private static int integer(Object value, int defaultValue) {
        if (value == null || string(value).trim().isEmpty()) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(string(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Return a positive request integer or its default.
     */
    private static int positiveInt(Map<String, Object> body, String key, int defaultValue) {
        int value = intValue(body, key, defaultValue);
        return value > 0 ? value : defaultValue;
    }

    /**
     * Convert an arbitrary value to a non-null number.
     */
    private static Number number(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value == null || string(value).trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(string(value));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Return a boolean request value with a default.
     */
    private static boolean booleanValue(Map<String, Object> body, String key, boolean defaultValue) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return defaultValue;
        }
        Object value = body.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return Boolean.parseBoolean(string(value));
    }

    /**
     * Validate a zero-or-range enum value.
     */
    private static boolean validOptionalType(int value, int max) {
        return value == 0 || value >= 1 && value <= max;
    }

    /**
     * Return the Unicode code-point length used by the Go validation helper.
     */
    private static int codePointLength(String value) {
        return value == null ? 0 : value.codePointCount(0, value.length());
    }

    /**
     * Format a JDBC timestamp using the Go response layout.
     */
    private static String formatDate(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp) {
            return DATE_TIME_FORMAT.format(((Timestamp) value).toLocalDateTime());
        }
        if (value instanceof LocalDateTime) {
            return DATE_TIME_FORMAT.format((LocalDateTime) value);
        }
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format((Date) value);
        }
        String text = string(value);
        return text.length() > 19 ? text.substring(0, 19) : text;
    }

    /**
     * Join SQL fragments with a fixed delimiter.
     */
    private static String join(List<String> values, String delimiter) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) {
                result.append(delimiter);
            }
            result.append(value);
        }
        return result.toString();
    }

    /**
     * Create a comma-separated JDBC placeholder list.
     */
    private static String placeholders(int count) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                result.append(',');
            }
            result.append('?');
        }
        return result.toString();
    }

    /**
     * Create a legacy parameter-validation error.
     */
    private static LocationTaskResult<?> parameterError(String message) {
        return LocationTaskResult.error(PARAM_ERROR, message);
    }

    /**
     * Create a legacy business error.
     */
    private static LocationTaskResult<?> error(int code, String message) {
        return LocationTaskResult.error(code, message);
    }

    /**
     * Immutable event values needed by the asynchronous Go AfterCreate hook.
     */
    private static final class EventSnapshot {
        private final EventKey key;
        private final String tenantId;
        private final String userId;
        private final String name;
        private final String item;
        private final String describe;
        private final Map<String, Object> point;
        private final List<String> pictures;
        private final int modType;
        private final String deviceId;

        /**
         * Capture committed event values without retaining the mutable request body.
         */
        private EventSnapshot(EventKey key, String tenantId, String userId, String name,
                              String item, String describe, Map<String, Object> point,
                              List<String> pictures, int modType, String deviceId) {
            this.key = key;
            this.tenantId = tenantId;
            this.userId = userId;
            this.name = name;
            this.item = item;
            this.describe = describe;
            this.point = new LinkedHashMap<String, Object>(point);
            this.pictures = new ArrayList<String>(pictures);
            this.modType = modType;
            this.deviceId = deviceId == null ? "" : deviceId;
        }
    }

    /**
     * Immutable authenticated user information passed from the compatibility controller.
     */
    public static final class UserContext {
        private final String token;
        private final String tenantId;
        private final String userId;
        private final String userName;
        private final String client;

        /**
         * Create a user context from the local Java token session.
         */
        public UserContext(String token, String tenantId, String userId, String userName, String client) {
            this.token = token;
            this.tenantId = tenantId;
            this.userId = userId;
            this.userName = userName;
            this.client = client;
        }

        /**
         * Return the accepted access token.
         */
        public String getToken() {
            return token;
        }

        /**
         * Return the authenticated tenant ID.
         */
        public String getTenantId() {
            return tenantId;
        }

        /**
         * Return the authenticated user ID.
         */
        public String getUserId() {
            return userId;
        }

        /**
         * Return the authenticated user display name.
         */
        public String getUserName() {
            return userName;
        }

        /**
         * Return the legacy login client marker.
         */
        public String getClient() {
            return client;
        }
    }

    /**
     * Parsed internal and external forms of the event composite primary key.
     */
    private static final class EventKey {
        private final String id;
        private final long no;

        /**
         * Create an event key from its database columns.
         */
        private EventKey(String id, long no) {
            this.id = id;
            this.no = no;
        }

        /**
         * Parse the exact three-segment composite ID accepted by Go.
         */
        private static EventKey parse(String externalId) {
            String[] parts = externalId == null ? new String[0] : externalId.split("-");
            if (parts.length == 3) {
                try {
                    return new EventKey(parts[0] + "-" + parts[1], Long.parseLong(parts[2]));
                } catch (NumberFormatException ignored) {
                    // Fall through to the original ID with sequence zero.
                }
            }
            return new EventKey(externalId == null ? "" : externalId, 0L);
        }

        /**
         * Return the internal ID column.
         */
        private String getId() {
            return id;
        }

        /**
         * Return the internal sequence column.
         */
        private long getNo() {
            return no;
        }

        /**
         * Return the external composite ID.
         */
        private String externalId() {
            return no == 0L ? id : id + "-" + no;
        }
    }

    /**
     * Value object carrying a successful V2 authorization or its legacy error.
     */
    private static final class AuthorizationResult {
        private final String appId;
        private final Map<String, Object> group;
        private final LocationTaskResult<?> error;

        /**
         * Create an authorization result.
         */
        private AuthorizationResult(String appId, Map<String, Object> group, LocationTaskResult<?> error) {
            this.appId = appId;
            this.group = group;
            this.error = error;
        }

        /**
         * Create a successful authorization result.
         */
        private static AuthorizationResult success(String appId, Map<String, Object> group) {
            return new AuthorizationResult(appId, group, null);
        }

        /**
         * Create a failed authorization result.
         */
        private static AuthorizationResult error(LocationTaskResult<?> error) {
            return new AuthorizationResult("", null, error);
        }

        /**
         * Return the authorized application ID.
         */
        private String getAppId() {
            return appId;
        }

        /**
         * Return the authorized group.
         */
        private Map<String, Object> getGroup() {
            return group;
        }

        /**
         * Return the authorization error, if any.
         */
        private LocationTaskResult<?> getError() {
            return error;
        }
    }
}
