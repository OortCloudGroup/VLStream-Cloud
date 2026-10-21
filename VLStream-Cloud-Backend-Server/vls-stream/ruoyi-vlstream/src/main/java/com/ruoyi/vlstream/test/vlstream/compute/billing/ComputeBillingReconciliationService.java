package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Fixed-window, durable-cursor external ledger import. Never repairs a wallet or issues refunds. */
@Service
public class ComputeBillingReconciliationService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ExternalBillingProperties properties;
    private final ObjectProvider<ExternalBillingGateway> gateways;
    private final ObjectMapper json;
    public ComputeBillingReconciliationService(JdbcTemplate jdbc, PlatformTransactionManager manager, ExternalBillingProperties properties,
                                               ObjectProvider<ExternalBillingGateway> gateways, ObjectMapper json) {
        this.jdbc = jdbc; this.transaction = new TransactionTemplate(manager); this.properties = properties; this.gateways = gateways; this.json = json;
    }
    public void tick() {
        if (!properties.isEnabled()) return;
        ExternalBillingGateway gateway = gateways.getIfAvailable(); String merchant = properties.getMerchantId();
        if (gateway == null || merchant == null) return;
        String lease = UUID.randomUUID().toString();
        Map<String, Object> page = transaction.execute(tx -> {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM vls_compute_billing_reconciliation WHERE merchant_id=? FOR UPDATE", merchant);
            long now = System.currentTimeMillis() / 1000;
            if (rows.isEmpty()) {
                Long from = jdbc.queryForObject("SELECT MIN(s.interval_start_ms) FROM vls_compute_billing_slice s JOIN vls_compute_billing_session u ON u.usage_id=s.usage_id WHERE u.merchant_id=?", Long.class, merchant);
                if (from == null || from / 1000 >= now) return null;
                long begin = from / 1000;
                jdbc.update("INSERT IGNORE INTO vls_compute_billing_reconciliation(merchant_id,from_second,to_second,state) VALUES(?,?,?,'PENDING')", merchant, begin, Math.min(begin + 86400, now));
            }
            Map<String, Object> row = jdbc.queryForMap("SELECT * FROM vls_compute_billing_reconciliation WHERE merchant_id=? FOR UPDATE", merchant);
            if ("CONFLICT".equals(row.get("state")) || number(row,"lease_until_ms") > System.currentTimeMillis() || number(row,"next_attempt_ms") > System.currentTimeMillis()) return null;
            if ("DONE".equals(row.get("state"))) {
                long begin = number(row,"to_second"); if (begin >= now) return null;
                jdbc.update("UPDATE vls_compute_billing_reconciliation SET from_second=?,to_second=?,page_cursor=NULL,state='PENDING' WHERE merchant_id=?", begin, Math.min(begin + 86400, now), merchant);
                row = jdbc.queryForMap("SELECT * FROM vls_compute_billing_reconciliation WHERE merchant_id=?", merchant);
            }
            jdbc.update("UPDATE vls_compute_billing_reconciliation SET lease_token=?,lease_until_ms=? WHERE merchant_id=?", lease, System.currentTimeMillis()+30000, merchant);
            return row;
        });
        if (page == null) return;
        try {
            ExternalBillingGateway.Result result = gateway.ledger(number(page,"from_second"), number(page,"to_second"), (String) page.get("page_cursor"));
            if (result.status != ExternalBillingGateway.Status.SUCCEEDED || result.charge == null) { fail(merchant, lease, "LEDGER_UNAVAILABLE", false); return; }
            JsonNode response = result.charge;
            if (!response.path("entries").isArray() || !response.path("has_more").isBoolean()) throw new IllegalArgumentException("Invalid ledger page");
            boolean more = response.path("has_more").asBoolean();
            String next = response.path("next_cursor").asText();
            if (more && (next.isEmpty() || next.length()>200 || next.equals(page.get("page_cursor")))) throw new IllegalArgumentException("Non-progressing ledger cursor");
            transaction.execute(tx -> {
                Map<String,Object> current=jdbc.queryForMap("SELECT * FROM vls_compute_billing_reconciliation WHERE merchant_id=? FOR UPDATE",merchant);
                if (!lease.equals(current.get("lease_token"))) return null;
                for (JsonNode entry : response.path("entries")) importEntry(merchant, entry);
                jdbc.update("UPDATE vls_compute_billing_reconciliation SET page_cursor=?,state=?,error_code=NULL,lease_token=NULL,lease_until_ms=0,next_attempt_ms=0 WHERE merchant_id=? AND lease_token=?", more?next:null,more?"PENDING":"DONE",merchant,lease);
                return null;
            });
        } catch (IllegalArgumentException e) { fail(merchant,lease,"LEDGER_CONTRACT_CONFLICT",true); }
        catch (Exception e) { fail(merchant,lease,"LEDGER_UNAVAILABLE",false); }
    }
    private void importEntry(String merchant, JsonNode entry) {
        String id=text(entry,"ledger_entry_id",100), ref=text(entry,"merchant_ref",120), type=text(entry,"entry_type",20);
        String delta=BillingSnapshot.required(entry,"quota_delta");
        boolean negative=delta.startsWith("-"); String absolute=negative?delta.substring(1):delta;
        ExactQuota.integer(absolute,true);
        if (!("CHARGE".equals(type)&&negative) && !("REFUND".equals(type)&&!negative)) throw new IllegalArgumentException("Invalid ledger amount/type");
        ObjectNode canonical=json.createObjectNode();
        for (String field : new String[]{"ledger_entry_id","merchant_ref","entry_type","quota_delta","user_id","reference_id","related_entry_id","balance_before","balance_after","created_at"}) canonical.put(field,entry.path(field).asText());
        String hash=ComputeBillingSettlementService.hash(canonical.toString());
        List<Map<String,Object>> old=jdbc.queryForList("SELECT entry_hash FROM vls_compute_billing_ledger_seen WHERE merchant_id=? AND ledger_entry_id=?",merchant,id);
        if (!old.isEmpty()) {if(!hash.equals(old.get(0).get("entry_hash")))throw new IllegalArgumentException("Immutable ledger changed");return;}
        String state="UNMATCHED";
        List<Map<String,Object>> charges=jdbc.queryForList("SELECT s.*,u.payer_user_id FROM vls_compute_billing_slice s JOIN vls_compute_billing_session u ON u.usage_id=s.usage_id WHERE s.merchant_charge_id=? AND u.merchant_id=?",ref,merchant);
        if ("REFUND".equals(type)) state="REFUND_REVIEW";
        else if (!charges.isEmpty()) {
            Map<String,Object> charge=charges.get(0);
            if (absolute.equals(charge.get("amount_text")) && entry.path("user_id").asText().equals(charge.get("payer_user_id")))
                state="SETTLED".equals(charge.get("state"))&&id.equals(charge.get("ledger_entry_id"))?"MATCHED":"AWAITING_CHARGE_QUERY";
        }
        jdbc.update("INSERT INTO vls_compute_billing_ledger_seen(merchant_id,ledger_entry_id,entry_hash,merchant_ref,entry_type,quota_delta,match_state) VALUES(?,?,?,?,?,?,?)",merchant,id,hash,ref,type,delta,state);
    }
    private void fail(String merchant,String lease,String error,boolean conflict) {
        jdbc.update("UPDATE vls_compute_billing_reconciliation SET state=?,error_code=?,lease_token=NULL,lease_until_ms=0,next_attempt_ms=? WHERE merchant_id=? AND lease_token=?",conflict?"CONFLICT":"RETRY",error,System.currentTimeMillis()+30000,merchant,lease);
    }
    private static long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private static String text(JsonNode entry,String field,int max){String value=entry.path(field).asText();if(value.isEmpty()||value.length()>max)throw new IllegalArgumentException("Invalid ledger identifier");return value;}
}
