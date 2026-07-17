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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Forces all tenant-shaped controller inputs to the configured single tenant.
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
    @Around("execution(public * com.ruoyi..controller..*(..))")
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
     * Recursively replace tenant values in maps, arrays, collections, and project request beans.
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
        }
        normalizeTenantBean(value);
        normalizeProjectBeanFields(value, visited);
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
     * Force every tenant-shaped String setter on project-owned request beans.
     */
    private void normalizeTenantBean(Object value) {
        Package valuePackage = value.getClass().getPackage();
        if (valuePackage == null || !valuePackage.getName().startsWith("com.ruoyi.")) {
            return;
        }
        for (Method setter : value.getClass().getMethods()) {
            if (!isTenantSetter(setter)) {
                continue;
            }
            try {
                setter.invoke(value, tenantId);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("无法覆盖请求中的租户ID", exception);
            } catch (InvocationTargetException exception) {
                throw new IllegalStateException("无法覆盖请求中的租户ID", exception.getCause());
            }
        }
    }

    /**
     * Traverse nested fields on project request beans so wrapped DTOs cannot hide a tenant value.
     */
    private void normalizeProjectBeanFields(Object value, Set<Object> visited) {
        Package valuePackage = value.getClass().getPackage();
        if (valuePackage == null || !valuePackage.getName().startsWith("com.ruoyi.")) {
            return;
        }
        for (Class<?> type = value.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                try {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    normalizeValue(field.get(value), visited);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("无法检查请求中的嵌套租户ID", exception);
                }
            }
        }
    }

    /**
     * Recognize String setters such as setTenantId, setToTenantId, and setSourceTenantId.
     */
    private static boolean isTenantSetter(Method method) {
        return method.getName().startsWith("set") && method.getParameterTypes().length == 1
            && method.getParameterTypes()[0] == String.class && isTenantKey(method.getName().substring(3));
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
