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
 * WorkOrderAppServiceImpl 单元测试
 * 验证 resolveAppPackageByApplicationId 查询顺序和兜底行为
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
     * 请求头应用 ID 命中 workorder_app 时，返回 workorder_app.appPackage
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
     * workorder_app 查不到、wf_app 命中时，返回 wf_app.appPackage
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
     * 两张表都查不到时，返回 null
     */
    @Test
    public void resolveAppPackageShouldReturnNullWhenBothTablesMiss() {
        when(workOrderAppMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(wfAppMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        String result = service.resolveAppPackageByApplicationId("app-3");

        assertNull(result);
    }

    /**
     * 参数为空时返回 null
     */
    @Test
    public void resolveAppPackageShouldReturnNullForBlankArgument() {
        assertNull(service.resolveAppPackageByApplicationId(null));
        assertNull(service.resolveAppPackageByApplicationId(""));
        assertNull(service.resolveAppPackageByApplicationId("   "));
    }

    /**
     * workorder_app 查到但 appPackage 为空时，继续查 wf_app
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
