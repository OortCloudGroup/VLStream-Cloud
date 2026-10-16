package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/** Local permission batch: changing any market publication requires explicit company authority. */
@Aspect
@Component
@RequiredArgsConstructor
public class CompanyComputePublicationGuard {
    private final CompanyComputeOfferAuthority authority;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private CompanyComputeOfferService offers;

    @Before(value="execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeMarketService.publish(..)) && args(id,..)",argNames="id")
    public void requireCompanyPublisherForNode(Long id) {
        if(offers!=null && offers.linked(id))offers.requireNodePublisher(id);
        else requireCompanyPublisher();
    }

    public void requireCompanyPublisher() { authority.requirePublisher(); }
}
