package com.ruoyi.web.controller.compat.tenant;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingLifecycleService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingPayer;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingSettlementService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.NewApiBillingClient;
import org.springblade.core.tool.api.R;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Local identity/session binding only; never creates an upstream grant, hold or charge. */
@RestController
@RequestMapping("/vlsCompute/billing")
@cn.dev33.satoken.annotation.SaCheckLogin
public class ComputeBillingBindingController {
    private final ComputePlatformAuthorizationService authorization;
    private final ObjectProvider<NewApiBillingClient> clients;
    private final ComputeBillingSettlementService settlements;
    private final ComputeBillingLifecycleService lifecycle;
    public ComputeBillingBindingController(ComputePlatformAuthorizationService authorization,ObjectProvider<NewApiBillingClient> clients,
                                         ComputeBillingSettlementService settlements,ComputeBillingLifecycleService lifecycle){
        this.authorization=authorization;this.clients=clients;this.settlements=settlements;this.lifecycle=lifecycle;
    }
    @GetMapping("/identity")
    public R<BillingPayer> identity(){return R.data(authorization.billingPayer(client()));}
    @PostMapping("/sessions/{usageId}/bind")
    public R<String> bind(@PathVariable String usageId){
        NewApiBillingClient client=client();BillingPayer payer=authorization.billingPayer(client);
        settlements.bindAcceptedUsage(usageId,payer,client.config());
        lifecycle.arm(usageId,payer);
        return R.data("已保存当前可信付款身份与实例占用快照；收费准入仍关闭");
    }
    private NewApiBillingClient client(){NewApiBillingClient client=clients.getIfAvailable();if(client==null)throw new ServiceException("计费接入默认关闭，未配置外部受限身份");return client;}
}
