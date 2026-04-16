package com.vlstream.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * Swagger Configuration Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Configuration
// @EnableSwagger2WebMvc  // Temporarily disable Swagger to resolve startup issues
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.vlstream.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("VLStream Cloud API Documentation")
                .description("VLStream Cloud Video Stream Management System Backend API Documentation")
                .version("1.0.0")
                .contact(new Contact("VLStream Team", "https://vlstream.com", "support@vlstream.com"))
                .build();
    }
} 