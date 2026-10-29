package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Container Pro only: https://www.autodl.com/docs/instance_pro_api/. No purchase/release API. */
@Component
public class AutoDlProPowerProvider implements ComputePowerProvider, ComputeCostProvider, ComputeInventoryProvider {
    static final String BASE = "https://api.autodl.com/api/v1/dev/instance/pro/";
    private final ComputeCredentialCipher cipher;
    private final boolean enabled;
    private final RestTemplate http;
    private final ObjectMapper json = new ObjectMapper();
    @Autowired(required = false)
    private ConfiguredComputeRegistry configured;
    @Autowired
    public AutoDlProPowerProvider(ComputeCredentialCipher cipher, @Value("${vlstream.compute.cloud-control.enabled:false}") boolean enabled) {
        this(cipher, enabled, transport());
    }
    AutoDlProPowerProvider(ComputeCredentialCipher cipher, boolean enabled, RestTemplate http) {
        this.cipher = cipher; this.enabled = enabled; this.http = http;
    }
    static RestTemplate transport() {
        // Pro documents GET with a JSON body; URLConnection would silently turn it into POST.
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(
            HttpClients.custom().disableAutomaticRetries().disableRedirectHandling().build()) {
            @Override protected org.apache.http.client.methods.HttpUriRequest createHttpUriRequest(HttpMethod method, URI uri) {
                if (method != HttpMethod.GET) return super.createHttpUriRequest(method, uri);
                HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase() { @Override public String getMethod() { return "GET"; } };
                request.setURI(uri); return request;
            }
        };
        factory.setConnectTimeout(5000); factory.setReadTimeout(10000); factory.setConnectionRequestTimeout(5000);
        return new RestTemplate(factory);
    }
    @Override public boolean supports(ComputeNode node) {
        return enabled && "API".equals(node.getControlMode()) && "autodl".equals(node.getProvider())
            && "AUTODL_PRO".equals(node.getProviderProduct()) && (node.isConfigurationManaged()
                ? configured!=null&&configured.providerTokenPresent(node) : node.isProviderCredentialConfigured())
            && node.getProviderInstanceId() != null && !node.getProviderInstanceId().isEmpty();
    }
    @Override public void submit(ComputeNode node, String action, String operationId) {
        if (!"start".equals(action) && !"stop".equals(action)) throw new IllegalArgumentException("Unsupported power action");
        Map<String,Object> body = body(node);
        if ("start".equals(action)) body.put("payload", "gpu");
        call(node, HttpMethod.POST, "start".equals(action) ? "power_on" : "power_off", body);
        // Success means accepted. This API has no documented client retry token.
    }
    @Override public ComputeInventoryProvider.Page inventoryPage(ComputeNode source,int page,int size) {
        JsonNode data = inventoryData(source, page, size);
        ComputeInventoryProvider.Page result=new ComputeInventoryProvider.Page();result.setIndex(page);result.setMaxPage(data.path("max_page").asInt());result.setTotal(data.path("result_total").asInt());
        java.util.List<ComputeInventoryProvider.Item> items=new java.util.ArrayList<>();
        for(JsonNode raw:data.path("list")) {
            ComputeInventoryProvider.Item item=new ComputeInventoryProvider.Item();item.setInstanceId(raw.path("uuid").asText());
            // Omit arbitrary supplier names/labels: identity/specification alone is sufficient for discovery.
            item.setName(item.getInstanceId());item.setGpuSpec(safeIdentifier(raw.path("gpu_spec_uuid").asText()));item.setGpuAmount(raw.path("req_gpu_amount").asInt());
            item.setPowerState("running".equals(raw.path("status").asText())?"RUNNING":"UNKNOWN");items.add(item);
        }
        result.setItems(items);return result;
    }
    private JsonNode inventoryData(ComputeNode source,int page,int size) {
        if(page<1 || page>50 || size<1 || size>20)throw new IllegalArgumentException("Invalid inventory pagination");
        Map<String,Object> body=new LinkedHashMap<>();body.put("page_index",page);body.put("page_size",size);
        JsonNode data=call(source,HttpMethod.POST,"list",body);
        if(!data.isObject() || !data.path("list").isArray() || !data.path("page_index").isIntegralNumber()
            || !data.path("max_page").isIntegralNumber() || !data.path("result_total").isIntegralNumber()
            || data.path("page_index").asInt()!=page || data.path("max_page").asInt()<0 || data.path("max_page").asInt()>50
            || !data.path("page_index").canConvertToInt() || !data.path("max_page").canConvertToInt() || !data.path("result_total").canConvertToInt()
            || data.path("result_total").asInt()<0 || data.path("result_total").asInt()>1000 || data.path("list").size()>size)
            throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN);
        for(JsonNode raw:data.path("list")) {
            if(!raw.path("uuid").isTextual() || !raw.path("uuid").asText().matches("[A-Za-z0-9_-]{1,100}")
                || !raw.path("req_gpu_amount").isIntegralNumber() || !raw.path("req_gpu_amount").canConvertToInt() || raw.path("req_gpu_amount").asInt()<0 || raw.path("req_gpu_amount").asInt()>16)
                throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN);
        }
        return data;
    }
    private static String safeIdentifier(String value) { return value!=null && value.matches("[A-Za-z0-9_-]{1,100}")?value:null; }
    @Override public Snapshot status(ComputeNode node) {
        JsonNode state = call(node, HttpMethod.GET, "status", body(node));
        // The documented list owns charge_type/req_gpu_amount. Snapshot is not their
        // contract and contains unrelated SSH/Jupyter secrets; do not fetch it here.
        JsonNode page = inventoryData(node, 1, 20);
        JsonNode metadata = null;
        for (JsonNode item : page.path("list")) if (node.getProviderInstanceId().equals(item.path("uuid").asText())) {
            if (metadata != null) throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN);
            metadata = item;
        }
        // Pro's own public console state map (pay-type.f4f44ce2.js) explicitly
        // labels shutdown as 已关机. Other error/migration states remain unconfirmed.
        String raw=state.isTextual()?state.asText():"";
        String power="running".equals(raw)?"RUNNING":"starting".equals(raw)?"STARTING":"shutting_down".equals(raw)?"STOPPING":"UNKNOWN";
        boolean matched=metadata!=null&&raw.equals(metadata.path("status").asText());
        if(matched&&"shutdown".equals(raw))power="STOPPED";
        Snapshot result = new Snapshot(power, "UNKNOWN", "RUNNING".equals(power)
            ? "AutoDL Pro 已确认运行；GPU 分配和环境仍需独立检查" : "STOPPED".equals(power)
            ? "AutoDL Pro 已确认关机，保留磁盘仍可能产生供应商费用" : "AutoDL Pro 尚未确认运行或关机终态");
        // One bounded page only: missing identity or disagreement cannot become proof.
        // Only the Pro console's shutdown literal is accepted; stopped_at alone
        // cannot prove that a new start failed or that supplier storage is free.
        if (matched) {
            result.gpuAmount = metadata.path("req_gpu_amount").asInt();
            if (metadata.path("charge_type").isTextual()) result.chargeType = safeLabel(metadata.path("charge_type").asText());
            if("STOPPED".equals(power))result.stoppedMode="AUTODL_PRO_PHYSICAL_STOPPED";
            result.providerStartedAt=eventTime(metadata.path("started_at"));
            result.providerStoppedAt=eventTime(metadata.path("stopped_at"));
        }
        return result;
    }
    private static java.time.Instant eventTime(JsonNode field) {
        if(!field.path("Valid").isBoolean()||!field.path("Valid").asBoolean()||!field.path("Time").isTextual())return null;
        try {java.time.Instant time=java.time.OffsetDateTime.parse(field.path("Time").asText()).toInstant();return time.isAfter(java.time.Instant.now())?null:time;}
        catch(RuntimeException invalid){return null;}
    }
    @Override public ComputeCostProvider.Observation cost(ComputeNode node) {
        JsonNode snapshot=call(node,HttpMethod.GET,"snapshot",body(node));
        ComputeCostProvider.Observation result=new ComputeCostProvider.Observation(); result.setSource("AUTODL_PRO_SNAPSHOT"); result.setPriceKind("RAW_PROVIDER_PRICE");
        result.setCurrency("UNKNOWN"); result.setAmountUnit("UNKNOWN"); result.setResourceScope("UNKNOWN");
        if (snapshot.path("payg_price").isNumber()) result.setAmount(snapshot.path("payg_price").decimalValue());
        if (snapshot.path("origin_pay_price").isNumber()) result.setOriginalAmount(snapshot.path("origin_pay_price").decimalValue());
        result.setMessage("价格原值已读取；缩放单位、币种及单卡/整实例口径尚未确认，不能作为收费成本，不乘卡数或猜除数"); return result;
    }
    private Map<String,Object> body(ComputeNode node) { Map<String,Object> body = new LinkedHashMap<>(); body.put("instance_uuid", node.getProviderInstanceId()); return body; }
    private JsonNode call(ComputeNode node, HttpMethod method, String path, Map<String,Object> body) {
        if (!supports(node)) throw new ComputeProviderException(ComputeProviderException.Kind.AUTHORIZATION);
        try {
            String token = node.isConfigurationManaged() ? configured.providerToken(node)
                : json.readTree(cipher.decrypt(node.getProviderCredentialCipher())).path("token").asText();
            if (token.isEmpty() || token.contains("\r") || token.contains("\n")) throw new ComputeProviderException(ComputeProviderException.Kind.AUTHORIZATION);
            HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON); headers.set("Authorization", token);
            String response = http.exchange(BASE + path, method, new HttpEntity<>(json.writeValueAsString(body), headers), String.class).getBody();
            if (response == null || response.length() > 1024 * 1024) throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN);
            JsonNode envelope = json.readTree(response);
            if (!"Success".equals(envelope.path("code").asText())) throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN);
            return envelope.path("data");
        } catch (ComputeProviderException e) { throw e; }
        catch (HttpStatusCodeException e) {
            throw new ComputeProviderException(e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN
                ? ComputeProviderException.Kind.AUTHORIZATION : ComputeProviderException.Kind.UNKNOWN);
        } catch (Exception e) { throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN); }
    }
    static String safeLabel(String value) { return value != null && value.matches("[a-zA-Z0-9_-]{1,30}") ? value : null; }
}
