/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.constant;

/**
 * 通用常量信息
 *
 * @author zhonghuixiong
 */
public interface PlatformConstants {

    /**
     * {@code 500 Server Error} (HTTP/1.0 - RFC 1945)
     */
    Integer SC_INTERNAL_SERVER_ERROR_500 = 500;
    /**
     * {@code 200 OK} (HTTP/1.0 - RFC 1945)
     */
    Integer SC_OK_200 = 200;

    /**
     * publicKey rsa公钥
     */
    String PEM_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n" + "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAzd2eYqgC6A3CvQN982C7\n" + "s3xo2zjL97b0lHkfcAFEy50YHg+g5QD6RbvZt0NTQVOoC5Vv867lp0UBrwAslNCj\n" + "t92QyxKkGLU+10UClkCJsiHpxy/J7AOOpS0wMGao80uWN94EZEsP/FKFfGNz3lJc\n" + "ZRtg4TGCMgXQGlKxBcYJDor/zK/06asTBGp4IxvTDAKTuMP+u88y5nQTUpKPnPst\n" + "wmiLfqZLOSE3y4kIA7VC98GxPY8SqLZ7b9HzLSNoZNXKvA9US7A2F8/A97b8tExX\n" + "sGPOodMQzrLFVr840ZV2WgpxJHqobqYReGhGMN4JdjfGlUvHyhYaCyOcSWalWuLh\n" + "18cDQkum8yUrw5Tki8r8VPDTEZhsOXcq46cMr4437HGYeDA2ib7TOArFq1d0DD9Z\n" + "0DAsjNfhgqqIP9A9kXrs6JIRrkz82skco2WQ5NUdpLT3yaAiXTxmFaajQGVIhFG2\n" + "4VL8CTloRo3FZmy9vMlUseCKmhfCBFbhUG9r7HOuhkO+jY4yfItE8BIrClbkQBAz\n" + "MBMTuRM84VQQ4MnYlbdT3uSt5Qw5WmGIxsAKk93o8Hyhrg/OX8FCwntw2h5AjGDG\n" + "n/H5H0TDAp8vX0NJgh4xhOpNT8pshuX7W1vcqr42sqOjM/mbRPV3s+tT4ynY0xQL\n" + "uqp0P7GW3fu0fT0/OeHPwcsCAwEAAQ==\n" + "-----END PUBLIC KEY-----";

    /**
     * appID
     */
    String APP_ID = "e1a36857e77c4e238703a06e0e57e7a0";

    /**
     * secretKey 字符集
     */
    String SECRET_KEY = "557d8735b655426cb21a4771b901de61";

    /**
     * tenantId
     */
    String TENANT_ID = "0e391fd7-1033-4f09-88c0-187582fee462";

    /**
     * 接口权限上报密钥
     */
    String DATA_REPORT_SECRET = "96eab6a9310c65c9e3f407350e33b995";


    /**
     * 请求头请求app类型
     */
    String APP = "app";

    /**
     * 请求头请求类型
     */
    String HEADER_REQUEST_TYPE = "requestType";

    /**
     * 请求头appID
     */
    String HEADER_APP_ID = "appID";

    /**
     * 请求头serverID
     */
    String HEADER_SERVER_ID = "serverID";

    /**
     * 请求头secretKey
     */
    String HEADER_SERVER_KEY = "secretKey";

    /**
     * 请求头tenantId
     */
    String HEADER_TENANT_ID = "tenantID";

    /**
     * 请求头accessToken
     */
    String HEADER_ACCESS_TOKEN = "accessToken";

}

