package com.ruoyi.common.utils;

import okhttp3.OkHttpClient;

public class OkHttpClientHolder {
    public static final OkHttpClient CLIENT =new OkHttpClient.Builder().build();
}
