package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.TimeStrategy;
import com.ruoyi.vlstream.service.IVlsTimeStrategyService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsTimeStrategyControllerTest {

    @Test
    void exposesFrontendTimeStrategyRoutes() throws Exception {
        Method get = VlsTimeStrategyController.class.getDeclaredMethod("getTimeStrategy", String.class);
        Method save = VlsTimeStrategyController.class.getDeclaredMethod("saveTimeStrategy", TimeStrategy.class);
        Method delete = VlsTimeStrategyController.class.getDeclaredMethod("deleteTimeStrategy", String.class);

        assertArrayEquals(new String[] {"/{deviceId}"}, get.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {""}, save.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{deviceId}"}, delete.getAnnotation(DeleteMapping.class).value());
    }

    @Test
    void getReturnsStrategyAliasesExpectedByFrontend() {
        IVlsTimeStrategyService service = mock(IVlsTimeStrategyService.class);
        VlsTimeStrategyController controller = new VlsTimeStrategyController(service);
        TimeStrategy strategy = strategy("13");

        when(service.getTimeStrategy("13")).thenReturn(strategy);

        BladeResult<TimeStrategy> response = controller.getTimeStrategy("13");

        assertEquals(200, response.getCode());
        assertSame(strategy, response.getData());
        assertEquals("weekly", response.getData().getStrategyType());
        assertSame(strategy.getWeeklyTimes(), response.getData().getWeeklyTimes());
        verify(service).getTimeStrategy("13");
    }

    @Test
    void saveAndDeleteForwardDeviceStrategyOperations() {
        IVlsTimeStrategyService service = mock(IVlsTimeStrategyService.class);
        VlsTimeStrategyController controller = new VlsTimeStrategyController(service);
        TimeStrategy strategy = strategy("13");

        when(service.saveTimeStrategy(strategy)).thenReturn(strategy);
        when(service.deleteTimeStrategy("13")).thenReturn(true);

        BladeResult<TimeStrategy> saved = controller.saveTimeStrategy(strategy);
        BladeResult<Boolean> deleted = controller.deleteTimeStrategy("13");

        assertSame(strategy, saved.getData());
        assertEquals(Boolean.TRUE, deleted.getData());
        verify(service).saveTimeStrategy(strategy);
        verify(service).deleteTimeStrategy("13");
    }

    private TimeStrategy strategy(String deviceId) {
        TimeStrategy strategy = new TimeStrategy();
        strategy.setDeviceId(deviceId);
        strategy.setStrategyType("weekly");
        strategy.setDailyTimes(Arrays.asList(1, 2, 3));
        Map<String, Object> weeklyTimes = new LinkedHashMap<String, Object>();
        weeklyTimes.put("1", Arrays.asList(4, 5, 6));
        strategy.setWeeklyTimes(weeklyTimes);
        return strategy;
    }
}
