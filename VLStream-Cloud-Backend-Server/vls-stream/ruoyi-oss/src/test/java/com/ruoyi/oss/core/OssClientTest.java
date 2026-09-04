package com.ruoyi.oss.core;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class OssClientTest {

    @Test
    void usesAwsDefaultRegionWhenConfigurationIsBlank() {
        assertEquals("us-east-1", OssClient.resolveRegion(null));
        assertEquals("us-east-1", OssClient.resolveRegion(""));
        assertEquals("us-east-1", OssClient.resolveRegion("  "));
    }

    @Test
    void preservesConfiguredRegionAfterTrimmingWhitespace() {
        assertEquals("cn-north-1", OssClient.resolveRegion(" cn-north-1 "));
    }
}
