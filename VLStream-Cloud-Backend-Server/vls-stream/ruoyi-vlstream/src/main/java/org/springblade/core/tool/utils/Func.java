package org.springblade.core.tool.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Identifier conversion helpers compatible with SpringBlade generated APIs.
 */
public final class Func {

    private Func() {
    }

    /**
     * Parses a comma-separated identifier list and fails on malformed identifiers.
     */
    public static List<Long> toLongList(String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String id : ids.split(",")) {
            result.add(Long.valueOf(id.trim()));
        }
        return result;
    }
}
