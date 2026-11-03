package com.ruoyi.vlstream.test.vlstream.compute.billing;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Client presence follows the same mode as admission; a credential alone grants nothing. */
public class ExternalBillingClientCondition implements Condition {
    @Override public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ExternalBillingProperties.Mode mode = context.getEnvironment().getProperty(
            "vlstream.compute.external-billing.mode", ExternalBillingProperties.Mode.class);
        return mode == null ? context.getEnvironment().getProperty("vlstream.compute.external-billing.enabled", Boolean.class, false)
            : mode != ExternalBillingProperties.Mode.OFF;
    }
}
