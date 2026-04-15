package com.vlstream.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Web配置类
 * 配置静态资源映射，支持视频文件访问
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${recording.storage.path:./recordings}")
    private String recordingStoragePath;
    
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 为所有控制器添加/api前缀，但排除图片上传等特殊控制器和测试控制器
//        configurer.addPathPrefix("/api", c -> !c.getSimpleName().contains("ImageUpload") && !c.getSimpleName().contains("Test"));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 临时禁用认证拦截器，用于测试
        System.out.println("Auth interceptor temporarily disabled for testing");
        
        // 注册认证拦截器，拦截所有API请求
        // registry.addInterceptor(authInterceptor)
        //         .addPathPatterns("/**")  // 拦截所有请求
        //         .excludePathPatterns(    // 排除不需要认证的路径
        //                 "/health",
        //                 "/auth/health",
        //                 "/swagger-ui/**",
        //                 "/swagger-resources/**",
        //                 "/v2/api-docs",
        //                 "/v3/api-docs",
        //                 "/doc.html",
        //                 "/webjars/**",
        //                 "/static/**",
        //                 "/recordings/**",
        //                 "/hls/**",
        //                 "/image/**",
        //                 "/api/video-record/file/**",
        //                 "/api/video-record/thumbnail/**",
        //                 "/api/webrtc/**",
        //                 "/test/**"
        //         );
        
        // System.out.println("Auth interceptor registered successfully");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)  // 临时禁用credentials
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取当前工作目录的绝对路径
        String currentDir = System.getProperty("user.dir");

        // 打印调试信息
        System.out.println("=== 图片路径配置调试信息 ===");
        System.out.println("当前工作目录: " + currentDir);
        System.out.println("================================");
        
        // 配置视频文件访问路径
        registry.addResourceHandler("/recordings/**")
                .addResourceLocations("file:" + recordingStoragePath + "/");
        
        // 配置HLS流文件访问路径
        registry.addResourceHandler("/hls/**")
                .addResourceLocations("file:" + recordingStoragePath + "/hls/");

        // 配置静态资源访问
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // 打印最终的资源映射配置
        System.out.println("=== 资源映射配置 ===");
        System.out.println("================================");
    }
} 