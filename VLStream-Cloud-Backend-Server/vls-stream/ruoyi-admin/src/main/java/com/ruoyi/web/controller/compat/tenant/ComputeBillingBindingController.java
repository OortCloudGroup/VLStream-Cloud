package com.ruoyi.web.controller.compat.tenant;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingLifecycleService;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePaidRunService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingPayer;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingSettlementService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingAccountService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingTrialGuard;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties;
import com.ruoyi.vlstream.test.vlstream.compute.billing.NewApiBillingClient;
import org.springblade.core.tool.api.R;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.LinkedHashMap;
import java.util.Map;

/** Current-payer product endpoints. Capabilities/quotes are separate from explicit consent
 * and durable paid-run acceptance; merchant credentials never leave the backend. */
@RestController
@RequestMapping("/vlsCompute/billing")
@cn.dev33.satoken.annotation.SaCheckLogin
public class ComputeBillingBindingController {
    private final ComputePlatformAuthorizationService authorization;
    private final ObjectProvider<NewApiBillingClient> clients;
    private final ComputeBillingSettlementService settlements;
    private final ComputeBillingLifecycleService lifecycle;
    @org.springframework.beans.factory.annotation.Autowired private ComputeBillingAccountService accounts;
    @org.springframework.beans.factory.annotation.Autowired private ExternalBillingProperties properties;
    @org.springframework.beans.factory.annotation.Autowired private ComputePaidRunService paidRuns;
    public ComputeBillingBindingController(ComputePlatformAuthorizationService authorization,ObjectProvider<NewApiBillingClient> clients,
                                         ComputeBillingSettlementService settlements,ComputeBillingLifecycleService lifecycle){
        this.authorization=authorization;this.clients=clients;this.settlements=settlements;this.lifecycle=lifecycle;
    }
    @GetMapping("/identity")
    public R<BillingPayer> identity(){return R.data(authorization.billingPayer(client()));}
    /** Configuration presence only: never contacts NewAPI or creates permissions. */
    @GetMapping("/capabilities")
    public R<Map<String,Object>> capabilities(){
        Map<String,Object> view=new LinkedHashMap<>();
        view.put("accountAvailable",properties.isEnabled()&&clients.getIfAvailable()!=null);
        view.put("paidAdmissionEnabled",properties.isEnabled()&&properties.isAllowWrites()&&properties.isSafetyStopEnabled()&&properties.isPaidAdmissionEnabled());
        return R.data(view);
    }
    public static class AccountCheck {
        public boolean acceptBillingPermissions;
    }
    @PostMapping("/account/check")
    public R<ComputeBillingAccountService.Account> account(@RequestBody AccountCheck request){
        if(request==null||!request.acceptBillingPermissions)throw new ServiceException("请先确认平台余额查询与GPU计费权限");
        NewApiBillingClient client=client();
        BillingPayer payer=newOperationPayer(client);
        try{return R.data(accounts.check(payer,client,client.config(),true));}
        catch(IllegalStateException unavailable){throw new ServiceException("BILLING_NOT_AUTHORIZED".equals(unavailable.getMessage())?"平台尚未接受当前账户的计费授权，请稍后重试":"平台余额暂未确认，请稍后重试");}
    }
    @PostMapping("/nodes/{nodeId}/quote")
    public R<ComputePaidRunService.View> quote(@PathVariable Long nodeId){
        NewApiBillingClient client=client();
        BillingPayer payer=newOperationPayer(client);
        return R.data(paidRuns.quote(nodeId,payer,client.config()));
    }
    @GetMapping("/runs")
    public R<java.util.List<ComputePaidRunService.View>> runs(){return R.data(paidRuns.list(authorization.billingPayer(client())));}
    @GetMapping("/runs/{runId}")
    public R<ComputePaidRunService.View> run(@PathVariable String runId){return R.data(paidRuns.get(runId,authorization.billingPayer(client())));}
    /** The original run identifies the pending archive; payer, instance and price stay server-owned.
     * This only prepares a fresh quote. The existing explicit accept flow owns funding/start. */
    @PostMapping("/runs/{sourceRunId}/archive-recovery-quote")
    public R<ComputePaidRunService.View> archiveRecoveryQuote(@PathVariable String sourceRunId){
        NewApiBillingClient client=client();
        BillingPayer payer=newOperationPayer(client);
        return R.data(paidRuns.quoteArchiveRecovery(sourceRunId,payer,client.config()));
    }
    @PostMapping("/runs/{runId}/accept")
    public R<ComputePaidRunService.View> accept(@PathVariable String runId,@RequestBody AccountCheck request){
        if(request==null||!request.acceptBillingPermissions)throw new ServiceException("请确认本次报价与平台计费权限");
        NewApiBillingClient client=client();BillingPayer payer=newOperationPayer(client);
        ComputePaidRunService.View original=paidRuns.get(runId,payer);
        if("QUOTED".equals(original.getState())){
            ComputeBillingAccountService.Account account=accounts.check(payer,client,client.config(),true);
            if(new java.math.BigInteger(account.getAvailableQuota()).compareTo(new java.math.BigInteger(original.getReserveQuota()))<0)
                throw new ServiceException("平台可用余额不足，请充值后重新确认本次开机");
        }
        return R.data(paidRuns.accept(runId,payer));
    }
    @PostMapping("/runs/{runId}/stop")
    public R<ComputePaidRunService.View> stop(@PathVariable String runId){return R.data(paidRuns.stop(runId,authorization.billingPayer(client())));}
    @PostMapping("/sessions/{usageId}/bind")
    public R<String> bind(@PathVariable String usageId){
        NewApiBillingClient client=client();BillingPayer payer=newOperationPayer(client);
        settlements.bindAcceptedUsage(usageId,payer,client.config());
        lifecycle.arm(usageId,payer);
        return R.data("已保存当前可信付款身份与实例占用快照；收费准入还须通过本次预冻结与安全停机检查");
    }
    /** Gate only new work, after the authoritative self lookup and before any wallet/config call.
     * Existing runs must remain queryable/stoppable even if the trial expires or becomes invalid. */
    private BillingPayer newOperationPayer(NewApiBillingClient client){
        BillingPayer payer=authorization.billingPayer(client);
        ComputeBillingTrialGuard.requirePayerScope(properties,payer);
        return payer;
    }
    private NewApiBillingClient client(){NewApiBillingClient client=clients.getIfAvailable();if(client==null)throw new ServiceException("计费接入默认关闭，未配置外部受限身份");return client;}
}
