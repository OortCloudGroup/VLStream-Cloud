package org.springblade.core.tool.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Java 8 collection factory used to replace Java 9 immutable map literals.
 */
public final class CollectionUtil {

    private CollectionUtil() {
    }

    /**
     * Builds an insertion-ordered immutable map from alternating key/value arguments.
     */
    public static <K, V> Map<K, V> mapOf(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Map entries must be key/value pairs");
        }
        Map<K, V> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) entries[index];
            @SuppressWarnings("unchecked")
            V value = (V) entries[index + 1];
            result.put(key, value);
        }
        return Collections.unmodifiableMap(result);
    }
}
