/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmReviewTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

public interface LlmReviewTaskMapper extends BaseMapper<LlmReviewTask> {

	@Insert("INSERT IGNORE INTO vls_llm_review_task (id,tenant_id,source_message_id,device_event_id,device_id,"
		+ "algorithm_id,provider_id,media_id,event_payload_json,config_snapshot_json,review_status,attempt_count,"
		+ "next_retry_time,create_time,update_time) VALUES (#{id},#{tenantId},#{sourceMessageId},#{deviceEventId},"
		+ "#{deviceId},#{algorithmId},#{providerId},#{mediaId},#{eventPayloadJson},#{configSnapshotJson},"
		+ "#{reviewStatus},#{attemptCount},#{nextRetryTime},#{createTime},#{updateTime})")
	int insertIgnore(LlmReviewTask task);

	@InterceptorIgnore(tenantLine = "true")
	@Update("UPDATE vls_llm_review_task SET review_status='PROCESSING', locked_by=#{workerId}, "
		+ "locked_at=#{now}, update_time=#{now} WHERE id IN (SELECT c.id FROM (SELECT id FROM "
		+ "vls_llm_review_task WHERE ((review_status IN ('PENDING','RETRY') AND next_retry_time<=#{now}) "
		+ "OR (review_status='PROCESSING' AND locked_at<=#{staleBefore})) ORDER BY next_retry_time,id "
		+ "LIMIT #{limit}) c) AND ((review_status IN ('PENDING','RETRY') AND next_retry_time<=#{now}) "
		+ "OR (review_status='PROCESSING' AND locked_at<=#{staleBefore}))")
	int claimBatch(@Param("workerId") String workerId, @Param("now") Date now,
		@Param("staleBefore") Date staleBefore, @Param("limit") int limit);

	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT * FROM vls_llm_review_task WHERE review_status='PROCESSING' AND locked_by=#{workerId} ORDER BY locked_at,id")
	List<LlmReviewTask> selectClaimed(@Param("workerId") String workerId);

	@Update("UPDATE vls_llm_review_task SET review_status=#{status}, attempt_count=#{attemptCount}, "
		+ "next_retry_time=#{nextRetryTime}, decision=#{decision}, confidence=#{confidence}, reason=#{reason}, "
		+ "raw_response=#{rawResponse}, last_error=#{lastError}, formal_event_id=#{formalEventId}, "
		+ "locked_by=NULL, locked_at=NULL, update_time=#{now} WHERE id=#{id} AND tenant_id=#{tenantId} "
		+ "AND review_status='PROCESSING' AND locked_by=#{workerId}")
	int finishAttempt(@Param("id") Long id, @Param("tenantId") String tenantId,
		@Param("workerId") String workerId, @Param("status") String status,
		@Param("attemptCount") int attemptCount, @Param("nextRetryTime") Date nextRetryTime,
		@Param("decision") String decision, @Param("confidence") java.math.BigDecimal confidence,
		@Param("reason") String reason, @Param("rawResponse") String rawResponse,
		@Param("lastError") String lastError, @Param("formalEventId") String formalEventId,
		@Param("now") Date now);
}
