/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.vlstream.compat.BladeResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/image")
public class ImageCompatController {

    @DeleteMapping("/delete")
    public BladeResult<Map<String, Object>> delete(@RequestParam("fileName") String fileName) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("fileName", fileName);
        data.put("deleted", Boolean.TRUE);
        return BladeResult.success(data);
    }
}
