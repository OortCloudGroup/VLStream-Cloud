package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Correlates short-lived device commands; unrelated and late replies are ignored. */
@Component
public class DeviceModelReplyHandler implements VlsMqttMessageHandler {
    private final ConcurrentHashMap<String, Pending> pending = new ConcurrentHashMap<>();

    public CompletableFuture<JSONObject> register(String id, String deviceId, String operation) {
        Pending item = new Pending(deviceId, operation);
        pending.put(id, item);
        return item.result;
    }

    public void remove(String id) { pending.remove(id); }

    @Override
    public boolean supports(String main, String sub) {
        return VlsMqttProtocol.AI_BIZ.equals(main)
            && ("modelQuery".equals(sub) || "modelDelete".equals(sub));
    }

    @Override
    public JSONObject handle(JSONObject envelope, String rawPayload) {
        JSONObject reply = envelope.getJSONObject("payload");
        String source = reply == null ? null : reply.getStr("sourceMsgId");
        Pending item = source == null ? null : pending.get(source);
        if (item != null && item.deviceId.equals(envelope.getStr("deviceId"))
            && item.operation.equals(envelope.getStr("subBizType"))) {
            item.result.complete(reply);
        }
        return null;
    }

    private static class Pending {
        final String deviceId;
        final String operation;
        final CompletableFuture<JSONObject> result = new CompletableFuture<>();
        Pending(String deviceId, String operation) {
            this.deviceId = deviceId;
            this.operation = operation;
        }
    }
}
