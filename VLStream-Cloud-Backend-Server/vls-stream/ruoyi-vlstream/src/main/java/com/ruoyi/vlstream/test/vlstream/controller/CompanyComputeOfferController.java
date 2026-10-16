package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.vlstream.test.vlstream.compute.CompanyComputeOfferService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vlsCompute/company-offers")
@SaCheckLogin
@SaCheckPermission({"vls:compute:company-publish", "vls:compute:publish", "vls:compute:configure"})
@RequiredArgsConstructor
public class CompanyComputeOfferController {
    private final CompanyComputeOfferService offers;

    @GetMapping
    public R<List<CompanyComputeOfferService.Record>> list() { return R.data(offers.managementList()); }

    @PutMapping("/{code}/publication")
    public R<String> publish(@PathVariable("code") String code, @RequestParam("published") boolean published) {
        offers.publish(code, published);
        return R.data("公司供给展示已更新；收费使用仍待开通");
    }
}
