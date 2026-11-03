package com.ruoyi.common.core.domain.model;

import java.util.Set;

/** Resolves the current permissions of a trusted authenticated user. */
public interface LivePermissionProvider {
    Set<String> getMenuPermissions(String userId);
    Set<String> getRolePermissions(String userId);
}
