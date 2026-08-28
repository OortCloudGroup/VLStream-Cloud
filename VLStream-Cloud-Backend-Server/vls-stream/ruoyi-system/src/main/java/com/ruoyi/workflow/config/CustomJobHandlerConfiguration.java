/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.config;

import com.ruoyi.workflow.handler.CustomTimerHandler;
import lombok.extern.slf4j.Slf4j;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Process
 */
@Slf4j
@Configuration
public class CustomJobHandlerConfiguration  {

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> customEngineConfiguration() {
        return engineConfiguration -> {
            engineConfiguration.addCustomJobHandler(new CustomTimerHandler());
        };
    }

}
