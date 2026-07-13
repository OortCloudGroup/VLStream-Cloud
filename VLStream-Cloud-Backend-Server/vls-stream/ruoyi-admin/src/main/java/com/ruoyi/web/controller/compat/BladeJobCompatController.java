package com.ruoyi.web.controller.compat;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class BladeJobCompatController {

    private static final String DEFAULT_EXECUTOR_APP = "xxl-job-executor";
    private static final String DEFAULT_EXECUTOR_URL = "http://127.0.0.1:18080";

    /**
     * Return the local workflow backend as the legacy job application option.
     */
    @GetMapping("/blade-job/job-server/select")
    public BladeResult<List<Map<String, Object>>> jobServerSelect() {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        rows.add(defaultJobServer());
        return BladeResult.success(rows);
    }

    /**
     * Return an empty SpringBlade page for the legacy job table.
     */
    @GetMapping("/blade-job/job-info/list")
    public BladeResult<BladePage<Map<String, Object>>> jobInfoList(HttpServletRequest request) {
        int current = parsePositiveInt(firstNonBlank(request.getParameter("current"), request.getParameter("pageNum")), 1);
        int size = parsePositiveInt(firstNonBlank(request.getParameter("size"), request.getParameter("pageSize")), 10);
        return BladeResult.success(BladePage.of(Collections.<Map<String, Object>>emptyList(), 0, size, current));
    }

    /**
     * Return a minimal detail payload so edit/view dialogs can open without an external job service.
     */
    @GetMapping("/blade-job/job-info/detail")
    public BladeResult<Map<String, Object>> jobInfoDetail(@RequestParam(value = "id", required = false) String id) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", id);
        row.put("jobServerId", 1);
        row.put("jobServerName", DEFAULT_EXECUTOR_APP);
        row.put("jobAppName", DEFAULT_EXECUTOR_APP);
        return BladeResult.success(row);
    }

    /**
     * Acknowledge legacy job removals because scheduled-job persistence is not local to this migration.
     */
    @PostMapping("/blade-job/job-info/remove")
    public BladeResult<Map<String, Object>> jobInfoRemove(@RequestParam(value = "ids", required = false) String ids) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("ids", ids);
        data.put("removed", true);
        return BladeResult.success(data);
    }

    /**
     * Echo submitted job data for compatibility with legacy add/update flows.
     */
    @PostMapping("/blade-job/job-info/submit")
    public BladeResult<Map<String, Object>> jobInfoSubmit(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> data = safeBody(body);
        data.put("submitted", true);
        return BladeResult.success(data);
    }

    /**
     * Acknowledge enable-state changes for legacy job rows.
     */
    @PostMapping("/blade-job/job-info/change")
    public BladeResult<Map<String, Object>> jobInfoChange(@RequestParam(value = "id", required = false) String id,
                                                          @RequestParam(value = "enable", required = false) Integer enable) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", id);
        data.put("enable", enable);
        data.put("changed", true);
        return BladeResult.success(data);
    }

    /**
     * Acknowledge manual job execution requests without calling an external XXL-JOB admin.
     */
    @PostMapping("/blade-job/job-info/run")
    public BladeResult<Map<String, Object>> jobInfoRun(@RequestParam(value = "id", required = false) String id) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", id);
        data.put("run", true);
        return BladeResult.success(data);
    }

    /**
     * Acknowledge sync requests and return the submitted payload.
     */
    @PostMapping("/blade-job/job-info/sync")
    public BladeResult<Map<String, Object>> jobInfoSync(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> data = safeBody(body);
        data.put("synced", true);
        return BladeResult.success(data);
    }

    /**
     * Build the single local executor option used by the frontend job form.
     */
    private static Map<String, Object> defaultJobServer() {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", 1);
        row.put("jobServerId", 1);
        row.put("jobServerName", DEFAULT_EXECUTOR_APP);
        row.put("jobServerUrl", DEFAULT_EXECUTOR_URL);
        row.put("jobAppName", DEFAULT_EXECUTOR_APP);
        row.put("jobAppPassword", "xxl-job");
        row.put("jobRemark", "VLStream workflow backend executor");
        return row;
    }

    /**
     * Copy a nullable request body into a mutable map.
     */
    private static Map<String, Object> safeBody(Map<String, Object> body) {
        return body == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(body);
    }

    /**
     * Parse positive integer query values with a safe fallback.
     */
    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * Return the first non-empty string from two candidates.
     */
    private static String firstNonBlank(String first, String second) {
        String firstValue = trimToNull(first);
        return firstValue == null ? trimToNull(second) : firstValue;
    }

    /**
     * Trim a string and normalize blanks to null.
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
