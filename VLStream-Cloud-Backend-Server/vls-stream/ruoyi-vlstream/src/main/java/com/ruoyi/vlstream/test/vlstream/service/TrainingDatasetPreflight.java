package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsRemoteServersMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TrainingDatasetPreflight {
    private final SSHService ssh;
    private final VlsSshProperties properties;
    private final VlsRemoteServersMapper servers;

    public void validate(String dataset, String model) {
        validate(dataset, model, "object_detection", null);
    }

    public void validate(String dataset, String model, String annotationType, Long datasetId) {
        com.ruoyi.vlstream.test.vlstream.data.AnnotationTaskType.of(annotationType);
        RemoteServers server = servers.selectActiveServer();
        if (server == null || server.getCondaEnv() == null || !server.getCondaEnv().matches("[A-Za-z0-9_-]+")) throw new ServiceException("训练服务器 Python 环境未正确配置");
        try {
            String script;
            boolean detection = "object_detection".equals(annotationType);
            try (java.io.InputStream in = new ClassPathResource(detection ? "training/validate_detection_dataset.py" : "training/validate_typed_dataset.py").getInputStream()) {
                script = Base64.getEncoder().encodeToString(StreamUtils.copyToByteArray(in));
            }
            String command = "timeout 180 " + quote("/data/work/anaconda3/envs/" + server.getCondaEnv() + "/bin/python")
                + " -c " + quote("import base64;exec(compile(base64.b64decode('" + script + "'),'dataset-preflight','exec'))")
                + " " + quote(dataset) + " " + quote(model)
                + (detection ? "" : " " + quote(annotationType) + (datasetId == null ? "" : " " + datasetId));
            SSHService.SSHExecutionResult response = ssh.executeCommand(properties.getHost(), properties.getPort(), properties.getUsername(), properties.getPassword(), command);
            String output = response == null ? null : response.getOutput();
            if (response == null || !response.isSuccess() || output == null || !output.contains("VLS_DATASET_CHECK=")) throw new ServiceException("训练数据校验未完成，请检查训练服务器连接及 Python/Pillow/PyYAML 环境");
            String result = output.substring(output.lastIndexOf("VLS_DATASET_CHECK=") + 18).trim();
            JsonNode check = new ObjectMapper().readTree(result);
            if (!check.path("valid").asBoolean()) throw new ServiceException("训练数据校验失败：" + check.path("message").asText("未知原因"));
        } catch (ServiceException e) { throw e; }
        catch (Exception e) { throw new ServiceException("训练数据校验执行失败，请检查服务器环境"); }
    }

    static String quote(String value) { return "'" + value.replace("'", "'\"'\"'") + "'"; }
}
