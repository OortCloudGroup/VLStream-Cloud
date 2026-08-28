/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.framework.config;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.framework.config.properties.RedissonProperties;
import com.ruoyi.framework.handler.KeyPrefixHandler;
import com.ruoyi.framework.manager.PlusSpringCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisconfiguration
 *
 * @author Lion Li
 */
@Slf4j
@Configuration
@EnableCaching
@EnableConfigurationProperties(RedissonProperties.class)
public class RedisConfig {

    @Autowired
    private RedissonProperties redissonProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer() {
        return config -> {
            config.setThreads(redissonProperties.getThreads())
                .setNettyThreads(redissonProperties.getNettyThreads())
                .setCodec(new JsonJacksonCodec(objectMapper));
            RedissonProperties.SingleServerConfig singleServerConfig = redissonProperties.getSingleServerConfig();
            if (ObjectUtil.isNotNull(singleServerConfig)) {
                //
                config.useSingleServer()
                    // Set redis key before
                    .setNameMapper(new KeyPrefixHandler(redissonProperties.getKeyPrefix()))
                    .setTimeout(singleServerConfig.getTimeout())
                    .setClientName(singleServerConfig.getClientName())
                    .setIdleConnectionTimeout(singleServerConfig.getIdleConnectionTimeout())
                    .setSubscriptionConnectionPoolSize(singleServerConfig.getSubscriptionConnectionPoolSize())
                    .setConnectionMinimumIdleSize(singleServerConfig.getConnectionMinimumIdleSize())
                    .setConnectionPoolSize(singleServerConfig.getConnectionPoolSize());
            }
            // configuration
            RedissonProperties.ClusterServersConfig clusterServersConfig = redissonProperties.getClusterServersConfig();
            if (ObjectUtil.isNotNull(clusterServersConfig)) {
                config.useClusterServers()
                    // Set redis key before
                    .setNameMapper(new KeyPrefixHandler(redissonProperties.getKeyPrefix()))
                    .setTimeout(clusterServersConfig.getTimeout())
                    .setClientName(clusterServersConfig.getClientName())
                    .setIdleConnectionTimeout(clusterServersConfig.getIdleConnectionTimeout())
                    .setSubscriptionConnectionPoolSize(clusterServersConfig.getSubscriptionConnectionPoolSize())
                    .setMasterConnectionMinimumIdleSize(clusterServersConfig.getMasterConnectionMinimumIdleSize())
                    .setMasterConnectionPoolSize(clusterServersConfig.getMasterConnectionPoolSize())
                    .setSlaveConnectionMinimumIdleSize(clusterServersConfig.getSlaveConnectionMinimumIdleSize())
                    .setSlaveConnectionPoolSize(clusterServersConfig.getSlaveConnectionPoolSize())
                    .setReadMode(clusterServersConfig.getReadMode())
                    .setSubscriptionMode(clusterServersConfig.getSubscriptionMode());
            }
            log.info("初始化 redis 配置");
        };
    }

    /**
     * Custom integrate spring-cache
     */
    @Bean
    public CacheManager cacheManager() {
        return new PlusSpringCacheManager();
    }

    /**
     * redis configuration yml
     *
     * --- # redis configuration( and only can need to )
     * spring:
     *   redis:
     *     cluster:
     *       nodes:
     *         - 192.168.0.100:6379
     *         - 192.168.0.101:6379
     *         - 192.168.0.102:6379
     * #
     *     password:
     * #
     *     timeout: 10s
     * # whether ssl
     *     ssl: false
     *
     * redisson:
     * #
     *   threads: 16
     * # Netty
     *   nettyThreads: 32
     * # configuration
     *   clusterServersConfig:
     * #
     *     clientName: ${ruoyi.name}
     * # master null / empty
     *     masterConnectionMinimumIdleSize: 32
     * # master
     *     masterConnectionPoolSize: 64
     * # slave null / empty
     *     slaveConnectionMinimumIdleSize: 32
     * # slave
     *     slaveConnectionPoolSize: 64
     * # null / empty , :
     *     idleConnectionTimeout: 10000
     * # etc. , :
     *     timeout: 3000
     * # and
     *     subscriptionConnectionPoolSize: 50
     * #
     *     readMode: "SLAVE"
     * #
     *     subscriptionMode: "MASTER"
     */

}
