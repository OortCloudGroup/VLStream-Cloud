/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.framework.config;

import cn.hutool.core.net.NetUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.ruoyi.framework.handler.CreateAndUpdateMetaObjectHandler;
import com.ruoyi.framework.config.properties.TokenProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * mybatis-plusconfiguration ( )
 *
 * @author Lion Li
 */
@EnableTransactionManagement(proxyTargetClass = true)
@Configuration
@MapperScan("${mybatis-plus.mapperPackage}")
public class MybatisPlusConfig {

    private final CustomTenantLineHandler tenantLineHandler;
    private final TokenProperties tokenProperties;

    public MybatisPlusConfig(CustomTenantLineHandler tenantLineHandler, TokenProperties tokenProperties) {
        this.tenantLineHandler = tenantLineHandler;
        this.tokenProperties = tokenProperties;
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
// // data Process
//        interceptor.addInnerInterceptor(dataPermissionInterceptor());
        //
        interceptor.addInnerInterceptor(paginationInnerInterceptor());
        //
        interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor());
        return interceptor;
    }

//    /**
// * data
//     */
//    public PlusDataPermissionInterceptor dataPermissionInterceptor() {
//        return new PlusDataPermissionInterceptor();
//    }

    /**
     * , data
     */
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();

        // Set , 500 , -1
        paginationInnerInterceptor.setMaxLimit(-1L);
        //
        paginationInnerInterceptor.setOverflow(true);
        return paginationInnerInterceptor;
    }

    /**
     *
     */
    public OptimisticLockerInnerInterceptor optimisticLockerInnerInterceptor() {
        return new OptimisticLockerInnerInterceptor();
    }

    /**
     * objectfieldfill control
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new CreateAndUpdateMetaObjectHandler(tokenProperties);
    }

    /**
     * info Generate
     * ID
     */
    @Bean
    public IdentifierGenerator idGenerator() {
        return new DefaultIdentifierGenerator(NetUtil.getLocalhost());
    }

    /**
     * PaginationInnerInterceptor , data
     * https://baomidou.com/pages/97710a/
     * OptimisticLockerInnerInterceptor
     * https://baomidou.com/pages/0d93c0/
     * MetaObjectHandler objectfieldfill control
     * https://baomidou.com/pages/4c6bcf/
     * ISqlInjector sql
     * https://baomidou.com/pages/42ea4a/
     * BlockAttackInnerInterceptor if is full Delete new operation, then will operation
     * https://baomidou.com/pages/f9a237/
     * IllegalSQLInnerInterceptor sql can ( SQL )
     * IdentifierGenerator Customprimary key
     * https://baomidou.com/pages/568eb2/
     * DynamicTableNameInnerInterceptor
     * https://baomidou.com/pages/2a45ff/
     */

}
