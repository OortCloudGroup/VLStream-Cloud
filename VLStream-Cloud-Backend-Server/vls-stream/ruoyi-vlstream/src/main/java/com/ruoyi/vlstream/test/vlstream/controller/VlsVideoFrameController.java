package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.data.*;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController @RequestMapping("/vlsData") @RequiredArgsConstructor
public class VlsVideoFrameController {
    private final VideoFrameService service;
    private final VideoFrameRegistry registry;
    @PostMapping("/video-frames")
    public R<DatasetImportJob> extract(@Valid @RequestBody VideoFrameRequest request){return R.data(service.enqueue(request));}
    @GetMapping("/datasets/{datasetId}/samples/{sampleId}/video-origins")
    public R<List<VideoFrameOrigin>> origins(@PathVariable Long datasetId,@PathVariable Long sampleId){return R.data(registry.origins(datasetId,sampleId));}
}
