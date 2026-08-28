/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import lombok.Data;


/**
 * object configuration object sys_oss_config
 *
 * @author Lion Li
 * @author
 * @date 2021-08-13
 */
@Data
@ExcelIgnoreUnannotated
public class SysOssConfigVo {

    private static final long serialVersionUID = 1L;

    /**
     * main
     */
    private Long ossConfigId;

    /**
     * configurationkey
     */
    private String configKey;

    /**
     * accessKey
     */
    private String accessKey;

    /**
     *
     */
    private String secretKey;

    /**
     *
     */
    private String bucketName;

    /**
     * before
     */
    private String prefix;

    /**
     *
     */
    private String endpoint;

    /**
     * Custom
     */
    private String domain;

    /**
     * whether https (Y= is ,N= )
     */
    private String isHttps;

    /**
     *
     */
    private String region;

    /**
     * whether (0= is ,1= )
     */
    private String status;

    /**
     * field
     */
    private String ext1;

    /**
     * remark
     */
    private String remark;

    /**
     * (0private 1public 2custom)
     */
    private String accessPolicy;

}
