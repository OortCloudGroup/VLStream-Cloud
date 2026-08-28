/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.flowable.listener;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * usertasklistener
 *
 * @author KonBAI
 * @since 2023/5/13
 */
@Component(value = "userTaskListener")
public class UserTaskListener implements TaskListener {

    /**
     * field ( and workflow field )
     */
    // private FixedValue field;

    @Override
    public void notify(DelegateTask delegateTask) {
        // TODO tasklistener
        System.out.println("执行任务监听器...");
    }

}
