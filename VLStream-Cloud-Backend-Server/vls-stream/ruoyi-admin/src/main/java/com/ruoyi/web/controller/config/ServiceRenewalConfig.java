/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.config;

import com.esotericsoftware.minlog.Log;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 服务续约到星火总线
 */
@Configuration
public class ServiceRenewalConfig {

    @Value("${registration.app}")
    private String app;
    @Value("${registration.url}")
    private String url;
    @Value("${registration.ipAddr}")
    private String ipAddr;
    @Value("${registration.port}")
    private Integer port;
    @Value("${registration.serviceID}")
    private String serviceID;
    @Value("${registration.secretKey}")
    private String secretKey;
    @Value("${registration.requestType}")
    private String requestType;
    @Value("${registration.switchSparkBus}")
    private Integer switchSparkBus;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RegistrationConfig.RegistrationService registrationService;

    @Scheduled(fixedRate = 30000) // 每30秒执行一次续约任务
    public void sendRenewalRequest() {
        if (switchSparkBus == 0) {
            return;
        }
        if(StringUtils.isBlank(ipAddr) || ObjectUtils.isEmpty(port)){
            Log.info("未设置服务ip或端口，不执行续约操作");
            return;
        }
        if(StringUtils.isBlank(ipAddr) || ObjectUtils.isEmpty(port)){
            Log.info("未设置服务ip或端口，不执行续约操作");
            return;
        }
        try {
            String urlString = url +"/eureka/apps/" + app + "/" + getLocalHostAndPort();
            Log.info("开始续约，服务续约的地址为 = " + urlString);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/xml");
            headers.set("requestType", requestType);
            headers.set("serviceID", serviceID);
            headers.set("secretKey", secretKey);

            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

            // 发送 HTTP PUT 请求
            ResponseEntity<String> responseEntity = restTemplate.exchange(urlString, HttpMethod.PUT, requestEntity, String.class);
            // 打印响应
            System.out.println("服务续约请求响应码：" + responseEntity.getStatusCodeValue());
            if (responseEntity.getStatusCodeValue() == 200) {
                Log.info("服务续约成功！");
            } else if (responseEntity.getStatusCodeValue() == 404) {
                Log.info("服务已过期！");
            } else {
                Log.error("服务续约请求失败！");
            }
        } catch (Exception e) {
            Log.error("服务续约请求失败！");
            Log.info("尝试重新注册！");
            registrationService.registerInstance();
        }
    }

    @Bean
    public RestTemplate restTemplate1() {
        return new RestTemplate();
    }


    public String getLocalHost() {
        return ipAddr;
    }

    public Integer getLocalPort() {
        return port;
    }

    public String getLocalHostAndPort() {
        return getLocalHost() + ":" + getLocalPort();
    }
}
