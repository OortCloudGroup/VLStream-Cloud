/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service.impl;

import com.ruoyi.workflow.mapper.WfUserInterfaceFieldMapper;
import com.ruoyi.workflow.service.IWfUserInterfaceFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class WfUserInterfaceFieldServiceImpl implements IWfUserInterfaceFieldService {

    private final WfUserInterfaceFieldMapper wfUserInterfaceFieldMapper;

    /**
     * Get user interface fieldconfiguration; null, not configuration, field
     */
    @Override
    public String getFieldCodes(String userId, String interfacePath) {
        String json = wfUserInterfaceFieldMapper.selectFieldCodes(userId, interfacePath);
        if (json == null) {
            return null;
        }
        return json;
    }

    /**
     * userconfiguration
     */
    @Override
    public int saveFieldCodes(String userId, String interfacePath, String codes) {
       return wfUserInterfaceFieldMapper.upsert(userId, interfacePath, codes);
    }
}
