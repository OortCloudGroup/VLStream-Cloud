/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.interceptor.TokenHeaderResolver;
import com.ruoyi.vlstream.test.compat.BladeResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class UserOpinionCompatController {

    private final BladeTokenUserStore tokenUserStore;
    private final UserOpinionCompatService opinionService;

    public UserOpinionCompatController(BladeTokenUserStore tokenUserStore,
                                       UserOpinionCompatService opinionService) {
        this.tokenUserStore = tokenUserStore;
        this.opinionService = opinionService;
    }

    /**
     * Return phrases belonging to the authenticated local user.
     */
    @PostMapping("/myOpinionList")
    public BladeResult<Map<String, Object>> list(HttpServletRequest request,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        SysUser user = currentUser(request);
        if (user == null) {
            return BladeResult.fail("认证失败，无法访问系统资源");
        }
        int isOpen = intValue(body, "is_open", 0);
        int page = intValue(body, "page", 0);
        int pageSize = intValue(body, "pagesize", 20);
        return BladeResult.success(opinionService.list(String.valueOf(user.getUserId()), isOpen, page, pageSize));
    }

    /**
     * Save a phrase for the authenticated local user; request-body tokens are intentionally ignored.
     */
    @PostMapping("/myOpinionSave")
    public BladeResult<Map<String, Object>> save(HttpServletRequest request,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        SysUser user = currentUser(request);
        if (user == null) {
            return BladeResult.fail("认证失败，无法访问系统资源");
        }
        String content = stringValue(body, "content");
        if (content == null || content.length() > 200) {
            return BladeResult.fail("常用语不能为空且不能超过200个字符");
        }
        String id = opinionService.save(String.valueOf(user.getUserId()), content, intValue(body, "is_open", 0));
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", id);
        return BladeResult.success(data);
    }

    /**
     * Delete a phrase only within the authenticated local user's data scope.
     */
    @PostMapping("/myOpinionDel")
    public BladeResult<Boolean> delete(HttpServletRequest request,
                                       @RequestBody(required = false) Map<String, Object> body) {
        SysUser user = currentUser(request);
        if (user == null) {
            return BladeResult.fail("认证失败，无法访问系统资源");
        }
        String id = stringValue(body, "id");
        if (id == null) {
            return BladeResult.fail("常用语ID不能为空");
        }
        return BladeResult.success(opinionService.delete(String.valueOf(user.getUserId()), id));
    }

    /**
     * Resolve the local user exclusively from the authenticated request header.
     */
    private SysUser currentUser(HttpServletRequest request) {
        String token = TokenHeaderResolver.resolve(request);
        return token == null ? null : tokenUserStore.get(token);
    }

    /**
     * Read and normalize a required string request value.
     */
    private static String stringValue(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        String value = String.valueOf(body.get(key)).trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Read an integer request value while retaining a safe default for malformed legacy input.
     */
    private static int intValue(Map<String, Object> body, String key, int defaultValue) {
        if (body == null || body.get(key) == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(body.get(key)));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
