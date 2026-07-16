package org.springblade.core.tool.utils;

import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * Spring bean lookup compatibility helper.
 */
public final class SpringUtil {

    private SpringUtil() {
    }

    /**
     * Resolves a bean from RuoYi's shared Spring application context.
     */
    public static <T> T getBean(Class<T> beanType) {
        return SpringUtils.getBean(beanType);
    }
}
