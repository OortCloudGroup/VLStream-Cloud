package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class BillingLedgerOriginTest {
    private final ObjectMapper json = new ObjectMapper();

    private ExternalBillingProperties properties(String url) {
        ExternalBillingProperties value = new ExternalBillingProperties();
        value.setBaseUrl(url); value.setMerchantId("same-merchant");
        return value;
    }

    @Test void normalizedHttpsIdentityKeepsPortAndPathWhileAcceptingEquivalentSpelling() {
        ExternalBillingProperties accepted = properties("HTTPS://LEDGER.fixture.invalid:443/api///");
        ObjectNode snapshot = json.createObjectNode(); BillingLedgerOrigin.bind(snapshot, accepted);
        assertEquals("https://ledger.fixture.invalid/api",snapshot.path(BillingLedgerOrigin.FIELD).path("base_url").asText());
        assertTrue(BillingLedgerOrigin.matches(snapshot.toString(),"same-merchant",properties("https://ledger.fixture.invalid/api")));
        assertFalse(BillingLedgerOrigin.matches(snapshot.toString(),"same-merchant",properties("https://ledger.fixture.invalid:2443/api")));
        assertFalse(BillingLedgerOrigin.matches(snapshot.toString(),"same-merchant",properties("https://ledger.fixture.invalid/another-api")));
        assertFalse(BillingLedgerOrigin.matches(snapshot.toString(),"same-merchant",properties("https://other.fixture.invalid/api")));
    }

    @Test void missingMalformedOrOverflowingVersionsNeverBindHistoricalEvidence() {
        ExternalBillingProperties current = properties("https://ledger.fixture.invalid/api");
        assertFalse(BillingLedgerOrigin.matches("{}","same-merchant",current));
        assertFalse(BillingLedgerOrigin.matches("not-json","same-merchant",current));
        for(String version:new String[]{"4294967297","9223372036854775809","0","-1"}) {
            ObjectNode snapshot=json.createObjectNode();BillingLedgerOrigin.bind(snapshot,current);
            ((ObjectNode)snapshot.get(BillingLedgerOrigin.FIELD)).put("version",new BigInteger(version));
            assertFalse(BillingLedgerOrigin.matches(snapshot.toString(),"same-merchant",current),version);
        }
        ObjectNode snapshot=json.createObjectNode();BillingLedgerOrigin.bind(snapshot,current);
        ((ObjectNode)snapshot.get(BillingLedgerOrigin.FIELD)).put("version","1");
        assertFalse(BillingLedgerOrigin.matches(snapshot.toString(),"same-merchant",current));
    }

    @Test void existingOriginCannotBeOverwrittenAndNamespaceNeverUsesMerchantAlone() {
        ExternalBillingProperties dev=properties("https://dev.fixture.invalid/api"),prod=properties("https://prod.fixture.invalid/api");
        ObjectNode snapshot=json.createObjectNode();BillingLedgerOrigin.bind(snapshot,dev);String original=snapshot.toString();
        assertThrows(IllegalStateException.class,()->BillingLedgerOrigin.bind(snapshot,prod));
        assertEquals(original,snapshot.toString());
        assertEquals(64,BillingLedgerOrigin.namespace(dev).length());
        assertNotEquals(BillingLedgerOrigin.namespace(dev),BillingLedgerOrigin.namespace(prod));
        assertNotEquals("same-merchant",BillingLedgerOrigin.namespace(dev));
    }

    @Test void insecureCredentialBearingAndAmbiguousTargetsAreRejected() {
        for(String url:new String[]{"http://ledger.fixture.invalid","https://user@ledger.fixture.invalid","https://ledger.fixture.invalid/api?x=y",
            "https://ledger.fixture.invalid/api#fragment","https://ledger.fixture.invalid/a/../api","https://ledger.fixture.invalid/%61pi"})
            assertThrows(IllegalArgumentException.class,()->BillingLedgerOrigin.normalizedBaseUrl(url),url);
    }
}
