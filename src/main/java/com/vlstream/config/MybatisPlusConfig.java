package com.vlstream.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus Configuration Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * Pagination plugin
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        return interceptor;
    }

    /**
     * Auto-fill configuration
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                // 支持多种时间字段格式
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
                
                // 支持多种创建人字段格式
                this.strictInsertFill(metaObject, "creator", String.class, "admin");
                this.strictInsertFill(metaObject, "updater", String.class, "admin");
                this.strictInsertFill(metaObject, "createdBy", String.class, "admin");
                this.strictInsertFill(metaObject, "updatedBy", String.class, "admin");
                
                // 支持Long类型的创建人字段
                this.strictInsertFill(metaObject, "createdBy", Long.class, 1L);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                // 支持多种时间字段格式
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
                
                // 支持多种更新人字段格式
                this.strictUpdateFill(metaObject, "updater", String.class, "admin");
                this.strictUpdateFill(metaObject, "updatedBy", String.class, "admin");
            }
        };
    }
} 