package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Device inventory is read from hardware, never inferred from dispatch records. */
@Service
@RequiredArgsConstructor
public class DeviceModelService {
    private final WvpVlStreamDeviceResolver resolver;
    private final VlsMqttBusService bus;
    private final DeviceModelReplyHandler replies;

    public JSONArray query(String deviceId) {
        JSONObject data = execute(deviceId, "modelQuery", new JSONObject());
        if (data.get("models") instanceof JSONArray) {
            JSONArray models = data.getJSONArray("models");
            Set<String> ids = new HashSet<>();
            for (Object model : models) {
                if (!(model instanceof JSONObject)) throw new ServiceException("设备模型列表格式不正确");
                Object modelId = ((JSONObject) model).get("modelId");
                if (!(modelId instanceof String) || StringUtils.isBlank((String) modelId)
                    || ((String) modelId).length() > 128 || !ids.add((String) modelId)) {
                    throw new ServiceException("设备模型 ID 必须是唯一的非空字符串");
                }
            }
            return models;
        }
        // Protocol 2.2 originally returned a single active model without a modelId.
        if (StringUtils.isNotBlank(data.getStr("algorithmId")) && StringUtils.isNotBlank(data.getStr("fileName"))) {
            return new JSONArray().put(new JSONObject().set("algorithmId", data.getStr("algorithmId"))
                .set("modelName", data.getStr("fileName")).set("format", data.getStr("modelType"))
                .set("sha256", data.getStr("sha256")).set("status", "loaded"));
        }
        throw new ServiceException("设备未返回有效模型列表，请升级固件支持 modelQuery");
    }

    public void delete(String deviceId, String modelId) {
        if (StringUtils.isBlank(modelId) || modelId.length() > 128) throw new ServiceException("模型 ID 无效");
        JSONObject data = execute(deviceId, "modelDelete", new JSONObject().set("modelId", modelId));
        if (!modelId.equals(data.getStr("modelId")) || !"SUCCESS".equals(data.getStr("status"))) {
            throw new ServiceException("设备未确认模型删除完成，请刷新列表核实");
        }
    }

    private JSONObject execute(String deviceId, String operation, JSONObject payload) {
        String topic = VlsMqttProtocol.deviceBusTopic(deviceId);
        resolver.resolveOnline(deviceId);
        String id = UUID.randomUUID().toString();
        CompletableFuture<JSONObject> future = replies.register(id, deviceId, operation);
        try {
            bus.publish(topic, new JSONObject().set("protocolVersion", VlsMqttProtocol.VERSION)
                .set("messageId", id).set("deviceId", deviceId).set("sentAt", Instant.now().toString())
                .set("msgDir", VlsMqttProtocol.PLATFORM_TO_DEVICE).set("mainBizType", VlsMqttProtocol.AI_BIZ)
                .set("subBizType", operation).set("payload", payload).set("extend", new JSONObject()));
            JSONObject reply = future.get(15, TimeUnit.SECONDS);
            if (!Integer.valueOf(200).equals(reply.getInt("code"))
                || (reply.getInt("errCode") != null && reply.getInt("errCode") != 0)) {
                throw new ServiceException("设备操作失败：" + StringUtils.defaultIfBlank(reply.getStr("msg"), "设备拒绝请求"));
            }
            JSONObject data = reply.getJSONObject("bizData");
            if (data == null) throw new ServiceException("设备回执缺少 bizData，请刷新后重试");
            return data;
        } catch (TimeoutException ex) {
            throw new ServiceException("设备回执超时，操作结果尚未确认，请刷新列表核实；设备可能未支持该协议");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException("等待设备回执中断，请刷新列表核实");
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("设备模型通信失败，请检查设备及 MQTT 连接");
        } finally {
            replies.remove(id);
        }
    }
}
