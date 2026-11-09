package com.ruoyi.vlstream.test.vlstream.cloudai;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cloud-device")
@SaCheckLogin
@SaCheckPermission("vls:menu:cloud-algorithm-config")
@RequiredArgsConstructor
public class CloudDeviceController {
    private final CloudDeviceService service;
    private final CloudAiService catalogue;
    private final CloudAiSources sources;
    private final CloudDeviceEvents events;
    @GetMapping("/nodes") public R<?> nodes(){return R.data(catalogue.nodeChoices());}
    @GetMapping("/models") public R<?> models(@RequestParam Long nodeId){return R.data(catalogue.modelChoices(nodeId));}
    @PostMapping("/sources") public R<?> bind(@RequestBody CloudAiController.Token body){return R.data(sources.bind(body.token));}
    @GetMapping("/bindings") public R<?> list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return R.data(service.page(page,size));}
    @PostMapping("/bindings") public R<?> apply(@RequestBody CloudDeviceService.Batch body){return R.data(service.apply(body));}
    public static class Toggle { public int revision;public boolean enabled; }
    @PutMapping("/bindings/{id}/enabled") public R<?> enabled(@PathVariable String id,@RequestBody Toggle body){service.enabled(id,body.revision,body.enabled);return R.success("配置已保存，正在应用");}
    @GetMapping("/events") public R<?> events(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return R.data(events.page(page,size,null,null,null,null));}
    @GetMapping("/events/{id}") public R<?> event(@PathVariable Long id){return R.data(events.detail(id));}
}
