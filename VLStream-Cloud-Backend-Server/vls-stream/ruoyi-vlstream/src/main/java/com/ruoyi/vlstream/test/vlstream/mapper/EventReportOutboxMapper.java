/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventReportOutbox;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/** Cross-tenant infrastructure access for the event outbox. */
@InterceptorIgnore(tenantLine = "true")
public interface EventReportOutboxMapper extends BaseMapper<EventReportOutbox> {

	@Update("UPDATE vls_event_report_outbox SET status = 'PROCESSING', locked_by = #{workerId}, "
		+ "locked_at = #{now}, update_time = #{now} WHERE id IN (SELECT candidate.id FROM "
		+ "(SELECT id FROM vls_event_report_outbox WHERE "
		+ "((status IN ('PENDING','RETRY') AND next_retry_time <= #{now}) "
		+ "OR (status = 'PROCESSING' AND locked_at <= #{staleBefore})) "
		+ "ORDER BY next_retry_time, id LIMIT #{limit}) candidate) AND "
		+ "((status IN ('PENDING','RETRY') AND next_retry_time <= #{now}) "
		+ "OR (status = 'PROCESSING' AND locked_at <= #{staleBefore}))")
	int claimBatch(@Param("workerId") String workerId, @Param("now") Date now,
				   @Param("staleBefore") Date staleBefore, @Param("limit") int limit);

	@Select("SELECT * FROM vls_event_report_outbox WHERE status = 'PROCESSING' "
		+ "AND locked_by = #{workerId} ORDER BY locked_at, id")
	List<EventReportOutbox> selectClaimed(@Param("workerId") String workerId);

	@Update("UPDATE vls_event_report_outbox SET status = 'SUCCESS', reported_at = #{now}, "
		+ "last_error = NULL, locked_by = NULL, locked_at = NULL, update_time = #{now} "
		+ "WHERE id = #{id} AND tenant_id = #{tenantId} AND status = 'PROCESSING' "
		+ "AND locked_by = #{workerId}")
	int markSuccess(@Param("id") Long id, @Param("tenantId") String tenantId,
				@Param("workerId") String workerId, @Param("now") Date now);

	@Update("UPDATE vls_event_report_outbox SET status = #{status}, retry_count = #{retryCount}, "
		+ "next_retry_time = #{nextRetryTime}, last_error = #{lastError}, locked_by = NULL, "
		+ "locked_at = NULL, update_time = #{now} WHERE id = #{id} AND tenant_id = #{tenantId} "
		+ "AND status = 'PROCESSING' AND locked_by = #{workerId}")
	int markFailure(@Param("id") Long id, @Param("tenantId") String tenantId,
				@Param("workerId") String workerId, @Param("status") String status,
				@Param("retryCount") int retryCount, @Param("nextRetryTime") Date nextRetryTime,
				@Param("lastError") String lastError, @Param("now") Date now);
}
