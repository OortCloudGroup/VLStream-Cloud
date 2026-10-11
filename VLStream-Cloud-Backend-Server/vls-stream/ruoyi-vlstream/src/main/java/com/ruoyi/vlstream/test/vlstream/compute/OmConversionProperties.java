package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vlstream.om-conversion")
public class OmConversionProperties {
    private boolean enabled = false;
    private String workDir = "/app/om-work";
}
