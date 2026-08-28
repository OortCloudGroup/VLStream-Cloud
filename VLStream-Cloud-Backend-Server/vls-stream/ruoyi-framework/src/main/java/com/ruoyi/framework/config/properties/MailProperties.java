/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JavaMail configurationproperty
 *
 * @author Michelle.Chung
 */
@Data
@Component
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    /**
     *
     */
    private Boolean enabled;

    /**
     * SMTPservice
     */
    private String host;

    /**
     * SMTPservice
     */
    private Integer port;

    /**
     * whether need to user
     */
    private Boolean auth;

    /**
     * user
     */
    private String user;

    /**
     *
     */
    private String pass;

    /**
     * , RFC-822
     */
    private String from;

    /**
     * STARTTLS full , STARTTLS is . to (TLS SSL), is .
     */
    private Boolean starttlsEnable;

    /**
     * SSL full
     */
    private Boolean sslEnable;

    /**
     * SMTP , , value
     */
    private Long timeout;

    /**
     * Socket value , , value
     */
    private Long connectionTimeout;
}
