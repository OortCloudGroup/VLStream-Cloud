package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.vlstream.test.vlstream.cloudai.CloudAiService;
import com.ruoyi.vlstream.test.vlstream.service.VideoSquareSceneService;
import com.ruoyi.vlstream.test.vlstream.service.VideoSquareSceneService.GenerateRequest;
import com.ruoyi.vlstream.test.vlstream.service.VideoSquareSceneService.SavedScene;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/vls/video-square/scenes")
@SaCheckPermission("vls:menu:video-square")
public class VideoSquareSceneController {
    private final VideoSquareSceneService service;
    private final ObjectMapper json;

    @GetMapping("/models")
    public R<VideoSquareSceneService.ModelChoices> models(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return R.data(service.models());
    }

    @GetMapping("/{mode}")
    public R<SavedScene> latest(@PathVariable String mode, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return R.data(service.latest(CloudAiService.tenant(), CloudAiService.actor(), mode));
    }

    /** Restores the page before the latest change. Generation failures never replace a page, so this covers runtime regressions. */
    @PostMapping("/{mode}/undo")
    public R<SavedScene> undo(@PathVariable String mode, @RequestBody VideoSquareSceneService.UndoRequest request,
                              HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return R.data(service.undo(CloudAiService.tenant(), CloudAiService.actor(), mode, request));
    }

    @PostMapping("/{mode}")
    public void generate(@PathVariable String mode, @RequestBody GenerateRequest request, HttpServletResponse response) throws IOException {
        String tenant = CloudAiService.tenant(), owner = CloudAiService.actor();
        response.setContentType("application/x-ndjson;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        Writer writer = new java.io.OutputStreamWriter(response.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8);
        try {
            write(writer, json.createObjectNode().put("type", "started"));
            ObjectNode terminal;
            try {
                SavedScene saved = service.generate(tenant, owner, mode, request, scene ->
                    write(writer, json.createObjectNode().put("type", "preview").set("scene", scene)));
                terminal = json.createObjectNode().put("type", "saved").set("value", json.valueToTree(saved));
            } catch (UncheckedIOException disconnected) {
                throw disconnected;
            } catch (ServiceException e) {
                terminal = json.createObjectNode().put("type", "error").put("message", e.getMessage())
                    .put("code", e.getCode() == null ? 500 : e.getCode());
            } catch (RuntimeException e) {
                log.error("Video square scene generation failed for mode {}", mode, e);
                terminal = json.createObjectNode().put("type", "error").put("code", 500)
                    .put("message", "服务暂时无法完成生成，请重新载入查看已保存的视图").put("reload", true);
            }
            write(writer, terminal);
        } catch (UncheckedIOException disconnected) {
            log.debug("Video square {} preview disconnected", mode);
        }
    }

    private void write(Writer writer, ObjectNode event) {
        try {
            writer.write(json.writeValueAsString(event));
            writer.write('\n');
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
