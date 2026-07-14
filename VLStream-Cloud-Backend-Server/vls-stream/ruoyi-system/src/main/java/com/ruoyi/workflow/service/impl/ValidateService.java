/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
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
