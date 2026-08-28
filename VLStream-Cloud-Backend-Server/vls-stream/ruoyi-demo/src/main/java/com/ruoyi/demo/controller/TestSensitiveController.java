/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.demo.controller;

import com.ruoyi.common.annotation.Sensitive;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.SensitiveStrategy;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * data control
 * <p>
 * administrator
 *
 *
 * @author Lion Li
 * @version 3.6.0
 * @see com.ruoyi.common.core.service.SensitiveService
 */
@RestController
@RequestMapping("/demo/sensitive")
public class TestSensitiveController extends BaseController {

    /**
     * data
     */
    @GetMapping("/test")
    public R<TestSensitive> test() {
        TestSensitive testSensitive = new TestSensitive();
        testSensitive.setIdCard("210397198608215431");
        testSensitive.setPhone("17640125371");
        testSensitive.setAddress("北京市朝阳区某某四合院1203室");
        testSensitive.setEmail("17640125371@163.com");
        testSensitive.setBankCard("6226456952351452853");
        return R.ok(testSensitive);
    }

    @Data
    static class TestSensitive {

        /**
         *
         */
        @Sensitive(strategy = SensitiveStrategy.ID_CARD)
        private String idCard;

        /**
         *
         */
        @Sensitive(strategy = SensitiveStrategy.PHONE)
        private String phone;

        /**
         *
         */
        @Sensitive(strategy = SensitiveStrategy.ADDRESS)
        private String address;

        /**
         *
         */
        @Sensitive(strategy = SensitiveStrategy.EMAIL)
        private String email;

        /**
         *
         */
        @Sensitive(strategy = SensitiveStrategy.BANK_CARD)
        private String bankCard;

    }

}
