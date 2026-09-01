/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.oss.core;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oss.constant.OssConstant;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.enumd.AccessPolicyType;
import com.ruoyi.oss.enumd.PolicyType;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.properties.OssProperties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * S3 all S3
 * minio
 *
 * @author Lion Li
 */
public class OssClient {

    private final String configKey;

    private final OssProperties properties;

    private final AmazonS3 client;

    public OssClient(String configKey, OssProperties ossProperties) {
        this.configKey = configKey;
        this.properties = ossProperties;
        try {
            this.client = buildClient(properties.getEndpoint());

            createBucket();
        } catch (Exception e) {
            if (e instanceof OssException) {
                throw e;
            }
            throw new OssException("配置错误! 请检查系统配置:[" + e.getMessage() + "]");
        }
    }

    public void createBucket() {
        try {
            String bucketName = properties.getBucketName();
            if (client.doesBucketExistV2(bucketName)) {
                return;
            }
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            AccessPolicyType accessPolicy = getAccessPolicy();
            createBucketRequest.setCannedAcl(accessPolicy.getAcl());
            client.createBucket(createBucketRequest);
            client.setBucketPolicy(bucketName, getPolicy(bucketName, accessPolicy.getPolicyType()));
        } catch (Exception e) {
            throw new OssException("创建Bucket失败, 请核对配置信息:[" + e.getMessage() + "]");
        }
    }

    public UploadResult upload(byte[] data, String path, String contentType) {
        return upload(new ByteArrayInputStream(data), path, contentType);
    }

