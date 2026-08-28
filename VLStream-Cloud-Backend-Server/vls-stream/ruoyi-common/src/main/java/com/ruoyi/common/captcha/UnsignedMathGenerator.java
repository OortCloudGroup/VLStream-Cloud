/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.captcha;

import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.math.Calculator;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.RandomUtil;
import com.ruoyi.common.utils.StringUtils;

/**
 * Generate
 *
 * @author Lion Li
 */
public class UnsignedMathGenerator implements CodeGenerator {

    private static final long serialVersionUID = -5514819971774091076L;

    private static final String OPERATORS = "+-*";

    /**
     * and
     */
    private final int numberLength;

    /**
     *
     */
    public UnsignedMathGenerator() {
        this(2);
    }

    /**
     *
     *
     * @param numberLength and
     */
    public UnsignedMathGenerator(int numberLength) {
        this.numberLength = numberLength;
    }

    @Override
    public String generate() {
        final int limit = getLimit();
        int a = RandomUtil.randomInt(limit);
        int b = RandomUtil.randomInt(limit);
        String max = Integer.toString(Math.max(a,b));
        String min = Integer.toString(Math.min(a,b));
        max = StringUtils.rightPad(max, this.numberLength, CharUtil.SPACE);
        min = StringUtils.rightPad(min, this.numberLength, CharUtil.SPACE);

        return max + RandomUtil.randomChar(OPERATORS) + min + '=';
    }

    @Override
    public boolean verify(String code, String userInputCode) {
        int result;
        try {
            result = Integer.parseInt(userInputCode);
        } catch (NumberFormatException e) {
            // user non-
            return false;
        }

        final int calculateResult = (int) Calculator.conversion(code);
        return result == calculateResult;
    }

    /**
     * Get
     *
     * @return
     */
    public int getLength() {
        return this.numberLength * 2 + 2;
    }

    /**
     * Get and value
     *
     * @return value
     */
    private int getLimit() {
        return Integer.parseInt("1" + StringUtils.repeat('0', this.numberLength));
    }
}
