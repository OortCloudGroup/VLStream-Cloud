package com.ruoyi.vlstream.test.vlstream.recording;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform-recordings")
@SaCheckLogin
@SaCheckPermission("vls:menu:video-playback")
@RequiredArgsConstructor
public class PlatformRecordingController {
    private final PlatformRecordingService service;
    @GetMapping("/runtime") public R<?> runtime() { return R.data(service.runtime()); }
    @PutMapping("/sources/{id}/retention") public R<?> retention(@PathVariable String id, @RequestBody Retention body) { service.retention(id, body.days); return R.success("录像保留时间已保存"); }
    public static class Retention { public Integer days; }
    @GetMapping("/sources") public R<?> sources(@RequestParam String protocol, @RequestParam String rowId) { return R.data(service.list(protocol, rowId)); }
    @PostMapping("/sources") public R<?> bind(@RequestBody Token token) { return R.data(service.bind(token.token)); }
    @PutMapping("/sources/{id}/manual") public R<?> manual(@PathVariable String id, @RequestBody Toggle body) { service.manual(id, body.enabled); return R.success("录像请求已保存"); }
    @PutMapping("/sources/{id}/plan") public R<?> plan(@PathVariable String id, @RequestBody RecordingPlan body) { service.plan(id, body); return R.success("录像计划已保存"); }
    @GetMapping("/sources/{id}/segments") public R<?> segments(@PathVariable String id, @RequestParam String date) { return R.data(service.segments(id, date)); }
    @GetMapping("/segments/{id}/file") public ResponseEntity<Resource> file(@PathVariable String id) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").header("Content-Type", "video/mp4")
            .body(new FileSystemResource(service.file(id)));
    }
    public static class Toggle { public boolean enabled; }
    public static class Token { public String token; }
}
