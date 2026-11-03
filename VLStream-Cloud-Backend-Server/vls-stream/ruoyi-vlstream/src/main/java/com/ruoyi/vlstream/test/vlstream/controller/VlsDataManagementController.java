package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vlsData/projects")
@RequiredArgsConstructor
@Validated
public class VlsDataManagementController {
    private final DataManagementService data;
    private final DataTransferService transfer;

    @SaCheckPermission(value = {"vls:menu:data-management", "vls:menu:smart-annotation"}, mode = SaMode.OR)
    @GetMapping
    public R<IPage<AlgorithmAnnotation>> projects(@RequestParam(required = false) @Size(max = 100) String keyword,
        @RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.data(data.projects(keyword, page, size));
    }

    @SaCheckPermission("vls:menu:data-management")
    @GetMapping("/{id}") public R<AlgorithmAnnotation> project(@PathVariable Long id) { return R.data(data.project(id)); }
    @SaCheckPermission("vls:menu:data-management")
    @PostMapping public R<AlgorithmAnnotation> create(@Valid @RequestBody DataRequests.Project request) { return R.data(data.saveProject(null, request)); }
    @SaCheckPermission("vls:menu:data-management")
    @PutMapping("/{id}") public R<AlgorithmAnnotation> update(@PathVariable Long id, @Valid @RequestBody DataRequests.Project request) { return R.data(data.saveProject(id, request)); }
    @SaCheckPermission("vls:menu:data-management")
    @GetMapping("/{id}/samples") public R<IPage<Map<String, Object>>> samples(@PathVariable Long id, @Valid DataRequests.SampleQuery query) { return R.data(data.samples(id, query)); }
    @SaCheckPermission("vls:menu:data-management")
    @GetMapping("/{id}/samples/{sampleId}") public R<Map<String, Object>> sample(@PathVariable Long id, @PathVariable Long sampleId) { return R.data(data.detail(id, sampleId)); }
    @SaCheckPermission("vls:menu:data-management")
    @PutMapping("/{id}/samples/{sampleId}") public R<String> edit(@PathVariable Long id, @PathVariable Long sampleId, @Valid @RequestBody DataRequests.SampleEdit request) {
        data.editSample(id, sampleId, request); return R.data("样本已更新");
    }
    @SaCheckPermission("vls:menu:data-management")
    @PostMapping("/{id}/samples/import") public R<Map<String, Object>> upload(@PathVariable Long id, @RequestParam MultipartFile[] files,
        @RequestParam(defaultValue = "upload") String source) { return R.data(transfer.upload(id, files, source)); }
    @SaCheckPermission("vls:menu:data-management")
    @PostMapping("/{id}/samples/import-archive") public R<Map<String, Object>> importArchive(@PathVariable Long id, @RequestParam MultipartFile file) throws IOException {
        return R.data(transfer.importArchive(id, file));
    }
    @SaCheckPermission("vls:menu:data-management")
    @PostMapping("/{id}/samples/batch") public R<String> batch(@PathVariable Long id, @Valid @RequestBody DataRequests.Batch request) {
        data.batch(id, request); return R.data("批量操作已完成");
    }
    @SaCheckPermission("vls:menu:data-management")
    @PostMapping("/{id}/samples/check") public R<Map<String, Object>> check(@PathVariable Long id, @RequestBody @NotEmpty @Size(max = 100) List<@NotNull Long> ids) {
        return R.data(transfer.inspect(id, ids));
    }
    @SaCheckPermission("vls:menu:data-management")
    @GetMapping("/{id}/samples/review") public R<List<Map<String, Object>>> review(@PathVariable Long id, @RequestParam(defaultValue = "10") @Min(1) @Max(50) int count) {
        return R.data(data.review(id, count));
    }
    @SaCheckPermission("vls:menu:data-management")
    @GetMapping("/{id}/statistics") public R<Map<String, Object>> stats(@PathVariable Long id) { return R.data(data.statistics(id)); }
    @SaCheckPermission("vls:menu:data-management")
    @PostMapping("/{id}/split") public R<DatasetVersion> split(@PathVariable Long id, @Valid @RequestBody DataRequests.Split request) { return R.data(data.split(id, request)); }
    @SaCheckPermission("vls:menu:data-management")
    @GetMapping("/{id}/versions") public R<List<DatasetVersion>> versions(@PathVariable Long id) { return R.data(data.versions(id)); }
    @SaCheckPermission("vls:menu:data-management")
    @PostMapping("/{id}/versions") public R<DatasetVersion> saveVersion(@PathVariable Long id, @Valid @RequestBody DataRequests.Version request) { return R.data(data.saveVersion(id, request)); }
    @SaCheckPermission("vls:menu:data-management")
    @GetMapping("/{id}/versions/{versionId}") public R<Map<String, Object>> version(@PathVariable Long id, @PathVariable Long versionId) { return R.data(data.versionDetail(id, versionId)); }
    @SaCheckPermission("vls:menu:data-management")
    @GetMapping("/{id}/versions/{versionId}/compare") public R<Map<String, Object>> compare(@PathVariable Long id, @PathVariable Long versionId, @RequestParam(required = false) Long to) { return R.data(data.compare(id, versionId, to)); }
    @SaCheckPermission("vls:menu:data-management")
    @PostMapping("/{id}/versions/{versionId}/restore") public R<DatasetVersion> restore(@PathVariable Long id, @PathVariable Long versionId) { return R.data(data.restore(id, versionId)); }

    @SaCheckPermission("vls:menu:data-management")
    @GetMapping("/{id}/samples/export")
    public void export(@PathVariable Long id, @Valid DataRequests.SampleQuery query, @RequestParam(required = false) List<Long> ids,
                       HttpServletResponse response) throws IOException {
        download(transfer.exportArchive(data.exportSnapshot(id, query, ids)), response);
    }

    @SaCheckPermission("vls:menu:data-management")
    @GetMapping("/{id}/versions/{versionId}/export")
    public void exportVersion(@PathVariable Long id, @PathVariable Long versionId, HttpServletResponse response) throws IOException {
        download(transfer.exportArchive(data.readSnapshot(data.version(id, versionId).getSnapshotJson())), response);
    }

    private void download(Path archive, HttpServletResponse response) throws IOException {
        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"vls-samples.zip\"");
            response.setContentLengthLong(Files.size(archive)); Files.copy(archive, response.getOutputStream());
        } finally { Files.deleteIfExists(archive); }
    }
}
