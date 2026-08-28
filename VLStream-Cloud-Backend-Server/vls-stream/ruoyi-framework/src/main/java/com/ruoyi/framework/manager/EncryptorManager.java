/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.manager;

import cn.hutool.core.util.ReflectUtil;
import com.ruoyi.common.annotation.EncryptField;
import com.ruoyi.common.encrypt.EncryptContext;
import com.ruoyi.common.encrypt.IEncryptor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *
 *
 * @author
 * @version 4.6.0
 */
@Slf4j
public class EncryptorManager {

    /**
     *
     */
    Map<EncryptContext, IEncryptor> encryptorMap = new ConcurrentHashMap<>();

    /**
     * field
     */
    Map<Class<?>, Set<Field>> fieldCache = new ConcurrentHashMap<>();

    /**
     * Get field
     */
    public Set<Field> getFieldCache(Class<?> sourceClazz) {
        return fieldCache.computeIfAbsent(sourceClazz, clazz -> {
            Field[] declaredFields = clazz.getDeclaredFields();
            Set<Field> fieldSet = Arrays.stream(declaredFields).filter(field ->
                    field.isAnnotationPresent(EncryptField.class) && field.getType() == String.class)
                .collect(Collectors.toSet());
            for (Field field : fieldSet) {
                field.setAccessible(true);
            }
            return fieldSet;
        });
    }

    /**
     * Execute
     *
     * @param encryptContext Execute need to relatedconfigurationparameter
     */
    public IEncryptor registAndGetEncryptor(EncryptContext encryptContext) {
        if (encryptorMap.containsKey(encryptContext)) {
            return encryptorMap.get(encryptContext);
        }
        IEncryptor encryptor = ReflectUtil.newInstance(encryptContext.getAlgorithm().getClazz(), encryptContext);
        encryptorMap.put(encryptContext, encryptor);
        return encryptor;
    }

    /**
     * in Execute
     *
     * @param encryptContext Execute need to relatedconfigurationparameter
     */
    public void removeEncryptor(EncryptContext encryptContext) {
        this.encryptorMap.remove(encryptContext);
    }

    /**
     * configuration . will algorithm and info.
     *
     * @param value value
     * @param encryptContext related configurationinfo
     */
    public String encrypt(String value, EncryptContext encryptContext) {
        IEncryptor encryptor = this.registAndGetEncryptor(encryptContext);
        return encryptor.encrypt(value, encryptContext.getEncode());
    }

    /**
     * configuration
     *
     * @param value value
     * @param encryptContext related configurationinfo
     */
    public String decrypt(String value, EncryptContext encryptContext) {
        IEncryptor encryptor = this.registAndGetEncryptor(encryptContext);
        return encryptor.decrypt(value);
    }

}
