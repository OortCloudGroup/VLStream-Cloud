package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeBillingPolicyTest {
    private ComputeBillingRate rate() {
        ComputeBillingRate rate = new ComputeBillingRate();
        rate.setVersion("test-rate-v1"); rate.setCostSource("VERIFIED_MANUAL"); rate.setCurrency("CNY");
        rate.setCostScope("INSTANCE_COMPUTE"); rate.setCostUnit("PER_INSTANCE_HOUR"); rate.setCostHourly(BigDecimal.ONE);
        rate.setValidFrom("2026-01-01T00:00:00Z"); rate.setValidUntil("2027-01-01T00:00:00Z");
        rate.setQuotaUnit("test-unit"); rate.setConversionVersion("test-conversion"); rate.setQuotaPerCurrencyUnit(new BigDecimal("100"));
        rate.setQuotaScale(0); rate.setQuotaRounding(RoundingMode.DOWN);
        rate.setPricePolicy("LOCKED_FOR_RUN"); rate.setProviderPriceLockConfirmed(true); rate.setPriceEvidence("TEST_ONLY_SUPPLIER_CONTRACT");
        return rate;
    }
    private ComputeNode economical() {
        ComputeNode node = new ComputeNode(); node.setId(1L); node.setProvider("aliyun"); node.setProviderProduct("ECS");
        node.setControlMode("API"); node.setRequestedStoppedMode("StopCharging"); node.setActualStoppedMode("StopCharging");
        node.setCapacitySource("PROVIDER_API"); node.setPowerState("STOPPED"); node.setProviderChargeType("PostPaid");
        node.setPowerCheckedAt(new Date()); node.setPowerOperationAction("stop"); return node;
    }
    private ComputeBillingRate fixed() {
        ComputeBillingRate value=rate();value.setPricePolicy("VLS_FIXED");value.setUserPriceSource("OFFICIAL_CONFIG");
        value.setFixedPriceConfigurationId("official-autodl");value.setFixedPriceUnit("PER_INSTANCE_HOUR");value.setFixedHourlyPrice(new BigDecimal("5"));
        value.setProviderPriceLockConfirmed(false);value.setPriceEvidence(null);value.setCostSource(null);value.setCostHourly(null);return value;
    }
    @Test void officialFixedPriceNeedsNeitherSupplierPriceNorLockAndDoesNotApplyLegacyMarkup() {
        ComputeBillingRate value=fixed();value.validate(Instant.now());
        assertEquals(0,new BigDecimal("500").compareTo(value.unroundedQuota(3600000)));
        value.setCostHourly(new BigDecimal("99999"));value.setSaleMultiplier(new BigDecimal("99"));
        assertEquals(0,new BigDecimal("500").compareTo(value.unroundedQuota(3600000)));
    }
    @Test void missingOfficialPriceOrWrongConfigSourceCannotFallBackToSupplierCost() {
        ComputeBillingRate value=fixed();value.setFixedHourlyPrice(null);value.setCostHourly(BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class,()->value.validate(Instant.now()));
        value.setFixedHourlyPrice(BigDecimal.ZERO);assertThrows(IllegalArgumentException.class,()->value.validate(Instant.now()));
        value.setFixedHourlyPrice(BigDecimal.ONE);value.setUserPriceSource("SUPPLIER_API");assertThrows(IllegalArgumentException.class,()->value.validate(Instant.now()));
        value.setUserPriceSource("OFFICIAL_CONFIG");value.setProviderPriceLockConfirmed(true);assertThrows(IllegalArgumentException.class,()->value.validate(Instant.now()));
    }
    @Test void acceptedFixedUserPriceRemainsUnchangedWhenDeploymentPriceChanges() throws Exception {
        ComputeBillingRate value=fixed();ObjectMapper mapper=new ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode config=mapper.readTree("{\"settlement_currency\":\"CNY\",\"quota_unit\":\"test-unit\",\"conversion_version\":\"test-conversion\",\"quota_per_currency_unit\":{\"numerator\":\"100\",\"denominator\":\"1\"},\"effective_at\":\"2026-01-01T00:00:00Z\",\"valid_until\":\"2027-01-01T00:00:00Z\"}");
        com.ruoyi.vlstream.test.vlstream.compute.billing.BillingSnapshot accepted=com.ruoyi.vlstream.test.vlstream.compute.billing.BillingSnapshot.fromExistingRate(value,config,Instant.now(),mapper);
        value.setFixedHourlyPrice(new BigDecimal("9"));
        assertEquals(0,new BigDecimal("5").compareTo(accepted.approvedHourlyPrice));
        ComputeBillingRate historical=mapper.treeToValue(mapper.readTree(accepted.json).path("accepted_rate"),ComputeBillingRate.class);
        assertEquals(0,new BigDecimal("500").compareTo(historical.unroundedQuota(3600000)));
    }
    @Test void defaultConfigurationKeepsRunningAndCannotEnableDeletionOrPowerAutomation() {
        ComputeBillingPolicy policy = new ComputeBillingPolicy(); policy.validate();
        assertEquals(3600, policy.getOpeningReserveSeconds()); assertEquals(7200, policy.getEarlyReminderSeconds());
        assertEquals(1800, policy.getUrgentReminderSeconds()); assertEquals(300, policy.getStopBudgetSeconds());
        assertEquals("KEEP_RUNNING", policy.getTrainingCompletionAction()); assertFalse(policy.isAutomaticDeletionEnabled());
    }
    @Test void springConfigurationBindsRiskDefaultsWithoutInstallingAnExecutor() {
        Map<String,Object> values = new HashMap<>(); values.put("vlstream.compute.billing-policy.stop-budget-seconds", "480");
        ComputeBillingPolicy policy = new Binder(new MapConfigurationPropertySource(values))
            .bind("vlstream.compute.billing-policy", Bindable.of(ComputeBillingPolicy.class)).get();
        policy.validate(); assertEquals(480, policy.getStopBudgetSeconds());
        assertEquals("KEEP_RUNNING", policy.getTrainingCompletionAction());
    }
    @Test void noCompensationAndPlatformMinimumCostRemainFixedWithoutAnAssumedManualStorageService() {
        ComputeBillingPolicy policy = new ComputeBillingPolicy(); policy.validate();
        assertFalse(policy.isFaultCompensationEnabled()); assertFalse(policy.isAutomaticDeletionEnabled());
        assertEquals("PLATFORM", policy.getSupplierMinimumCostBearer());
        policy.setFaultCompensationEnabled(true); assertThrows(IllegalArgumentException.class, policy::validate);
        policy = new ComputeBillingPolicy(); policy.setSupplierMinimumCostBearer("USER");
        IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class, policy::validate);
        assertTrue(rejected.getMessage().contains("错误扣款仍须纠正"));
    }
    @Test void invalidThresholdsAndUnsupportedLifecycleActionsFailClosed() {
        ComputeBillingPolicy policy = new ComputeBillingPolicy(); policy.setOpeningReserveSeconds(0);
        assertThrows(IllegalArgumentException.class, policy::validate);
        policy = new ComputeBillingPolicy(); policy.setUrgentReminderSeconds(300);
        assertThrows(IllegalArgumentException.class, policy::validate);
        policy = new ComputeBillingPolicy(); policy.setEarlyReminderSeconds(1800);
        assertThrows(IllegalArgumentException.class, policy::validate);
        policy = new ComputeBillingPolicy(); policy.setPollingSafetySeconds(300);
        assertThrows(IllegalArgumentException.class, policy::validate);
        policy = new ComputeBillingPolicy(); policy.setStopBudgetSeconds(299);
        assertThrows(IllegalArgumentException.class, policy::validate);
        policy = new ComputeBillingPolicy(); policy.setTrainingCompletionAction("STOP_AFTER_TRAINING");
        assertThrows(IllegalArgumentException.class, policy::validate);
        policy = new ComputeBillingPolicy(); policy.setAutomaticDeletionEnabled(true);
        assertThrows(IllegalArgumentException.class, policy::validate);
    }
    @Test void unknownOrSlowProviderStopCannotFitAnAssumedFiveMinuteWindow() {
        ComputeBillingPolicy policy = new ComputeBillingPolicy();
        assertEquals(300, policy.checkedStopBudgetSeconds(240));
        assertThrows(IllegalArgumentException.class, () -> policy.checkedStopBudgetSeconds(null));
        assertThrows(IllegalArgumentException.class, () -> policy.checkedStopBudgetSeconds(0));
        assertThrows(IllegalArgumentException.class, () -> policy.checkedStopBudgetSeconds(241));
        assertThrows(IllegalArgumentException.class, () -> policy.checkedStopBudgetSeconds(Integer.MAX_VALUE));
    }
    @Test void initialEstimateUsesOnlyConfiguredComputeWindowAndDoesNotAssumeAUserDiskFee() {
        ComputeBillingPolicy policy = new ComputeBillingPolicy();
        assertEquals(0, new BigDecimal("200").compareTo(policy.openingComputeReserveQuota(rate())));
        policy.setOpeningReserveSeconds(1800);
        assertEquals(0, new BigDecimal("100").compareTo(policy.openingComputeReserveQuota(rate())));
    }
    @Test void unknownOpeningPriceOrConversionCannotBeDefaultedToZero() {
        ComputeBillingPolicy policy = new ComputeBillingPolicy();
        assertThrows(IllegalArgumentException.class, () -> policy.openingComputeReserveQuota(null));
        ComputeBillingRate unknown = rate(); unknown.setCostSource("REFERENCE_QUOTE");
        assertThrows(IllegalArgumentException.class, () -> policy.openingComputeReserveQuota(unknown));
        unknown.setCostSource("VERIFIED_MANUAL"); unknown.setQuotaPerCurrencyUnit(null);
        assertThrows(IllegalArgumentException.class, () -> policy.openingComputeReserveQuota(unknown));
    }
    @Test void minimumSupplierCostDifferenceIsAnInternalEstimateAndNeverRaisesUserSeconds() {
        ComputeBillingPolicy policy = new ComputeBillingPolicy(); ComputeBillingRate rate = rate();
        assertEquals(0, new BigDecimal("5").compareTo(rate.unroundedQuota(90000))); // user still pays exactly 90 sec
        assertEquals(0, new BigDecimal("0.01").compareTo(policy.platformCostDifference(rate, 90000, new BigDecimal("0.035"), "CNY")));
        assertEquals(BigDecimal.ZERO, policy.platformCostDifference(rate, 90000, new BigDecimal("0.02"), "CNY"));
        assertEquals(0, new BigDecimal("5").compareTo(rate.unroundedQuota(90000)));
        assertThrows(IllegalArgumentException.class, () -> policy.platformCostDifference(rate, 90000, null, "CNY"));
        assertThrows(IllegalArgumentException.class, () -> policy.platformCostDifference(rate, 90000, BigDecimal.ONE, "USD"));
    }
    @Test void supplierLockRequiresExplicitEvidenceAndUnknownHistoricalSnapshotsCannotInferIt() throws Exception {
        ComputeBillingRate rate = rate(); rate.setProviderPriceLockConfirmed(false);
        assertThrows(IllegalArgumentException.class, () -> rate.validate(Instant.now()));
        rate.setProviderPriceLockConfirmed(true); rate.setPriceEvidence(" ");
        assertThrows(IllegalArgumentException.class, () -> rate.validate(Instant.now()));
        ComputeBillingRate historical = new ObjectMapper().readValue("{\"version\":\"legacy\"}", ComputeBillingRate.class);
        assertEquals("UNVERIFIED", historical.getPricePolicy());
        assertThrows(IllegalArgumentException.class, () -> historical.validate(Instant.now()));
    }
    @Test void verifiedPriceNoticeDoesNotGetTreatedAsAWholeRunLockByTheSinglePeriodLedger() {
        ComputeBillingRate rate = rate(); rate.setPricePolicy("SEGMENTED_NOTICE"); rate.setProviderPriceLockConfirmed(false);
        rate.validate(Instant.now()); JdbcTemplate jdbc = mock(JdbcTemplate.class);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> new ComputeBillingLedgerService(jdbc, new ObjectMapper())
            .reserve("test-server", "test-tenant", "test-local", "test-sso", rate, false, "test-op"));
        assertTrue(exception.getMessage().contains("分段")); verifyNoInteractions(jdbc);
    }
    @Test void acceptedSnapshotIsUnaffectedByLaterPriceChangesAndOldPeriodsAreNotRepriced() throws Exception {
        ComputeBillingRate current = rate(); ObjectMapper json = new ObjectMapper(); String snapshot = json.writeValueAsString(current);
        current.setCostHourly(new BigDecimal("9")); current.setVersion("new-rate");
        ComputeBillingRate accepted = json.readValue(snapshot, ComputeBillingRate.class);
        accepted.validate(Instant.parse("2026-10-10T00:00:00Z"));
        assertEquals("test-rate-v1", accepted.getVersion());
        assertEquals(0, new BigDecimal("5").compareTo(accepted.unroundedQuota(90000)));
        assertEquals(0, new BigDecimal("45").compareTo(current.unroundedQuota(90000)));
    }
    @Test void freshActualEconomicalPostpaidStopCanPassOnlyTheOfferGuard() {
        assertDoesNotThrow(() -> ComputeBillingPolicy.requireEconomicalPaidOffer(economical(), System.currentTimeMillis()));
    }
    @Test void requestedModeOrdinaryStopPrepaidStaleAndManualEvidenceCannotAdmitPaidSupply() {
        ComputeNode node = economical(); node.setActualStoppedMode("KeepCharging"); reject(node);
        node = economical(); node.setActualStoppedMode(null); reject(node);
        node = economical(); node.setRequestedStoppedMode("KeepCharging"); reject(node);
        node = economical(); node.setProviderChargeType("PrePaid"); reject(node);
        node = economical(); node.setPowerState("RUNNING"); reject(node);
        node = economical(); node.setCapacitySource("OPERATOR_CONSOLE"); reject(node);
        node = economical(); node.setPowerCheckedAt(new Date(System.currentTimeMillis() - ComputeAvailability.FRESHNESS_MS - 1000)); reject(node);
    }
    private void reject(ComputeNode node) {
        assertThrows(RuntimeException.class, () -> ComputeBillingPolicy.requireEconomicalPaidOffer(node, System.currentTimeMillis()));
    }
    @Test void ordinaryAutodlProAndOwnServerCannotBorrowEcsStopProof() {
        ComputeNode node = economical(); node.setProvider("autodl"); node.setProviderProduct(null); reject(node);
        node.setProviderProduct("AUTODL_PRO"); reject(node); node.setProvider("self"); reject(node);
    }
    @Test void powerOffAndComputeBillingEndAreIndependentEvenWhenStopChargingWasRequested() {
        ComputeNode node = economical(); ComputePowerProvider.Snapshot stopped = new ComputePowerProvider.Snapshot("STOPPED", "UNKNOWN", "TEST_ONLY");
        assertFalse(ComputeBillingPolicy.computeStopConfirmed(node, stopped));
        stopped.chargeType = "PostPaid"; stopped.stoppedMode = "KeepCharging";
        assertFalse(ComputeBillingPolicy.computeStopConfirmed(node, stopped));
        stopped.stoppedMode = "StopCharging"; assertTrue(ComputeBillingPolicy.computeStopConfirmed(node, stopped));
        stopped.chargeType = "PrePaid"; assertFalse(ComputeBillingPolicy.computeStopConfirmed(node, stopped));
        stopped.chargeType = "PostPaid"; node.setProvider("autodl"); assertFalse(ComputeBillingPolicy.computeStopConfirmed(node, stopped));
    }
    @Test void ordinaryStoppedObservationRetainsUnresolvedUsageWithoutAnEndOrSettlement() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class); ComputeNode node = economical();
        Map<String,Object> row = new HashMap<>(); row.put("id", "test-usage"); row.put("billing_start_at", Timestamp.from(Instant.now().minusSeconds(90)));
        when(jdbc.queryForList(contains("active_server_key"), eq("node-server:1"))).thenReturn(Collections.singletonList(row));
        ComputeBillingLedgerService ledger = spy(new ComputeBillingLedgerService(jdbc, new ObjectMapper()));
        ComputePowerProvider.Snapshot stopped = new ComputePowerProvider.Snapshot("STOPPED", "UNKNOWN", "TEST_ONLY");
        stopped.stoppedMode = "KeepCharging"; stopped.chargeType = "PostPaid";
        ledger.observePower(node, stopped, 1L);
        verify(jdbc).update(contains("state='RECONCILE'"), eq("test-usage"));
        verify(ledger, never()).confirmedStopped(anyString(), any(), any(), anyString());
        verify(jdbc, never()).update(contains("billing_end_at"), any(Object[].class));
    }
}
