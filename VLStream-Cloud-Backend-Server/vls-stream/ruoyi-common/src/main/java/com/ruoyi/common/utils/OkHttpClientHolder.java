/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.utils;

import okhttp3.OkHttpClient;

public class OkHttpClientHolder {
    public static final OkHttpClient CLIENT =new OkHttpClient.Builder().build();
}
