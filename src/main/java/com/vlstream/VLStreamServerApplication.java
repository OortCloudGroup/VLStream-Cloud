package com.vlstream;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * VLStream Cloud 后端服务启动类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.vlstream.mapper")
@ComponentScan(basePackages = {"com.vlstream", "com.vlstream.controller", "com.vlstream.server", "com.vlstream.service", "com.vlstream.mapper"})
public class VLStreamServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VLStreamServerApplication.class, args);
        System.out.println("============ VLStream Cloud 后端服务启动成功 ============");
    }
} 