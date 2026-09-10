package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.data.*;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/vlsData")
@RequiredArgsConstructor
@Validated
public class VlsDatasetImportController {
    private final DatasetUploadService uploads;
    private final DatasetSourceService sources;
    @GetMapping("/import-capabilities") public R<Map<String,Object>> capabilities(){return R.data(DataManagementService.map("chunkSize",DatasetUploadService.CHUNK_SIZE,"guaranteedBytes",DatasetFileIO.FOUR_GIB,"maxFileBytes",uploads.maxFileBytes(),"resumeDays",7));}
    @PostMapping("/uploads") public R<Map<String,Object>> initialize(@RequestBody @Valid DatasetImportRequests.Upload request){return R.data(uploads.initialize(request));}
    @GetMapping("/uploads/{id}") public R<Map<String,Object>> status(@PathVariable Long id){return R.data(uploads.status(id));}
    @PutMapping("/uploads/{id}/parts/{part}") public R<String> upload(@PathVariable Long id,@PathVariable @Min(1) int part,
        @RequestParam @Pattern(regexp="[a-f0-9]{64}") String sha256,@RequestParam MultipartFile file)throws IOException{uploads.part(id,part,sha256,file);return R.data("分片已保存");}
    @PostMapping("/uploads/{id}/complete") public R<DatasetImportJob> complete(@PathVariable Long id){return R.data(uploads.complete(id));}
    @DeleteMapping("/uploads/{id}") public R<String> cancel(@PathVariable Long id){uploads.cancel(id);return R.data("任务已取消");}
    @GetMapping("/imports") public R<List<DatasetImportJob>> jobs(@RequestParam(required=false) Long datasetId){return R.data(uploads.list(datasetId));}
    @PostMapping("/imports/{id}/retry") public R<String> retry(@PathVariable Long id){uploads.retry(id);return R.data("已重新排队");}
    @GetMapping("/sources") public R<?> list(@RequestParam(required=false) @Size(max=100) String keyword,@RequestParam(defaultValue="1") @Min(1) int page){return R.data(sources.list(keyword,page));}
    @PostMapping("/sources") public R<DatasetSource> create(@RequestBody @Valid DatasetImportRequests.Source request){return R.data(sources.save(null,request));}
    @PutMapping("/sources/{id}") public R<DatasetSource> update(@PathVariable Long id,@RequestBody @Valid DatasetImportRequests.Source request){return R.data(sources.save(id,request));}
    @DeleteMapping("/sources/{id}") public R<String> remove(@PathVariable Long id){sources.remove(id);return R.data("来源已删除");}
    @GetMapping("/sources/{id}/files") public R<Map<String,Object>> browse(@PathVariable Long id,@RequestParam(required=false) @Size(max=2000) String path,@RequestParam(required=false) @Size(max=4000) String token){return R.data(sources.browse(id,path,token));}
    @PostMapping("/imports/remote") public R<DatasetImportJob> remote(@RequestBody @Valid DatasetImportRequests.Remote request){return R.data(sources.enqueue(request));}
}
