/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * stream
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StreamUtils {

    /**
     * collection
     *
     * @param collection need to collection
     * @param function method
     * @return after list
     */
    public static <E> List<E> filter(Collection<E> collection, Predicate<E> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream().filter(function).collect(Collectors.toList());
    }

    /**
     * collection
     *
     * @param collection need to collection
     * @param function method
     * @return after list
     */
    public static <E> String join(Collection<E> collection, Function<E, String> function) {
        return join(collection, function, StringUtils.SEPARATOR);
    }

    /**
     * collection
     *
     * @param collection need to collection
     * @param function method
     * @param delimiter
     * @return after list
     */
    public static <E> String join(Collection<E> collection, Function<E, String> function, CharSequence delimiter) {
        if (CollUtil.isEmpty(collection)) {
            return StringUtils.EMPTY;
        }
        return collection.stream().map(function).filter(Objects::nonNull).collect(Collectors.joining(delimiter));
    }

    /**
     * collection
     *
     * @param collection need to collection
     * @param comparing method
     * @return after list
     */
    public static <E> List<E> sorted(Collection<E> collection, Comparator<E> comparing) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream().sorted(comparing).collect(Collectors.toList());
    }

    /**
     * collection to map<br>
     * <B>{@code Collection<V>  ---->  Map<K,V>}</B>
     *
     * @param collection need to collection
     * @param key V to K lambda method
     * @param <V> collection in
     * @param <K> map in key
     * @return after map
     */
    public static <V, K> Map<K, V> toIdentityMap(Collection<V> collection, Function<V, K> key) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream().collect(Collectors.toMap(key, Function.identity(), (l, r) -> l));
    }

    /**
     * Collection to map(value and collection )<br>
     * <B>{@code Collection<E> -----> Map<K,V>  }</B>
     *
     * @param collection need to collection
     * @param key E to K lambda method
     * @param value E to V lambda method
     * @param <E> collection in
     * @param <K> map in key
     * @param <V> map in value
     * @return after map
     */
    public static <E, K, V> Map<K, V> toMap(Collection<E> collection, Function<E, K> key, Function<E, V> value) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream().collect(Collectors.toMap(key, value, (l, r) -> l));
    }

    /**
     * collection ( id) map<br>
     * <B>{@code Collection<E> -------> Map<K,List<E>> } </B>
     *
     * @param collection need to collection
     * @param key
     * @param <E> collection in
     * @param <K> map in key
     * @return after map
     */
    public static <E, K> Map<K, List<E>> groupByKey(Collection<E> collection, Function<E, K> key) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection
            .stream()
            .collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * collection ( id, id) layer map<br>
     * <B>{@code Collection<E>  --->  Map<T,Map<U,List<E>>> } </B>
     *
     * @param collection need to collection
     * @param key1
     * @param key2
     * @param <E> collectionelement
     * @param <K> map in key
     * @param <U> map in key
     * @return after map
     */
    public static <E, K, U> Map<K, Map<U, List<E>>> groupBy2Key(Collection<E> collection, Function<E, K> key1, Function<E, U> key2) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection
            .stream()
            .collect(Collectors.groupingBy(key1, LinkedHashMap::new, Collectors.groupingBy(key2, LinkedHashMap::new, Collectors.toList())));
    }

    /**
     * collection ( id, id) layer map<br>
     * <B>{@code Collection<E>  --->  Map<T,Map<U,E>> } </B>
     *
     * @param collection need to collection
     * @param key1
     * @param key2
     * @param <T> map in key
     * @param <U> map in key
     * @param <E> collection in
     * @return after map
     */
    public static <E, T, U> Map<T, Map<U, E>> group2Map(Collection<E> collection, Function<E, T> key1, Function<E, U> key2) {
        if (CollUtil.isEmpty(collection) || key1 == null || key2 == null) {
            return MapUtil.newHashMap();
        }
        return collection
            .stream()
            .collect(Collectors.groupingBy(key1, LinkedHashMap::new, Collectors.toMap(key2, Function.identity(), (l, r) -> l)));
    }

    /**
     * collection to Listcollection, is <br>
     * <B>{@code Collection<E>  ------>  List<T> } </B>
     *
     * @param collection need to collection
     * @param function collection in to list lambda
     * @param <E> collection in
     * @param <T> List in
     * @return after list
     */
    public static <E, T> List<T> toList(Collection<E> collection, Function<E, T> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection
            .stream()
            .map(function)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * collection to Setcollection, is <br>
     * <B>{@code Collection<E>  ------>  Set<T> } </B>
     *
     * @param collection need to collection
     * @param function collection in to set lambda
     * @param <E> collection in
     * @param <T> Set in
     * @return after Set
     */
    public static <E, T> Set<T> toSet(Collection<E> collection, Function<E, T> function) {
        if (CollUtil.isEmpty(collection) || function == null) {
            return CollUtil.newHashSet();
        }
        return collection
            .stream()
            .map(function)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }


    /**
     * key map
     *
     * @param map1 need to map
     * @param map2 need to map
     * @param merge lambda, key value1 value2 , value can is empty
     * @param <K> map in key
     * @param <X> map value
     * @param <Y> map value
     * @param <V> map value
     * @return after map
     */
    public static <K, X, Y, V> Map<K, V> merge(Map<K, X> map1, Map<K, Y> map2, BiFunction<X, Y, V> merge) {
        if (MapUtil.isEmpty(map1) && MapUtil.isEmpty(map2)) {
            return MapUtil.newHashMap();
        } else if (MapUtil.isEmpty(map1)) {
            map1 = MapUtil.newHashMap();
        } else if (MapUtil.isEmpty(map2)) {
            map2 = MapUtil.newHashMap();
        }
        Set<K> key = new HashSet<>();
        key.addAll(map1.keySet());
        key.addAll(map2.keySet());
        Map<K, V> map = new HashMap<>();
        for (K t : key) {
            X x = map1.get(t);
            Y y = map2.get(t);
            V z = merge.apply(x, y);
            if (z != null) {
                map.put(t, z);
            }
        }
        return map;
    }

}
