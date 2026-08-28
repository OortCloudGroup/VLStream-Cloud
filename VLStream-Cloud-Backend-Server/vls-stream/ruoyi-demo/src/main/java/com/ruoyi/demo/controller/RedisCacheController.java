/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.controller;

import com.ruoyi.common.constant.CacheNames;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * spring-cache
 *
 * @author Lion Li
 */
// configuration
//@CacheConfig(cacheNames = CacheNames.DEMO_CACHE)
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/cache")
public class RedisCacheController {

    /**
     * @Cacheable
     * <p>
     * method can , method value will
     * method before, will whether in already value
     * if then , method
     * if , then method , after
     * 「 in Query method 」
     * <p>
     * : and data can
     * : data will and data
     * <p>
     * cacheNames {@link CacheNames} parameter
     */
    @Cacheable(cacheNames = "demo:cache#60s#10m#20", key = "#key", condition = "#key != null")
    @GetMapping("/test1")
    public R<String> test1(String key, String value) {
        return R.ok("操作成功", value);
    }

    /**
     * @CachePut
     * <p>
     * @CachePut method , will method value put ,
     * 「 in Add new method 」
     * <p>
     * cacheNames {@link CacheNames} parameter
     */
    @CachePut(cacheNames = CacheNames.DEMO_CACHE, key = "#key", condition = "#key != null")
    @GetMapping("/test2")
    public R<String> test2(String key, String value) {
        return R.ok("操作成功", value);
    }

    /**
     * @CacheEvict
     * <p>
     * CacheEvict method , will null / empty
     * 「 in Delete method 」
     * <p>
     * cacheNames {@link CacheNames} parameter
     */
    @CacheEvict(cacheNames = CacheNames.DEMO_CACHE, key = "#key", condition = "#key != null")
    @GetMapping("/test3")
    public R<String> test3(String key, String value) {
        return R.ok("操作成功", value);
    }

    /**
     * Set
     * Set 10
     * 11 afterGet Check whether etc.
     */
    @GetMapping("/test6")
    public R<Boolean> test6(String key, String value) {
        RedisUtils.setCacheObject(key, value);
        boolean flag = RedisUtils.expire(key, Duration.ofSeconds(10));
        System.out.println("***********" + flag);
        try {
            Thread.sleep(11 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Object obj = RedisUtils.getCacheObject(key);
        return R.ok(value.equals(obj));
    }

}
