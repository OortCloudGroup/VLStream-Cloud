/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * swagger3 method
 *
 * @author Lion Li
 */
@RestController
@RequestMapping("/swagger/demo")
public class Swagger3DemoController {

    /**
     *
     * @RequestPart annotation to
     *
     * @param file
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<String> upload(@RequestPart("file") MultipartFile file) {
        return R.ok("操作成功", file.getOriginalFilename());
    }

}
