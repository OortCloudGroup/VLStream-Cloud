/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.domain.AlgorithmTraining;
import com.ruoyi.vlstream.service.IVlsAlgorithmTrainingService;
import com.ruoyi.vlstream.service.SshService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Algorithm training routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
@RequestMapping("/vlsAlgorithmTraining")
public class VlsAlgorithmTrainingController extends VlsControllerSupport {

    private final IVlsAlgorithmTrainingService trainingService;
    private final SshService sshService;
    private final VlsSshProperties sshProperties;

    /** Preserve the controller's original unit-test construction surface. */
    public VlsAlgorithmTrainingController(IVlsAlgorithmTrainingService trainingService) {
        this.trainingService = trainingService;
        this.sshService = null;
        this.sshProperties = null;
    }

    @GetMapping("/page")
    public BladeResult<BladePage<AlgorithmTraining>> getTrainingPage(@RequestParam(required = false) Long current,
                                                                     @RequestParam(required = false) Long size,
                                                                     @RequestParam(required = false) String taskName,
                                                                     @RequestParam(required = false) String trainStatus,
                                                                     @RequestParam(required = false) String createdTimeBegin,
                                                                     @RequestParam(required = false) String createdTimeEnd) {
        return BladeResult.success(trainingService.getTrainingPage(current, size, taskName, trainStatus, createdTimeBegin, createdTimeEnd));
    }

    @GetMapping("/{id}")
    public BladeResult<AlgorithmTraining> getTrainingById(@PathVariable Long id) {
        AlgorithmTraining training = trainingService.getTrainingById(id);
        return training == null ? BladeResult.<AlgorithmTraining>fail("Training task does not exist") : BladeResult.success(training);
    }

