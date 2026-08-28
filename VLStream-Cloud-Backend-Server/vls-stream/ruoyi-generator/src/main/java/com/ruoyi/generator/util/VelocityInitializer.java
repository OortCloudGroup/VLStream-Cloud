/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.generator.util;

import com.ruoyi.common.constant.Constants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.velocity.app.Velocity;

import java.util.Properties;

/**
 * VelocityEngine
 *
 * @author ruoyi
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VelocityInitializer {

    /**
     * Initialize vm method
     */
    public static void initVelocity() {
        Properties p = new Properties();
        try {
            // Load classpath vm
            p.setProperty("resource.loader.file.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            //
            p.setProperty(Velocity.INPUT_ENCODING, Constants.UTF8);
            // Initialize Velocity , configurationProperties
            Velocity.init(p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
