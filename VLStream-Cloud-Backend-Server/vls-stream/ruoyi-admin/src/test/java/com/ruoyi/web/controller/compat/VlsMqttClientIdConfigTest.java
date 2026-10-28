package com.ruoyi.web.controller.compat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag("dev")
class VlsMqttClientIdConfigTest {
    @Test void repositoryDevDefaultIsPreservedForBothPorts() throws Exception {
        String single = environment("8080").getProperty("vlstream.model-dispatch.mqtt-client-id");
        String multi = environment("8094").getProperty("vlstream.model-dispatch.mqtt-client-id");
        assertEquals("vls-model-dispatch-local-testhost", single);
        assertEquals(single, multi);
    }

    @Test void explicitLocalLaunchOverrideChangesOnlyTheMultiInstance() throws Exception {
        MockEnvironment single = environment("8080");
        MockEnvironment multi = environment("8094");
        multi.setProperty("vlstream.model-dispatch.mqtt-client-id", "vls-model-dispatch-local-testhost-8094");
        assertEquals("vls-model-dispatch-local-testhost-8094", multi.getProperty("vlstream.model-dispatch.mqtt-client-id"));
        assertEquals("vls-model-dispatch-local-testhost", single.getProperty("vlstream.model-dispatch.mqtt-client-id"));
        assertNotEquals(single.getProperty("vlstream.model-dispatch.mqtt-client-id"), multi.getProperty("vlstream.model-dispatch.mqtt-client-id"));
    }

    @Test void explicitlyConfiguredClientIdIsPreserved() throws Exception {
        MockEnvironment environment = environment("8094");
        environment.setProperty("VLSTREAM_MODEL_DISPATCH_MQTT_CLIENT_ID", "explicit-client");
        assertEquals("explicit-client", environment.getProperty("vlstream.model-dispatch.mqtt-client-id"));
    }

    private MockEnvironment environment(String port) throws Exception {
        // The source YAML includes local credentials. Never log whole configuration documents.
        ((ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("org.springframework.boot.env.OriginTrackedYamlLoader"))
            .setLevel(ch.qos.logback.classic.Level.WARN);
        MockEnvironment environment = new MockEnvironment().withProperty("COMPUTERNAME", "testhost").withProperty("server.port", port);
        for (PropertySource<?> source : new YamlPropertySourceLoader().load("dev", new ClassPathResource("application-dev.yml"))) {
            environment.getPropertySources().addLast(source);
        }
        return environment;
    }
}
