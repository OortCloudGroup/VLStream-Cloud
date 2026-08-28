/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.utils;

import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * Get i18n
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageUtils {

    private static final MessageSource MESSAGE_SOURCE = SpringUtils.getBean(MessageSource.class);

    /**
     * and parameter Get spring messageSource
     *
     * @param code
     * @param args parameter
     * @return Get value
     */
    public static String message(String code, Object... args) {
        // LocaleContextHolder.getLocale() Get current , zh_CN
        return MESSAGE_SOURCE.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * @param code
     * @param language
     * @param country
     * @param args parameter
     * @return Get value
     */
    public static String message(String code, String language, String country,Object... args) {
        return MESSAGE_SOURCE.getMessage(code, args, new Locale(language, country));
    }
}
