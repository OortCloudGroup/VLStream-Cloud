package org.springblade.core.tool.utils;

import java.util.Map;

/**
 * Bean conversion compatibility helpers backed by Hutool.
 */
public final class BeanUtil {

    private BeanUtil() {
    }

    /**
     * Copies matching properties into a newly-created target instance.
     */
    public static <T> T copyProperties(Object source, Class<T> targetType) {
        if (source == null) {
            return null;
        }
        return cn.hutool.core.bean.BeanUtil.toBean(source, targetType);
    }

    /**
     * Copies matching properties into an existing target object.
     */
    public static void copy(Object source, Object target) {
        if (source != null && target != null) {
            cn.hutool.core.bean.BeanUtil.copyProperties(source, target);
        }
    }

    /**
     * Converts a bean or map into a mutable property map.
     */
    public static Map<String, Object> beanToMap(Object source) {
        return cn.hutool.core.bean.BeanUtil.beanToMap(source);
    }
}
