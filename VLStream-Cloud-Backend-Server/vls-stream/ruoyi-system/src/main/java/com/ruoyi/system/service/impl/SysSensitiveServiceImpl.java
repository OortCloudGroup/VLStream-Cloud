/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.service.impl;

import com.ruoyi.common.core.service.SensitiveService;
import com.ruoyi.common.helper.LoginHelper;
import org.springframework.stereotype.Service;

/**
 * service
 * administrator
 *
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Service
public class SysSensitiveServiceImpl implements SensitiveService {

    /**
     * whether
     */
    @Override
    public boolean isSensitive() {
        return !LoginHelper.isAdmin();
    }

}
