package com.ruoyi.web.controller.config;


import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.framework.config.properties.SecurityProperties;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.web.controller.interceptor.TokenInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * token 配置
 */
@RequiredArgsConstructor
@Slf4j
@Configuration
public class WebMvcConfigToken implements WebMvcConfigurer {
    private final SecurityProperties securityProperties;
    private final ISysUserService sysUserServiceImpl;

//    @Value("${http.apaas-sso}")
//    private String tokenVerificationUrl;
    @Autowired
    private AuthorizationInterceptor authorizationInterceptor;
    @Value(value = "${platform.verifyDataScopeUrl}")
    private String verifyDataScopeUrl;
    @Value("${token.tenantType}")
    private String  tenantType;
    @Value("${token.singleTenantVerifyTokenAddress}")
    private String singleVerifyUrl;
    @Value("${token.singleTenantId}")
    private String singleTenantId;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TokenInterceptor(sysUserServiceImpl, verifyDataScopeUrl, singleVerifyUrl, tenantType,singleTenantId))
            .addPathPatterns("/**")
            .excludePathPatterns(securityProperties.getExcludes());
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/**");
    }
}
