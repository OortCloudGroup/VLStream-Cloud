package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.common.exception.ServiceException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import javax.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Local guardrails, not a wallet, notification sender or automatic power/deletion executor. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.compute.billing-policy")
public class ComputeBillingPolicy {
    private int openingReserveSeconds = 3600;
    private int earlyReminderSeconds = 7200;
    private int urgentReminderSeconds = 1800;
    private int stopBudgetSeconds = 300;
    private int pollingSafetySeconds = 60;
    private String trainingCompletionAction = "KEEP_RUNNING";
    private boolean automaticDeletionEnabled = false;
    private boolean faultCompensationEnabled = false;
    private String supplierMinimumCostBearer = "PLATFORM";

    @PostConstruct
    public void validate() {
        if (openingReserveSeconds < stopBudgetSeconds || pollingSafetySeconds <= 0 || stopBudgetSeconds < 300 || stopBudgetSeconds <= pollingSafetySeconds
            || urgentReminderSeconds <= stopBudgetSeconds || earlyReminderSeconds <= urgentReminderSeconds)
            throw new IllegalArgumentException("算力预留和余额提醒/停机安全阈值配置无效");
        if (!"KEEP_RUNNING".equals(trainingCompletionAction) || automaticDeletionEnabled)
            throw new IllegalArgumentException("安全自动停机/磁盘释放闭环未接入，不能启用完成后关机或自动删除");
        if (faultCompensationEnabled || !"PLATFORM".equals(supplierMinimumCostBearer))
            throw new IllegalArgumentException("首版不提供故障赔付，供应商最低费差额由平台承担；错误扣款仍须纠正");
    }

    /** Exact COMPUTE-only estimate. No real freeze or user disk charge is performed.
     * GPU disks are platform workspaces; no personal disk reserve or post-stop disk charge is performed. */
    public BigDecimal openingComputeReserveQuota(ComputeBillingRate rate) {
        validate();
        if (rate == null) throw new IllegalArgumentException("可信计算价格尚未确认");
        rate.validate(Instant.now());
        return rate.unroundedQuota(openingReserveSeconds * 1000L);
    }

    /** No guessed provider shutdown latency: unknown/insufficient coverage cannot authorize an automatic stop budget. */
    public int checkedStopBudgetSeconds(Integer reliableProviderStopSeconds) {
        validate();
        if (reliableProviderStopSeconds == null || reliableProviderStopSeconds <= 0
            || (long)reliableProviderStopSeconds + pollingSafetySeconds > stopBudgetSeconds)
            throw new IllegalArgumentException("供应商可靠停机耗时未核验或超出安全预算，需调整阈值");
        return stopBudgetSeconds;
    }

    /** Internal cost reconciliation estimate, NOT an additional user debit or an assumed supplier minimum.
     * Only a verified charge for this same compute period/component may be supplied. */
    public BigDecimal platformCostDifference(ComputeBillingRate rate, long elapsedMillis,
                                             BigDecimal verifiedSupplierComputeCost, String currency) {
        if (rate == null || elapsedMillis < 0 || verifiedSupplierComputeCost == null
            || verifiedSupplierComputeCost.signum() < 0 || currency == null || !currency.equals(rate.getCurrency())
            || rate.getCostHourly() == null || rate.getCostHourly().signum() <= 0)
            throw new IllegalArgumentException("供应商实际计算成本及同一币种/期间/组件尚未核验");
        BigDecimal proportionalCost = rate.getCostHourly().multiply(BigDecimal.valueOf(elapsedMillis))
            .divide(BigDecimal.valueOf(3600000), MathContext.DECIMAL128);
        return verifiedSupplierComputeCost.subtract(proportionalCost).max(BigDecimal.ZERO);
    }

    /** Official Pro user billing ends at verified physical stop, independently of supplier costs.
     * Existing ECS supply still requires its original economical-stop contract. */
    static void requireEconomicalPaidOffer(ComputeNode node, long now) {
        if(officialPro(node)&&"API".equals(node.getControlMode())&&"PROVIDER_API".equals(node.getCapacitySource())
            &&ComputeAvailability.fresh(node.getPowerCheckedAt(),now)&&"STOPPED".equals(node.getPowerState())
            &&"AUTODL_PRO_PHYSICAL_STOPPED".equals(node.getActualStoppedMode()))return;
        if (!"aliyun".equals(node.getProvider()) || !"ECS".equals(node.getProviderProduct())
            || !"API".equals(node.getControlMode()) || !"StopCharging".equals(node.getRequestedStoppedMode())
            || !"PROVIDER_API".equals(node.getCapacitySource()) || !ComputeAvailability.fresh(node.getPowerCheckedAt(), now)
            || !"STOPPED".equals(node.getPowerState()) || !"StopCharging".equals(node.getActualStoppedMode())
            || !"PostPaid".equals(node.getProviderChargeType()))
            throw new ServiceException("尚无当前供应商节省停机的可信证明，首版拒绝收费上架；普通停机/未知模式不代表停止计算收费");
    }

    /** Billing termination is separate from successful power-off and from storage release. */
    static boolean computeStopConfirmed(ComputeNode node, ComputePowerProvider.Snapshot snapshot) {
        if(officialPro(node))return "STOPPED".equals(snapshot.power)&&"AUTODL_PRO_PHYSICAL_STOPPED".equals(snapshot.stoppedMode);
        return "aliyun".equals(node.getProvider()) && "ECS".equals(node.getProviderProduct())
            && "STOPPED".equals(snapshot.power) && "StopCharging".equals(snapshot.stoppedMode)
            && "PostPaid".equals(snapshot.chargeType);
    }
    private static boolean officialPro(ComputeNode node) {
        return node.isConfigurationManaged()&&"autodl".equals(node.getProvider())&&"AUTODL_PRO".equals(node.getProviderProduct());
    }
}
