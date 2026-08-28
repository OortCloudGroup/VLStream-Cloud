/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.job.dto;

import lombok.Data;

import java.util.Date;

@Data
public class CommonJobInfo {
    private int id;                // primary key ID

    private int jobGroup;        // Execute primary key ID
    private String jobDesc;     // task

    private String author;        //
    private String alarmEmail;    //

    private String scheduleType;            //
    private String scheduleConf;         //Cron
    private String cronGenDisplay;
    private String scheduleConfCRON;

    private Date addTime;
    private Date updateTime;

    // configuration, value
    private String misfireStrategy;            //

    private String executorRouteStrategy;    // Execute
    private String executorHandler;            // Execute , taskHandler
    private String executorParam;            // Execute , taskparameter
    private String executorBlockStrategy;    // Process
    private int executorTimeout;            // taskExecute ,
    private int executorFailRetryCount;        // failed

    private String glueType;        // GLUE #com.xxl.job.core.glue.GlueTypeEnum
    private String glueSource;        // GLUE
    private String glueRemark;        // GLUEremark
    private Date glueUpdatetime;    // GLUEupdate time

    private String childJobId;        // sub taskID,

    private int triggerStatus;        // : 0- , 1-
    private long triggerLastTime;    //
    private long triggerNextTime;    //
}
