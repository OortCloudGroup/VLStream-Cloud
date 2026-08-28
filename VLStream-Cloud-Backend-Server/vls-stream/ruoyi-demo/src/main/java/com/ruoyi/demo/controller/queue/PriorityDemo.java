/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.controller.queue;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * will
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Data
@NoArgsConstructor
public class PriorityDemo implements Comparable<PriorityDemo> {
    private String name;
    private Integer orderNum;

    @Override
    public int compareTo(PriorityDemo other) {
        return Integer.compare(getOrderNum(), other.getOrderNum());
    }
}
