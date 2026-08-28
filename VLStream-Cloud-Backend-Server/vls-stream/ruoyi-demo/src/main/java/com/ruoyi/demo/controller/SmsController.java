/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.sms.config.properties.SmsProperties;
import com.ruoyi.sms.core.SmsTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * method
 *
 * @author Lion Li
 * @version 4.2.0
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/sms")
public class SmsController {

    private final SmsProperties smsProperties;
// private final SmsTemplate smsTemplate; // spring
// private final AliyunSmsTemplate smsTemplate; // also

    /**
     * Aliyun
     *
     * @param phones
     * @param templateId ID
     */
    @GetMapping("/sendAliyun")
    public R<Object> sendAliyun(String phones, String templateId) {
        if (!smsProperties.getEnabled()) {
            return R.fail("当前系统没有开启短信功能！");
        }
        if (!SpringUtils.containsBean("aliyunSmsTemplate")) {
            return R.fail("阿里云依赖未引入！");
        }
        SmsTemplate smsTemplate = SpringUtils.getBean(SmsTemplate.class);
        Map<String, String> map = new HashMap<>(1);
        map.put("code", "1234");
        Object send = smsTemplate.send(phones, templateId, map);
        return R.ok(send);
    }

    /**
     * Tencent
     *
     * @param phones
     * @param templateId ID
     */
    @GetMapping("/sendTencent")
    public R<Object> sendTencent(String phones, String templateId) {
        if (!smsProperties.getEnabled()) {
            return R.fail("当前系统没有开启短信功能！");
        }
        if (!SpringUtils.containsBean("tencentSmsTemplate")) {
            return R.fail("腾讯云依赖未引入！");
        }
        SmsTemplate smsTemplate = SpringUtils.getBean(SmsTemplate.class);
        Map<String, String> map = new HashMap<>(1);
// map.put("2", " ");
        map.put("1", "1234");
        Object send = smsTemplate.send(phones, templateId, map);
        return R.ok(send);
    }

}
