/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.oss.properties;

import lombok.Data;

/**
 * OSSobject configurationproperty
 *
 * @author Lion Li
 */
@Data
public class OssProperties {

    /**
     *
     */
    private String endpoint;

    /**
     * Custom
     */
    private String domain;

    /**
     * before
     */
    private String prefix;

    /**
     * ACCESS_KEY
     */
    private String accessKey;

    /**
     * SECRET_KEY
     */
    private String secretKey;

    /**
     * null / empty
     */
    private String bucketName;

    /**
     *
     */
    private String region;

    /**
     * whether https (Y= is ,N= )
     */
    private String isHttps;

    /**
     * (0private 1public 2custom)
     */
    private String accessPolicy;

    /**
     * 获取节点终端地址。
     *
     * @return 终端地址
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * 设置节点终端地址。
     *
     * @param endpoint 终端地址
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * 获取自定义域名。
     *
     * @return 自定义域名
     */
    public String getDomain() {
        return domain;
    }

    /**
     * 设置自定义域名。
     *
     * @param domain 自定义域名
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * 获取前缀。
     *
     * @return 前缀
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 设置前缀。
     *
     * @param prefix 前缀
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * 获取访问密钥。
     *
     * @return 访问密钥
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * 设置访问密钥。
     *
     * @param accessKey 访问密钥
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * 获取加密密钥。
     *
     * @return 加密密钥
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * 设置加密密钥。
     *
     * @param secretKey 加密密钥
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * 获取存储桶名称。
     *
     * @return 存储桶名称
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * 设置存储桶名称。
     *
     * @param bucketName 存储桶名称
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * 获取所属区域。
     *
     * @return 所属区域
     */
    public String getRegion() {
        return region;
    }

    /**
     * 设置所属区域。
     *
     * @param region 所属区域
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * 获取是否使用 HTTPS。
     *
     * @return 是否使用 HTTPS
     */
    public String getIsHttps() {
        return isHttps;
    }

    /**
     * 设置是否使用 HTTPS。
     *
     * @param isHttps 是否使用 HTTPS
     */
    public void setIsHttps(String isHttps) {
        this.isHttps = isHttps;
    }

    /**
     * 获取访问策略类型。
     *
     * @return 访问策略类型
     */
    public String getAccessPolicy() {
        return accessPolicy;
    }

    /**
     * 设置访问策略类型。
     *
     * @param accessPolicy 访问策略类型
     */
    public void setAccessPolicy(String accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OssProperties that = (OssProperties) o;
        return java.util.Objects.equals(endpoint, that.endpoint) &&
            java.util.Objects.equals(domain, that.domain) &&
            java.util.Objects.equals(prefix, that.prefix) &&
            java.util.Objects.equals(accessKey, that.accessKey) &&
            java.util.Objects.equals(secretKey, that.secretKey) &&
            java.util.Objects.equals(bucketName, that.bucketName) &&
            java.util.Objects.equals(region, that.region) &&
            java.util.Objects.equals(isHttps, that.isHttps) &&
            java.util.Objects.equals(accessPolicy, that.accessPolicy);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(endpoint, domain, prefix, accessKey, secretKey, bucketName, region, isHttps, accessPolicy);
    }
}
