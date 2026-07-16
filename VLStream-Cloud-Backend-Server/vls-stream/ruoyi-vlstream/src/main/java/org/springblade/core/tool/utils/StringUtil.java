package org.springblade.core.tool.utils;

/**
 * String predicate compatibility helper.
 */
public final class StringUtil {

    private StringUtil() {
    }

    /**
     * Returns whether the value is null, empty, or whitespace-only.
     */
    public static boolean isBlank(CharSequence value) {
        return cn.hutool.core.util.StrUtil.isBlank(value);
    }

    /**
     * Returns whether the value contains at least one non-whitespace character.
     */
    public static boolean isNotBlank(CharSequence value) {
        return cn.hutool.core.util.StrUtil.isNotBlank(value);
    }
}
