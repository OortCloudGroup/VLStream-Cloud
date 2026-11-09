package com.ruoyi.vlstream.test.vlstream.recording;

import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Device-authorized, read-only indicator; exposes no recording files or grants. */
@RestController
@SaCheckLogin
@RequiredArgsConstructor
public class PlatformRecordingStatusController {
    private final PlatformRecordingService service;
    @GetMapping("/platform-recordings/status")
    public R<?> status(@RequestParam String protocol, @RequestParam String rowId) {
        return R.data(service.recording(protocol, rowId));
    }
}
