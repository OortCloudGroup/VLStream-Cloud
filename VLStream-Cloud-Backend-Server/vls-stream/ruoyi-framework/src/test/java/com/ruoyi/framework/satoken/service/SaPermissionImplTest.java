package com.ruoyi.framework.satoken.service;

import com.ruoyi.common.core.domain.model.LivePermissionProvider;
import com.ruoyi.common.core.domain.model.LoginUser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class SaPermissionImplTest {
    private static class RecordingProvider implements LivePermissionProvider {
        int permissionCalls;
        int roleCalls;
        boolean enabled = true;

        @Override
        public Set<String> getMenuPermissions(String userId) {
            permissionCalls++;
            return enabled ? Collections.singleton("page:" + userId) : Collections.emptySet();
        }

        @Override
        public Set<String> getRolePermissions(String userId) {
            roleCalls++;
            return enabled ? Collections.singleton("role:" + userId) : Collections.emptySet();
        }
    }

    private static class TestPermission extends SaPermissionImpl {
        private final LoginUser user;
        TestPermission(RecordingProvider provider, LoginUser user) {
            super(provider);
            this.user = user;
        }
        @Override
        LoginUser currentLoginUser() {
            return user;
        }
    }

    @Test
    void queriesOnlyForTheCurrentExactSystemLoginId() {
        RecordingProvider provider = new RecordingProvider();
        LoginUser user = new LoginUser();
        user.setUserType("sys_user");
        user.setUserId("7");
        SaPermissionImpl permission = new TestPermission(provider, user);
        assertEquals(Collections.singletonList("page:7"), permission.getPermissionList("sys_user:7", "login"));
        assertEquals(Collections.singletonList("role:7"), permission.getRoleList("sys_user:7", "login"));
        for (Object wrong : new Object[] {"sys_user:8", "app_user:7", "7", null}) {
            assertTrue(permission.getPermissionList(wrong, "login").isEmpty());
            assertTrue(permission.getRoleList(wrong, "login").isEmpty());
        }
        assertEquals(1, provider.permissionCalls);
        assertEquals(1, provider.roleCalls);
    }

    @Test
    void missingIdentityAndApplicationUsersRemainDenied() {
        RecordingProvider provider = new RecordingProvider();
        SaPermissionImpl missing = new TestPermission(provider, null);
        assertTrue(missing.getPermissionList("sys_user:7", "login").isEmpty());
        LoginUser appUser = new LoginUser();
        appUser.setUserType("app_user");
        appUser.setUserId("7");
        SaPermissionImpl app = new TestPermission(provider, appUser);
        assertTrue(app.getPermissionList("app_user:7", "login").isEmpty());
        assertTrue(app.getRoleList("app_user:7", "login").isEmpty());
        assertEquals(0, provider.permissionCalls);
        assertEquals(0, provider.roleCalls);
    }

    @Test
    void sameSessionSeesRevocationOnTheNextPermissionAndRoleCheck() {
        RecordingProvider provider = new RecordingProvider();
        LoginUser user = new LoginUser();
        user.setUserType("sys_user");
        user.setUserId("7");
        SaPermissionImpl permission = new TestPermission(provider, user);
        assertEquals(Collections.singletonList("page:7"), permission.getPermissionList("sys_user:7", "login"));
        assertEquals(Collections.singletonList("role:7"), permission.getRoleList("sys_user:7", "login"));
        provider.enabled = false;
        assertTrue(permission.getPermissionList("sys_user:7", "login").isEmpty());
        assertTrue(permission.getRoleList("sys_user:7", "login").isEmpty());
        assertEquals(2, provider.permissionCalls);
        assertEquals(2, provider.roleCalls);
    }
}
