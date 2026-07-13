package com.ruoyi.web.controller.config;

import com.esotericsoftware.minlog.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * 服务注册到星火总线
 */
@Configuration
public class RegistrationConfig {

    @Value("${registration.url}")
    private String url;
    @Value("${registration.app}")
    private String app;
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
    private Environment env;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RegistrationService registrationService() {
        return new RegistrationService(restTemplate());
    }

    @Bean
    public RegistrationListener registrationListener(RegistrationService registrationService) {
        String registrationUrl = url + "/eureka/apps/" + app;
      //  Log.info("服务注册的地址为 = " + registrationUrl);
        String xmlPayload = registrationService.buildXmlPayload();
        return new RegistrationListener(registrationService, registrationUrl, xmlPayload);
    }

    public class RegistrationService {
        private final RestTemplate restTemplate;

        public RegistrationService(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

        public void registerInstance() {
            if (switchSparkBus == 0) {
                return;
            }
            String registrationUrl = url + "/eureka/apps/" + app;
            String xmlPayload = buildXmlPayload();
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/xml");
                headers.set("requestType", requestType);
                headers.set("serviceID", serviceID);
                headers.set("secretKey", secretKey);
                HttpEntity<String> requestEntity = new HttpEntity<>(xmlPayload, headers);
                // 发送 HTTP POST 请求
                ResponseEntity<String> responseEntity = restTemplate.postForEntity(registrationUrl, requestEntity, String.class);
                // 打印响应
                Log.info("Response status code: " + responseEntity.getStatusCodeValue());
                if ("204".equals(responseEntity.getStatusCodeValue())) {
                    Log.info("---------注册到星火成功---------");
                }
            } catch (Exception e) {
                Log.error("---------注册到星火失败---------");
            }
        }
//        public void registerInstance(String registrationUrl, String xmlPayload) {
//            try {
//                if(StringUtils.isBlank(ipAddr) || ObjectUtils.isEmpty(port)){
//                    Log.info("未设置服务ip或端口，不执行注册操作");
//                    return;
//                }
//                HttpHeaders headers = new HttpHeaders();
//                headers.set("Content-Type", "application/xml");
//                headers.set("requestType", requestType);
//                headers.set("serviceID", serviceID);
//                headers.set("secretKey", secretKey);
//                HttpEntity<String> requestEntity = new HttpEntity<>(xmlPayload, headers);
//                // 发送 HTTP POST 请求
//                ResponseEntity<String> responseEntity = restTemplate.postForEntity(registrationUrl, requestEntity, String.class);
//                // 打印响应
//                Log.info("Response status code: " + responseEntity.getStatusCodeValue());
//                if (204 == (responseEntity.getStatusCodeValue())) {
//                    Log.info("---------注册到星火成功---------");
//                }
//            } catch (Exception e){
//                Log.error("---------注册到星火失败---------");
//                e.printStackTrace();
//            }
//
//        }

        public String buildXmlPayload() {
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<instance>");
            xmlBuilder.append("<dataCenterInfo class=\"com.netflix.appinfo.InstanceInfo\">");
            xmlBuilder.append("<name>MyOwn</name>");
            xmlBuilder.append("</dataCenterInfo>");
            xmlBuilder.append("<instanceId>").append(getLocalHostAndPort()).append("</instanceId>");
            xmlBuilder.append("<hostName>").append(getLocalHostAndPort()).append("</hostName>");
            xmlBuilder.append("<app>").append(app).append("</app>");
            xmlBuilder.append("<ipAddr>").append(getLocalHost()).append("</ipAddr>");
            xmlBuilder.append("<status>").append("up").append("</status>");
            xmlBuilder.append("<port enabled=\"true\">").append(getLocalPort()).append("</port>");
            xmlBuilder.append("<securePort enabled=\"false\">").append(443).append("</securePort>");
            xmlBuilder.append("<leaseInfo>");
            xmlBuilder.append("<renewalIntervalInSecs>").append(30).append("</renewalIntervalInSecs>");
            xmlBuilder.append("<durationInSecs>").append(90).append("</durationInSecs>");
            xmlBuilder.append("</leaseInfo>");
            xmlBuilder.append("<homePageUrl>").append("http://" + getLocalHostAndPort() + "/").append("</homePageUrl>");
            xmlBuilder.append("<statusPageUrl>").append("http://" + getLocalHostAndPort() + "/info").append("</statusPageUrl>");
            xmlBuilder.append("<healthCheckUrl>").append("http://" + getLocalHostAndPort() + "/health").append("</healthCheckUrl>");
            xmlBuilder.append("<vipAddress>").append("</vipAddress>");
            xmlBuilder.append("<secureVipAddress>").append("</secureVipAddress>");
            xmlBuilder.append("</instance>");
            return xmlBuilder.toString();
        }
    }

    public class RegistrationListener {

        private final RegistrationService registrationService;
        private final String registrationUrl;
        private final String xmlPayload;

        public RegistrationListener(RegistrationService registrationService, String registrationUrl, String xmlPayload) {
            this.registrationService = registrationService;
            this.registrationUrl = registrationUrl;
            this.xmlPayload = xmlPayload;
            // 在构造函数中调用注册方法
            this.registrationService.registerInstance();
//            this.registrationService.registerInstance(registrationUrl, xmlPayload);
        }
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
