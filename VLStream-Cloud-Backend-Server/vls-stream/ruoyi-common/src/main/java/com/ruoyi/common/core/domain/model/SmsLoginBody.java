/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.core.domain.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * object
 *
 * @author Lion Li
 */

@Data
public class SmsLoginBody {

    /**
     *
     */
    @NotBlank(message = "{user.phonenumber.not.blank}")
    private String phonenumber;

    /**
     * code
     */
    @NotBlank(message = "{sms.code.not.blank}")
    private String smsCode;

}
