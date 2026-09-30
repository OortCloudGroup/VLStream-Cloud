package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.compute.*;
import com.ruoyi.vlstream.test.vlstream.service.RemoteTrainingService;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/vlsCompute")
@RequiredArgsConstructor
public class VlsComputeController {
    private final ComputeNodeService nodes;
    private final CloudTrainingService training;
    @GetMapping("/nodes") public R<List<ComputeNode>> list() { return R.data(nodes.list()); }
    @PostMapping("/nodes") public R<ComputeNode> create(@Valid @RequestBody ComputeRequests.Node request) { return R.data(nodes.save(null, request)); }
    @PutMapping("/nodes/{id}") public R<ComputeNode> update(@PathVariable Long id, @Valid @RequestBody ComputeRequests.Node request) { return R.data(nodes.save(id, request)); }
    @PostMapping("/nodes/{id}/probe") public R<ComputeNode> probe(@PathVariable Long id) { return R.data(nodes.probe(id)); }
    @DeleteMapping("/nodes/{id}") public R<String> delete(@PathVariable Long id) { nodes.delete(id); return R.data("实例接入记录已移除"); }
    @PostMapping("/trainings/{id}/start") public R<RemoteTrainingService.StartResult> start(@PathVariable Long id, @Valid @RequestBody ComputeRequests.Start request) { return R.data(training.start(id, request)); }
}
