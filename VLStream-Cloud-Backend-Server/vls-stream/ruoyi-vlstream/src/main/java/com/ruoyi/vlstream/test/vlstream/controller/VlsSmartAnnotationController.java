package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.vlstream.test.vlstream.data.*;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/vlsData/smart-annotation")
@RequiredArgsConstructor
public class VlsSmartAnnotationController {
    private final SmartAnnotationService service;
    private final SmartAnnotationWorker worker;

    @GetMapping public R<IPage<SmartAnnotationTask>> list(@RequestParam(required = false) Long datasetId, @RequestParam(defaultValue = "1") int page) { return R.data(service.list(datasetId, page)); }
    @GetMapping("/options/{datasetId}") public R<Map<String, Object>> options(@PathVariable Long datasetId) { return R.data(service.options(datasetId)); }
    @PostMapping public R<SmartAnnotationTask> create(@Valid @RequestBody SmartAnnotationRequests.Create request) { return R.data(service.create(request)); }
    @GetMapping("/{id}") public R<Map<String, Object>> detail(@PathVariable Long id) { return R.data(service.detail(id)); }
    @GetMapping("/{id}/predictions") public R<Map<String, Object>> predictions(@PathVariable Long id, @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "PENDING") String state, @RequestParam(defaultValue = "false") boolean hardOnly) { return R.data(service.predictions(id, page, state, hardOnly)); }
    @PostMapping("/{id}/predictions/{candidateId}") public R<Boolean> review(@PathVariable Long id, @PathVariable Long candidateId, @Valid @RequestBody SmartAnnotationRequests.Review request) { service.review(id, candidateId, request); return R.data(true); }
    @PostMapping("/{id}/confirm") public R<Boolean> confirm(@PathVariable Long id, @Valid @RequestBody SmartAnnotationRequests.ConfirmBatch request) { service.confirmBatch(id, request.getIds()); return R.data(true); }
    @PostMapping("/{id}/confirm-all") public R<Boolean> confirmAll(@PathVariable Long id) { service.confirmAll(id); return R.data(true); }
    @GetMapping("/{id}/predictions/{candidateId}") public R<Map<String, Object>> prediction(@PathVariable Long id, @PathVariable Long candidateId) { return R.data(service.prediction(id, candidateId)); }
    @PostMapping("/{id}/next") public R<Boolean> next(@PathVariable Long id) { service.next(id); return R.data(true); }
    @PostMapping("/{id}/finish") public R<Boolean> finish(@PathVariable Long id) { service.finish(id); return R.data(true); }
    @PostMapping("/{id}/retry") public R<Boolean> retry(@PathVariable Long id) { service.retry(id); return R.data(true); }
    @PostMapping("/{id}/cancel") public R<Boolean> cancel(@PathVariable Long id) { service.cancel(id); return R.data(true); }
    @GetMapping("/{id}/logs") public R<String> logs(@PathVariable Long id) { return R.data(worker.logs(id)); }
}
