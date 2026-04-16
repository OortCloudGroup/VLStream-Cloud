package com.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vlstream.common.Result;
import com.vlstream.entity.AlgorithmModel;
import com.vlstream.dto.AlgorithmModelQueryDTO;
import com.vlstream.dto.AlgorithmModelCreateDTO;
import com.vlstream.dto.AlgorithmModelUpdateDTO;
import com.vlstream.dto.AlgorithmModelStatisticsDTO;
import com.vlstream.service.AlgorithmModelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Algorithm Model Controller
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Api(tags = "Algorithm Model Management")
@RestController
@RequestMapping("/api/algorithm-model")
public class AlgorithmModelController {

    @Autowired
    private AlgorithmModelService algorithmModelService;

    @ApiOperation(value = "Query algorithm models with pagination")
    @PostMapping("/page")
    public Result<IPage<AlgorithmModel>> getModelPage(@RequestBody AlgorithmModelQueryDTO queryDTO) {
        try {
            IPage<AlgorithmModel> page = algorithmModelService.getModelPage(queryDTO);
            return Result.success(page);
        } catch (Exception e) {
            log.error("分页查询算法模型失败", e);
            return Result.error("分页查询算法模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Query algorithm model details by ID")
    @GetMapping("/{id}")
    public Result<AlgorithmModel> getModelById(@ApiParam("模型ID") @PathVariable Long id) {
        try {
            AlgorithmModel model = algorithmModelService.getModelById(id);
            if (model == null) {
                return Result.error("模型不存在");
            }
            return Result.success(model);
        } catch (Exception e) {
            log.error("查询算法模型详情失败", e);
            return Result.error("查询算法模型详情失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Create algorithm model")
    @PostMapping("/create")
    public Result<AlgorithmModel> createModel(@Valid @RequestBody AlgorithmModelCreateDTO createDTO) {
        try {
            AlgorithmModel model = algorithmModelService.createModel(createDTO);
            return Result.success(model);
        } catch (Exception e) {
            log.error("创建算法模型失败", e);
            return Result.error("创建算法模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Update algorithm model")
    @PostMapping("/update")
    public Result<AlgorithmModel> updateModel(@Valid @RequestBody AlgorithmModelUpdateDTO updateDTO) {
        try {
            AlgorithmModel model = algorithmModelService.updateModel(updateDTO);
            return Result.success(model);
        } catch (Exception e) {
            log.error("更新算法模型失败", e);
            return Result.error("更新算法模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Delete algorithm model")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteModel(@ApiParam("模型ID") @PathVariable Long id) {
        try {
            boolean success = algorithmModelService.deleteModel(id);
            return Result.success(success);
        } catch (Exception e) {
            log.error("删除算法模型失败", e);
            return Result.error("删除算法模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Batch delete algorithm models")
    @DeleteMapping("/batch")
    public Result<Boolean> batchDeleteModel(@RequestBody List<Long> ids) {
        try {
            boolean success = algorithmModelService.batchDeleteModel(ids);
            return Result.success(success);
        } catch (Exception e) {
            log.error("批量删除算法模型失败", e);
            return Result.error("批量删除算法模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Query model list by algorithm ID")
    @GetMapping("/algorithm/{algorithmId}")
    public Result<List<AlgorithmModel>> getModelsByAlgorithmId(@ApiParam("算法ID") @PathVariable Long algorithmId) {
        try {
            List<AlgorithmModel> models = algorithmModelService.getModelsByAlgorithmId(algorithmId);
            return Result.success(models);
        } catch (Exception e) {
            log.error("根据算法ID查询模型列表失败", e);
            return Result.error("根据算法ID查询模型列表失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Query model list by training task ID")
    @GetMapping("/training/{trainingId}")
    public Result<List<AlgorithmModel>> getModelsByTrainingId(@ApiParam("训练任务ID") @PathVariable Long trainingId) {
        try {
            List<AlgorithmModel> models = algorithmModelService.getModelsByTrainingId(trainingId);
            return Result.success(models);
        } catch (Exception e) {
            log.error("根据训练任务ID查询模型列表失败", e);
            return Result.error("根据训练任务ID查询模型列表失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Query model list by status")
    @GetMapping("/status/{status}")
    public Result<List<AlgorithmModel>> getModelsByStatus(@ApiParam("状态") @PathVariable String status) {
        try {
            List<AlgorithmModel> models = algorithmModelService.getModelsByStatus(status);
            return Result.success(models);
        } catch (Exception e) {
            log.error("根据状态查询模型列表失败", e);
            return Result.error("根据状态查询模型列表失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Publish model")
    @PostMapping("/publish/{id}")
    public Result<Boolean> publishModel(@ApiParam("模型ID") @PathVariable Long id) {
        try {
            boolean success = algorithmModelService.publishModel(id);
            return Result.success(success);
        } catch (Exception e) {
            log.error("发布模型失败", e);
            return Result.error("发布模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Unpublish model")
    @PostMapping("/unpublish/{id}")
    public Result<Boolean> unpublishModel(@ApiParam("模型ID") @PathVariable Long id) {
        try {
            boolean success = algorithmModelService.unpublishModel(id);
            return Result.success(success);
        } catch (Exception e) {
            log.error("撤销发布模型失败", e);
            return Result.error("撤销发布模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Batch publish models")
    @PostMapping("/batch-publish")
    public Result<Boolean> batchPublishModel(@RequestBody List<Long> ids) {
        try {
            boolean success = algorithmModelService.batchPublishModel(ids);
            return Result.success(success);
        } catch (Exception e) {
            log.error("批量发布模型失败", e);
            return Result.error("批量发布模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Download model")
    @GetMapping("/download/{id}")
    public Result<String> downloadModel(@ApiParam("模型ID") @PathVariable Long id) {
        try {
            String filePath = algorithmModelService.downloadModel(id);
            return Result.success(filePath);
        } catch (Exception e) {
            log.error("下载模型失败", e);
            return Result.error("下载模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Deploy model")
    @PostMapping("/deploy/{id}")
    public Result<Boolean> deployModel(@ApiParam("模型ID") @PathVariable Long id) {
        try {
            boolean success = algorithmModelService.deployModel(id);
            return Result.success(success);
        } catch (Exception e) {
            log.error("部署模型失败", e);
            return Result.error("部署模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Get model statistics")
    @GetMapping("/statistics")
    public Result<AlgorithmModelStatisticsDTO> getStatistics() {
        try {
            AlgorithmModelStatisticsDTO statistics = algorithmModelService.getStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取模型统计信息失败", e);
            return Result.error("获取模型统计信息失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Check if model name and version exist")
    @GetMapping("/check-name-version")
    public Result<Boolean> checkModelNameAndVersion(
            @ApiParam("模型名称") @RequestParam String modelName,
            @ApiParam("模型版本") @RequestParam Integer version,
            @ApiParam("排除的ID") @RequestParam(required = false) Long excludeId) {
        try {
            boolean exists = algorithmModelService.checkModelNameAndVersion(modelName, version, excludeId);
            return Result.success(exists);
        } catch (Exception e) {
            log.error("检查模型名称和版本失败", e);
            return Result.error("检查模型名称和版本失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Query model by algorithm ID and version")
    @GetMapping("/algorithm/{algorithmId}/version/{version}")
    public Result<AlgorithmModel> getModelByAlgorithmIdAndVersion(
            @ApiParam("算法ID") @PathVariable Long algorithmId,
            @ApiParam("版本") @PathVariable Integer version) {
        try {
            AlgorithmModel model = algorithmModelService.getModelByAlgorithmIdAndVersion(algorithmId, version);
            return Result.success(model);
        } catch (Exception e) {
            log.error("根据算法ID和版本查询模型失败", e);
            return Result.error("根据算法ID和版本查询模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Get latest model version by algorithm ID")
    @GetMapping("/algorithm/{algorithmId}/latest")
    public Result<AlgorithmModel> getLatestModelByAlgorithmId(@ApiParam("算法ID") @PathVariable Long algorithmId) {
        try {
            AlgorithmModel model = algorithmModelService.getLatestModelByAlgorithmId(algorithmId);
            return Result.success(model);
        } catch (Exception e) {
            log.error("获取算法下最新版本的模型失败", e);
            return Result.error("获取算法下最新版本的模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Query popular models")
    @GetMapping("/popular")
    public Result<List<AlgorithmModel>> getPopularModels(@ApiParam("限制数量") @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<AlgorithmModel> models = algorithmModelService.getPopularModels(limit);
            return Result.success(models);
        } catch (Exception e) {
            log.error("查询热门模型失败", e);
            return Result.error("查询热门模型失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Count models by creator")
    @GetMapping("/count/creator/{createdBy}")
    public Result<Long> countModelsByCreatedBy(@ApiParam("创建人ID") @PathVariable Long createdBy) {
        try {
            Long count = algorithmModelService.countModelsByCreatedBy(createdBy);
            return Result.success(count);
        } catch (Exception e) {
            log.error("根据创建人查询模型数量失败", e);
            return Result.error("根据创建人查询模型数量失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "Get total algorithm model size")
    @GetMapping("/total-size")
    public Result<Long> getTotalModelSize() {
        try {
            Long totalSize = algorithmModelService.getTotalModelSize();
            return Result.success(totalSize);
        } catch (Exception e) {
            log.error("获取算法模型的总大小失败", e);
            return Result.error("获取算法模型的总大小失败：" + e.getMessage());
        }
    }
} 