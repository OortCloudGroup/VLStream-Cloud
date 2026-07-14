/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.compat;

import java.io.Serializable;

/**
 * Login payload shape expected by the VLStream SpringBlade-compatible frontend.
 */
public class BladeAuthInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String token;
    private String tokenType;
    private String account;
    private String userName;
    private String tenantId;
    private long expiresIn;

    public BladeAuthInfo() {
    }

    private BladeAuthInfo(String accessToken, String account, String userName, String tenantId, long expiresIn) {
        this.accessToken = accessToken;
        this.token = accessToken;
        this.tokenType = "Bearer";
        this.account = account;
        this.userName = userName;
        this.tenantId = tenantId;
        this.expiresIn = expiresIn;
    }

    /**
     * Create the password-login token payload consumed by VLStream-Web.
     */
    public static BladeAuthInfo passwordToken(String accessToken, String account, String userName, String tenantId,
                                              long expiresIn) {
        return new BladeAuthInfo(accessToken, account, userName, tenantId, expiresIn);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
