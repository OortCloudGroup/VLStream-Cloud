/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserOpinionCompatService {

    private final JdbcTemplate jdbc;

    public UserOpinionCompatService(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /**
     * Ensure the small compatibility table exists even when optional seed initialization is disabled.
     */
    @PostConstruct
    public void initializeSchema() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS oort_user_opinion ("
            + "id char(36) NOT NULL, "
            + "user_id varchar(64) NOT NULL, "
            + "content varchar(200) NOT NULL, "
            + "is_open tinyint(1) NOT NULL DEFAULT 0, "
            + "created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), "
            + "updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), "
            + "PRIMARY KEY (id), "
            + "KEY idx_oort_user_opinion_user_id (user_id), "
            + "KEY idx_oort_user_opinion_created_at (created_at)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
    }

    /**
     * Query one page of phrases owned by the authenticated local user.
     */
    public Map<String, Object> list(String userId, int isOpen, int page, int pageSize) {
        int safePage = Math.max(page, 0);
        int safePageSize = Math.min(Math.max(pageSize, 1), 999);
        int offset = safePage * safePageSize;
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(1) FROM oort_user_opinion WHERE user_id = ? AND is_open = ?",
            Integer.class, userId, isOpen);
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT id, content, is_open, created_at, updated_at "
                + "FROM oort_user_opinion WHERE user_id = ? AND is_open = ? "
                + "ORDER BY created_at DESC LIMIT ? OFFSET ?",
            userId, isOpen, safePageSize, offset);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("list", rows);
        data.put("count", count == null ? 0 : count);
        data.put("total", count == null ? 0 : count);
        return data;
    }

    /**
     * Save a phrase for the authenticated local user and return its identifier.
     */
    @Transactional
    public String save(String userId, String content, int isOpen) {
        String id = UUID.randomUUID().toString();
        jdbc.update(
            "INSERT INTO oort_user_opinion (id, user_id, content, is_open, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, NOW(3), NOW(3))",
            id, userId, content, isOpen);
        return id;
    }

    /**
     * Delete a phrase only when it belongs to the authenticated local user.
     */
    @Transactional
    public boolean delete(String userId, String id) {
        return jdbc.update("DELETE FROM oort_user_opinion WHERE id = ? AND user_id = ?", id, userId) > 0;
    }
}
