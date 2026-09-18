package com.ruoyi.framework.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class I18nConfigTest {

    @Test
    void parsesSupportedRegionalAndLegacyLocaleFormats() {
        assertEquals(Locale.forLanguageTag("es-MX"), I18nConfig.I18nLocaleResolver.parseLocale("es-MX"));
        assertEquals(Locale.forLanguageTag("pt-BR"), I18nConfig.I18nLocaleResolver.parseLocale("pt_BR"));
        assertEquals(Locale.forLanguageTag("ar"), I18nConfig.I18nLocaleResolver.parseLocale("ar"));
    }

    @Test
    void normalizesLanguageOnlyAndAcceptLanguageValues() {
        assertEquals(Locale.forLanguageTag("en-US"), I18nConfig.I18nLocaleResolver.parseLocale("en-GB,en;q=0.9"));
        assertEquals(Locale.forLanguageTag("fr-FR"), I18nConfig.I18nLocaleResolver.parseLocale("fr"));
    }

    @Test
    void fallsBackToSimplifiedChineseForMissingOrUnsupportedLocales() {
        assertEquals(Locale.SIMPLIFIED_CHINESE, I18nConfig.I18nLocaleResolver.parseLocale(null));
        assertEquals(Locale.SIMPLIFIED_CHINESE, I18nConfig.I18nLocaleResolver.parseLocale("xx-YY"));
    }
}
