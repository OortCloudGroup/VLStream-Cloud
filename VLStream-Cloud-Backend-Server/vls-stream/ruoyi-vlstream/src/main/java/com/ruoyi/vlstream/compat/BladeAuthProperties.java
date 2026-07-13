/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.compat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SM2 key settings shared with the VLStream-Web login page.
 */
@Component
@ConfigurationProperties(prefix = "vlstream.auth")
public class BladeAuthProperties {

    public static final String DEFAULT_PUBLIC_KEY =
        "049787e408dea94acb3655acc5a7c7c7010bb9f140c84926c667ea616366082a118141c8dcb3e78a9d85d64fb765a250ff73448b18938f2219b94f782e28e1df64";
    public static final String DEFAULT_PRIVATE_KEY =
        "7204a0e7dd3f23783fb04a82b7ae211441b12a8cb42694f9997e02babc4bf65b";

    private String publicKey = DEFAULT_PUBLIC_KEY;
    private String privateKey = DEFAULT_PRIVATE_KEY;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
