package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.ContainerInstance;
import com.ruoyi.vlstream.service.IVlsContainerInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Container instance routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsContainerInstance")
public class VlsContainerInstanceController {

    private final IVlsContainerInstanceService containerInstanceService;

    /**
     * Return a page of frontend-compatible container instances.
     */
    @GetMapping("/page")
    public BladeResult<BladePage<ContainerInstance>> getContainerInstancePage(@RequestParam(required = false) Long current,
                                                                              @RequestParam(required = false) Long size,
                                                                              @RequestParam(required = false) String name,
                                                                              @RequestParam(required = false) String status,
                                                                              @RequestParam(required = false) Long algorithmId,
                                                                              @RequestParam(required = false) String healthStatus,
                                                                              @RequestParam(required = false) String startTime,
                                                                              @RequestParam(required = false) String endTime) {
        return BladeResult.success(containerInstanceService.getContainerInstancePage(current, size, name, status, algorithmId, healthStatus, startTime, endTime));
    }

    /**
     * Return one container instance by ID.
     */
    @GetMapping("/{id}")
    public BladeResult<ContainerInstance> getContainerInstanceById(@PathVariable Long id) {
        ContainerInstance instance = containerInstanceService.getContainerInstanceById(id);
        return instance == null ? BladeResult.<ContainerInstance>fail("Container instance does not exist") : BladeResult.success(instance);
    }

    /**
     * Create a container instance from the frontend create form.
     */
    @PostMapping("")
    public BladeResult<ContainerInstance> createContainerInstance(@RequestBody ContainerInstance instance) {
        try {
            return BladeResult.success(containerInstanceService.createContainerInstance(instance));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Update a container instance from the frontend edit form.
     */
    @PutMapping("")
    public BladeResult<ContainerInstance> updateContainerInstance(@RequestBody ContainerInstance instance) {
        try {
            return BladeResult.success(containerInstanceService.updateContainerInstance(instance));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Delete a stopped container instance.
     */
    @DeleteMapping("/{id}")
    public BladeResult<Boolean> deleteContainerInstance(@PathVariable Long id) {
        try {
            return BladeResult.success(containerInstanceService.deleteContainerInstance(id));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Batch delete stopped container instances.
     */
    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteContainerInstances(@RequestBody Map<String, Object> body) {
        try {
            return BladeResult.success(containerInstanceService.batchDeleteContainerInstances(toLongList(body == null ? null : body.get("ids"))));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Start a container instance through the compatibility state transition.
     */
    @PostMapping("/{id}/start")
    public BladeResult<Boolean> startContainerInstance(@PathVariable Long id) {
        try {
            return BladeResult.success(containerInstanceService.startContainerInstance(id));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Stop a container instance through the compatibility state transition.
     */
    @PostMapping("/{id}/stop")
    public BladeResult<Boolean> stopContainerInstance(@PathVariable Long id) {
        try {
            return BladeResult.success(containerInstanceService.stopContainerInstance(id));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Restart a container instance through the compatibility state transition.
     */
    @PostMapping("/{id}/restart")
    public BladeResult<Boolean> restartContainerInstance(@PathVariable Long id) {
        try {
            return BladeResult.success(containerInstanceService.restartContainerInstance(id));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Return aggregate container instance statistics.
     */
    @GetMapping("/statistics")
    public BladeResult<Map<String, Object>> getContainerInstanceStatistics() {
        return BladeResult.success(containerInstanceService.getContainerInstanceStatistics());
    }

    /**
     * Return running container instances.
     */
    @GetMapping("/running")
    public BladeResult<List<ContainerInstance>> getRunningContainerInstances() {
        return BladeResult.success(containerInstanceService.getRunningContainerInstances());
    }

    /**
     * Return error-state container instances.
     */
    @GetMapping("/error")
    public BladeResult<List<ContainerInstance>> getErrorContainerInstances() {
        return BladeResult.success(containerInstanceService.getErrorContainerInstances());
    }

    /**
     * Return unhealthy container instances.
     */
    @GetMapping("/unhealthy")
    public BladeResult<List<ContainerInstance>> getUnhealthyContainerInstances() {
        return BladeResult.success(containerInstanceService.getUnhealthyContainerInstances());
    }

    /**
     * Return whether a container instance name already exists.
     */
    @GetMapping("/check-name")
    public BladeResult<Boolean> checkContainerInstanceName(@RequestParam String name,
                                                           @RequestParam(required = false) Long excludeId) {
        return BladeResult.success(containerInstanceService.checkContainerInstanceName(name, excludeId));
    }

    /**
     * Return container instances for a given algorithm.
     */
    @GetMapping("/algorithm/{algorithmId}")
    public BladeResult<List<ContainerInstance>> getContainerInstancesByAlgorithm(@PathVariable Long algorithmId) {
        return BladeResult.success(containerInstanceService.getContainerInstancesByAlgorithm(algorithmId));
    }

    /**
     * Update monitoring usage values for a container instance.
     */
    @PutMapping("/{id}/monitoring")
    public BladeResult<Boolean> updateContainerInstanceMonitoring(@PathVariable Long id,
                                                                  @RequestBody Map<String, Object> body) {
        return BladeResult.success(containerInstanceService.updateContainerInstanceMonitoring(
            id,
            toBigDecimal(body == null ? null : body.get("cpuUsage")),
            toBigDecimal(body == null ? null : body.get("memoryUsage")),
            toBigDecimal(body == null ? null : body.get("gpuUsage"))
        ));
    }

    private List<Long> toLongList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<Long>();
        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                Long id = toLong(item);
                if (id != null) {
                    ids.add(id);
                }
            }
            return ids;
        }
        if (value instanceof String) {
            String[] parts = ((String) value).split(",");
            for (String part : parts) {
                Long id = toLong(part);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.valueOf(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            try {
                return BigDecimal.valueOf(Double.parseDouble(((String) value).trim()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
