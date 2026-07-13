package com.ruoyi.workflow.convert.listeners;

import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component
public class TimeoutTaskListener implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {

    }
}
