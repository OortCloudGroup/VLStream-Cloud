package com.ruoyi.vlstream.test.vlstream.compute;

import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;

/** Explicit DEV acceptance cleanup; normal paid-run completion never invokes this endpoint. */
@RestController
@SaCheckLogin
@RequiredArgsConstructor
@RequestMapping("/vlsCompute/billing/admin/runs")
public class ComputeTrialRefundController {
    private final ComputeTrialRefundService refunds;

    @GetMapping("/{runId}/trial-refund")
    public R<ComputeTrialRefundService.View> review(@PathVariable String runId) {
        return R.data(refunds.review(runId));
    }

    @PostMapping("/{runId}/trial-refund")
    public R<ComputeTrialRefundService.Result> advance(@PathVariable String runId,
            @RequestBody ComputeTrialRefundService.Request request) {
        return R.data(refunds.advance(runId, request));
    }
}
