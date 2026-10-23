package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class FundingContractsTest {
    private final ObjectMapper json = new ObjectMapper();
    private static final String MAX = "9223372036854775807";
    private static final String ABOVE_DOUBLE_PRECISION = "9007199254740993";

    @Test void holdHasOnlyDocumentedFieldsAndPreservesExactNumbersAndOriginalExpiry() {
        ObjectNode body = hold(ABOVE_DOUBLE_PRECISION, MAX, "1700000000001");
        assertEquals(fields("merchant_id", "merchant_hold_id", "user_id", "hold_quota", "expires_at", "reason"), fieldNames(body));
        assertEquals("merchant-test", body.path("merchant_id").textValue());
        assertEquals("vls:session-test:hold:0", body.path("merchant_hold_id").textValue());
        assertTrue(body.path("user_id").isIntegralNumber());
        assertEquals(new BigInteger(ABOVE_DOUBLE_PRECISION), body.path("user_id").bigIntegerValue());
        assertTrue(body.path("hold_quota").isTextual());
        assertEquals(MAX, body.path("hold_quota").textValue());
        assertTrue(body.path("expires_at").isTextual());
        assertEquals("1700000000001", body.path("expires_at").textValue());
        assertEquals("GPU usage reserve", body.path("reason").textValue());
        assertEquals(body, hold(ABOVE_DOUBLE_PRECISION, MAX, "1700000000001"));
    }

    @Test void releaseHasOnlyItsThreeDocumentedFieldsAndExactPayer() {
        ObjectNode body = FundingContracts.release("merchant-test", MAX, ABOVE_DOUBLE_PRECISION);
        assertEquals(fields("merchant_id", "user_id", "release_quota"), fieldNames(body));
        assertEquals(new BigInteger(MAX), body.path("user_id").bigIntegerValue());
        assertTrue(body.path("release_quota").isTextual());
        assertEquals(ABOVE_DOUBLE_PRECISION, body.path("release_quota").textValue());
        assertFalse(body.has("hold_id"));
    }

    @Test void numericOrTextHoldIdsRemainExactBeyondDoublePrecisionAndAtInt64Limit() throws Exception {
        for (String id : new String[]{ABOVE_DOUBLE_PRECISION, MAX}) {
            for (String representation : new String[]{id, "\"" + id + "\""}) {
                FundingContracts.Receipt receipt = FundingContracts.holdReceipt(response("{\"hold_id\":" + representation
                    + ",\"available_quota\":\"" + MAX + "\",\"held_quota\":\"0\"}"));
                assertEquals(id, receipt.holdId);
                assertEquals(MAX, receipt.availableQuota);
                assertEquals("0", receipt.heldQuota);
            }
        }
    }

    @Test void releaseOnlyRequiresHeldQuotaAndDoesNotInventOtherBalancesOrState() throws Exception {
        FundingContracts.Receipt minimal = FundingContracts.releaseReceipt(response("{\"held_quota\":\"0\"}"));
        assertNull(minimal.holdId);
        assertNull(minimal.availableQuota);
        assertEquals("0", minimal.heldQuota);
        FundingContracts.Receipt explicitNull = FundingContracts.releaseReceipt(response("{\"hold_id\":null,\"available_quota\":null,\"held_quota\":\"1\"}"));
        assertNull(explicitNull.holdId);
        assertNull(explicitNull.availableQuota);
        FundingContracts.Receipt complete = FundingContracts.releaseReceipt(response("{\"hold_id\":" + MAX
            + ",\"available_quota\":\"0\",\"held_quota\":\"" + ABOVE_DOUBLE_PRECISION + "\"}"));
        assertEquals(MAX, complete.holdId);
        assertEquals("0", complete.availableQuota);
        assertEquals(ABOVE_DOUBLE_PRECISION, complete.heldQuota);
    }

    @Test void requestsRejectZeroNoncanonicalAndOverflowAmountsPayersAndExpiry() {
        for (String invalid : new String[]{null, "", "0", "01", "+1", "-1", "1.0", "1e3", " 1", "1 ", "9223372036854775808"}) {
            assertThrows(IllegalArgumentException.class, () -> hold(invalid, "1", "1"));
            assertThrows(IllegalArgumentException.class, () -> hold("1", invalid, "1"));
            assertThrows(IllegalArgumentException.class, () -> hold("1", "1", invalid));
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.release("merchant-test", "1", invalid));
        }
        assertEquals(MAX, hold(MAX, MAX, MAX).path("expires_at").textValue());
    }

    @Test void receiptsRejectFloatingExponentOverflowAndNoncanonicalHoldIds() throws Exception {
        for (String invalid : new String[]{"null", "true", "1.0", "1e3", "9007199254740993.0", "9223372036854775808", "0", "-1",
            "\"01\"", "\"1e3\"", "\"9223372036854775808\"", "{}", "[]"}) {
            JsonNode response = response("{\"hold_id\":" + invalid + ",\"available_quota\":\"0\",\"held_quota\":\"1\"}");
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.holdReceipt(response));
            if (!"null".equals(invalid)) assertThrows(IllegalArgumentException.class, () -> FundingContracts.releaseReceipt(response));
        }
    }

    @Test void receiptsRequireNonnegativeCanonicalStringQuotasEvenWhenFieldsAreOptional() throws Exception {
        for (String invalid : new String[]{"null", "0", "1", "1.0", "1e3", "true", "\"-1\"", "\"01\"", "\"+1\"", "\"1.0\"", "\"1e3\"", "\"9223372036854775808\""}) {
            JsonNode available = response("{\"hold_id\":1,\"available_quota\":" + invalid + ",\"held_quota\":\"0\"}");
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.holdReceipt(available));
            if (!"null".equals(invalid)) assertThrows(IllegalArgumentException.class, () -> FundingContracts.releaseReceipt(available));
            JsonNode held = response("{\"hold_id\":1,\"available_quota\":\"0\",\"held_quota\":" + invalid + "}");
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.holdReceipt(held));
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.releaseReceipt(held));
        }
    }

    @Test void receiptsRequireBooleanSuccessAndObjectDataWithRequiredFields() throws Exception {
        for (String invalid : new String[]{"null", "{}", "[]", "{\"success\":false,\"data\":{}}", "{\"success\":\"true\",\"data\":{}}",
            "{\"success\":1,\"data\":{}}", "{\"success\":true}", "{\"success\":true,\"data\":null}", "{\"success\":true,\"data\":[]}", "{\"success\":true,\"data\":{}}"}) {
            JsonNode response = json.readTree(invalid);
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.holdReceipt(response));
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.releaseReceipt(response));
        }
        assertThrows(IllegalArgumentException.class, () -> FundingContracts.holdReceipt(null));
        assertThrows(IllegalArgumentException.class, () -> FundingContracts.releaseReceipt(null));
        assertThrows(IllegalArgumentException.class, () -> FundingContracts.holdReceipt(response("{\"held_quota\":\"0\"}")));
        assertThrows(IllegalArgumentException.class, () -> FundingContracts.holdReceipt(response("{\"hold_id\":1,\"held_quota\":\"0\"}")));
    }

    @Test void merchantAndBusinessReferencesUseBoundedSafeAsciiAndReasonHasNoControls() {
        for (String invalid : new String[]{null, "", " ", "merchant/name", "merchant?name", "merchant\\name", "merchant\nname", "商户", repeat('a', 129)}) {
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.hold(invalid, "hold-test", "1", "1", "1", "reason"));
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.hold("merchant-test", invalid, "1", "1", "1", "reason"));
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.release(invalid, "1", "1"));
        }
        assertEquals(repeat('a', 128), FundingContracts.hold(repeat('a', 128), repeat('b', 128), "1", "1", "1", repeat('r', 255)).path("merchant_id").textValue());
        assertEquals("GPU 计费预留", FundingContracts.hold("merchant-test", "hold-test", "1", "1", "1", "GPU 计费预留").path("reason").textValue());
        for (String invalid : new String[]{null, "", " \t", "reason\n", "reason\r", "reason\u0000", "reason\u007f", repeat('r', 256)})
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.hold("merchant-test", "hold-test", "1", "1", "1", invalid));
    }

    @Test void newReleaseHasBusinessReferenceWhileLegacyBodyIsUnchanged() {
        ObjectNode body = FundingContracts.release("merchant-test", "release-final", MAX, "11");
        assertEquals(fields("merchant_id", "merchant_release_id", "user_id", "release_quota"), fieldNames(body));
        assertEquals("release-final", body.path("merchant_release_id").textValue());
        assertEquals(new BigInteger(MAX), body.path("user_id").bigIntegerValue());
        assertEquals(3, FundingContracts.release("merchant-test", MAX, "11").size());
        assertThrows(IllegalArgumentException.class, () -> FundingContracts.release("merchant-test", "bad/ref", "1", "1"));
    }

    @Test void topupCannotExtendExpiryAndRequiresItsOwnBusinessReference() {
        ObjectNode body = FundingContracts.topup("merchant-test", "topup-1", MAX, ABOVE_DOUBLE_PRECISION, "reserve");
        assertEquals(fields("merchant_id", "merchant_topup_id", "user_id", "topup_quota", "reason"), fieldNames(body));
        assertFalse(body.has("expires_at"));
        assertEquals(ABOVE_DOUBLE_PRECISION, body.path("topup_quota").textValue());
        assertThrows(IllegalArgumentException.class, () -> FundingContracts.topup("merchant-test", "topup-1", "1", "0", "reserve"));
    }

    @Test void holdDetailsDistinguishMillisecondsSecondsAndValidateExactArithmetic() throws Exception {
        JsonNode data = json.readTree("{\"hold_id\":9007199254740993,\"merchant_hold_id\":\"hold-1\",\"user_id\":123,"
            + "\"hold_quota\":\"20\",\"captured_quota\":\"7\",\"released_quota\":\"2\",\"remaining_quota\":\"11\","
            + "\"hold_status\":\"PARTIALLY_CAPTURED\",\"expires_at\":1700000000001,\"balance_version\":8,\"created_at\":1700000000,\"updated_at\":1700000010}");
        FundingContracts.HoldDetail detail = FundingContracts.holdDetail(data);
        assertEquals(ABOVE_DOUBLE_PRECISION, detail.holdId);
        assertEquals("1700000000001", detail.expiresAt);
        assertEquals("1700000000", detail.createdAt);
        assertEquals("11", detail.remainingQuota);
        assertEquals("123", detail.userId);
        ((ObjectNode) data).put("remaining_quota", "12");
        assertThrows(IllegalArgumentException.class, () -> FundingContracts.holdDetail(data));
        ((ObjectNode) data).put("remaining_quota", "11");
        ((ObjectNode) data).put("hold_quota", 20);
        assertThrows(IllegalArgumentException.class, () -> FundingContracts.holdDetail(data));
    }

    @Test void newReleaseActualAmountIsNotCumulativeAndDoesNotRequireTopupFields() throws Exception {
        FundingContracts.ReleaseReceipt receipt = FundingContracts.releaseResult(json.readTree("{\"hold_id\":101,\"held_quota\":\"4\","
            + "\"released_quota\":\"6\",\"remaining_quota\":\"0\",\"hold_status\":\"RELEASED\"}"));
        assertEquals("6", receipt.releasedQuota); assertEquals("4", receipt.heldQuota); assertEquals("0", receipt.remainingQuota);
        assertThrows(IllegalArgumentException.class, () -> FundingContracts.topupReceipt(json.readTree("{\"hold_id\":101,\"held_quota\":\"4\","
            + "\"released_quota\":\"6\",\"remaining_quota\":\"0\",\"hold_status\":\"RELEASED\"}")));
    }

    @Test void newReceiptsAcceptOnlyBooleanTrueEnvelopesOrDocumentedDirectObjects() throws Exception {
        String data = "{\"hold_id\":101,\"held_quota\":\"0\",\"released_quota\":\"0\",\"remaining_quota\":\"0\",\"hold_status\":\"EXPIRED\"}";
        assertEquals("EXPIRED", FundingContracts.releaseResult(response(data)).holdStatus);
        for (String success : new String[]{"false", "\"true\"", "1", "null"})
            assertThrows(IllegalArgumentException.class, () -> FundingContracts.releaseResult(json.readTree("{\"success\":" + success + ",\"data\":" + data + "}")));
        assertThrows(IllegalArgumentException.class, () -> FundingContracts.releaseResult(json.readTree("{\"success\":true,\"error\":\"BAD_STATE\",\"data\":" + data + "}")));
        assertThrows(IllegalArgumentException.class, () -> FundingContracts.releaseResult(json.readTree(data.replace("EXPIRED", "NEW_STATUS"))));
    }

    private ObjectNode hold(String userId, String amount, String expiresAt) {
        return FundingContracts.hold("merchant-test", "vls:session-test:hold:0", userId, amount, expiresAt, "GPU usage reserve");
    }

    private JsonNode response(String data) throws Exception {
        return json.readTree("{\"success\":true,\"data\":" + data + "}");
    }

    private static Set<String> fields(String... names) { return new HashSet<>(Arrays.asList(names)); }

    private static Set<String> fieldNames(JsonNode value) {
        Set<String> names = new HashSet<>();
        value.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static String repeat(char value, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, value);
        return new String(chars);
    }
}
