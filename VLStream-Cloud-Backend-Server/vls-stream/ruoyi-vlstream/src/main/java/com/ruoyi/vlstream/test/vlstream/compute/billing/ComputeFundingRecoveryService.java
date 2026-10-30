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
            || ComputeFundingIntentService.CONFLICT.equals(original.state) || ComputeFundingIntentService.REJECTED.equals(original.state)
            || ComputeFundingIntentService.OPERATION_CONFIRMED.equals(original.state)) return original;
        ExternalBillingGateway gateway = gateways.get();
        if (gateway == null) return original;
        if (ComputeFundingIntentService.PREPARED.equals(original.state)) {
            try {
                // Read the current conversion outside the journal transaction, before committing
                // UNKNOWN. Config failure must not claim an HTTP request that was never sent.
                com.fasterxml.jackson.databind.JsonNode config = journal.requiresTrialConfig(intentId) ? gateway.config() : null;
                ComputeFundingIntentService.Intent claim = journal.claimInitialSend(intentId, nowMillis, config);
                if (claim == null) return journal.recover(intentId);
                return sendOriginal(gateway, claim);
            } catch (RuntimeException denied) {
                return journal.recover(intentId);
            }
        }
        try {
            ExternalBillingGateway.Result lookup = null;
            if (ComputeFundingIntentService.UNKNOWN.equals(original.state)) {
                lookup = gateway.holdOperation(original.operation, original.reference);
                if (lookup != null) {
                    original = journal.checkOperationLookup(intentId, original.requestHash, lookup.charge);
                    if (ComputeFundingIntentService.CONFLICT.equals(original.state)) return original;
                    if (blocked(lookup)) return journal.recordFailure(intentId, original.requestHash, lookup);
                    if (lookup.status == ExternalBillingGateway.Status.PROCESSING) return original;
                }
            }
            long queryStartedAtMs = System.currentTimeMillis();
            ExternalBillingGateway.Result query = queryHold(gateway, original, queryStartedAtMs);
            if (blocked(query)) return journal.recordFailure(intentId, original.requestHash, query);
            original = journal.recover(intentId);
            if (lookup != null) original = journal.checkOperationLookup(intentId, original.requestHash, lookup.charge);
            if (ComputeFundingIntentService.CONFLICT.equals(original.state)) return original;
            if (ComputeFundingIntentService.RECEIPT_UNVERIFIED.equals(original.state) && !"HOLD".equals(original.operation))
                return successful(query) ? journal.confirmOperation(intentId, queryStartedAtMs) : original;
            if (!ComputeFundingIntentService.UNKNOWN.equals(original.state)) return original;
            // Replay requires a fresh validated parent and a fully associated minimal lookup.
            // A missing/unknown HOLD query is not a supported NOT_FOUND proof.
            if (!successful(query)) return original;
            if (query != null && query.status == ExternalBillingGateway.Status.PROCESSING) return original;
            ComputeFundingIntentService.Intent replay = journal.prepareOriginalReplay(intentId, nowMillis, queryStartedAtMs,
                lookup == null ? null : lookup.charge);
            return replay == null ? journal.recover(intentId) : sendOriginal(gateway, replay);
        } catch (RuntimeException uncertain) { /* Keep the immutable journal and current uncertainty. */ }
        return journal.recover(intentId);
    }

    /** Called at most once per advance. Multiple HTTP attempts use the identical immutable
     * key/path/body; server idempotence, not HTTP attempt count, prevents a second side effect. */
    private ComputeFundingIntentService.Intent sendOriginal(ExternalBillingGateway gateway, ComputeFundingIntentService.Intent original) {
        try {
            ExternalBillingGateway.Result result;
            if ("HOLD".equals(original.operation)) result = gateway.createHold(original.key, original.body);
            else {
                String holdId = journal.recover(original.parentId).holdId;
                result = "TOPUP".equals(original.operation) ? gateway.topupHold(holdId, original.key, original.body)
                    : gateway.releaseHold(holdId, original.key, original.body);
            }
            if (!successful(result)) return journal.recordFailure(original.id, original.requestHash, result);
            ComputeFundingIntentService.Intent receipt = journal.recordReceipt(original.id, original.requestHash, result.charge);
            if (!"HOLD".equals(original.operation) && ComputeFundingIntentService.RECEIPT_UNVERIFIED.equals(receipt.state)) {
                long queryStartedAtMs = System.currentTimeMillis();
                ExternalBillingGateway.Result query = queryHold(gateway, receipt, queryStartedAtMs);
                if (blocked(query)) return journal.recordFailure(original.id, original.requestHash, query);
                if (successful(query)) return journal.confirmOperation(original.id, queryStartedAtMs);
            }
            return journal.recover(original.id);
        } catch (RuntimeException uncertain) { return journal.recordUnknown(original.id, original.requestHash); }
    }

    private ExternalBillingGateway.Result queryHold(ExternalBillingGateway gateway, ComputeFundingIntentService.Intent intent, long startedAt) {
        ComputeFundingIntentService.Intent hold = "HOLD".equals(intent.operation) ? intent : journal.recover(intent.parentId);
        ExternalBillingGateway.Result query = "HOLD".equals(intent.operation)
            ? gateway.holdByReference(hold.reference) : gateway.holdById(hold.holdId);
        if (successful(query)) journal.recordHoldDetail(hold.id, hold.requestHash, query.charge, startedAt);
        return query;
    }

    private static boolean successful(ExternalBillingGateway.Result result) {
        return result != null && result.status == ExternalBillingGateway.Status.SUCCEEDED && result.charge != null;
    }

    private static boolean blocked(ExternalBillingGateway.Result result) {
        return result != null && (result.status == ExternalBillingGateway.Status.NOT_AUTHORIZED
            || result.status == ExternalBillingGateway.Status.CONFLICT || result.status == ExternalBillingGateway.Status.REJECTED);
    }
}
