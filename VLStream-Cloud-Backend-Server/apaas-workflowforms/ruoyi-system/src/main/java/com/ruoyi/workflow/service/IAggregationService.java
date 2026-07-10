package com.ruoyi.workflow.service;

import java.util.Map;

public interface IAggregationService {
    Map<String, Object> getFormAndAppId(String applicationId,String formType);
}
