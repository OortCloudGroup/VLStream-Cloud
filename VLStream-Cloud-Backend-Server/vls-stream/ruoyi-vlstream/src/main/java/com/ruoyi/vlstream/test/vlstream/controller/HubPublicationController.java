package com.ruoyi.vlstream.test.vlstream.controller;

import cn.hutool.json.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.service.HubPublicationService;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsModelHub/publications")
public class HubPublicationController {
    private final HubPublicationService service;
    @GetMapping("/{kind}/{id}") public R<Map<String,Object>> status(@PathVariable String kind,@PathVariable Long id) { return R.data(service.status(kind,id)); }
    @PutMapping("/{kind}/{id}/featured") public R<Map<String,Object>> featured(@PathVariable String kind,@PathVariable Long id,@RequestBody JSONObject body) {
        if(!(body.get("featured") instanceof Boolean)) throw new ServiceException("请明确指定精选状态");
        return R.data(service.featured(kind,id,body.getBool("featured")));
    }
    @PostMapping("/{kind}/{id}/publish") public R<Map<String,Object>> publish(@PathVariable String kind,@PathVariable Long id,
        @RequestBody(required=false) JSONObject body,@RequestHeader(value="X-Model-Hub-Token",required=false) String token,
        @RequestHeader(value="X-Model-Hub-Tenant",required=false) String tenant) {
        return R.data(service.publish(kind,id,body,token,tenant));
    }
    @PostMapping("/{kind}/{id}/reconcile") public R<Map<String,Object>> reconcile(@PathVariable String kind,@PathVariable Long id,
        @RequestBody(required=false) JSONObject body,@RequestHeader(value="X-Model-Hub-Token",required=false) String token,
        @RequestHeader(value="X-Model-Hub-Tenant",required=false) String tenant) {
        return R.data(service.reconcile(kind,id,body,token,tenant));
    }
    @PostMapping("/{kind}/{id}/resume") public R<Map<String,Object>> resume(@PathVariable String kind,@PathVariable Long id,
        @RequestBody(required=false) JSONObject body,@RequestHeader(value="X-Model-Hub-Token",required=false) String token,
        @RequestHeader(value="X-Model-Hub-Tenant",required=false) String tenant) {
        return R.data(service.resume(kind,id,body,token,tenant));
    }
}
