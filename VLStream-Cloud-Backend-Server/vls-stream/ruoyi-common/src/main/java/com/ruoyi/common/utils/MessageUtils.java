/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.utils;

import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * 获取i18n资源文件
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageUtils {

    private static final MessageSource MESSAGE_SOURCE = SpringUtils.getBean(MessageSource.class);

    /**
     * 根据消息键和参数 获取消息 委托给spring messageSource
     *
     * @param code 消息键
     * @param args 参数
     * @return 获取国际化翻译值
     */
    public static String message(String code, Object... args) {
        // LocaleContextHolder.getLocale() 获取当前国际化的语言的标识 , 如 zh_CN
        return MESSAGE_SOURCE.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * @param code 消息键
     * @param language 语言
     * @param country 国家
     * @param args 参数
     * @return 获取国际化翻译值
     */
    public static String message(String code, String language, String country,Object... args) {
        return MESSAGE_SOURCE.getMessage(code, args, new Locale(language, country));
    }
}
