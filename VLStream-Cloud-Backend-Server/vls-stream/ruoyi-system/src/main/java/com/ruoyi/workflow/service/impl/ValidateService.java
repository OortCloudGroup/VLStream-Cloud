/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import com.ruoyi.workflow.event.ValidationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 分类删除效验
 */
@Service
@RequiredArgsConstructor
public class ValidateService {


    private final ApplicationEventPublisher eventPublisher;

    public void validateBeforeDeletion(Collection<String> ids) {
            eventPublisher.publishEvent(new ValidationEvent(ids));
    }
}
