package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.controller.VlsDataManagementController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsDatasetImportController;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.bind.annotation.*;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.*;
import java.nio.file.Files;
import java.util.List;

/** Loopback-only test server using the current local MinIO and dedicated MySQL; never loads production app jobs. */
public class DatasetImportBrowserHarness {
    public static void main(String[] args)throws Exception{
        System.setProperty("vlstream.data-management.temp-directory",DatasetImportTestConfiguration.artifacts().resolve("temporary").toString());
        System.setProperty("vlstream.data-management.allow-private-s3","true");
        Files.createDirectories(DatasetImportTestConfiguration.artifacts().resolve("temporary"));
        ((ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(ch.qos.logback.classic.Level.WARN);
        Tomcat tomcat=new Tomcat();tomcat.setPort(38081);tomcat.setHostname("127.0.0.1");tomcat.setBaseDir(DatasetImportTestConfiguration.artifacts().resolve("tomcat").toString());
        tomcat.getConnector().setProperty("address","127.0.0.1");
        Context servlet=tomcat.addContext("",DatasetImportTestConfiguration.artifacts().toString());
        AnnotationConfigWebApplicationContext web=new AnnotationConfigWebApplicationContext();web.register(Web.class);
        org.apache.catalina.Wrapper wrapper=Tomcat.addServlet(servlet,"dispatcher",new DispatcherServlet(web));wrapper.setLoadOnStartup(1);
        wrapper.setMultipartConfigElement(new MultipartConfigElement(DatasetImportTestConfiguration.artifacts().resolve("temporary").toString(),16777216,20000000,0));
        servlet.addServletMappingDecoded("/","dispatcher");tomcat.start();
        TenantContextHolder.setTenantId("tenant-import-test");
        DataRequests.Project request=new DataRequests.Project();request.setProjectCode("DS-BROWSER-"+System.currentTimeMillis());request.setAnnotationName("断点续传浏览器验收");
        web.getBean(DataManagementService.class).saveProject(null,request);TenantContextHolder.clear();
        System.out.println("DATASET_IMPORT_BROWSER_READY");tomcat.getServer().await();
    }
    @org.springframework.boot.test.context.TestConfiguration
    @EnableWebMvc @EnableScheduling @Import(DatasetImportTestConfiguration.class)
    public static class Web implements WebMvcConfigurer {
        @org.springframework.beans.factory.annotation.Autowired ObjectMapper json;
        @Bean VlsDataManagementController dataController(DataManagementService data,DataTransferService transfer){return new VlsDataManagementController(data,transfer);}
        @Bean VlsDatasetImportController importController(DatasetUploadService uploads,DatasetSourceService sources){return new VlsDatasetImportController(uploads,sources);}
        @Bean com.ruoyi.vlstream.test.vlstream.controller.VlsVideoFrameController frameController(VideoFrameService service,VideoFrameRegistry registry){return new com.ruoyi.vlstream.test.vlstream.controller.VlsVideoFrameController(service,registry);}
        @Bean StandardServletMultipartResolver multipartResolver(){return new StandardServletMultipartResolver();}
        @Bean Errors errors(){return new Errors();}
        @Override public void extendMessageConverters(List<HttpMessageConverter<?>> converters){for(HttpMessageConverter<?> converter:converters)if(converter instanceof MappingJackson2HttpMessageConverter)((MappingJackson2HttpMessageConverter)converter).setObjectMapper(json);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new HandlerInterceptor(){
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){TenantContextHolder.setTenantId("tenant-import-test");return true;}
            @Override public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception e){TenantContextHolder.clear();}
        });}
    }
    @RestControllerAdvice public static class Errors {
        @ExceptionHandler(ServiceException.class) public Object business(ServiceException e){return DataManagementService.map("code",500,"success",false,"msg",e.getMessage());}
    }
}
