/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.utils.redis;

import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.redisson.api.RMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Set;

/**
 * operation {@link }
 *
 * @author Michelle.Chung
 * @date 2022/8/13
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings(value = {"unchecked"})
public class CacheUtils {

    private static final CacheManager CACHE_MANAGER = SpringUtils.getBean(CacheManager.class);

    /**
     * Get all KEY
     *
     * @param cacheNames
     */
    public static Set<Object> keys(String cacheNames) {
        RMap<Object, Object> rmap = (RMap<Object, Object>) CACHE_MANAGER.getCache(cacheNames).getNativeCache();
        return rmap.keySet();
    }

    /**
     * Get value
     *
     * @param cacheNames
     * @param key key
     */
    public static <T> T get(String cacheNames, Object key) {
        Cache.ValueWrapper wrapper = CACHE_MANAGER.getCache(cacheNames).get(key);
        return wrapper != null ? (T) wrapper.get() : null;
    }

    /**
     * value
     *
     * @param cacheNames
     * @param key key
     * @param value value
     */
    public static void put(String cacheNames, Object key, Object value) {
        CACHE_MANAGER.getCache(cacheNames).put(key, value);
    }

    /**
     * Delete value
     *
     * @param cacheNames
     * @param key key
     */
    public static void evict(String cacheNames, Object key) {
        CACHE_MANAGER.getCache(cacheNames).evict(key);
    }

    /**
     * null / empty value
     *
     * @param cacheNames
     */
    public static void clear(String cacheNames) {
        CACHE_MANAGER.getCache(cacheNames).clear();
    }

}
