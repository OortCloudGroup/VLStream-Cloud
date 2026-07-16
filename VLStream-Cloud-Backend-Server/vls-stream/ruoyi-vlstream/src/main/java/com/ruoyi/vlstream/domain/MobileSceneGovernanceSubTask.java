package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** Persisted executable child task generated from a cyclic governance definition. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_mobile_scene_governance_sub_task")
public class MobileSceneGovernanceSubTask extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    private Long governanceId;
    private String name;
    private LocalDateTime executeTime;
    private String taskStatus;
    private String locationIds;
    private String algorithmIds;
    private String cameraIds;
}
