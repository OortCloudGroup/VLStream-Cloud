package com.ruoyi.vlstream.test.vlstream.compute.billing;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Internal funding transport coordinator; no public endpoint or automatic scheduler.
 * Both write gates default OFF. All external calls occur after the journal transaction ends. */
@Service
public class ComputeFundingRecoveryService {
    private final ComputeFundingIntentService journal;
    private final Supplier<ExternalBillingGateway> gateways;
    private final ExternalBillingProperties properties;

    @Autowired
    public ComputeFundingRecoveryService(ComputeFundingIntentService journal, ObjectProvider<ExternalBillingGateway> gateways,
                                        ExternalBillingProperties properties) {
        this(journal, (Supplier<ExternalBillingGateway>) gateways::getIfAvailable, properties);
    }

    public ComputeFundingRecoveryService(ComputeFundingIntentService journal, Supplier<ExternalBillingGateway> gateways,
                                        ExternalBillingProperties properties) {
        this.journal = journal; this.gateways = gateways; this.properties = properties;
    }

    public ComputeFundingIntentService.Intent advance(String intentId, long nowMillis) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Funding transport cannot run inside a database transaction");
        ComputeFundingIntentService.Intent original = journal.recover(intentId);
        if (!properties.isEnabled() || !properties.isAllowWrites() || !"HOLDS_V3".equals(original.contractVersion)
            || ComputeFundingIntentService.CONFLICT.equals(original.state)) return original;
        ExternalBillingGateway gateway = gateways.get();
        if (gateway == null) return original;
        if (ComputeFundingIntentService.PREPARED.equals(original.state)) {
            ComputeFundingIntentService.Intent claim = journal.claimInitialSend(intentId, nowMillis);
            if (claim == null) return journal.recover(intentId);
            ExternalBillingGateway.Result result;
            try {
                if ("HOLD".equals(claim.operation)) result = gateway.createHold(claim.key, claim.body);
                else {
                    String holdId = journal.recover(claim.parentId).holdId;
                    result = "TOPUP".equals(claim.operation) ? gateway.topupHold(holdId, claim.key, claim.body)
                        : gateway.releaseHold(holdId, claim.key, claim.body);
                }
            } catch (RuntimeException uncertain) { return journal.recordUnknown(intentId, claim.requestHash); }
            if (result != null && result.status == ExternalBillingGateway.Status.SUCCEEDED && result.charge != null) {
                try { return journal.recordReceipt(intentId, claim.requestHash, result.charge); }
                catch (IllegalArgumentException unverified) { return journal.recordUnknown(intentId, claim.requestHash); }
            }
            return journal.recordFailure(intentId, claim.requestHash, result);
        }
        // A detail query can recover a HOLD resource. It cannot identify which topup/release
        // produced the current counters; their operation query remains fail-closed.
        try {
            gateway.holdOperation(original.operation, original.reference);
            ComputeFundingIntentService.Intent hold = "HOLD".equals(original.operation) ? original : journal.recover(original.parentId);
            long queryStartedAtMs = System.currentTimeMillis();
            ExternalBillingGateway.Result query = "HOLD".equals(original.operation)
                ? gateway.holdByReference(hold.reference) : gateway.holdById(hold.holdId);
            if (query != null && query.status == ExternalBillingGateway.Status.SUCCEEDED && query.charge != null)
                journal.recordHoldDetail(hold.id, hold.requestHash, query.charge, queryStartedAtMs);
        } catch (RuntimeException uncertain) { /* Keep the immutable journal and current uncertainty. */ }
        return journal.recover(intentId);
    }
}
