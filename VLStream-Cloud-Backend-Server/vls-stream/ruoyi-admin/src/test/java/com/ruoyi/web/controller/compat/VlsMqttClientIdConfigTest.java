package com.ruoyi.web.controller.compat;

import com.ruoyi.vlstream.test.vlstream.config.VlsModelDispatchProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag("dev")
class VlsMqttClientIdConfigTest {
    @Test void backendDefaultIsStable() {
        assertEquals("vls-model-dispatch-backend", bind(environment("8080")).getMqttClientId());
        assertEquals("vls-model-dispatch-backend", bind(environment("8094")).getMqttClientId());
    }

    @Test void explicitLocalLaunchOverrideChangesOnlyTheMultiInstance() {
        MockEnvironment single = environment("8080").withProperty(
            "vlstream.model-dispatch.mqtt-client-id", "vls-model-dispatch-local-testhost");
        MockEnvironment multi = environment("8094").withProperty(
            "vlstream.model-dispatch.mqtt-client-id", "vls-model-dispatch-local-testhost-8094");
        assertEquals("vls-model-dispatch-local-testhost-8094", bind(multi).getMqttClientId());
        assertEquals("vls-model-dispatch-local-testhost", bind(single).getMqttClientId());
        assertNotEquals(bind(single).getMqttClientId(), bind(multi).getMqttClientId());
    }

    @Test void explicitlyConfiguredClientIdIsPreserved() {
        MockEnvironment environment = environment("8094");
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
            "systemEnvironment", Collections.<String, Object>singletonMap(
                "VLSTREAM_MODEL_DISPATCH_MQTT_CLIENT_ID", "explicit-client")));
        assertEquals("explicit-client", bind(environment).getMqttClientId());
    }

    private MockEnvironment environment(String port) {
        return new MockEnvironment().withProperty("COMPUTERNAME", "testhost").withProperty("server.port", port);
    }

    private VlsModelDispatchProperties bind(MockEnvironment environment) {
        return Binder.get(environment).bind("vlstream.model-dispatch", Bindable.of(VlsModelDispatchProperties.class))
            .orElseGet(VlsModelDispatchProperties::new);
    }
}
