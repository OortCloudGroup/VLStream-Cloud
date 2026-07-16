package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/** Mobile immediate or cyclic scene-governance task. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_mobile_scene_governance")
public class MobileSceneGovernance extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    private String name;
    private String governanceMode;
    private String cycleType;
    private Integer intervalDays;
    private String weeklyDays;
    private String monthlyDays;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String triggerTimes;
    private String locationIds;
    private String algorithmIds;
    private String cameraIds;
    private String description;

    @TableField(exist = false)
    private String algorithmNames;

    @TableField(exist = false)
    private String cameraNames;

    @TableField(exist = false)
    private List<MobileSceneGovernanceSubTask> subTaskList;
}
