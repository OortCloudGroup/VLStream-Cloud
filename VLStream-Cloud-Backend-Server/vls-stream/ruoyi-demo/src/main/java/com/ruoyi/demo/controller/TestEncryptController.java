/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.demo.domain.TestDemoEncrypt;
import com.ruoyi.demo.mapper.TestDemoEncryptMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


/**
 * data can
 *
 * @author Lion Li
 */
@Validated
@RestController
@RequestMapping("/demo/encrypt")
public class TestEncryptController {

    @Autowired
    private TestDemoEncryptMapper mapper;
    @Value("${mybatis-encryptor.enable}")
    private Boolean encryptEnable;

    /**
     * data
     *
     * @param key key
     * @param value value
     */
    @GetMapping()
    public R<Map<String, TestDemoEncrypt>> test(String key, String value) {
        if (!encryptEnable) {
            throw new RuntimeException("加密功能未开启!");
        }
        Map<String, TestDemoEncrypt> map = new HashMap<>(2);
        TestDemoEncrypt demo = new TestDemoEncrypt();
        demo.setTestKey(key);
        demo.setValue(value);
        mapper.insert(demo);
        map.put("加密", demo);
        TestDemoEncrypt testDemo = mapper.selectById(demo.getId());
        map.put("解密", testDemo);
        return R.ok(map);
    }


}
