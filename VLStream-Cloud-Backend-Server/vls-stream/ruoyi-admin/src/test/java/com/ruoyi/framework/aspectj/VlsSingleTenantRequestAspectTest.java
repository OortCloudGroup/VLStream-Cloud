/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.aspectj;

import com.ruoyi.framework.handler.CreateAndUpdateMetaObjectHandler;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VlsSingleTenantRequestAspectTest {

    /**
     * Verify that request and internally-created entity tenant values all become the configured tenant.
     */
    @Test
    void normalizesEverySupportedTenantInputShape() {
        VlsSingleTenantRequestAspect aspect = new VlsSingleTenantRequestAspect("fixed-tenant");
        LinkedHashMap<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("device_tenant_id", "untrusted-device-tenant");
        LinkedHashMap<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("tenantId", "untrusted-query-tenant");
        request.put("payload", nested);
        LinkedHashMap<String, Object> requestWithoutTenant = new LinkedHashMap<String, Object>();
        requestWithoutTenant.put("status", 1);

        DeviceInfo entity = new DeviceInfo();
        entity.setTenantId("untrusted-entity-tenant");
        DeviceInfo internallyCreatedEntity = new DeviceInfo();

        aspect.normalizeArguments(new Object[] {request, requestWithoutTenant, Arrays.asList(entity)});
        new CreateAndUpdateMetaObjectHandler("fixed-tenant")
            .insertFill(SystemMetaObject.forObject(internallyCreatedEntity));

        assertEquals("fixed-tenant", request.get("tenantId"));
        assertEquals("fixed-tenant", requestWithoutTenant.get("tenantId"));
        assertEquals("fixed-tenant", nested.get("device_tenant_id"));
        assertEquals("fixed-tenant", entity.getTenantId());
        assertEquals("fixed-tenant", internallyCreatedEntity.getTenantId());
    }
}
