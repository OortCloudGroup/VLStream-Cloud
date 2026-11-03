package com.ruoyi.vlstream.test.vlstream.compute;

import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administrator authentication and tenant checks are enforced again inside the service.
 * The atomic funding journal is the durable audit; request bodies are not operation-log payloads. */
@RestController
@SaCheckLogin
@RequiredArgsConstructor
@RequestMapping("/vlsCompute/billing/admin/runs")
public class ComputeOriginalReleaseController {
    private final ComputeOriginalReleaseService releases;

    @PostMapping("/{runId}/adopt-original-release")
    public R<String> adopt(@PathVariable String runId,@RequestBody ComputeOriginalReleaseService.Request request) {
        return R.data(releases.reconcile(runId,request));
    }
}
