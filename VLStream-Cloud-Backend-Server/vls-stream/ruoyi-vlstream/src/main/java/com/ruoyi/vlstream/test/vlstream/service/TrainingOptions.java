package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import lombok.Getter;
import java.util.LinkedHashMap;
import java.util.Map;

/** A bounded configuration contract, never arbitrary shell arguments. */
@Getter
public class TrainingOptions {
    private final int epochs, batchSize, imgSize;
    private final String mode;
    private final boolean autoPublish;

    public TrainingOptions(Integer epochs, Integer batch, Integer size, String extra) {
        JsonNode json;
        try { json = new ObjectMapper().readTree(extra == null || extra.trim().isEmpty() ? "{}" : extra); }
        catch (Exception e) { throw new ServiceException("训练配置必须是有效 JSON"); }
        if (!json.isObject()) throw new ServiceException("训练配置必须为对象");
        json.fieldNames().forEachRemaining(key -> {
            if (!"mode".equals(key) && !"autoPublish".equals(key)) throw new ServiceException("不支持的训练配置：" + key);
        });
        mode = json.has("mode") ? json.get("mode").asText() : "advanced";
        if (!"auto".equals(mode) && !"advanced".equals(mode)) throw new ServiceException("无效的训练模式");
        if (json.has("autoPublish") && !json.get("autoPublish").isBoolean()) throw new ServiceException("自动发布必须为布尔值");
        autoPublish = json.path("autoPublish").asBoolean(false);
        this.epochs = epochs == null ? 10 : epochs;
        batchSize = "auto".equals(mode) ? 16 : batch == null ? 16 : batch;
        imgSize = "auto".equals(mode) ? 640 : size == null ? 640 : size;
        if (this.epochs < 1 || this.epochs > 10000) throw new ServiceException("训练轮数必须在 1 至 10000 之间");
        if (batchSize < 1 || batchSize > 256) throw new ServiceException("批大小必须在 1 至 256 之间");
        if (imgSize < 320 || imgSize > 1280 || imgSize % 32 != 0) throw new ServiceException("输入尺寸必须为 320 至 1280 范围内的 32 的倍数");
    }

    public String toJson() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", mode); values.put("epochs", epochs); values.put("batchSize", batchSize);
        values.put("imgSize", imgSize); values.put("autoPublish", autoPublish);
        try { return new ObjectMapper().writeValueAsString(values); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
