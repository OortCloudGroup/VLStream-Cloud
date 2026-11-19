/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmLlmReviewConfig;
import com.ruoyi.vlstream.test.vlstream.service.AlgorithmFormService;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class VlsAlgorithmFormController {
    private final AlgorithmFormService forms;

    @SaCheckPermission("vls:menu:algorithm-management")
    @PostMapping(value = "/vlsAlgorithm/form", consumes = "multipart/form-data")
    public R<String> create(@Valid @RequestPart("algorithm") Algorithm algorithm,
                            @RequestPart(value = "file", required = false) MultipartFile file,
                            @RequestPart(value = "review", required = false) AlgorithmLlmReviewConfig review) throws IOException {
        return R.data(forms.create(algorithm, file, review).toString());
    }
}
