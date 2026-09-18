/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.framework.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * configuration
 *
 * @author Lion Li
 */
@Configuration
public class I18nConfig {

    @Bean
    public LocaleResolver localeResolver() {
        return new I18nLocaleResolver();
    }

    /**
     * Get info
     */
    static class I18nLocaleResolver implements LocaleResolver {

        private static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;
        private static final Set<String> SUPPORTED_LANGUAGE_TAGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "zh-CN", "en-US", "es-MX", "ar", "de-DE", "fr-FR", "ja-JP", "pt-BR", "ru-RU", "ko-KR", "id-ID", "tr-TR"
        )));

        @Override
        public Locale resolveLocale(HttpServletRequest httpServletRequest) {
            String language = httpServletRequest.getHeader("content-language");
            if (StrUtil.isBlank(language)) {
                language = httpServletRequest.getHeader("accept-language");
            }
            return parseLocale(language);
        }

        static Locale parseLocale(String language) {
            if (StrUtil.isBlank(language)) {
                return DEFAULT_LOCALE;
            }
            String firstLanguage = language.split(",", 2)[0].trim().replace('_', '-');
            Locale requested = Locale.forLanguageTag(firstLanguage);
            if (SUPPORTED_LANGUAGE_TAGS.contains(requested.toLanguageTag())) {
                return requested;
            }
            for (String supportedTag : SUPPORTED_LANGUAGE_TAGS) {
                Locale supported = Locale.forLanguageTag(supportedTag);
                if (supported.getLanguage().equalsIgnoreCase(requested.getLanguage())) {
                    return supported;
                }
            }
            return DEFAULT_LOCALE;
        }

        @Override
        public void setLocale(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Locale locale) {

        }
    }
}
