/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.config;

import org.hibernate.validator.HibernateValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.Validator;
import java.util.Properties;

/**
 * Validate configuration
 *
 * @author Lion Li
 */
@Configuration
public class ValidatorConfig {

    @Autowired
    private MessageSource messageSource;

    /**
     * configurationValidate
     */
    @Bean
    public Validator validator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        //
        factoryBean.setValidationMessageSource(messageSource);
        // Set HibernateValidator Validate
        factoryBean.setProviderClass(HibernateValidator.class);
        Properties properties = new Properties();
        // Set
        properties.setProperty("hibernate.validator.fail_fast", "true");
        factoryBean.setValidationProperties(properties);
        // Load configuration
        factoryBean.afterPropertiesSet();
        return factoryBean.getValidator();
    }

}
