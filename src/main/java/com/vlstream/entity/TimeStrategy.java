package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 时间策略实体类
 */
@Data
@Accessors(chain = true)
@TableName("time_strategy")
public class TimeStrategy {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 策略类型：everyday-每天, weekly-每周
     */
    private String strategyType;
    
    /**
     * 每天模式的时间段，存储为JSON字符串
     */
    @TableField("daily_times")
    @JsonIgnore
    private String dailyTimesJson;
    
    /**
     * 每周模式的时间段，存储为JSON字符串
     */
    @TableField("weekly_times")
    @JsonIgnore
    private String weeklyTimesJson;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 业务字段（不映射到数据库）
    @TableField(exist = false)
    private List<Integer> dailyTimes;
    
    @TableField(exist = false)
    private Map<String, List<Integer>> weeklyTimes;
    
    /**
     * 获取每天时间段
     */
    public List<Integer> getDailyTimes() {
        if (dailyTimes == null && dailyTimesJson != null && !dailyTimesJson.trim().isEmpty()) {
            try {
                dailyTimes = objectMapper.readValue(dailyTimesJson, new TypeReference<List<Integer>>() {});
            } catch (JsonProcessingException e) {
                dailyTimes = new ArrayList<>();
            }
        }
        return dailyTimes != null ? dailyTimes : new ArrayList<>();
    }
    
    /**
     * 设置每天时间段
     */
    public void setDailyTimes(List<Integer> dailyTimes) {
        this.dailyTimes = dailyTimes;
        try {
            this.dailyTimesJson = dailyTimes != null ? objectMapper.writeValueAsString(dailyTimes) : null;
        } catch (JsonProcessingException e) {
            this.dailyTimesJson = null;
        }
    }
    
    /**
     * 获取每周时间段
     */
    public Map<String, List<Integer>> getWeeklyTimes() {
        if (weeklyTimes == null && weeklyTimesJson != null && !weeklyTimesJson.trim().isEmpty()) {
            try {
                weeklyTimes = objectMapper.readValue(weeklyTimesJson, new TypeReference<Map<String, List<Integer>>>() {});
            } catch (JsonProcessingException e) {
                weeklyTimes = new HashMap<>();
            }
        }
        return weeklyTimes != null ? weeklyTimes : new HashMap<>();
    }
    
    /**
     * 设置每周时间段
     */
    public void setWeeklyTimes(Map<String, List<Integer>> weeklyTimes) {
        this.weeklyTimes = weeklyTimes;
        try {
            this.weeklyTimesJson = weeklyTimes != null ? objectMapper.writeValueAsString(weeklyTimes) : null;
        } catch (JsonProcessingException e) {
            this.weeklyTimesJson = null;
        }
    }
} 