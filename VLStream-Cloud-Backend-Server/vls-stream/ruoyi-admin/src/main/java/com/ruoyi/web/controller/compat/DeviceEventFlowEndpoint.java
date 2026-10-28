package com.ruoyi.web.controller.compat;

import com.ruoyi.vlstream.test.vlstream.service.DeviceEventFlowDiagnostics;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import java.util.Map;

/** Uses the existing management surface; exposes no event data or credentials. */
@Component
@Endpoint(id = "vlstreameventflow")
public class DeviceEventFlowEndpoint {
    private final DeviceEventFlowDiagnostics diagnostics;
    public DeviceEventFlowEndpoint(DeviceEventFlowDiagnostics diagnostics) { this.diagnostics = diagnostics; }
    @ReadOperation public Map<String, Object> counts() { return diagnostics.snapshot(); }
}
