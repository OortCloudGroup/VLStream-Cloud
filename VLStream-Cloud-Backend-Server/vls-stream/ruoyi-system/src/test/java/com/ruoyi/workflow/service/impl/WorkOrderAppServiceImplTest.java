/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.workflow.domain.WfApp;
import com.ruoyi.workflow.domain.WorkOrderApp;
import com.ruoyi.workflow.mapper.WfAppMapper;
import com.ruoyi.workflow.mapper.WfFormAppMapper;
import com.ruoyi.workflow.mapper.WorkOrderAppMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WorkOrderAppServiceImpl
 * resolveAppPackageByApplicationId Query and to
 */
@Tag("dev")
public class WorkOrderAppServiceImplTest {

    private final WorkOrderAppMapper workOrderAppMapper = mock(WorkOrderAppMapper.class);
    private final WfAppMapper wfAppMapper = mock(WfAppMapper.class);
    private final WfFormAppMapper wfFormAppMapper = mock(WfFormAppMapper.class);
    private final ValidateService validateService = mock(ValidateService.class);
    private final WorkOrderAppServiceImpl service = new WorkOrderAppServiceImpl(
            workOrderAppMapper, validateService, wfAppMapper, wfFormAppMapper
    );

    /**
     * ID in workorder_app , workorder_app.appPackage
     */
    @Test
    public void resolveAppPackageShouldReturnWorkOrderAppPackageWhenMatched() {
        WorkOrderApp app = new WorkOrderApp();
        app.setApplicationId("app-1");
        app.setAppPackage("com.example.workorder");

        when(workOrderAppMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(app);

        String result = service.resolveAppPackageByApplicationId("app-1");

        assertEquals("com.example.workorder", result);
    }

    /**
     * workorder_app 、wf_app in , wf_app.appPackage
     */
    @Test
    public void resolveAppPackageShouldFallbackToWfAppWhenWorkOrderAppNotFound() {
        when(workOrderAppMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        WfApp wfApp = new WfApp();
        wfApp.setApplicationId("app-2");
        wfApp.setAppPackage("com.example.wf");

        when(wfAppMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(wfApp);

        String result = service.resolveAppPackageByApplicationId("app-2");

        assertEquals("com.example.wf", result);
    }

    /**
     * , null
     */
    @Test
    public void resolveAppPackageShouldReturnNullWhenBothTablesMiss() {
        when(workOrderAppMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(wfAppMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        String result = service.resolveAppPackageByApplicationId("app-3");

        assertNull(result);
    }

    /**
     * parameter is empty null
     */
    @Test
    public void resolveAppPackageShouldReturnNullForBlankArgument() {
        assertNull(service.resolveAppPackageByApplicationId(null));
        assertNull(service.resolveAppPackageByApplicationId(""));
        assertNull(service.resolveAppPackageByApplicationId("   "));
    }

    /**
     * workorder_app appPackage is empty , wf_app
     */
    @Test
    public void resolveAppPackageShouldFallbackToWfAppWhenWorkOrderAppPackageIsBlank() {
        WorkOrderApp app = new WorkOrderApp();
        app.setApplicationId("app-4");
        app.setAppPackage(null);

        when(workOrderAppMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(app);

        WfApp wfApp = new WfApp();
        wfApp.setApplicationId("app-4");
        wfApp.setAppPackage("com.example.wf");

        when(wfAppMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(wfApp);

        String result = service.resolveAppPackageByApplicationId("app-4");

        assertEquals("com.example.wf", result);
    }
}
