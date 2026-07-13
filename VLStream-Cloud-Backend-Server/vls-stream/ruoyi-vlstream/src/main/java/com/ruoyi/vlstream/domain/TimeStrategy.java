/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * VLS time strategy entity mapped to vls_time_strategy.
 */
@Data
@TableName(value = "vls_time_strategy", autoResultMap = true)
public class TimeStrategy implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String tenantId;

    private String deviceId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object protectionTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long createUser;

    private String createDept;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private String strategyType;

    @TableField(exist = false)
    private Object dailyTimes;

    @TableField(exist = false)
    private Object weeklyTimes;

    @TableField(exist = false)
    private Object monthlyTimes;

    @TableField(exist = false)
    private Object customTimes;

    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
        putProtectionValue("strategyType", strategyType);
    }

    public void setDailyTimes(Object dailyTimes) {
        this.dailyTimes = dailyTimes;
        putProtectionValue("dailyTimes", dailyTimes);
    }

    public void setWeeklyTimes(Object weeklyTimes) {
        this.weeklyTimes = weeklyTimes;
        putProtectionValue("weeklyTimes", weeklyTimes);
    }

    public void setMonthlyTimes(Object monthlyTimes) {
        this.monthlyTimes = monthlyTimes;
        putProtectionValue("monthlyTimes", monthlyTimes);
    }

    public void setCustomTimes(Object customTimes) {
        this.customTimes = customTimes;
        putProtectionValue("customTimes", customTimes);
    }

    /**
     * Copy stored JSON values to the frontend alias fields.
     */
    @SuppressWarnings("unchecked")
    public void fillFrontendAliases() {
        if (protectionTime instanceof Map) {
            Map<String, Object> config = (Map<String, Object>) protectionTime;
            if (strategyType == null && config.get("strategyType") != null) {
                strategyType = String.valueOf(config.get("strategyType"));
            }
            if (dailyTimes == null) {
                dailyTimes = config.get("dailyTimes");
            }
            if (weeklyTimes == null) {
                weeklyTimes = config.get("weeklyTimes");
            }
            if (monthlyTimes == null) {
                monthlyTimes = config.get("monthlyTimes");
            }
            if (customTimes == null) {
                customTimes = config.get("customTimes");
            }
        }
        if (strategyType == null || strategyType.trim().isEmpty()) {
            strategyType = "everyday";
        }
        if (dailyTimes == null) {
            dailyTimes = Collections.emptyList();
        }
        if (weeklyTimes == null) {
            weeklyTimes = Collections.emptyMap();
        }
    }

    /**
     * Store frontend alias fields under the JSON DB field.
     */
    @SuppressWarnings("unchecked")
    public void normalizeProtectionTime() {
        Map<String, Object> config;
        if (protectionTime instanceof Map) {
            config = new LinkedHashMap<String, Object>((Map<String, Object>) protectionTime);
        } else {
            config = new LinkedHashMap<String, Object>();
        }
        if (strategyType == null || strategyType.trim().isEmpty()) {
            strategyType = valueAsString(config.get("strategyType"), "everyday");
        }
        if (dailyTimes == null && config.containsKey("dailyTimes")) {
            dailyTimes = config.get("dailyTimes");
        }
        if (weeklyTimes == null && config.containsKey("weeklyTimes")) {
            weeklyTimes = config.get("weeklyTimes");
        }
        if (dailyTimes == null) {
            dailyTimes = Collections.emptyList();
        }
        if (weeklyTimes == null) {
            weeklyTimes = Collections.emptyMap();
        }
        config.put("strategyType", strategyType);
        config.put("dailyTimes", dailyTimes);
        config.put("weeklyTimes", weeklyTimes);
        if (monthlyTimes != null) {
            config.put("monthlyTimes", monthlyTimes);
        }
        if (customTimes != null) {
            config.put("customTimes", customTimes);
        }
        protectionTime = config;
    }

    @SuppressWarnings("unchecked")
    private void putProtectionValue(String key, Object value) {
        if (value == null) {
            return;
        }
        Map<String, Object> config;
        if (protectionTime instanceof Map) {
            config = (Map<String, Object>) protectionTime;
        } else {
            config = new LinkedHashMap<String, Object>();
            protectionTime = config;
        }
        config.put(key, value);
    }

    private String valueAsString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }
}
