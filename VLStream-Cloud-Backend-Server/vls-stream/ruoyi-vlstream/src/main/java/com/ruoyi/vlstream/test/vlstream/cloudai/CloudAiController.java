package com.ruoyi.vlstream.test.vlstream.cloudai;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/cloud-ai")
@SaCheckLogin
@RequiredArgsConstructor
public class CloudAiController {
    private static final String SCENES = "vls:menu:scene-governance";
    private static final String TASKS = "vls:menu:algorithm-task";
    private static final String RUNS = "vls:menu:task-status";
    private static final String ANALYSIS = "vls:menu:intelligent-analysis-request";
    private static final String EVENTS = "active-safety:events:list";
    private final CloudAiService service;
    private final CloudAiSources sources;
    private final CloudAiEvents events;
    private final CloudAiModels models;
    @SaCheckPermission(value = {SCENES, TASKS}, mode = SaMode.OR)
    @GetMapping("/nodes") public R<?> nodes() { return R.data(service.nodeChoices()); }
    @SaCheckPermission(SCENES)
    @GetMapping("/models") public R<?> models(@RequestParam Long nodeId) { return R.data(service.modelChoices(nodeId)); }
    @SaCheckPermission(SCENES)
    @PostMapping("/models/{id}/archive") public R<?> archiveModel(@PathVariable Long id) { models.archiveApproved(id); return R.success("已核验模型和类别已归档"); }
    @SaCheckPermission(SCENES)
    @PostMapping("/sources") public R<?> bind(@RequestBody Token body) { return R.data(sources.bind(body.token)); }
    @SaCheckPermission(value = {SCENES, TASKS, RUNS, ANALYSIS}, mode = SaMode.OR)
    @GetMapping("/scenes") public R<?> scenes(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String search) { return R.data(service.page("scene", page, size, search, null, null)); }
    @SaCheckPermission(value = {SCENES, TASKS}, mode = SaMode.OR)
    @GetMapping("/scenes/{id}") public R<?> scene(@PathVariable String id) { return R.data(service.scene(id)); }
    @SaCheckPermission(SCENES)
    @PostMapping("/scenes") public R<?> addScene(@RequestBody CloudAiConfiguration body) { return R.data(service.saveScene(null, body)); }
    @SaCheckPermission(SCENES)
    @PutMapping("/scenes/{id}") public R<?> editScene(@PathVariable String id, @RequestBody CloudAiConfiguration body) { return R.data(service.saveScene(id, body)); }
    @SaCheckPermission(SCENES)
    @PutMapping("/scenes/{id}/enabled") public R<?> sceneEnabled(@PathVariable String id, @RequestBody Toggle body) { service.sceneEnabled(id, body.enabled); return R.success("场景状态已更新"); }
    @SaCheckPermission(SCENES)
    @DeleteMapping("/scenes/{id}") public R<?> deleteScene(@PathVariable String id) { service.deleteScene(id); return R.success("场景已删除"); }
    @SaCheckPermission(TASKS)
    @GetMapping("/tasks") public R<?> tasks(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String search, @RequestParam(required=false) String sceneId) { return R.data(service.page("task", page, size, search, null, sceneId)); }
    @SaCheckPermission(TASKS)
    @GetMapping("/tasks/{id}") public R<?> task(@PathVariable String id) { return R.data(service.task(id)); }
    @SaCheckPermission(TASKS)
    @PostMapping("/tasks") public R<?> addTask(@RequestBody CloudAiConfiguration.Task body) { return R.data(service.saveTask(null, body)); }
    @SaCheckPermission(TASKS)
    @PutMapping("/tasks/{id}") public R<?> editTask(@PathVariable String id, @RequestBody CloudAiConfiguration.Task body) { return R.data(service.saveTask(id, body)); }
    @SaCheckPermission(TASKS)
    @PutMapping("/tasks/{id}/enabled") public R<?> taskEnabled(@PathVariable String id, @RequestBody Toggle body) { service.taskEnabled(id, body.enabled); return R.success("定时配置已更新"); }
    @SaCheckPermission(TASKS)
    @DeleteMapping("/tasks/{id}") public R<?> deleteTask(@PathVariable String id) { service.deleteTask(id); return R.success("任务配置已删除，历史运行仍保留"); }
    @SaCheckPermission(value = {TASKS, RUNS}, mode = SaMode.OR)
    @GetMapping("/runs") public R<?> runs(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String search, @RequestParam(required=false) String state, @RequestParam(required=false) String sceneId) { return R.data(service.page("run", page, size, search, state, sceneId)); }
    @SaCheckPermission(value = {TASKS, RUNS}, mode = SaMode.OR)
    @GetMapping("/runs/{id}") public R<?> run(@PathVariable String id) { return R.data(service.run(id)); }
    @SaCheckPermission(value = {TASKS, RUNS}, mode = SaMode.OR)
    @PostMapping("/runs/{id}/stop") public R<?> stop(@PathVariable String id) { service.stop(id); return R.success("已请求停止，请等待资源释放确认"); }
    @SaCheckPermission(ANALYSIS)
    @GetMapping("/events") public R<?> events(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String search, @RequestParam(required=false) String status, @RequestParam(required=false) String sceneId, @RequestParam(required=false) String runId) { return R.data(events.page(page,size,search,status,sceneId,runId)); }
    @SaCheckPermission(value = {ANALYSIS, EVENTS}, mode = SaMode.OR)
    @GetMapping("/events/{eventId}") public R<?> event(@PathVariable Long eventId) { return R.data(events.detail(eventId)); }
    public static class Toggle { public boolean enabled; }
    public static class Token { public String token; }
}
