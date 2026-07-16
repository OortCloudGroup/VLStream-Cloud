package org.springblade.core.tool.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Date constants and formatting used by the copied VLS API.
 */
public final class DateUtil {

    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";

    private DateUtil() {
    }

    /**
     * Returns today's date in the original export filename format.
     */
    public static String today() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
