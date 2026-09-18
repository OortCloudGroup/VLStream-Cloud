package com.ruoyi.vlstream.test.vlstream.controller;

import cn.hutool.json.JSONObject;
import com.ruoyi.vlstream.test.vlstream.service.ModelHubCatalogService;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsModelHub")
public class VlsModelHubController {
    private final ModelHubCatalogService service;

    @GetMapping("/models")
    public R<JSONObject> models(@RequestParam(defaultValue="1") int page,
        @RequestParam(defaultValue="12") int size, @RequestParam(defaultValue="") String keyword,
        @RequestParam(defaultValue="") String category) {
        return R.data(service.list(page, size, keyword, category));
    }

    @PostMapping("/categories")
    public R<Object> categories() {
        return R.data(service.categories());
    }

    @PostMapping("/files")
    public R<Object> files(@RequestBody JSONObject body) {
        return R.data(service.files(body.getStr("uid"), body.getStr("path"), body.getStr("branch")));
    }

    @PostMapping("/download")
    public void download(@RequestBody JSONObject body,
        @RequestHeader(value="X-Model-Hub-Token", required=false) String token,
        @RequestHeader(value="X-Model-Hub-Tenant", required=false) String tenant, HttpServletResponse response) {
        service.download(body.getStr("uid"), body.getStr("path"), body.getStr("branch"), token, tenant, response);
    }
}
