package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.cloudai.CloudAiService;
import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import com.ruoyi.vlstream.test.vlstream.service.LlmReviewScopes;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsLlmReview/scopes/{type}/{id}")
@SaCheckPermission("vls:menu:llm-review-tasks")
public class VlsLlmReviewScopeController {
    private final LlmReviewScopes scopes;
    private final VlsLlmReviewProperties properties;

    private String checkedKey(String type, String id) {
        StpUtil.checkPermission("SCENE".equals(type) ? "vls:menu:scene-governance" : "vls:menu:vlstream:device");
        scopes.validateTarget(type, id);
        return type + ":" + id;
    }

    @GetMapping
    public R<Boolean> get(@PathVariable String type, @PathVariable String id) {
        return R.data(scopes.enabled(CloudAiService.tenant(), checkedKey(type, id)));
    }

    @PutMapping
    public R<Boolean> set(@PathVariable String type, @PathVariable String id, @RequestBody Map<String,Object> body) {
        String key = checkedKey(type, id);
        Object enabled = body.get("enabled");
        if (!(enabled instanceof Boolean)) throw new ServiceException("请选择复核开关状态");
        if (Boolean.TRUE.equals(enabled) && !Boolean.TRUE.equals(properties.getEnabled()))
            throw new ServiceException("当前环境的大模型复核服务未启用");
        scopes.setEnabled(key, (Boolean)enabled);
        return R.data((Boolean)enabled);
    }
}
