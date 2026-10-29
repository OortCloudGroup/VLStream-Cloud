package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeQueueSnapshot;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeQueueSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vlsCompute/selection")
@RequiredArgsConstructor
public class VlsComputeQueueController {
    private final ComputeQueueSnapshotService queues;

    @SaCheckLogin
    @GetMapping("/{id}/queue")
    public R<ComputeQueueSnapshot> queue(@PathVariable Long id) { return R.data(queues.snapshot(id)); }
}
