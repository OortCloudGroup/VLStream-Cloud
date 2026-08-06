/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

/**
 * Numeric wire contracts retained from the former Go location-task service.
 *
 * <p>These values are API and persistence contracts rather than Java ordinal
 * values. Keep them synchronized with the corresponding system dictionaries.</p>
 */
public final class LocationTaskEventContracts {

    /** Prevent utility-class instantiation. */
    private LocationTaskEventContracts() {
    }

    /** Common contract exposed by every numeric business enumeration. */
    public interface NumericContract {

        /** Return the stable API and database code. */
        int getCode();

        /** Return the Chinese display label. */
        String getLabel();
    }

    /** Event processing status stored in {@code oort_task_event.status}. */
    public enum EventStatus implements NumericContract {
        COMPLETED(1, "已完成"),
        PENDING(2, "待处理");

        private final int code;
        private final String label;

        /** Create one event-status contract value. */
        EventStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /** Return the stable event-status code. */
        @Override
        public int getCode() {
            return code;
        }

        /** Return the event-status label. */
        @Override
        public String getLabel() {
            return label;
        }
    }

    /** Event source module stored in {@code oort_task_event.mod_type}. */
    public enum ModuleType implements NumericContract {
        EVENT_CAPTURE(1, "事件拍传"),
        ACTIVE_SAFETY(2, "主动安全");

        private final int code;
        private final String label;

        /** Create one source-module contract value. */
        ModuleType(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /** Return the stable source-module code. */
        @Override
        public int getCode() {
            return code;
        }

        /** Return the source-module label. */
        @Override
        public String getLabel() {
            return label;
        }
    }

    /** Raw active-safety alarm status stored in {@code oort_task_event.mod_status}. */
    public enum AlarmStatus implements NumericContract {
        PENDING_CONFIRMATION(0, "待确认"),
        REAL_ALARM(1, "真实告警"),
        MAINTENANCE(2, "维保"),
        FALSE_ALARM(3, "误报");

        private final int code;
        private final String label;

        /** Create one persisted alarm-status contract value. */
        AlarmStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /** Return the stable persisted alarm-status code. */
        @Override
        public int getCode() {
            return code;
        }

        /** Return the persisted alarm-status label. */
        @Override
        public String getLabel() {
            return label;
        }
    }

    /**
     * Request-only confirmation filter used by event-list endpoints.
     *
     * <p>Code 2 means all confirmed raw statuses and must not be confused with
     * persisted alarm status 2, which means maintenance.</p>
     */
    public enum AlarmFilter implements NumericContract {
        ALL(0, "全部"),
        PENDING(1, "待确认"),
        CONFIRMED(2, "已确认");

        private final int code;
        private final String label;

        /** Create one alarm-filter contract value. */
        AlarmFilter(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /** Return the stable alarm-filter code. */
        @Override
        public int getCode() {
            return code;
        }

        /** Return the alarm-filter label. */
        @Override
        public String getLabel() {
            return label;
        }
    }

    /** Work-order conversion status stored in {@code oort_task_event.work_order_status}. */
    public enum WorkOrderStatus implements NumericContract {
        NOT_CONVERTED(0, "未转工单"),
        CONVERTED(1, "已转工单");

        private final int code;
        private final String label;

        /** Create one work-order-status contract value. */
        WorkOrderStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /** Return the stable work-order-status code. */
        @Override
        public int getCode() {
            return code;
        }

        /** Return the work-order-status label. */
        @Override
        public String getLabel() {
            return label;
        }
    }

    /** Event executor kind stored in {@code oort_task_event_user.u_type}. */
    public enum ExecutorType implements NumericContract {
        USER(1, "用户"),
        VEHICLE(2, "车辆");

        private final int code;
        private final String label;

        /** Create one executor-type contract value. */
        ExecutorType(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /** Return the stable executor-type code. */
        @Override
        public int getCode() {
            return code;
        }

        /** Return the executor-type label. */
        @Override
        public String getLabel() {
            return label;
        }
    }

    /** Relationship scope accepted by {@code /task/v2/myevent_list}. */
    public enum ExecutionScope implements NumericContract {
        BY_ME(1, "我执行的"),
        CREATED_BY_ME(2, "我发布的"),
        FED_BACK_BY_ME(3, "我反馈过的"),
        ALL_RELATED(4, "与我相关的全部");

        private final int code;
        private final String label;

        /** Create one execution-scope contract value. */
        ExecutionScope(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /** Return the stable execution-scope code. */
        @Override
        public int getCode() {
            return code;
        }

        /** Return the execution-scope label. */
        @Override
        public String getLabel() {
            return label;
        }
    }

    /** Picture-presence filter accepted by event-list endpoints. */
    public enum PictureFilter implements NumericContract {
        ALL(0, "全部"),
        WITH_PICTURE(1, "有图片"),
        WITHOUT_PICTURE(2, "无图片");

        private final int code;
        private final String label;

        /** Create one picture-filter contract value. */
        PictureFilter(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /** Return the stable picture-filter code. */
        @Override
        public int getCode() {
            return code;
        }

        /** Return the picture-filter label. */
        @Override
        public String getLabel() {
            return label;
        }
    }

    /** Statistics relationship accepted by {@code /task/v1/event_statistics}. */
    public enum StatisticsType implements NumericContract {
        BY_EXECUTOR(1, "按执行人"),
        BY_CREATOR(2, "按发起人");

        private final int code;
        private final String label;

        /** Create one statistics-type contract value. */
        StatisticsType(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /** Return the stable statistics-type code. */
        @Override
        public int getCode() {
            return code;
        }

        /** Return the statistics-type label. */
        @Override
        public String getLabel() {
            return label;
        }
    }

    /** V2 device-group dimension. */
    public enum GroupType implements NumericContract {
        REGION(1, "区域"),
        GROUP(2, "分组"),
        TAG(3, "标签");

        private final int code;
        private final String label;

        /** Create one group-type contract value. */
        GroupType(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /** Return the stable group-type code. */
        @Override
        public int getCode() {
            return code;
        }

        /** Return the group-type label. */
        @Override
        public String getLabel() {
            return label;
        }
    }

    /** Coordinate-system identifier carried in event point objects. */
    public enum CoordinateSystem implements NumericContract {
        WGS84(1, "WGS84"),
        GCJ02(2, "GCJ02"),
        BD09(3, "BD09");

        private final int code;
        private final String label;

        /** Create one coordinate-system contract value. */
        CoordinateSystem(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /** Return the stable coordinate-system code. */
        @Override
        public int getCode() {
            return code;
        }

        /** Return the coordinate-system label. */
        @Override
        public String getLabel() {
            return label;
        }
    }

    /** Determine whether a code belongs to an enum contract. */
    public static boolean contains(NumericContract[] values, int code, boolean allowZero) {
        if (allowZero && code == 0) {
            return true;
        }
        for (NumericContract value : values) {
            if (value.getCode() == code) {
                return true;
            }
        }
        return false;
    }
}
