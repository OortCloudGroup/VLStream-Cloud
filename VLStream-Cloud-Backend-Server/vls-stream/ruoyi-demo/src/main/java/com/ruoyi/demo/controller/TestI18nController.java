/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.MessageUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


/**
 *
 *
 * @author Lion Li
 */
@Validated
@RestController
@RequestMapping("/demo/i18n")
public class TestI18nController {

    /**
     * codeGet
     * code to messages.properties in key
     * <p>
     * user.register.success
     *
     * @param code code
     */
    @GetMapping()
    public R<Void> get(String code) {
        return R.ok(MessageUtils.message(code));
    }

    /**
     * Validator Validate
     * value
     * <p>
     * not.null
     */
    @GetMapping("/test1")
    public R<Void> test1(@NotBlank(message = "{not.null}") String str) {
        return R.ok(str);
    }

    /**
     * Bean Validate
     * value
     * <p>
     * not.null
     */
    @GetMapping("/test2")
    public R<TestI18nBo> test2(@Validated TestI18nBo bo) {
        return R.ok(bo);
    }

    @Data
    public static class TestI18nBo {

        @NotBlank(message = "{not.null}")
        private String name;

        @NotNull(message = "{not.null}")
        @Range(min = 0, max = 100, message = "{length.not.valid}")
        private Integer age;
    }
}
