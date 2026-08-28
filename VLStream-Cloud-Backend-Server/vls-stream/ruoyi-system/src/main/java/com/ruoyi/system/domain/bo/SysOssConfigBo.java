/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * object configuration object sys_oss_config
 *
 * @author Lion Li
 * @author
 * @date 2021-08-13
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class SysOssConfigBo extends BaseEntity {

    /**
     * main
     */
    @NotNull(message = "主建不能为空", groups = {EditGroup.class})
    private Long ossConfigId;

    /**
     * configurationkey
     */
    @NotBlank(message = "配置key不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "configKey长度必须介于{min}和{max} 之间")
    private String configKey;

    /**
     * accessKey
     */
    @NotBlank(message = "accessKey不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "accessKey长度必须介于{min}和{max} 之间")
    private String accessKey;

    /**
     *
     */
    @NotBlank(message = "secretKey不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "secretKey长度必须介于{min}和{max} 之间")
    private String secretKey;

    /**
     *
     */
    @NotBlank(message = "桶名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "bucketName长度必须介于{min}和{max}之间")
    private String bucketName;

    /**
     * before
     */
    private String prefix;

    /**
     *
     */
    @NotBlank(message = "访问站点不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "endpoint长度必须介于{min}和{max}之间")
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
     * whether (0= is ,1= )
     */
    private String status;

    /**
     *
     */
    private String region;

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
    @NotBlank(message = "桶权限类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String accessPolicy;

}