    @PostMapping
    public BladeResult<AlgorithmTraining> createTraining(@RequestBody AlgorithmTraining training) {
        try {
            return BladeResult.success(trainingService.createTraining(training));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public BladeResult<AlgorithmTraining> updateTraining(@PathVariable Long id, @RequestBody AlgorithmTraining training) {
        try {
            return BladeResult.success(trainingService.updateTraining(id, training));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public BladeResult<Boolean> deleteTraining(@PathVariable Long id) {
        return operationResult(trainingService.deleteTraining(id), "Training task was not deleted");
    }

    @PostMapping("/{id}/start")
    public BladeResult<Map<String, Object>> startTraining(@PathVariable Long id,
                                                         @RequestParam(required = false, defaultValue = "10") Integer epochs,
                                                         @RequestParam(required = false) Long datasetId,
                                                         @RequestParam(required = false, defaultValue = "16") Integer batchSize,
                                                         @RequestParam(required = false) Integer imgSize,
                                                         @RequestParam(required = false) String extraParams) {
        try {
            return BladeResult.success(trainingService.startTraining(id, epochs, datasetId, batchSize, imgSize, extraParams));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/{id}/stop")
    public BladeResult<Boolean> stopTraining(@PathVariable Long id) {
        return BladeResult.success(trainingService.stopTraining(id));
    }

    @GetMapping("/{id}/logs")
    public BladeResult<Map<String, Object>> getTrainingLogs(@PathVariable Long id,
                                                           @RequestParam(required = false) String logPath,
                                                           @RequestParam(required = false, defaultValue = "200") Integer lines) {
        try {
            return BladeResult.success(trainingService.getTrainingLogs(id, logPath, lines));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/{id}/status")
    public BladeResult<Map<String, Object>> getTrainingStatus(@PathVariable Long id,
                                                             @RequestParam(required = false) String logPath) {
        try {
            return BladeResult.success(trainingService.getTrainingStatus(id, logPath));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteTraining(@RequestBody List<Long> ids) {
        return operationResult(trainingService.deleteTrainings(ids), "No training tasks were deleted");
    }

    @PostMapping("/{id}/convert-model")
    public BladeResult<Map<String, Object>> convertModel(@PathVariable Long id) {
        try {
            return BladeResult.success(trainingService.convertModel(id));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/download-model")
    public ResponseEntity<byte[]> downloadModel(@RequestParam String id, @RequestParam String type) {
        return trainingService.downloadModel(id, type);
    }

    /** Return one training task through the SpringBlade detail route. */
    @GetMapping("/detail")
    public BladeResult<AlgorithmTraining> detail(@RequestParam Long id) {
        return getTrainingById(id);
    }

    /** Return a training page through the SpringBlade list route. */
    @GetMapping("/list")
    public BladeResult<BladePage<AlgorithmTraining>> list(@RequestParam(required = false) Long current,
                                                          @RequestParam(required = false) Long size,
                                                          @RequestParam(required = false) String taskName,
                                                          @RequestParam(required = false) String trainStatus,
                                                          @RequestParam(required = false) String createdTimeBegin,
                                                          @RequestParam(required = false) String createdTimeEnd) {
        return getTrainingPage(current, size, taskName, trainStatus, createdTimeBegin, createdTimeEnd);
    }

    /** Create a training task through the SpringBlade save route. */
    @PostMapping("/save")
    public BladeResult<AlgorithmTraining> save(@RequestBody AlgorithmTraining training) {
        return createTraining(training);
    }

    /** Update a training task through the SpringBlade update route. */
    @PostMapping("/update")
    public BladeResult<AlgorithmTraining> update(@RequestBody AlgorithmTraining training) {
        if (training == null || training.getId() == null) {
            return BladeResult.fail("Training task ID is required");
        }
        return updateTraining(training.getId(), training);
    }

    /** Insert or update a training task through the SpringBlade submit route. */
    @PostMapping("/submit")
    public BladeResult<AlgorithmTraining> submit(@RequestBody AlgorithmTraining training) {
        return training != null && training.getId() != null
            ? updateTraining(training.getId(), training)
            : createTraining(training);
    }

    /** Delete training tasks by comma-separated IDs. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(trainingService.deleteTrainings(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export the actual filtered training-task rows. */
    @GetMapping("/export-vlsAlgorithmTraining")
    public void exportVlsAlgorithmTraining(@RequestParam(required = false) String taskName,
                                           @RequestParam(required = false) String trainStatus,
                                           @RequestParam(required = false) String createdTimeBegin,
                                           @RequestParam(required = false) String createdTimeEnd,
                                           HttpServletResponse response) {
        BladePage<AlgorithmTraining> page = trainingService.getTrainingPage(Long.valueOf(1L),
            Long.valueOf(Integer.MAX_VALUE), taskName, trainStatus, createdTimeBegin, createdTimeEnd);
        ExcelUtil.exportExcel(page.getRecords(), "Algorithm Training Tasks", AlgorithmTraining.class, response);
    }

    /** Persist a requested status value after checking that the training task exists. */
    @PutMapping("/{id}/status")
    public BladeResult<AlgorithmTraining> updateTrainingStatus(@PathVariable Long id,
                                                               @RequestBody Map<String, String> body) {
        AlgorithmTraining existing = trainingService.getTrainingById(id);
        if (existing == null) {
            return BladeResult.fail("Training task does not exist");
        }
        String status = body == null ? null : body.get("trainStatus");
        if (status == null || status.trim().isEmpty()) {
            return BladeResult.fail("trainStatus is required");
        }
        AlgorithmTraining update = new AlgorithmTraining();
        update.setId(id);
        update.setTrainStatus(status.trim());
        return updateTraining(id, update);
    }

    /** Execute the main-source conda diagnostic command through the real SSH service. */
    @GetMapping("/diagnose-conda")
    public BladeResult<String> diagnoseConda() {
        String command = "echo '=== Environment Diagnosis ===' && echo 'PATH: '$PATH && "
            + "which conda 2>/dev/null || echo 'conda not in PATH'; "
            + "find /home /opt /usr -name conda -type f 2>/dev/null | head -5; "
            + "which yolo 2>/dev/null || echo 'yolo not in PATH'; "
            + "which python 2>/dev/null || which python3 2>/dev/null || echo 'python not found'";
        try {
            SshService.SshExecutionResult result = sshService.executeCommand(sshProperties.getHost(),
                sshProperties.getPort(), sshProperties.getUsername(), sshProperties.getPassword(), command);
            return result.isSuccess() ? BladeResult.success(result.getOutput())
                : BladeResult.<String>fail("Conda diagnosis failed: " + result.getErrorMsg());
        } catch (RuntimeException ex) {
            return BladeResult.fail("Conda diagnosis failed: " + ex.getMessage());
        }
    }
}
