package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeNode;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePlatformAuthorization;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic conversion and isolated H2 only; never a current wallet rate or live permission. */
@Tag("dev")
class ComputeBillingTrialGuardTest {
    private final ObjectMapper json = new ObjectMapper();
    private final String run = "91de8320-9030-4263-8d5f-8602dcc9482e";
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private ExternalBillingProperties properties;
    private ComputeBillingTrialGuard guard;
    private ObjectNode price, config;
    private ComputeNode node;
    private BillingPayer payer;

    @BeforeEach void setup() {
        DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source); tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("CREATE TABLE vls_compute_billing_session(usage_id VARCHAR PRIMARY KEY,tenant_id VARCHAR,opening_user_id VARCHAR,merchant_id VARCHAR,payer_user_id VARCHAR,instance_ref VARCHAR,accepted_snapshot VARCHAR)");
        jdbc.execute("CREATE TABLE vls_compute_paid_run(run_id VARCHAR PRIMARY KEY,usage_id VARCHAR,node_id BIGINT,instance_key VARCHAR,reserve_quota VARCHAR,tenant_id VARCHAR,opening_user_id VARCHAR,payer_user_id VARCHAR,merchant_id VARCHAR,quote_snapshot VARCHAR,hold_intent_id VARCHAR)");
        jdbc.execute("CREATE TABLE vls_compute_billing_slice(merchant_charge_id VARCHAR PRIMARY KEY,usage_id VARCHAR,amount_text VARCHAR,state VARCHAR,error_code VARCHAR,charge_id VARCHAR,ledger_entry_id VARCHAR,lease_token VARCHAR,lease_until_ms BIGINT DEFAULT 0)");
        jdbc.execute("CREATE TABLE vls_compute_funding_intent(intent_id VARCHAR PRIMARY KEY,usage_id VARCHAR,operation_kind VARCHAR)");
        jdbc.execute("CREATE TABLE vls_compute_refund_intent(refund_intent_id VARCHAR PRIMARY KEY,usage_id VARCHAR)");
        properties = new ExternalBillingProperties(); properties.setBaseUrl(ComputeBillingTrialGuard.TARGET); properties.setMerchantId("vls"); properties.setMaximumRunSeconds(600);
        properties.getTrial().setEnabled(true); properties.getTrial().setRunId(run); properties.getTrial().setMaximumWalletCny("3.47"); properties.getTrial().setExpiresAt(Instant.now().plusSeconds(3600).toString());
        guard = new ComputeBillingTrialGuard(jdbc, json, properties);
        node = new ComputeNode(); node.setId(-6000L); node.setConfigurationManaged(true); node.setConfigurationCode("official-autodl"); node.setProvider("autodl"); node.setProviderProduct("AUTODL_PRO"); node.setProviderInstanceId("pro-7920a22db144"); node.setPublicInstanceKey("fixture-instance");
        ComputePlatformAuthorization.Context actor = new ComputePlatformAuthorization.Context(); actor.setAuthorized(true); actor.setVlsUserId("actor"); actor.setVlsTenantId("tenant"); actor.setPlatformUserId("verified-subject");
        payer = BillingPayer.fromVerifiedSelf(actor,json.createObjectNode().put("id",13));
        price = json.createObjectNode().put("price_snapshot_id","fixture-fixed").put("approved_hourly_price","13.00").put("quota_unit","fixture-quota").put("conversion_version","fixture-v1").put("numerator","1000").put("denominator","1").put("effective_at",Instant.now().minusSeconds(3600).toString()).put("valid_until",Instant.now().plusSeconds(7200).toString());
        price.putObject("accepted_rate").put("currency","CNY").put("pricePolicy","VLS_FIXED");
        config = json.createObjectNode().put("settlement_currency","CNY");
        for(String key:new String[]{"quota_unit","conversion_version","effective_at","valid_until"}) config.set(key,price.get(key));
        config.putObject("quota_per_currency_unit").put("numerator","1000").put("denominator","1");
    }
    private void bind() {
        assertEquals(run,guard.quoteRunId(node,payer,price,960000,new BigInteger("3467")));
        jdbc.update("INSERT INTO vls_compute_billing_session VALUES('usage','tenant','actor','vls','13','node-server:-6000',?)",price.toString());
        jdbc.update("INSERT INTO vls_compute_paid_run VALUES(?,'usage',-6000,'fixture-instance','3467','tenant','actor','13','vls',?,'hold-intent')",run,price.toString());
    }
    private void slice(String id,String amount,String state,String error) {
        jdbc.update("INSERT INTO vls_compute_billing_slice(merchant_charge_id,usage_id,amount_text,state,error_code) VALUES(?,'usage',?,?,?)",id,amount,state,error);
    }
    private void budget(String extra) { tx.execute(s->{guard.requireCaptureBudget("usage",new BigInteger(extra));return null;}); }

    @Test void defaultsNeverEnableDevEvenWhenOtherFlagsWereEnabled() {
        properties.setTrial(new ExternalBillingProperties.Trial());
        properties.setEnabled(true);properties.setAllowWrites(true);properties.setPaidAdmissionEnabled(true);
        assertThrows(IllegalStateException.class,()->guard.requirePayer(payer));
    }
    @Test void normalUnconfiguredDestinationPreservesExistingScope() {
        properties.setBaseUrl("https://billing.example.test"); properties.setTrial(new ExternalBillingProperties.Trial());
        assertFalse(guard.requested());assertNull(guard.requestedRunId());
    }
    @Test void explicitOffOrMissingRunNeverGrantsARequest() {
        properties.getTrial().setEnabled(false);assertThrows(IllegalStateException.class,()->guard.requestedRunId());
        properties.getTrial().setEnabled(true);properties.getTrial().setRunId(null);assertThrows(IllegalStateException.class,()->guard.requestedRunId());
    }
    @Test void equivalentDevHostnameCannotBypassDefaultClosedPermit() {
        properties.setBaseUrl("HTTPS://WORKUP-DEV.MYOUMUAMUA.COM:6433/bus/apaas-newapi/");
        guard.requirePayer(payer);
        properties.setTrial(new ExternalBillingProperties.Trial());
        assertTrue(guard.requested());assertThrows(IllegalStateException.class,()->guard.requirePayer(payer));
    }
    @Test void differentPortOrPathOnDevHostCannotEscapeIsolation() {
        for(String target:new String[]{"http://workup-dev.myoumuamua.com:6433/bus/apaas-newapi","https://workup-dev.myoumuamua.com:443/bus/apaas-newapi","https://workup-dev.myoumuamua.com:6433/bus/%61paas-newapi","https://workup-dev.myoumuamua.com.:6433/bus/apaas-newapi"}) {
            properties.setBaseUrl(target);assertTrue(guard.requested());assertThrows(IllegalStateException.class,()->guard.requirePayer(payer));
        }
    }
    @Test void incorrectInstanceIsRejectedBeforeAnyExternalWork() {
        node.setProviderInstanceId("another-pro");assertThrows(IllegalStateException.class,()->guard.requireNode(node));
    }
    @Test void capUsesFloorWhileReservationUsesCeilingWithoutHistoricRate() {
        price.put("numerator","1");
        assertThrows(IllegalStateException.class,()->guard.quoteRunId(node,payer,price,960000,new BigInteger("4")));
    }
    @Test void fractionalConversionMustNotRoundApprovedCapUp() {
        price.put("numerator","7").put("denominator","3");
        assertThrows(IllegalStateException.class,()->guard.quoteRunId(node,payer,price,960000,new BigInteger("9")));
    }
    @Test void mismatchedPriceDurationAndExpiredPermissionRejectQuote() {
        assertThrows(IllegalStateException.class,()->guard.quoteRunId(node,payer,price,960001,new BigInteger("3467")));
        price.put("approved_hourly_price","14");assertThrows(IllegalStateException.class,()->guard.quoteRunId(node,payer,price,960000,new BigInteger("3467")));
        properties.getTrial().setExpiresAt(Instant.now().minusSeconds(1).toString());assertThrows(IllegalStateException.class,()->guard.requirePayer(payer));
    }
    @Test void newProcessRecoversSameImmutablePermitAndCurrentConversion() {
        bind();new ComputeBillingTrialGuard(jdbc,json,properties).requireNewUsage("usage",config);
        assertEquals("3470",price.path("trial_guard").path("maximum_quota").asText());
    }
    @Test void selectedOriginalCleanupIgnoresDisabledOrExpiredSpendParameters() {
        bind(); properties.getTrial().setEnabled(false); properties.getTrial().setMaximumWalletCny(null);
        properties.getTrial().setExpiresAt(Instant.now().minusSeconds(60).toString());
        assertTrue(guard.selectsRun(run)); assertTrue(guard.selectsUsage("usage"));
        assertDoesNotThrow(()->guard.requireCleanupScope("usage"));
        assertThrows(IllegalStateException.class,()->guard.requireNewUsage("usage",config));
    }
    @Test void cleanupCannotTargetAnotherRunOrPayer() {
        bind(); properties.getTrial().setRunId(UUID.randomUUID().toString());
        assertFalse(guard.selectsRun(run)); assertFalse(guard.selectsUsage("usage"));
        assertThrows(IllegalStateException.class,()->guard.requireCleanupScope("usage"));
        properties.getTrial().setRunId(run); jdbc.update("UPDATE vls_compute_billing_session SET payer_user_id='14' WHERE usage_id='usage'");
        assertThrows(IllegalStateException.class,()->guard.requireCleanupScope("usage"));
    }
    @Test void cleanupRejectsAnUnmarkedLegacyDevUsage() {
        bind(); price.remove(ComputeBillingTrialGuard.MARKER);
        jdbc.update("UPDATE vls_compute_billing_session SET accepted_snapshot=? WHERE usage_id='usage'",price.toString());
        jdbc.update("UPDATE vls_compute_paid_run SET quote_snapshot=? WHERE run_id=?",price.toString(),run);
        assertThrows(IllegalStateException.class,()->guard.requireCleanupScope("usage"));
    }
    @Test void cleanupNeverUsesAChangedEndpointOrMerchant() {
        bind(); properties.setBaseUrl("https://billing.example.test");
        assertThrows(IllegalStateException.class,()->guard.requireCleanupScope("usage"));
        properties.setBaseUrl(ComputeBillingTrialGuard.TARGET); properties.setMerchantId("another");
        assertThrows(IllegalStateException.class,()->guard.requireCleanupScope("usage"));
    }
    @Test void missingChangedOrNonCnyConfigStopsOnlyNewRequests() {
        bind();assertThrows(IllegalStateException.class,()->guard.requireNewUsage("usage",null));
        config.put("conversion_version","fixture-v2");assertThrows(IllegalStateException.class,()->guard.requireNewUsage("usage",config));
        config.put("conversion_version","fixture-v1");((ObjectNode)config.path("quota_per_currency_unit")).put("numerator","1001");assertThrows(IllegalStateException.class,()->guard.requireNewUsage("usage",config));
        config.put("settlement_currency","USD");assertThrows(IllegalStateException.class,()->guard.requireNewUsage("usage",config));
    }
    @Test void durableMarkerCannotBeBypassedByDisablingOrChangingDestination() {
        bind();properties.setTrial(new ExternalBillingProperties.Trial());properties.setBaseUrl("https://billing.example.test");
        assertTrue(guard.protectedUsage("usage"));assertThrows(IllegalStateException.class,()->guard.requireNewUsage("usage",config));
    }
    @Test void changedCapOrRunIdentityDoesNotRewriteExistingPermission() {
        bind();properties.getTrial().setMaximumWalletCny("3.46");assertThrows(IllegalStateException.class,()->guard.requireNewUsage("usage",config));
        properties.getTrial().setMaximumWalletCny("3.47");jdbc.update("UPDATE vls_compute_paid_run SET opening_user_id='other'");assertThrows(IllegalStateException.class,()->guard.requireNewUsage("usage",config));
    }
    @Test void unknownSettledAndBlockedWithoutErrorAllConsumeGrossBudget() {
        bind();slice("a","3000","UNKNOWN",null);slice("b","467","BLOCKED",null);budget("0");
        assertThrows(IllegalStateException.class,()->budget("1"));
        jdbc.update("UPDATE vls_compute_billing_slice SET state='SETTLED'");assertThrows(IllegalStateException.class,()->budget("1"));
    }
    @Test void neverAllocatedFutureMeteringDoesNotPreventPriorAllocatedSlice() {
        bind();slice("a","3467","PREPARED",null);slice("b","2000","BLOCKED","TRIAL_UNALLOCATED_BUDGET");budget("0");
        jdbc.update("UPDATE vls_compute_billing_slice SET lease_token='possibly-sent' WHERE merchant_charge_id='b'");
        assertThrows(IllegalStateException.class,()->budget("0"));
    }
    @Test void refundNeverReplenishesGrossBudgetOrAllowsAnyNewCapture() {
        bind();slice("a","100","SETTLED",null);jdbc.update("INSERT INTO vls_compute_refund_intent VALUES('refund','usage')");
        assertThrows(IllegalStateException.class,()->budget("0"));
    }
    @Test void captureReservationRequiresTransactionLock() {
        bind();assertThrows(IllegalStateException.class,()->guard.requireCaptureBudget("usage",BigInteger.ONE));
    }
    @Test void fundingAllowsOnlyOneOriginalBoundHoldAndNeverTopup() {
        bind();jdbc.update("INSERT INTO vls_compute_funding_intent VALUES('hold-intent','usage','HOLD')");
        guard.requireNewFunding("usage","HOLD","3467",config);
        assertThrows(IllegalStateException.class,()->guard.requireNewFunding("usage","TOPUP","1",config));
        jdbc.update("INSERT INTO vls_compute_funding_intent VALUES('other-hold','usage','HOLD')");
        assertThrows(IllegalStateException.class,()->guard.requireNewFunding("usage","HOLD","3467",config));
    }
    @Test void cleanupPermissionDoesNotDependOnTrialOrConversionStillBeingValid() {
        bind();properties.getTrial().setEnabled(false);properties.getTrial().setExpiresAt(Instant.now().minusSeconds(1).toString());
        assertDoesNotThrow(()->guard.requireFundingPreparation("usage","RELEASE","3467"));
        assertDoesNotThrow(()->guard.requireNewFunding("usage","RELEASE","3467",null));
        assertThrows(IllegalStateException.class,()->guard.requireNewFunding("usage","HOLD","3467",config));
    }
}
