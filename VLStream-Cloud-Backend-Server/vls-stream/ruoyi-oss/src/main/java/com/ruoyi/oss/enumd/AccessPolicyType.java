/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.oss.enumd;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * configuration
 *
 * @author
 */
@Getter
@AllArgsConstructor
public enum AccessPolicyType {

    /**
     * private
     */
    PRIVATE("0", CannedAccessControlList.Private, PolicyType.WRITE),

    /**
     * public
     */
    PUBLIC("1", CannedAccessControlList.PublicRead, PolicyType.READ),

    /**
     * custom
     */
    CUSTOM("2",CannedAccessControlList.PublicRead, PolicyType.READ);

    /**
     *
     */
    private final String type;

    /**
     * object
     */
    private final CannedAccessControlList acl;

    /**
     *
     */
    private final PolicyType policyType;

    public static AccessPolicyType getByType(String type) {
        for (AccessPolicyType value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        throw new RuntimeException("'type' not found By " + type);
    }

}
