package com.ruoyi.workflow.convert.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("ccDelegate")
public class CcDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) {

    }
}
