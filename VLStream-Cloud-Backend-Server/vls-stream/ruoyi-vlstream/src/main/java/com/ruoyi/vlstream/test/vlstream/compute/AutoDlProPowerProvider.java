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
    @Value("${vlstream.compute.official-autodl-control.enabled:#{null}}")
    private Boolean officialControl;
    @Autowired(required = false)
    private com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties billing;
    @Autowired(required = false)
    private ComputeBillingPolicy billingPolicy;
    @Value("${vlstream.compute.official-autodl-control.instance-id:}")
    private String officialControlInstanceId;
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
        boolean unified=billing!=null&&billing.getMode()!=null;
        boolean officialEnabled=unified?billing.getMode()==com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties.Mode.PAID:Boolean.TRUE.equals(officialControl);
        boolean scoped=officialEnabled&&node.isConfigurationManaged()&&Long.valueOf(-6000L).equals(node.getId())
            &&"official-autodl".equals(node.getConfigurationCode())&&officialControlInstanceId!=null
            &&officialControlInstanceId.matches("pro-[a-f0-9]{12}")&&officialControlInstanceId.equals(node.getProviderInstanceId());
        if(unified&&officialEnabled&&configured!=null&&configured.official(node)&&Long.valueOf(-6000L).equals(node.getId())&&"official-autodl".equals(node.getConfigurationCode())
            &&node.getProviderInstanceId()!=null&&node.getProviderInstanceId().matches("pro-[a-f0-9]{12}"))scoped=true;
        if(node.isConfigurationManaged()&&unified&&!scoped)return false;
        if(!unified&&Boolean.TRUE.equals(officialControl)&&!scoped)return false;
        return (enabled||scoped) && "API".equals(node.getControlMode()) && "autodl".equals(node.getProvider())
            && "AUTODL_PRO".equals(node.getProviderProduct()) && (node.isConfigurationManaged()
                ? configured!=null&&configured.providerTokenPresent(node) : node.isProviderCredentialConfigured())
            && node.getProviderInstanceId() != null && !node.getProviderInstanceId().isEmpty();
    }
    @javax.annotation.PostConstruct
    public void validateOfficialMode() {
        if(billing!=null&&billing.getMode()!=null&&officialControl!=null
            &&officialControl!=(billing.getMode()==com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties.Mode.PAID))
            throw new IllegalStateException("external-billing.mode 与旧 official-autodl-control.enabled 冲突，请移除旧开关");
        if(billing!=null&&billing.getMode()==com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties.Mode.PAID&&billingPolicy!=null) {
            int budget=billingPolicy.checkedStopBudgetSeconds(billing.getVerifiedProviderStopSeconds());
            if(billing.getHoldStopLeadSeconds()<budget||billing.getSettlementIntervalSeconds()<1)
                throw new IllegalStateException("PAID 模式停机提前量未覆盖 billing-policy.stop-budget-seconds，或结算周期无效");
        }
        if(billing!=null&&billing.getMode()==com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties.Mode.PAID&&configured!=null) {
            for(ComputeNode node:configured.published())if(configured.official(node)) {
                if(!Long.valueOf(-6000L).equals(node.getId())||!"official-autodl".equals(node.getConfigurationCode())
                    ||!configured.providerTokenPresent(node)||!"VLS_FIXED".equals(node.getPricePolicy())||node.getUserHourlyPrice()==null)
                    throw new IllegalStateException("PAID 模式的官方 AutoDL 资源缺少匹配的实例控制身份或固定价格，请检查 company-resources 配置");
                if(officialControlInstanceId!=null&&!officialControlInstanceId.isEmpty()&&!officialControlInstanceId.equals(node.getProviderInstanceId()))
                    throw new IllegalStateException("旧 official-autodl-control.instance-id 与当前官方资源不一致，请移除旧配置并核对实例");
            }
        }
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
    /** Explicit administrative read, independent of paid power admission. No inventory,
     * snapshot, boot or stop is reachable from this method. */
    Snapshot reconciliationStatus(ComputeNode node) {
        if(configured==null || !Long.valueOf(-6000L).equals(node.getId()) || !"official-autodl".equals(node.getConfigurationCode())
            || node.getProviderInstanceId()==null || !node.getProviderInstanceId().matches("pro-[a-f0-9]{12}"))
            throw new ComputeProviderException(ComputeProviderException.Kind.AUTHORIZATION);
        configured.verifyOfficial(node);
        return configuredStatus(call(node,HttpMethod.GET,"status",body(node),true));
    }
    private Snapshot configuredStatus(JsonNode state) {
        String raw=state.isTextual()?state.asText():"";
        String power="running".equals(raw)?"RUNNING":"shutdown".equals(raw)?"STOPPED":"starting".equals(raw)?"STARTING":"shutting_down".equals(raw)?"STOPPING":"UNKNOWN";
        Snapshot direct=new Snapshot(power,"UNKNOWN","AutoDL Pro 指定实例状态；磁盘保留费用独立");
        if("STOPPED".equals(power))direct.stoppedMode="AUTODL_PRO_PHYSICAL_STOPPED";
        return direct;
    }
    @Override public Snapshot status(ComputeNode node) {
        JsonNode state = call(node, HttpMethod.GET, "status", body(node));
        // Real Pro GET endpoints bind instance_uuid from the query, despite the
        // public guide's request-body example. Exact target status needs no inventory enumeration.
        if(node.isConfigurationManaged()) {
            return configuredStatus(state);
        }
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
    /** Secrets in this response must remain in the bounded runtime SSH context only. */
    JsonNode connectionSnapshot(ComputeNode node) { return call(node,HttpMethod.GET,"snapshot",body(node)); }
    private Map<String,Object> body(ComputeNode node) { Map<String,Object> body = new LinkedHashMap<>(); body.put("instance_uuid", node.getProviderInstanceId()); return body; }
    private JsonNode call(ComputeNode node, HttpMethod method, String path, Map<String,Object> body) {
        return call(node,method,path,body,false);
    }
    private JsonNode call(ComputeNode node, HttpMethod method, String path, Map<String,Object> body, boolean reconciliation) {
        if(reconciliation) {
            if(method!=HttpMethod.GET || !"status".equals(path) || configured==null)throw new ComputeProviderException(ComputeProviderException.Kind.AUTHORIZATION);
            configured.verifyOfficial(node);
        } else if (!supports(node)) throw new ComputeProviderException(ComputeProviderException.Kind.AUTHORIZATION);
        try {
            String token = node.isConfigurationManaged() ? configured.providerToken(node)
                : json.readTree(cipher.decrypt(node.getProviderCredentialCipher())).path("token").asText();
            if (token.isEmpty() || token.contains("\r") || token.contains("\n")) throw new ComputeProviderException(ComputeProviderException.Kind.AUTHORIZATION);
            HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON); headers.set("Authorization", token);
            String url=BASE+path;
            String requestBody=json.writeValueAsString(body);
            if(method==HttpMethod.GET){url+="?instance_uuid="+java.net.URLEncoder.encode(node.getProviderInstanceId(),"UTF-8");requestBody=null;}
            ResponseEntity<String> reply = http.exchange(url, method, new HttpEntity<>(requestBody, headers), String.class);
            String response = reply.getBody();
            if (response == null || response.length() > 1024 * 1024) throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN, reply.getStatusCodeValue(), "EMPTY_RESPONSE");
            JsonNode envelope = json.readTree(response);
            if (!"Success".equals(envelope.path("code").asText())) {
                // No raw vendor code/message/body reaches logs: these responses may
                // contain credentials. Only the documented, bounded request_id can be
                // retained for tracing. Generic BadRequest is not a no-power proof.
                String code=envelope.path("code").asText();
                String category="BadRequest".equals(code)?"BUSINESS_BAD_REQUEST":
                    ("Unauthorized".equals(code)||"Forbidden".equals(code))?"BUSINESS_AUTHORIZATION":
                    "NotFound".equals(code)?"BUSINESS_NOT_FOUND":"BUSINESS_OTHER";
                throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN,reply.getStatusCodeValue(),category,safeRequestId(envelope,token));
            }
            return envelope.path("data");
        } catch (ComputeProviderException e) { throw e; }
        catch (HttpStatusCodeException e) {
            throw new ComputeProviderException(e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN
                ? ComputeProviderException.Kind.AUTHORIZATION : ComputeProviderException.Kind.UNKNOWN,e.getRawStatusCode(),"HTTP_ERROR");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN,null,"INVALID_RESPONSE");
        } catch (Exception e) { throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN,null,transportDiagnostic(e)); }
    }
    private static String safeRequestId(JsonNode envelope,String token) {
        JsonNode field=envelope.path("request_id");
        if(!field.isTextual())return null;
        String value=field.textValue();
        // A malformed response must not turn an echoed authorization token into a trace ID.
        if(!value.matches("[a-fA-F0-9]{16,64}"))return null;
        String lower=value.toLowerCase(java.util.Locale.ROOT),credential=token.toLowerCase(java.util.Locale.ROOT);
        return credential.contains(lower) || lower.contains(credential) ? null : value;
    }
    private static String transportDiagnostic(Throwable failure) {
        for(Throwable e=failure;e!=null;e=e.getCause()) {
            if(e instanceof java.net.SocketTimeoutException||e instanceof org.apache.http.conn.ConnectTimeoutException)return "TIMEOUT";
            if(e instanceof java.net.UnknownHostException)return "DNS";
            if(e instanceof javax.net.ssl.SSLException)return "TLS";
        }
        return "TRANSPORT";
    }
    static String safeLabel(String value) { return value != null && value.matches("[a-zA-Z0-9_-]{1,30}") ? value : null; }
}
