/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.framework.config.properties;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * swagger configurationproperty
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "swagger")
public class SwaggerProperties {

    /**
     * info
     */
    @NestedConfigurationProperty
    private InfoProperties info = new InfoProperties();

    /**
     *
     */
    @NestedConfigurationProperty
    private ExternalDocumentation externalDocs;

    /**
     *
     */
    private List<Tag> tags = null;

    /**
     *
     */
    @NestedConfigurationProperty
    private Paths paths = null;

    /**
     * component
     */
    @NestedConfigurationProperty
    private Components components = null;

    /**
     * <p>
     * propertyinfo
     * </p>
     *
     * @see io.swagger.v3.oas.models.info.Info
     *
     * to springboot configurationprompt / tipinfo,
     */
    @Data
    public static class InfoProperties {

        /**
         *
         */
        private String title = null;

        /**
         *
         */
        private String description = null;

        /**
         * info
         */
        @NestedConfigurationProperty
        private Contact contact = null;

        /**
         *
         */
        @NestedConfigurationProperty
        private License license = null;

        /**
         *
         */
        private String version = null;

    }

}