    public UploadResult upload(InputStream inputStream, String path, String contentType) {
        if (!(inputStream instanceof ByteArrayInputStream)) {
            inputStream = new ByteArrayInputStream(IoUtil.readBytes(inputStream));
        }
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(inputStream.available());
            PutObjectRequest putObjectRequest = new PutObjectRequest(properties.getBucketName(), path, inputStream, metadata);
            // Set object Acl to
            putObjectRequest.setCannedAcl(getAccessPolicy().getAcl());
            client.putObject(putObjectRequest);
        } catch (Exception e) {
            throw new OssException("上传文件失败，请检查配置信息:[" + e.getMessage() + "]");
        }
        return UploadResult.builder().url(getUrl() + "/" + path).filename(path).build();
    }

    public void delete(String path) {
        path = path.replace(getUrl() + "/", "");
        try {
            client.deleteObject(properties.getBucketName(), path);
        } catch (Exception e) {
            throw new OssException("删除文件失败，请检查配置信息:[" + e.getMessage() + "]");
        }
    }

    public UploadResult uploadSuffix(byte[] data, String suffix, String contentType) {
        return upload(data, getPath(properties.getPrefix(), suffix), contentType);
    }

    public UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType) {
        return upload(inputStream, getPath(properties.getPrefix(), suffix), contentType);
    }

    /**
     * Get data
     *
     * @param path
     */
    public ObjectMetadata getObjectMetadata(String path) {
        path = path.replace(getUrl() + "/", "");
        return client.getObjectMetadata(properties.getBucketName(), path);
    }

    public InputStream getObjectContent(String path) {
        path = path.replace(getUrl() + "/", "");
        S3Object object = client.getObject(properties.getBucketName(), path);
        return object.getObjectContent();
    }

    public String getUrl() {
        String domain = properties.getDomain();
        String endpoint = properties.getEndpoint();
        String header = OssConstant.IS_HTTPS.equals(properties.getIsHttps()) ? "https://" : "http://";
        // service
        if (StringUtils.containsAny(endpoint, OssConstant.CLOUD_SERVICE)) {
            if (StringUtils.isNotBlank(domain)) {
                return header + domain;
            }
            return header + properties.getBucketName() + "." + endpoint;
        }
        // minio Process
        if (StringUtils.isNotBlank(domain)) {
            return header + domain + "/" + properties.getBucketName();
        }
        return header + endpoint + "/" + properties.getBucketName();
    }

    public String getPath(String prefix, String suffix) {
        // Generate uuid
        String uuid = IdUtil.fastSimpleUUID();
        //
        String path = DateUtils.datePath() + "/" + uuid;
        if (StringUtils.isNotBlank(prefix)) {
            path = prefix + "/" + path;
        }
        return path + suffix;
    }


    public String getConfigKey() {
        return configKey;
    }

    /**
     * Returns the bucket used by this client so callers can retain an object key
     * without persisting a volatile pre-signed URL.
     */
    public String getBucketName() {
        return properties.getBucketName();
    }

    /**
     * Get URL
     *
     * @param objectKey objectKEY
     * @param second
     */
    public String getPrivateUrl(String objectKey, Integer second) {
        return getPrivateUrl(objectKey, second, null);
    }

    /**
     * Get URL .
     * publicEndpoint is empty after OSS endpoint.
     */
    public String getPrivateUrl(String objectKey, Integer second, String publicEndpoint) {
        AmazonS3 signingClient = signingClient(publicEndpoint);
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
            new GeneratePresignedUrlRequest(properties.getBucketName(), objectKey)
                .withMethod(HttpMethod.GET)
                .withExpiration(new Date(System.currentTimeMillis() + 1000L * second));
        try {
            URL url = signingClient.generatePresignedUrl(generatePresignedUrlRequest);
            return url.toString();
        } finally {
            shutdownSigningClient(signingClient);
        }
    }

    /**
     * Get object PUT . only object ,
     * need to also object AccessKey/SecretKey.
     *
     * @param objectKey objectKEY
     * @param contentType Content-Type
     * @param second
     */
    public String getPresignedPutUrl(String objectKey, String contentType, Integer second) {
        return getPresignedPutUrl(objectKey, contentType, second, null);
    }

    /**
     * Get object PUT .
     */
    public String getPresignedPutUrl(String objectKey, String contentType, Integer second,
                                     String publicEndpoint) {
        AmazonS3 signingClient = signingClient(publicEndpoint);
        GeneratePresignedUrlRequest request =
            new GeneratePresignedUrlRequest(properties.getBucketName(), objectKey)
                .withMethod(HttpMethod.PUT)
                .withExpiration(new Date(System.currentTimeMillis() + 1000L * second));
        if (StringUtils.isNotBlank(contentType)) {
            request.setContentType(contentType);
        }
        try {
            return signingClient.generatePresignedUrl(request).toString();
        } finally {
            shutdownSigningClient(signingClient);
        }
    }

    /**
     * Check objectwhether already .
     */
    public boolean doesObjectExist(String objectKey) {
        return client.doesObjectExist(properties.getBucketName(), objectKey);
    }

    /**
     * configurationwhether
     */
    public boolean checkPropertiesSame(OssProperties properties) {
        return this.properties.equals(properties);
    }

    /**
     * Get current
     *
     * @return current code
     */
    public AccessPolicyType getAccessPolicy() {
        return AccessPolicyType.getByType(properties.getAccessPolicy());
    }

    private AmazonS3 signingClient(String publicEndpoint) {
        if (StringUtils.isBlank(publicEndpoint)
            || StringUtils.equals(publicEndpoint, properties.getEndpoint())) {
            return client;
        }
        return buildClient(publicEndpoint);
    }

    private void shutdownSigningClient(AmazonS3 signingClient) {
        if (signingClient != client) {
            signingClient.shutdown();
        }
    }

    private AmazonS3 buildClient(String endpoint) {
        AwsClientBuilder.EndpointConfiguration endpointConfig =
            new AwsClientBuilder.EndpointConfiguration(endpoint, properties.getRegion());
        AWSCredentials credentials = new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey());
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        ClientConfiguration clientConfig = new ClientConfiguration();
        if (StringUtils.startsWithIgnoreCase(endpoint, "https://")
            || OssConstant.IS_HTTPS.equals(properties.getIsHttps())) {
            clientConfig.setProtocol(Protocol.HTTPS);
        } else {
            clientConfig.setProtocol(Protocol.HTTP);
        }
        AmazonS3ClientBuilder build = AmazonS3Client.builder()
            .withEndpointConfiguration(endpointConfig)
            .withClientConfiguration(clientConfig)
            .withCredentials(credentialsProvider)
            .disableChunkedEncoding();
        if (!StringUtils.containsAny(endpoint, OssConstant.CLOUD_SERVICE)) {
            // MinIO , Host and .
            build.enablePathStyleAccess();
        }
        return build.build();
    }

    private static String getPolicy(String bucketName, PolicyType policyType) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n\"Statement\": [\n{\n\"Action\": [\n");
        if (policyType == PolicyType.WRITE) {
            builder.append("\"s3:GetBucketLocation\",\n\"s3:ListBucketMultipartUploads\"\n");
        } else if (policyType == PolicyType.READ_WRITE) {
            builder.append("\"s3:GetBucketLocation\",\n\"s3:ListBucket\",\n\"s3:ListBucketMultipartUploads\"\n");
        } else {
            builder.append("\"s3:GetBucketLocation\"\n");
        }
        builder.append("],\n\"Effect\": \"Allow\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
        builder.append(bucketName);
        builder.append("\"\n},\n");
        if (policyType == PolicyType.READ) {
            builder.append("{\n\"Action\": [\n\"s3:ListBucket\"\n],\n\"Effect\": \"Deny\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
            builder.append(bucketName);
            builder.append("\"\n},\n");
        }
        builder.append("{\n\"Action\": ");
        switch (policyType) {
            case WRITE:
                builder.append("[\n\"s3:AbortMultipartUpload\",\n\"s3:DeleteObject\",\n\"s3:ListMultipartUploadParts\",\n\"s3:PutObject\"\n],\n");
                break;
            case READ_WRITE:
                builder.append("[\n\"s3:AbortMultipartUpload\",\n\"s3:DeleteObject\",\n\"s3:GetObject\",\n\"s3:ListMultipartUploadParts\",\n\"s3:PutObject\"\n],\n");
                break;
            default:
                builder.append("\"s3:GetObject\",\n");
                break;
        }
        builder.append("\"Effect\": \"Allow\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
        builder.append(bucketName);
        builder.append("/*\"\n}\n],\n\"Version\": \"2012-10-17\"\n}\n");
        return builder.toString();
    }

}
