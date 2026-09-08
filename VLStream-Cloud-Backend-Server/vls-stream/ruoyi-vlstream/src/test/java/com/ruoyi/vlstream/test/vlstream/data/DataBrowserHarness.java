package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.controller.VlsDataManagementController;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.nio.file.*;
import java.util.*;

/** Manual browser harness with production controllers/services and an isolated MySQL plus local test object store. */
public class DataBrowserHarness {
    public static void main(String[] args) throws Exception {
        System.setProperty("vlstream.data-management.temp-directory", DataTestConfiguration.artifacts().resolve("temporary").toString());
        Files.createDirectories(DataTestConfiguration.artifacts().resolve("temporary"));
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(ch.qos.logback.classic.Level.WARN);
        Tomcat tomcat = new Tomcat(); tomcat.setPort(38081); tomcat.setHostname("127.0.0.1");
        tomcat.setBaseDir(DataTestConfiguration.artifacts().resolve("tomcat").toString());
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context servlet = tomcat.addContext("", DataTestConfiguration.artifacts().toString());
        AnnotationConfigWebApplicationContext web = new AnnotationConfigWebApplicationContext(); web.register(WebConfig.class);
        org.apache.catalina.Wrapper dispatcher = Tomcat.addServlet(servlet, "dispatcher", new DispatcherServlet(web));
        dispatcher.setLoadOnStartup(1); dispatcher.setMultipartConfigElement(new MultipartConfigElement(DataTestConfiguration.artifacts().resolve("temporary").toString(), 524288000, 536870912, 0));
        servlet.addServletMappingDecoded("/", "dispatcher"); tomcat.start();
        DataTestConfiguration.initialize(web.getBean(DataSource.class));
        seed(web); System.out.println("DATA_BROWSER_HARNESS_READY http://127.0.0.1:38081");
        tomcat.getServer().await();
    }

    private static void seed(AnnotationConfigWebApplicationContext web) throws Exception {
        TenantContextHolder.setTenantId("tenant-a");
        DataManagementService data = web.getBean(DataManagementService.class); DataTransferService transfer = web.getBean(DataTransferService.class);
        SqlSessionTemplate sql = web.getBean(SqlSessionTemplate.class);
        DataRequests.Project request = new DataRequests.Project(); request.setProjectCode("ROAD-2026"); request.setAnnotationName("道路车辆样本项目"); request.setProjectType("交通"); request.setRemark("用于验证样本导入、质量审核、划分和版本恢复");
        Long project = data.saveProject(null, request).getId();
        for (int i = 1; i <= 6; i++) transfer.upload(project, new MockMultipartFile[]{new MockMultipartFile("files", "road-" + i + ".png", "image/png", SampleMediaInspectorTest.image(true, i))}, i <= 3 ? "路口 A" : "路口 B");
        AnnotationLabel label = new AnnotationLabel(); label.setAnnotationId(project); label.setName("车辆"); label.setColor("#409eff"); label.setIsDeleted(0); label.setStatus(1); sql.getMapper(VlsAnnotationLabelMapper.class).insert(label);
        for (AnnotationImage sample : data.snapshot(project).getSamples()) {
            AnnotationInstance annotation = new AnnotationInstance(); annotation.setAnnotationId(project); annotation.setImageId(sample.getId()); annotation.setLabelId(label.getId()); annotation.setAnnotationType(AlgorithmAnnotationTypeEnum.rect); annotation.setAnnotationData("{\"x\":10,\"y\":10,\"width\":40,\"height\":40}"); annotation.setIsDeleted(0); annotation.setStatus(1); sql.getMapper(VlsAnnotationInstanceMapper.class).insert(annotation);
        }
        TenantContextHolder.clear();
    }

    @org.springframework.boot.test.context.TestConfiguration
    @EnableWebMvc
    @Import(DataTestConfiguration.class)
    public static class WebConfig implements WebMvcConfigurer {
        @org.springframework.beans.factory.annotation.Autowired private ObjectMapper mapper;
        @Bean VlsDataManagementController controller(DataManagementService data, DataTransferService transfer) { return new VlsDataManagementController(data, transfer); }
        @Bean StandardServletMultipartResolver multipartResolver() { return new StandardServletMultipartResolver(); }
        @Override public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new HandlerInterceptor() {
                @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) { TenantContextHolder.setTenantId("tenant-a"); return true; }
                @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) { TenantContextHolder.clear(); }
            });
        }
        @Override public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
            converters.stream().filter(MappingJackson2HttpMessageConverter.class::isInstance).forEach(converter -> ((MappingJackson2HttpMessageConverter) converter).setObjectMapper(mapper));
        }
        @Override public void addResourceHandlers(ResourceHandlerRegistry registry) { registry.addResourceHandler("/test-media/**").addResourceLocations(DataTestConfiguration.artifacts().resolve("test-objects").toUri().toString() + "/"); }
    }
}
