package com.ruoyi.vlstream.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Tag("dev")
class BladeUserInfoBuilderTest {

    @Test
    void buildsBladeUserInfoAliasesFromRuoYiUser() {
        SysUser user = new SysUser();
        user.setUserId("user-1");
        user.setTenantId("tenant-a");
        user.setUserName("admin");
        Set<String> roles = new LinkedHashSet<String>(Arrays.asList("admin", "ops"));
        Set<String> permissions = new LinkedHashSet<String>(Arrays.asList("system:user:list", "workflow:task:list"));

        Map<String, Object> info = new BladeUserInfoBuilder().build("sa-token", user, roles, permissions);

        assertSame(user, info.get("user"));
        assertEquals(roles, info.get("roles"));
        assertEquals(permissions, info.get("permissions"));
        assertEquals("admin", info.get("account"));
        assertEquals("admin", info.get("userName"));
        assertEquals("admin", info.get("realName"));
        assertEquals("tenant-a", info.get("tenantId"));
        assertEquals("sa-token", info.get("accessToken"));
        assertEquals("sa-token", info.get("token"));
        assertEquals("Bearer", info.get("tokenType"));
    }
}
