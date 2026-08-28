/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.SimpleCache;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.cglib.core.Converter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * bean ( cglib can )
 * <p>
 * cglib object
 * : object ( object)
 * ` ` and ` `
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanCopyUtils {

    /**
     * object class
     *
     * @param source data
     * @param desc object Convert after object
     * @return desc
     */
    public static <T, V> V copy(T source, Class<V> desc) {
        if (ObjectUtil.isNull(source)) {
            return null;
        }
        if (ObjectUtil.isNull(desc)) {
            return null;
        }
        final V target = ReflectUtil.newInstanceIfPossible(desc);
        return copy(source, target);
    }

    /**
     * object object
     *
     * @param source data
     * @param desc Convert after object
     * @return desc
     */
    public static <T, V> V copy(T source, V desc) {
        if (ObjectUtil.isNull(source)) {
            return null;
        }
        if (ObjectUtil.isNull(desc)) {
            return null;
        }
        BeanCopier beanCopier = BeanCopierCache.INSTANCE.get(source.getClass(), desc.getClass(), null);
        beanCopier.copy(source, desc, null);
        return desc;
    }

    /**
     * object class
     *
     * @param sourceList data
     * @param desc object Convert after object
     * @return desc
     */
    public static <T, V> List<V> copyList(List<T> sourceList, Class<V> desc) {
        if (ObjectUtil.isNull(sourceList)) {
            return null;
        }
        if (CollUtil.isEmpty(sourceList)) {
            return CollUtil.newArrayList();
        }
        return StreamUtils.toList(sourceList, source -> {
            V target = ReflectUtil.newInstanceIfPossible(desc);
            copy(source, target);
            return target;
        });
    }

    /**
     * bean map
     *
     * @param bean data
     * @return mapobject
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, Object> copyToMap(T bean) {
        if (ObjectUtil.isNull(bean)) {
            return null;
        }
        return BeanMap.create(bean);
    }

    /**
     * map bean
     *
     * @param map data
     * @param beanClass bean
     * @return beanobject
     */
    public static <T> T mapToBean(Map<String, Object> map, Class<T> beanClass) {
        if (MapUtil.isEmpty(map)) {
            return null;
        }
        if (ObjectUtil.isNull(beanClass)) {
            return null;
        }
        T bean = ReflectUtil.newInstanceIfPossible(beanClass);
        return mapToBean(map, bean);
    }

    /**
     * map bean
     *
     * @param map data
     * @param bean beanobject
     * @return beanobject
     */
    public static <T> T mapToBean(Map<String, Object> map, T bean) {
        if (MapUtil.isEmpty(map)) {
            return null;
        }
        if (ObjectUtil.isNull(bean)) {
            return null;
        }
        BeanMap.create(bean).putAll(map);
        return bean;
    }

    /**
     * map map
     *
     * @param map data
     * @param clazz object
     * @return mapobject
     */
    public static <T, V> Map<String, V> mapToMap(Map<String, T> map, Class<V> clazz) {
        if (MapUtil.isEmpty(map)) {
            return null;
        }
        if (ObjectUtil.isNull(clazz)) {
            return null;
        }
        Map<String, V> copyMap = new LinkedHashMap<>(map.size());
        map.forEach((k, v) -> copyMap.put(k, copy(v, clazz)));
        return copyMap;
    }

    /**
     * BeanCopierproperty <br>
     * can
     *
     * @author Looly
     * @since 5.4.1
     */
    public enum BeanCopierCache {
        /**
         * BeanCopierproperty
         */
        INSTANCE;

        private final SimpleCache<String, BeanCopier> cache = new SimpleCache<>();

        /**
         * and Convert Generate key in {@link BeanCopier} Map in element
         *
         * @param srcClass Bean
         * @param targetClass Bean
         * @param converter Convert
         * @return Map in BeanCopier
         */
        public BeanCopier get(Class<?> srcClass, Class<?> targetClass, Converter converter) {
            final String key = genKey(srcClass, targetClass, converter);
            return cache.get(key, () -> BeanCopier.create(srcClass, targetClass, converter != null));
        }

        /**
         * and Convert Generate key
         *
         * @param srcClass Bean
         * @param targetClass Bean
         * @param converter Convert
         * @return property and Map key
         */
        private String genKey(Class<?> srcClass, Class<?> targetClass, Converter converter) {
            final StringBuilder key = StrUtil.builder()
                .append(srcClass.getName()).append('#').append(targetClass.getName());
            if (null != converter) {
                key.append('#').append(converter.getClass().getName());
            }
            return key.toString();
        }
    }

}
