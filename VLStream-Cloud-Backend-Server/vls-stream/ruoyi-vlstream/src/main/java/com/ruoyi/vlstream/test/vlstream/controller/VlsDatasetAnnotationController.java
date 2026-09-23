package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.data.DatasetAnnotationEditor;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/vlsData/projects/{datasetId}/samples/{sampleId}/annotation")
@RequiredArgsConstructor
public class VlsDatasetAnnotationController {
    private final DatasetAnnotationEditor editor;
    @GetMapping public R<Map<String, Object>> read(@PathVariable Long datasetId, @PathVariable Long sampleId) { return R.data(editor.read(datasetId, sampleId)); }
    @PutMapping public R<Boolean> save(@PathVariable Long datasetId, @PathVariable Long sampleId, @Valid @RequestBody DatasetAnnotationEditor.Edit request) { editor.save(datasetId, sampleId, request); return R.data(true); }
}
