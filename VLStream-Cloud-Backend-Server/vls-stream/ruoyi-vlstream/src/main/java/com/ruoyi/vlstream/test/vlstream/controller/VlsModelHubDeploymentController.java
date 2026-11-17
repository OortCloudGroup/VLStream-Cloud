package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ruoyi.vlstream.test.vlstream.service.ModelHubDeploymentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsModelHub")
@SaCheckPermission(value={"vls:menu:algorithm-arrangement","vls:menu:vlstream:device"},mode=SaMode.OR)
public class VlsModelHubDeploymentController {
    private final ModelHubDeploymentService service;
    @PostMapping("/deploy")
    public R<List<Map<String,Object>>> deploy(@RequestBody Request request,
        @RequestHeader(value="X-Model-Hub-Token",required=false)String token,
        @RequestHeader(value="X-Model-Hub-Tenant",required=false)String tenant) throws IOException {
        return R.data(service.deploy(request.uid,request.commit,request.algorithmId,request.deviceIds,request.operationId,token,tenant));
    }
    @Data public static class Request {
        private String uid; private String commit; private Long algorithmId;
        private List<String> deviceIds; private String operationId;
    }
}
