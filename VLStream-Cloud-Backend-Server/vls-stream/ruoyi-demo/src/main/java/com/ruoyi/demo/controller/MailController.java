/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.email.MailUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;


/**
 *
 *
 * @author Michelle.Chung
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/mail")
public class MailController {

    /**
     *
     *
     * @param to
     * @param subject
     * @param text
     */
    @GetMapping("/sendSimpleMessage")
    public R<Void> sendSimpleMessage(String to, String subject, String text) {
        MailUtils.sendText(to, subject, text);
        return R.ok();
    }

    /**
     * ( )
     *
     * @param to
     * @param subject
     * @param text
     * @param filePath
     */
    @GetMapping("/sendMessageWithAttachment")
    public R<Void> sendMessageWithAttachment(String to, String subject, String text, String filePath) {
        MailUtils.sendText(to, subject, text, new File(filePath));
        return R.ok();
    }

}
