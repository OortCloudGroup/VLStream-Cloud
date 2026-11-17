package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ComputeCatalogConfigurationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties
    @Import({ModelHubProperties.class, ComputeCatalogProperties.class})
    static class CatalogConfiguration {
    }

    @Test
    void catalogUsesTheSameDeploymentBeanAsModelHub() {
        new ApplicationContextRunner().withUserConfiguration(CatalogConfiguration.class)
            .withPropertyValues(
                "vls.model-hub.base-url=https://deployment.example/bus/apaas-deployment-service",
                "vls.model-hub.app-id=shared-app",
                "vls.model-hub.secret-key=shared-test-secret",
                "vlstream.compute.catalog.base-url=https://obsolete.example",
                "vlstream.compute.catalog.app-id=obsolete-app",
                "vlstream.compute.catalog.secret-key=obsolete-test-secret",
                "vlstream.compute.catalog.refresh-millis=3000")
            .run(context -> {
                assertNull(context.getStartupFailure());
                ModelHubProperties deployment = context.getBean(ModelHubProperties.class);
                ComputeCatalogProperties catalog = context.getBean(ComputeCatalogProperties.class);
                assertSame(deployment, catalog.getDeployment());
                assertEquals("https://deployment.example/bus/apaas-deployment-service", catalog.getDeployment().getBaseUrl());
                assertEquals("shared-app", catalog.getDeployment().getAppId());
                assertEquals("shared-test-secret", catalog.getDeployment().getSecretKey());
                assertTrue(catalog.configured());
                assertEquals(3000, catalog.getRefreshMillis());
            });
    }

    @Test
    void emptySharedDeploymentAddressLeavesHistoricalConfigurationAvailable() {
        new ApplicationContextRunner().withUserConfiguration(CatalogConfiguration.class)
            .withPropertyValues("vls.model-hub.base-url=")
            .run(context -> {
                assertNull(context.getStartupFailure());
                assertFalse(context.getBean(ComputeCatalogProperties.class).configured());
            });
    }

}
