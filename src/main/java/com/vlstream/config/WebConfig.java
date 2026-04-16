package com.vlstream.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Web Configuration Class
 * Configures static resource mappings and supports video file access
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${recording.storage.path:./recordings}")
    private String recordingStoragePath;
    
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Add /api prefix to all controllers, except special controllers like ImageUpload and Test
//        configurer.addPathPrefix("/api", c -> !c.getSimpleName().contains("ImageUpload") && !c.getSimpleName().contains("Test"));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Temporarily disable auth interceptor for testing
        System.out.println("Auth interceptor temporarily disabled for testing");
        
        // Register auth interceptor to intercept all API requests
        // registry.addInterceptor(authInterceptor)
        //         .addPathPatterns("/**")  // Intercept all requests
        //         .excludePathPatterns(    // Exclude paths that don't need authentication
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
        // Get absolute path of current working directory
        String currentDir = System.getProperty("user.dir");

        // Print debug information
        System.out.println("=== Image path configuration debug info ===");
        System.out.println("Current working directory: " + currentDir);
        System.out.println("===============================");
        
        // Configure video file access path
        registry.addResourceHandler("/recordings/**")
                .addResourceLocations("file:" + recordingStoragePath + "/");
        
        // Configure HLS stream file access path
        registry.addResourceHandler("/hls/**")
                .addResourceLocations("file:" + recordingStoragePath + "/hls/");

        // Configure static resource access
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // Print final resource mapping configuration
        System.out.println("=== Resource mapping configuration ===");
        System.out.println("===============================");
    }
} 