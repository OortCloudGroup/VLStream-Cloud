/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.config.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.redisson.config.ReadMode;
import org.redisson.config.SubscriptionMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redisson configurationproperty
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "redisson")
public class RedissonProperties {

    /**
     * redis key before
     */
    private String keyPrefix;

    /**
     * , value = current Process * 2
     */
    private int threads;

    /**
     * Netty , value = current Process * 2
     */
    private int nettyThreads;

    /**
     * serviceconfiguration
     */
    private SingleServerConfig singleServerConfig;

    /**
     * serviceconfiguration
     */
    private ClusterServersConfig clusterServersConfig;

    @Data
    @NoArgsConstructor
    public static class SingleServerConfig {

        /**
         *
         */
        private String clientName;

        /**
         * null / empty
         */
        private int connectionMinimumIdleSize;

        /**
         *
         */
        private int connectionPoolSize;

        /**
         * null / empty , :
         */
        private int idleConnectionTimeout;

        /**
         * etc. , :
         */
        private int timeout;

        /**
         * and
         */
        private int subscriptionConnectionPoolSize;

    }

    @Data
    @NoArgsConstructor
    public static class ClusterServersConfig {

        /**
         *
         */
        private String clientName;

        /**
         * master null / empty
         */
        private int masterConnectionMinimumIdleSize;

        /**
         * master
         */
        private int masterConnectionPoolSize;

        /**
         * slave null / empty
         */
        private int slaveConnectionMinimumIdleSize;

        /**
         * slave
         */
        private int slaveConnectionPoolSize;

        /**
         * null / empty , :
         */
        private int idleConnectionTimeout;

        /**
         * etc. , :
         */
        private int timeout;

        /**
         * and
         */
        private int subscriptionConnectionPoolSize;

        /**
         *
         */
        private ReadMode readMode;

        /**
         *
         */
        private SubscriptionMode subscriptionMode;

    }

}
