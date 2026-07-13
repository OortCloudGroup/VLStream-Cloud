/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.event;

import java.util.Collection;

public class ValidationEvent {

    private final Collection<String> ids;

    public ValidationEvent(Collection<String> ids) {
        this.ids = ids;
    }

    public Collection<String> getIds() {
        return ids;
    }
}
