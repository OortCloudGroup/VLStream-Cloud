package com.ruoyi.vlstream.test.vlstream.cloudai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.*;

/** Operator-enrolled legacy weights, pinned by tenant, model ID, source asset and SHA-256. */
@Data
@Component
@ConfigurationProperties(prefix="vlstream.cloud-ai")
public class CloudAiTrustedModels {
    private List<ApprovedModel> trustedModels = new ArrayList<>();
    public ApprovedModel find(Long id) {
        for (ApprovedModel model : trustedModels) if (Objects.equals(id,model.modelId) && CloudAiService.tenant().equals(model.tenantId)
                && model.sourceNodeId != null && model.sha256 != null && model.sha256.matches("[a-f0-9]{64}")) return model;
        return null;
    }
    @Data public static class ApprovedModel {
        private String tenantId;
        private Long modelId;
        private Long sourceNodeId;
        private String sha256;
    }
}
