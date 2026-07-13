/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.config;

import com.ruoyi.workflow.handler.CustomTimerHandler;
import lombok.extern.slf4j.Slf4j;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册自定义作业处理器
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
