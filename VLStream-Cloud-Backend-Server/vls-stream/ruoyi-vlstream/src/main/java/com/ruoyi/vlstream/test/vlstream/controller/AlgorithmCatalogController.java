package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import com.ruoyi.vlstream.test.vlstream.service.AlgorithmCatalogService;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAlgorithmCatalog")
public class AlgorithmCatalogController {
    private final AlgorithmCatalogService catalog;

    @GetMapping("/categories")
    public R<List<AlgorithmRepository>> categories() { return R.data(catalog.categories()); }

    @PostMapping("/categories")
    public R<?> create(@RequestBody AlgorithmRepository row) {
        row.setId(null);
        return catalog.saveCategory(row) ? R.data(row) : R.fail("分类保存失败");
    }

    @PutMapping("/categories/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody AlgorithmRepository row) {
        row.setId(id);
        return catalog.saveCategory(row) ? R.data(row) : R.fail("分类保存失败");
    }

    @DeleteMapping("/categories")
    public R<?> delete(@RequestBody List<Long> ids) { return R.status(catalog.deleteCategories(ids)); }

    @GetMapping("/algorithms")
    public R<Page<Algorithm>> algorithms(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "24") long size) {
        return R.data(catalog.algorithmPage(categoryId, keyword, type, current, size));
    }

    @GetMapping("/settings")
    public R<String> settings() { return R.data(catalog.viewMode()); }

    @PutMapping("/settings")
    public R<?> settings(@RequestBody AlgorithmCatalogService.Settings settings) {
        catalog.saveSettings(settings);
        return R.success("设置已保存");
    }
}
