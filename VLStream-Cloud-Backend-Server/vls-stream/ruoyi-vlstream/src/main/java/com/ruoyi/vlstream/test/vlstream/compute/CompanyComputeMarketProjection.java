package com.ruoyi.vlstream.test.vlstream.compute;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** Additive market projection, allowing an old-artifact delta without replacing existing execution code. */
@Aspect
@Component
@RequiredArgsConstructor
public class CompanyComputeMarketProjection {
    private final CompanyComputeOfferService offers;

    @Around(value = "execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeMarketService.list(boolean)) && args(selection)", argNames = "call,selection")
    @SuppressWarnings("unchecked")
    public Object list(ProceedingJoinPoint call, boolean selection) throws Throwable {
        Object existing = call.proceed();
        List<ComputeMarketService.Resource> result = new ArrayList<>((List<ComputeMarketService.Resource>) existing);
        result.removeIf(resource -> {
            try { return offers.linked(Long.valueOf(resource.getId())); }
            catch (NumberFormatException invalidId) { return false; }
        });
        if (selection) return result;
        // The authenticated list is not paginated; fetch in bounded SQL batches without dropping supply.
        for (int offset = 0; ; offset += 200) {
            List<ComputeMarketService.Resource> batch = offers.marketResources(200, offset);
            result.addAll(batch);
            if (batch.size() < 200) return result;
        }
    }
}
