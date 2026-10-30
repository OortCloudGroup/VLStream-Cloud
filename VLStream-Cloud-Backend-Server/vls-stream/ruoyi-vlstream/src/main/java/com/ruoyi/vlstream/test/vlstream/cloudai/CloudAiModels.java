package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.compute.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudAiModels {
    private final IVlsAlgorithmModelService models;
    private final ComputePublicTaskAccess access;
    private final ComputeSharedQueueService queue;
    private final ModelArtifactObjectStore objects;
    private final ObjectMapper json;
    private final CloudAiTrustedModels trusted;
    private final ComputeNodeService nodes;
    private final ComputeSsh ssh;

    public List<ObjectNode> choices(ComputeNode node) {
        QueryWrapper<AlgorithmModel> query = new QueryWrapper<>();
        query.eq("tenant_id", CloudAiService.tenant()).eq("is_deleted", 0).orderByDesc("create_time").last("LIMIT 500");
        access.filter(query, "training_id");
        List<ObjectNode> result = new ArrayList<>();
        for (AlgorithmModel model : models.list(query)) {
            ObjectNode item = summary(model);
            try { item = snapshot(model.getId(), node); item.remove(Arrays.asList("path", "sha256", "size")); item.put("available", true); }
            catch (RuntimeException error) { item.put("available", false); item.put("reason", error.getMessage()); item.put("canArchive",trusted.find(model.getId()) != null); }
            result.add(item);
        }
        return result;
    }

    public ObjectNode snapshot(Long id, ComputeNode node) {
        if (id == null) throw new ServiceException("请选择已保存的模型版本");
        access.model(id);
        AlgorithmModel model = models.getById(id);
        if (model == null || !CloudAiService.tenant().equals(model.getTenantId())) throw new ServiceException("模型不存在或无权访问", 403);
        String path = model.getModelPath();
        CloudAiTrustedModels.ApprovedModel approved = trusted.find(id);
        if (!("object_detection".equals(model.getAnnotationType()) || model.getAnnotationType() == null && approved != null) || path == null || !path.toLowerCase(Locale.ROOT).endsWith(".pt"))
            throw new ServiceException("首期仅支持YOLO物体检测PT模型");
        if (queue.isShared(node) && approved == null) queue.verifyBaseModel(path, "object_detection", queue.viewer());
        ModelArtifactObjectStore.StoredArtifact stored = objects.find(path);
        if (stored == null) throw new ServiceException("模型尚未完成对象存储归档，请先在算法库完成模型保存");
        if (approved != null && !approved.getSha256().equals(stored.getSha256())) throw new ServiceException("模型与部署核验的固定版本不一致");
        ObjectNode result = summary(model);
        result.put("path", path); result.put("sha256", stored.getSha256()); result.put("size", stored.getFileSize());
        result.set("labels", json.valueToTree(labels(path))); return result;
    }

    public void archiveApproved(Long id) {
        access.model(id);
        AlgorithmModel model = models.getById(id);
        CloudAiTrustedModels.ApprovedModel approved = trusted.find(id);
        if (model == null || !CloudAiService.tenant().equals(model.getTenantId()) || approved == null) throw new ServiceException("该模型尚未经部署管理员核验",403);
        ComputeNode source = nodes.get(approved.getSourceNodeId());
        // An enrolled artifact can only be fetched from a configured company asset, never a caller URL.
        if (!source.isConfigurationManaged() || !queue.isShared(source)) throw new ServiceException("模型源须为已核验公司GPU",403);
        queue.requireTarget(source);
        String path = model.getModelPath();
        if (path == null || !path.startsWith("/") || !path.endsWith(".pt") || path.contains("\n") || path.contains("\r")) throw new ServiceException("模型源路径无效");
        ModelArtifactObjectStore.StoredArtifact existing = objects.find(path);
        if (existing != null && !approved.getSha256().equals(existing.getSha256())) throw new ServiceException("模型既有归档与核验版本不一致");
        try (ComputeSsh.Connection connection = ssh.open(source)) {
            String directory = source.getWorkDir()+"/.cloud-ai-approved-models";
            connection.mkdir(directory);
            String script = directory+"/inspect_model.py";
            try (InputStream input = new org.springframework.core.io.ClassPathResource("cloudai/inspect_model.py").getInputStream()) { connection.put(script,input); }
            String command = "env -u PYTHONHOME PYTHONPATH='' PYTHONNOUSERSITE=1 "+ComputeSsh.quote(source.getPythonPath())+" -B "+ComputeSsh.quote(script)+" "+ComputeSsh.quote(path)+" "+ComputeSsh.quote(approved.getSha256());
            String output = connection.execute(command,60); int marker = output.lastIndexOf("VLS_MODEL=");
            if (marker < 0) throw new java.io.IOException("MODEL_INSPECTION_MISSING");
            com.fasterxml.jackson.databind.JsonNode inspected = json.readTree(output.substring(marker+10).trim());
            if (!"detect".equals(inspected.path("task").asText()) || !approved.getSha256().equals(inspected.path("sha256").asText()) || !inspected.path("names").isObject()) throw new java.io.IOException("INVALID_MODEL_METADATA");
            boolean saved = objects.archive(path,"model.pt",target -> { connection.downloadBounded(path,target,1024L*1024*1024); if (!approved.getSha256().equals(CloudAiWorker.sha(target))) throw new java.io.IOException("MODEL_CHECKSUM_MISMATCH"); });
            byte[] classes = json.writeValueAsBytes(Collections.singletonMap("names", inspected.path("names")));
            boolean labelsSaved = objects.archive(ModelClassFileService.storagePath(path),"data.yaml",target -> Files.write(target,classes));
            if (!saved || !labelsSaved) throw new ServiceException("模型正在归档，请稍后刷新");
        } catch (java.io.IOException error) { throw new ServiceException("模型归档未完成，请检查源文件、固定版本和对象存储"); }
    }

    private Map<Integer,String> labels(String path) {
        ModelArtifactObjectStore.StoredArtifact stored = objects.find(ModelClassFileService.storagePath(path));
        if (stored == null || stored.getFileSize() < 1 || stored.getFileSize() > 1024 * 1024) throw new ServiceException("模型缺少已归档的类别文件");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            objects.stream(stored, new java.io.FilterOutputStream(bytes) {
                private long size;
                @Override public void write(int value) throws java.io.IOException {
                    if (++size > 1024 * 1024) throw new java.io.IOException("CLASS_FILE_TOO_LARGE");
                    out.write(value);
                }
                @Override public void write(byte[] data, int offset, int length) throws java.io.IOException {
                    if (length < 0 || size + length > 1024 * 1024) throw new java.io.IOException("CLASS_FILE_TOO_LARGE");
                    size += length; out.write(data, offset, length);
                }
            });
            if (!ModelArtifactObjectStore.hash(bytes.toString("UTF-8")).equals(stored.getSha256())) throw new IllegalStateException();
            LoaderOptions options = new LoaderOptions(); options.setCodePointLimit(1024 * 1024); options.setMaxAliasesForCollections(0);
            Object yaml = new Yaml(new SafeConstructor(options)).load(bytes.toString("UTF-8"));
            Object names = ((Map<?,?>) yaml).get("names"); Map<Integer,String> result = new TreeMap<>();
            if (names instanceof List) {
                List<?> list = (List<?>) names;
                for (int i = 0; i < list.size(); i++) result.put(i, String.valueOf(list.get(i)));
            } else if (names instanceof Map) {
                for (Map.Entry<?,?> entry : ((Map<?,?>) names).entrySet()) result.put(Integer.valueOf(String.valueOf(entry.getKey())), String.valueOf(entry.getValue()));
            }
            if (result.isEmpty() || result.size() > 10000) throw new IllegalStateException();
            for (int i = 0; i < result.size(); i++) if (!result.containsKey(i) || result.get(i).isEmpty()) throw new IllegalStateException();
            return result;
        } catch (Exception error) { throw new ServiceException("模型类别归档校验失败，请重新核对模型"); }
    }
    private ObjectNode summary(AlgorithmModel model) {
        ObjectNode result = json.createObjectNode(); result.put("modelId", String.valueOf(model.getId()));
        result.put("algorithmId", String.valueOf(model.getAlgorithmId())); result.put("name", model.getModelName());
        result.put("version", model.getVersion() == null ? 0 : model.getVersion()); return result;
    }
}
