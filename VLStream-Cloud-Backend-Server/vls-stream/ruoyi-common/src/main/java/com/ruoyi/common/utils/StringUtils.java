/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.AntPathMatcher;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils extends org.apache.commons.lang3.StringUtils {

    public static final String SEPARATOR = ",";

    /**
     * Get parameter is empty value
     *
     * @param str defaultValue need to Check value
     * @return value value
     */
    public static String blankToDefault(String str, String defaultValue) {
        return StrUtil.blankToDefault(str, defaultValue);
    }

    /**
     * * Check whether is empty
     *
     * @param str String
     * @return true: is empty false: non- null / empty
     */
    public static boolean isEmpty(String str) {
        return StrUtil.isEmpty(str);
    }

    /**
     * * Check whether to non- null / empty
     *
     * @param str String
     * @return true: non- null / empty false: null / empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * null / empty
     */
    public static String trim(String str) {
        return StrUtil.trim(str);
    }

    /**
     *
     *
     * @param str
     * @param start start
     * @return
     */
    public static String substring(final String str, int start) {
        return substring(str, start, str.length());
    }

    /**
     *
     *
     * @param str
     * @param start start
     * @param end finish
     * @return
     */
    public static String substring(final String str, int start, int end) {
        return StrUtil.sub(str, start, end);
    }

    /**
     * Format , {} <br>
     * method only is {} Replace to parameter<br>
     * if {} \\ { , if {} before \ \\\\ <br>
     * : <br>
     * : format("this is {} for {}", "a", "b") -> this is a for b<br>
     * {}: format("this is \\{} for {}", "a", "b") -> this is {} for a<br>
     * \: format("this is \\\\{} for {}", "a", "b") -> this is \a for b<br>
     *
     * @param template , Replace {}
     * @param params parameter value
     * @return Format after
     */
    public static String format(String template, Object... params) {
        return StrUtil.format(template, params);
    }

    /**
     * whether to http(s)://
     *
     * @param link
     * @return
     */
    public static boolean ishttp(String link) {
        return Validator.isUrl(link);
    }

    /**
     * set
     *
     * @param str
     * @param sep
     * @return setcollection
     */
    public static Set<String> str2Set(String str, String sep) {
        return new HashSet<>(str2List(str, sep, true, false));
    }

    /**
     * list
     *
     * @param str
     * @param sep
     * @param filterBlank null / empty
     * @param trim null / empty
     * @return listcollection
     */
    public static List<String> str2List(String str, String sep, boolean filterBlank, boolean trim) {
        List<String> list = new ArrayList<>();
        if (isEmpty(str)) {
            return list;
        }

        // null / empty
        if (filterBlank && isBlank(str)) {
            return list;
        }
        String[] split = str.split(sep);
        for (String string : split) {
            if (filterBlank && isBlank(string)) {
                continue;
            }
            if (trim) {
                string = trim(string);
            }
            list.add(string);
        }

        return list;
    }
    /**
     * CharSequence whether before .
     *
     * @param str need to CharSequence can to null
     * @param prefixs need to find before can to null
     * @return whether
     */
    public static boolean startWithAnyIgnoreCase(CharSequence str, CharSequence... prefixs) {
        // Check whether is
        for (CharSequence prefix : prefixs) {
            if (StringUtils.startsWithIgnoreCase(str, prefix)) {
                return true;
            }
        }
        return false;
    }
    /**
     * find whether in
     *
     * @param cs
     * @param searchCharSequences need to array
     * @return whether
     */
    public static boolean containsAnyIgnoreCase(CharSequence cs, CharSequence... searchCharSequences) {
        return StrUtil.containsAnyIgnoreCase(cs, searchCharSequences);
    }

    /**
     *
     */
    public static String toUnderScoreCase(String str) {
        return StrUtil.toUnderlineCase(str);
    }

    /**
     * whether
     *
     * @param str
     * @param strs
     * @return true
     */
    public static boolean inStringIgnoreCase(String str, String... strs) {
        return StrUtil.equalsAnyIgnoreCase(str, strs);
    }

    /**
     * Convert to . if Convert before is empty, null / empty . : HELLO_WORLD->HelloWorld
     *
     * @param name Convert before
     * @return Convert after
     */
    public static String convertToCamelCase(String name) {
        return StrUtil.upperFirst(StrUtil.toCamelCase(name));
    }

    /**
     * method : user_name->userName
     */
    public static String toCamelCase(String s) {
        return StrUtil.toCamelCase(s);
    }

    /**
     * find whether in
     *
     * @param str
     * @param strs need to array
     * @return whether
     */
    public static boolean matches(String str, List<String> strs) {
        if (isEmpty(str) || CollUtil.isEmpty(strs)) {
            return false;
        }
        for (String pattern : strs) {
            if (isMatch(pattern, str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check urlwhether and configuration:
     * ? ;
     * * layer , layer ;
     * ** layer ;
     *
     * @param pattern
     * @param url need to url
     */
    public static boolean isMatch(String pattern, String url) {
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(pattern, url);
    }

    /**
     * 0, . , if Convert to after, size, only aftersize .
     *
     * @param num object
     * @param size
     * @return , to .
     */
    public static String padl(final Number num, final int size) {
        return padl(num.toString(), size, '0');
    }

    /**
     * . if s size, only aftersize .
     *
     * @param s
     * @param size
     * @param c
     * @return , .
     */
    public static String padl(final String s, final int size, final char c) {
        final StringBuilder sb = new StringBuilder(size);
        if (s != null) {
            final int len = s.length();
            if (s.length() <= size) {
                for (int i = size - len; i > 0; i--) {
                    sb.append(c);
                }
                sb.append(s);
            } else {
                return s.substring(len - size, len);
            }
        } else {
            for (int i = size; i > 0; i--) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * ( )
     *
     * @param str
     * @return after data
     */
    public static List<String> splitList(String str) {
        return splitTo(str, Convert::toStr);
    }

    /**
     *
     *
     * @param str
     * @param separator
     * @return after data
     */
    public static List<String> splitList(String str, String separator) {
        return splitTo(str, separator, Convert::toStr);
    }

    /**
     * CustomConvert ( )
     *
     * @param str
     * @param mapper CustomConvert
     * @return after data
     */
    public static <T> List<T> splitTo(String str, Function<? super Object, T> mapper) {
        return splitTo(str, SEPARATOR, mapper);
    }

    /**
     * CustomConvert
     *
     * @param str
     * @param separator
     * @param mapper CustomConvert
     * @return after data
     */
    public static <T> List<T> splitTo(String str, String separator, Function<? super Object, T> mapper) {
        if (isBlank(str)) {
            return new ArrayList<>(0);
        }
        return StrUtil.split(str, separator)
            .stream()
            .filter(Objects::nonNull)
            .map(mapper)
            .collect(Collectors.toList());
    }

}
