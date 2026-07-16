/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springblade.core.mp.base.TenantEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Forces all tenant-shaped inputs of VLStream business and location-task APIs to the configured tenant.
 */
@Aspect
@Component
public class VlsSingleTenantRequestAspect {

    private final String tenantId;

    /**
     * Create the request normalizer with the deployment's fixed tenant identifier.
     */
    public VlsSingleTenantRequestAspect(@Value("${vls.tenant.id:000000}") String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Normalize controller arguments before any business method can consume client-supplied tenant values.
     */
    @Around("execution(public * com.ruoyi.vlstream..controller..*(..))"
        + " || execution(public * com.ruoyi.web.controller.compat.LocationTaskCompatController.*(..))")
    public Object forceSingleTenant(ProceedingJoinPoint joinPoint) throws Throwable {
        normalizeArguments(joinPoint.getArgs());
        return joinPoint.proceed();
    }

    /**
     * Normalize an argument array while protecting against cyclic request object graphs.
     */
    void normalizeArguments(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return;
        }
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        for (Object argument : arguments) {
            normalizeValue(argument, visited);
            if (argument instanceof Map) {
                ensureTopLevelTenant((Map<?, ?>) argument);
            }
        }
    }

    /**
     * Add the configured tenant to top-level map requests when the client omitted every tenant alias.
     */
    @SuppressWarnings("unchecked")
    private void ensureTopLevelTenant(Map<?, ?> source) {
        for (Object key : source.keySet()) {
            if (isTenantKey(key)) {
                return;
            }
        }
        ((Map<Object, Object>) source).put("tenantId", tenantId);
    }

    /**
     * Recursively replace tenant values in maps, arrays, collections, and VLStream request beans.
     */
    private void normalizeValue(Object value, Set<Object> visited) {
        if (value == null || isSimpleValue(value) || !visited.add(value)) {
            return;
        }
        if (value instanceof Map) {
            normalizeMap((Map<?, ?>) value, visited);
            return;
        }
        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                normalizeValue(item, visited);
            }
            return;
        }
        if (value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) {
                normalizeValue(Array.get(value, index), visited);
            }
            return;
        }
        if (value instanceof TenantEntity) {
            ((TenantEntity) value).setTenantId(tenantId);
            return;
        }
        normalizeTenantBean(value);
    }

    /**
     * Replace every tenant-shaped map entry and recursively normalize all other values.
     */
    @SuppressWarnings("unchecked")
    private void normalizeMap(Map<?, ?> source, Set<Object> visited) {
        Map<Object, Object> map = (Map<Object, Object>) source;
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if (isTenantKey(entry.getKey())) {
                entry.setValue(tenantId);
            } else {
                normalizeValue(entry.getValue(), visited);
            }
        }
    }

    /**
     * Force tenantId on VLStream DTOs that do not inherit the compatibility TenantEntity base class.
     */
    private void normalizeTenantBean(Object value) {
        Package valuePackage = value.getClass().getPackage();
        if (valuePackage == null || !valuePackage.getName().startsWith("com.ruoyi.vlstream.")) {
            return;
        }
        try {
            Method setter = value.getClass().getMethod("setTenantId", String.class);
            setter.invoke(value, tenantId);
        } catch (NoSuchMethodException ignored) {
            // Most request DTOs do not carry tenant data and need no normalization.
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法覆盖VLStream请求中的tenantId", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("无法覆盖VLStream请求中的tenantId", exception.getCause());
        }
    }

    /**
     * Recognize camel-case and snake-case tenant keys, including legacy device and target variants.
     */
    private static boolean isTenantKey(Object rawKey) {
        if (rawKey == null) {
            return false;
        }
        String key = rawKey.toString().replace("_", "").replace("-", "").toLowerCase();
        return key.endsWith("tenantid");
    }

    /**
     * Skip immutable scalar values that cannot contain nested tenant input.
     */
    private static boolean isSimpleValue(Object value) {
        return value instanceof CharSequence || value instanceof Number || value instanceof Boolean
            || value instanceof Character || value instanceof Enum || value instanceof Class;
    }
}
