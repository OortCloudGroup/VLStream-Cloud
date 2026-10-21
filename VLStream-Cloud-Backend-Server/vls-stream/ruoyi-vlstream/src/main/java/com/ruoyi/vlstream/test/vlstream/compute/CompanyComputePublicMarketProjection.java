package com.ruoyi.vlstream.test.vlstream.compute;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/** Optional adapter for the independently deployed anonymous market; adds only safe dated price display fields for an exact verified instance. */
@Aspect
@Component
@ConditionalOnClass(name = "com.ruoyi.vlstream.test.vlstream.compute.ComputePublicMarketService")
@RequiredArgsConstructor
public class CompanyComputePublicMarketProjection {
    private final CompanyComputeOfferService offers;

    @Around(value = "execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputePublicMarketService.list(int,int)) && args(page,size)", argNames = "call,page,size")
    public Object list(ProceedingJoinPoint call, int page, int size) throws Throwable {
        ComputePublicMarketService.Page existing = (ComputePublicMarketService.Page) call.proceed();
        List<ComputePublicMarketService.Offer> items = new ArrayList<>(existing.getItems());
        int remaining = size - items.size();
        if (remaining > 0) {
            int offset = (int) Math.max(0, (long) (page - 1) * size - existing.getTotal());
            items.addAll(offers.publicMarketOffers(remaining, offset));
        }
        return new ComputePublicMarketService.Page(items, existing.getTotal() + offers.publishedCount(), page, size);
    }
}
