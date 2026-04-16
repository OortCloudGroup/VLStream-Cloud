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
 * Time Strategy Entity Class
 */
@Data
@Accessors(chain = true)
@TableName("time_strategy")
public class TimeStrategy {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Primary key ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * Device ID
     */
    private String deviceId;
    
    /**
     * Strategy type: everyday-daily, weekly-weekly
     */
    private String strategyType;
    
    /**
     * Daily time periods, stored as JSON string
     */
    @TableField("daily_times")
    @JsonIgnore
    private String dailyTimesJson;
    
    /**
     * Weekly time periods, stored as JSON string
     */
    @TableField("weekly_times")
    @JsonIgnore
    private String weeklyTimesJson;
    
    /**
     * Creation time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * Update time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // Business fields (not mapped to database)
    @TableField(exist = false)
    private List<Integer> dailyTimes;
    
    @TableField(exist = false)
    private Map<String, List<Integer>> weeklyTimes;
    
    /**
     * Get daily time periods
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
     * Set daily time periods
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
     * Get weekly time periods
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
     * Set weekly time periods
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