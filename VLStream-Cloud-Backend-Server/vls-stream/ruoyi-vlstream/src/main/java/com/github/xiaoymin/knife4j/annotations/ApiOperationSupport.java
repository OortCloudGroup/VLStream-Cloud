package com.github.xiaoymin.knife4j.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documentation-order compatibility annotation used by the copied controllers.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiOperationSupport {

    /**
     * Returns the desired API documentation display order.
     */
    int order() default 0;
}
