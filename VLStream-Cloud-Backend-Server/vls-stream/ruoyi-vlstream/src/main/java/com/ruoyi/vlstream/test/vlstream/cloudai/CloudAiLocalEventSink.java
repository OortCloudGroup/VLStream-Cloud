package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import java.util.List;

/** Single-tenant active safety owns the event, feedback and work-order lifecycle. */
public interface CloudAiLocalEventSink {
    void create(String tenant, long eventId, ActiveSafetyEventReport report);
    void completeEvidence(String tenant, long eventId, List<String> images, List<String> videos);
}
