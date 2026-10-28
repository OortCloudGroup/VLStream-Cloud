package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/** Configuration resources expose only selection/training/health; CRUD and cloud actions stay closed. */
@Aspect
@Component
@RequiredArgsConstructor
public class ConfiguredComputeManagementGuard {
    private final ConfiguredComputeRegistry configured;

    @Before(value="(execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeProviderSettingsService.save(Long,..))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeProviderStateService.refresh(Long,..))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeCostObservationService.query(Long))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeProviderInventoryService.sync(Long))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeProviderInventoryService.refresh(Long,..))) && args(id,..)",argNames="id")
    public void requireMutable(Long id) { configured.requireMutable(id); }
}
