package com.ruoyi.vlstream.test.vlstream.compute;

import java.math.BigDecimal;
import java.util.*;
import lombok.Data;

public interface ComputeCostProvider {
    boolean supports(ComputeNode node);
    Observation cost(ComputeNode node);
    @Data
    class Observation {
        private String source;
        private String priceKind;
        private String currency;
        private String amountUnit;
        private String resourceScope;
        private BigDecimal amount;
        private BigDecimal originalAmount;
        private Date observedAt = new Date();
        private boolean billableCost = false;
        private Map<String,BigDecimal> components = new LinkedHashMap<>();
        private String message;
    }
}
