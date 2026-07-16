package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladeResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shared parsing helpers for SpringBlade-compatible VLS controllers. */
abstract class VlsControllerSupport {

    /** Convert a real mutation outcome into a business success or an explicit failure. */
    protected BladeResult<Boolean> operationResult(boolean changed, String failureMessage) {
        return changed ? BladeResult.success(Boolean.TRUE) : BladeResult.<Boolean>fail(failureMessage);
    }

    /** Parse a comma-separated primary-key list without silently accepting malformed values. */
    protected List<Long> parseIds(String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<Long>();
        for (String part : ids.split(",")) {
            String value = part == null ? "" : part.trim();
            if (!value.isEmpty()) {
                result.add(Long.valueOf(value));
            }
        }
        return result;
    }

    /** Normalize a SpringBlade current-page parameter. */
    protected long current(Long current) {
        return current == null || current.longValue() < 1L ? 1L : current.longValue();
    }

    /** Normalize a SpringBlade page-size parameter. */
    protected long size(Long size) {
        return size == null || size.longValue() < 1L ? 10L : Math.min(size.longValue(), 500L);
    }
}
