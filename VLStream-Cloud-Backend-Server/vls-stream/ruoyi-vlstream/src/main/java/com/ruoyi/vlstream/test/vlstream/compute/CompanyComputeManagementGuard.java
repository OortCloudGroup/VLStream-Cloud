package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.common.exception.ServiceException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/** The approved node is manageable metadata, while SSH identity and execution remain unapproved. */
@Aspect
@Component
@RequiredArgsConstructor
public class CompanyComputeManagementGuard {
    private final CompanyComputeOfferService offers;

    @Around("execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeNodeService.list())")
    @SuppressWarnings("unchecked")
    public Object list(ProceedingJoinPoint call) throws Throwable {
        List<ComputeNode> company=offers.publisherManagementNodes();
        List<ComputeNode> existing;
        try { existing=new ArrayList<>((List<ComputeNode>)call.proceed()); }
        catch(ServiceException denied) {
            if(company.isEmpty() || (!Integer.valueOf(47001).equals(denied.getCode()) && !Integer.valueOf(47003).equals(denied.getCode())))throw denied;
            existing=new ArrayList<>();
        }
        for(ComputeNode node:company) {
            if(existing.stream().noneMatch(row -> row.getId().equals(node.getId())))existing.add(node);
        }
        List<ComputeNode> result=new ArrayList<>();
        for(ComputeNode node:existing) result.add(decorate(node));
        return result;
    }

    @Around(value="execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeNodeService.getManaged(Long)) && args(id)",argNames="call,id")
    public Object detail(ProceedingJoinPoint call,Long id) throws Throwable {
        for(ComputeNode node:offers.publisherManagementNodes())if(node.getId().equals(id))return decorate(node);
        return decorate((ComputeNode)call.proceed());
    }

    ComputeNode decorate(ComputeNode node) {
        if(!offers.linked(node.getId()))return node;
        CompanyManagedComputeNode result=new CompanyManagedComputeNode();
        BeanUtils.copyProperties(node,result);result.setCompanyOfferCode(offers.code(node.getId()));
        result.setCompanyPublicationAllowed(offers.canPublish(node));return result;
    }

    @Around(value="execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeMarketService.publish(Long,..)) && args(id,request)",argNames="call,id,request")
    public Object publication(ProceedingJoinPoint call,Long id,ComputeRequests.Offer request) throws Throwable {
        if(!offers.linked(id))return call.proceed();
        offers.publishNode(id,request);return null;
    }

    @Before(value="(execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeNodeService.probe(Long))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeNodeService.save(Long,..))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeNodeService.delete(Long))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeNodeService.requireReady(Long))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeNodeService.authorizePaidOperation(Long))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeHealthRefreshService.refresh(Long))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeMarketService.claim(Long))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeMarketService.release(Long))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeMarketService.power(Long,..))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeMarketService.publicIdentity(Long,..))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeMarketService.observe(Long,..))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeProviderSettingsService.save(Long,..))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeProviderStateService.refresh(Long,..))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeCostObservationService.query(Long))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeProviderInventoryService.sync(Long))"
        + " || execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeProviderInventoryService.refresh(Long,..))) && args(id,..)",argNames="id")
    public void blockOperation(Long id) {
        if(offers.linked(id))throw new ServiceException(CompanyComputeOfferService.PENDING,403);
    }

    @Before(value="execution(* com.ruoyi.vlstream.test.vlstream.compute.ComputeSsh.open(..)) && args(node)",argNames="node")
    public void blockConnection(ComputeNode node) { blockOperation(node.getId()); }
}
