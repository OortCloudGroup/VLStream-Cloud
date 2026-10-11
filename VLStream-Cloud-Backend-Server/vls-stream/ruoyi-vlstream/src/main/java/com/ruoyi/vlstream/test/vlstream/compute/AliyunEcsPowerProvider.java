package com.ruoyi.vlstream.test.vlstream.compute;

import com.aliyuncs.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.*;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Purchased ECS/EGS only. Official SDK signs RPC; no creation, purchase, release or sale-stock calls. */
@Component
public class AliyunEcsPowerProvider implements ComputePowerProvider, ComputeCostProvider {
    interface ClientFactory { IAcsClient create(String region, String accessKeyId, String accessKeySecret); }
    private final ComputeCredentialCipher cipher;
    private final boolean enabled;
    private final ClientFactory clients;
    private final ObjectMapper json = new ObjectMapper();
    @Autowired
    public AliyunEcsPowerProvider(ComputeCredentialCipher cipher, @Value("${vlstream.compute.cloud-control.enabled:false}") boolean enabled) {
        this(cipher, enabled, (region, key, secret) -> {
            DefaultProfile profile = DefaultProfile.getProfile(region, key, secret);
            profile.setCloseTrace(true);
            DefaultAcsClient client = new DefaultAcsClient(profile);
            client.setAutoRetry(false); client.setMaxRetryNumber(0);
            return client;
        });
    }
    AliyunEcsPowerProvider(ComputeCredentialCipher cipher, boolean enabled, ClientFactory clients) {
        this.cipher = cipher; this.enabled = enabled; this.clients = clients;
    }
    @Override public boolean supports(ComputeNode node) {
        return enabled && "API".equals(node.getControlMode()) && "aliyun".equals(node.getProvider())
            && "ECS".equals(node.getProviderProduct()) && node.isProviderCredentialConfigured()
            && node.getProviderInstanceId() != null && node.getProviderRegion() != null;
    }
    @Override public void submit(ComputeNode node, String action, String operationId) {
        if (!"start".equals(action) && !"stop".equals(action)) throw new IllegalArgumentException("Unsupported power action");
        CommonRequest request = request(node, "start".equals(action) ? "StartInstance" : "StopInstance");
        request.putQueryParameter("InstanceId", node.getProviderInstanceId());
        if ("start".equals(action)) request.putQueryParameter("InitLocalDisk", "false");
        else {
            if (!"KeepCharging".equals(node.getRequestedStoppedMode()) && !"StopCharging".equals(node.getRequestedStoppedMode()))
                throw new ComputeProviderException(ComputeProviderException.Kind.WRONG_STATE);
            request.putQueryParameter("ForceStop", "false");
            request.putQueryParameter("StoppedMode", node.getRequestedStoppedMode());
        }
        JsonNode result = call(node, request);
        if (!result.path("RequestId").isTextual() || result.path("RequestId").asText().isEmpty())
            throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN);
        // StartInstance/StopInstance document no ClientToken. DB persists submitted-before-send instead.
    }
    @Override public Snapshot status(ComputeNode node) {
        CommonRequest request = request(node, "DescribeInstances");
        request.putQueryParameter("InstanceIds", "[\"" + node.getProviderInstanceId() + "\"]");
        request.putQueryParameter("MaxResults", "10");
        JsonNode items = call(node, request).path("Instances").path("Instance");
        if (!items.isArray() || items.size() != 1 || !node.getProviderInstanceId().equals(items.get(0).path("InstanceId").asText())
            || !node.getProviderRegion().equals(items.get(0).path("RegionId").asText()))
            throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN);
        JsonNode instance = items.get(0);
        String power;
        switch (instance.path("Status").asText()) {
            case "Running": power = "RUNNING"; break;
            case "Stopped": power = "STOPPED"; break;
            case "Starting": power = "STARTING"; break;
            case "Stopping": power = "STOPPING"; break;
            default: power = "UNKNOWN"; // Pending or future states aren't a scheduling assurance.
        }
        Snapshot result = new Snapshot(power, "UNKNOWN", "ECS 已查询当前状态；实例规格和停机模式不保证重启库存，GPU 与训练环境须独立检查");
        if (instance.path("GPUAmount").isIntegralNumber()) result.gpuAmount = instance.path("GPUAmount").asInt();
        String mode = instance.path("StoppedMode").asText();
        result.stoppedMode = "KeepCharging".equals(mode) || "StopCharging".equals(mode) ? mode : null;
        result.chargeType = AutoDlProPowerProvider.safeLabel(instance.path("InstanceChargeType").asText());
        if ("STOPPED".equals(power)) {
            String message = "StopCharging".equals(result.stoppedMode)
                ? "ECS 已回查节省停机；磁盘等保留资源仍收费，实际截止和费用须核验"
                : "KeepCharging".equals(result.stoppedMode)
                    ? "ECS 已关机但为普通停机，计算费继续；停机不代表停止计算收费"
                    : "ECS 已关机，实际停机收费模式未知；不能认定计算费停止";
            Snapshot stopped = new Snapshot(power, "UNKNOWN", message);
            stopped.gpuAmount = result.gpuAmount; stopped.stoppedMode = result.stoppedMode;
            stopped.chargeType = result.chargeType;
            result = stopped;
        }
        JsonNode locks = instance.path("OperationLocks").path("LockReason");
        if (locks.isArray()) result.locked = !locks.isEmpty();
        return result;
    }
    private CommonRequest request(ComputeNode node, String action) {
        CommonRequest request = new CommonRequest(); request.setSysDomain("ecs.aliyuncs.com");
        request.setSysVersion("2014-05-26"); request.setSysAction(action); request.setSysRegionId(node.getProviderRegion());
        request.setSysProtocol(ProtocolType.HTTPS); request.setSysMethod(MethodType.POST); request.setSysAccept(FormatType.JSON);
        request.setSysConnectTimeout(5000); request.setSysReadTimeout(10000);
        request.putQueryParameter("RegionId", node.getProviderRegion()); return request;
    }
    @Override public ComputeCostProvider.Observation cost(ComputeNode node) {
        CommonRequest describe=request(node,"DescribeInstances"); describe.putQueryParameter("InstanceIds","[\""+node.getProviderInstanceId()+"\"]"); describe.putQueryParameter("MaxResults","10");
        JsonNode items=call(node,describe).path("Instances").path("Instance");
        if (!items.isArray()||items.size()!=1||!node.getProviderInstanceId().equals(items.get(0).path("InstanceId").asText())||!node.getProviderRegion().equals(items.get(0).path("RegionId").asText())) throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN);
        String type=items.get(0).path("InstanceType").asText(); if (!type.matches("[a-zA-Z0-9_.-]{1,100}")) throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN);
        CommonRequest quote=request(node,"DescribePrice"); quote.putQueryParameter("ResourceType","instance"); quote.putQueryParameter("InstanceType",type);
        quote.putQueryParameter("PriceUnit","Hour"); quote.putQueryParameter("Period","1"); quote.putQueryParameter("Amount","1");
        JsonNode price=call(node,quote).path("PriceInfo").path("Price");
        ComputeCostProvider.Observation result=new ComputeCostProvider.Observation(); result.setSource("ALIYUN_DESCRIBE_PRICE"); result.setPriceKind("REFERENCE_QUOTE");
        String currency=price.path("Currency").asText(); result.setCurrency(currency.matches("[A-Z]{3}")?currency:"UNKNOWN"); result.setAmountUnit("PER_INSTANCE_HOUR"); result.setResourceScope("CONFIG_REFERENCE");
        if (price.path("TradePrice").isNumber()) result.setAmount(price.path("TradePrice").decimalValue());
        if (price.path("OriginalPrice").isNumber()) result.setOriginalAmount(price.path("OriginalPrice").decimalValue());
        JsonNode parts=price.path("DetailInfos").path("DetailInfo");
        if(parts.isArray()) for(JsonNode part:parts) {
            String resource=part.path("Resource").asText();
            if(java.util.Arrays.asList("instanceType","systemDisk","dataDisk","bandwidth","image").contains(resource)&&part.path("TradePrice").isNumber())
                result.getComponents().merge(resource,part.path("TradePrice").decimalValue(),java.math.BigDecimal::add);
        }
        result.setMessage("配置参考报价可能包含默认磁盘等组件，不是已购实例折后实际账单；不能自动作为收费成本。账单有延迟，算力与其他费用须单独核对"); return result;
    }
    private JsonNode call(ComputeNode node, CommonRequest request) {
        if (!supports(node)) throw new ComputeProviderException(ComputeProviderException.Kind.AUTHORIZATION);
        IAcsClient client = null;
        try {
            JsonNode secret = json.readTree(cipher.decrypt(node.getProviderCredentialCipher()));
            if (secret.path("accessKeyId").asText().isEmpty() || secret.path("accessKeySecret").asText().isEmpty())
                throw new ComputeProviderException(ComputeProviderException.Kind.AUTHORIZATION);
            client = clients.create(node.getProviderRegion(), secret.path("accessKeyId").asText(), secret.path("accessKeySecret").asText());
            CommonResponse response = client.getCommonResponse(request);
            String value = response.getData();
            if (value == null || value.length() > 1024 * 1024) throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN);
            JsonNode result = json.readTree(value);
            if (response.getHttpStatus() >= 400 || result.has("Code"))
                throw new ComputeProviderException(ComputeProviderException.aliyunKind(result.path("Code").asText()));
            return result;
        } catch (ComputeProviderException e) { throw e; }
        catch (ClientException e) { throw new ComputeProviderException(ComputeProviderException.aliyunKind(e.getErrCode())); }
        catch (Exception e) { throw new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN); }
        finally { if (client != null) client.shutdown(); }
    }
}
