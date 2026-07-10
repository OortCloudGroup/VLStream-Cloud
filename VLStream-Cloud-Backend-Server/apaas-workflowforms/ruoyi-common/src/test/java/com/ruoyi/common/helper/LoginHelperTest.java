package com.ruoyi.common.helper;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.application.SaSetValueInterface;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaStorage;
import com.ruoyi.common.core.domain.model.LoginUser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class LoginHelperTest {

    @Test
    void getUsernameReturnsCurrentLoginUserUsernameForWorkflowServices() {
        SaTokenContext previousContext = SaManager.getSaTokenContext();
        MapSaStorage storage = new MapSaStorage();
        LoginUser loginUser = new LoginUser();
        loginUser.setUsername("workflow-admin");
        loginUser.setNickName("Workflow Admin");

        try {
            SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
            SaTokenContextForThreadLocalStorage.setBox(null, null, storage);
            storage.set(LoginHelper.LOGIN_USER_KEY, loginUser);

            assertEquals("workflow-admin", LoginHelper.getUsername());
        } finally {
            SaTokenContextForThreadLocalStorage.clearBox();
            SaManager.setSaTokenContext(previousContext);
        }
    }

    private static class MapSaStorage implements SaStorage {

        private final Map<String, Object> values = new HashMap<String, Object>();

        @Override
        public Object getSource() {
            return values;
        }

        @Override
        public Object get(String key) {
            return values.get(key);
        }

        @Override
        public SaStorage set(String key, Object value) {
            values.put(key, value);
            return this;
        }

        @Override
        public SaStorage delete(String key) {
            values.remove(key);
            return this;
        }
    }
}
