package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.compat.SingleTenant;
import com.ruoyi.vlstream.test.vlstream.config.VlsFirmwareProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.FirmwareDeployTaskMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareDeployTaskView;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.FirmwareDeployTask;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;

/** Persists monotonic OTA progress reported by the platform and hardware. */
@Service
@RequiredArgsConstructor
public class FirmwareDeployTaskService {

	private static final String SUCCESS = "SUCCESS";
	private static final String FAILED = "FAILED";
	private static final String CANCELLED = "CANCELLED";
	private static final String TIMED_OUT = "TIMED_OUT";
	private static final java.util.List<String> ACTIVE_STATUSES = Arrays.asList(
		"CREATED", "PUBLISHED", "ACCEPTED", "DOWNLOADING", "VERIFYING", "INSTALLING", "REBOOTING");
	private static final java.util.List<String> DOWNLOAD_STATUSES = Arrays.asList(
		"CREATED", "PUBLISHED", "ACCEPTED", "DOWNLOADING");
	private static final java.util.List<String> EXECUTION_STATUSES = Arrays.asList(
		"VERIFYING", "INSTALLING", "REBOOTING");

	private final FirmwareDeployTaskMapper mapper;
	private final VlsFirmwareProperties properties;

	public void create(FirmwareDeployTask task) {
		Date now = new Date();
		task.setTenantId(SingleTenant.DEFAULT_TENANT_ID);
		task.setStatus(1);
		task.setIsDeleted(0);
		task.setCreateTime(now);
		task.setUpdateTime(now);
		try {
			mapper.insert(task);
		} catch (DuplicateKeyException exception) {
			throw new ServiceException("该设备的 " + task.getTarget() + " OTA 任务仍在处理中");
		}
	}

	public FirmwareDeployTask getByRequestId(String requestId) {
		if (StringUtils.isBlank(requestId)) {
			return null;
		}
		return mapper.selectOne(new LambdaQueryWrapper<FirmwareDeployTask>()
			.eq(FirmwareDeployTask::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
			.eq(FirmwareDeployTask::getRequestId, requestId)
			.eq(FirmwareDeployTask::getIsDeleted, 0)
			.last("limit 1"));
	}

	public FirmwareDeployTask latestForDevice(Long deviceRowId) {
		return mapper.selectOne(new LambdaQueryWrapper<FirmwareDeployTask>()
			.eq(FirmwareDeployTask::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
			.eq(FirmwareDeployTask::getDeviceRowId, deviceRowId)
			.eq(FirmwareDeployTask::getIsDeleted, 0)
			.orderByDesc(FirmwareDeployTask::getCreateTime)
			.last("limit 1"));
	}

	public boolean hasActiveTask(Long deviceRowId, String target) {
		return mapper.selectCount(new LambdaQueryWrapper<FirmwareDeployTask>()
			.eq(FirmwareDeployTask::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
			.eq(FirmwareDeployTask::getDeviceRowId, deviceRowId)
			.eq(FirmwareDeployTask::getTarget, target)
			.in(FirmwareDeployTask::getDeployStatus, ACTIVE_STATUSES)
			.eq(FirmwareDeployTask::getIsDeleted, 0)) > 0;
	}

	public boolean isActiveStatus(String status) {
		return ACTIVE_STATUSES.contains(StringUtils.upperCase(StringUtils.trimToEmpty(status)));
	}

	public FirmwareDeployTaskView cancelActiveTask(Long deviceRowId, String requestId) {
		FirmwareDeployTask task = getByRequestId(requestId);
		if (task == null || !deviceRowId.equals(task.getDeviceRowId())) {
			throw new ServiceException("OTA 任务不存在或不属于该设备");
		}
		if (!isActiveStatus(task.getDeployStatus())) {
			throw new ServiceException("OTA 任务已结束，无需终止");
		}
		Date now = new Date();
		String reason = "管理员终止任务；仅解除平台任务锁，未向设备发送取消指令";
		int updated = mapper.update(null, new LambdaUpdateWrapper<FirmwareDeployTask>()
			.eq(FirmwareDeployTask::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
			.eq(FirmwareDeployTask::getRequestId, requestId)
			.in(FirmwareDeployTask::getDeployStatus, ACTIVE_STATUSES)
			.set(FirmwareDeployTask::getDeployStatus, CANCELLED)
			.set(FirmwareDeployTask::getFailureReason, reason)
			.set(FirmwareDeployTask::getCompletedAt, now)
			.set(FirmwareDeployTask::getUpdateTime, now));
		if (updated == 0) {
			throw new ServiceException("OTA 任务状态已变化，请刷新后重试");
		}
		task.setDeployStatus(CANCELLED);
		task.setFailureReason(reason);
		task.setCompletedAt(now);
		task.setUpdateTime(now);
		return toView(task);
	}

	public int expireStaleTasks() {
		return expireStaleTasks(null, null);
	}

	public int expireStaleTasksForDevice(Long deviceRowId, String target) {
		return expireStaleTasks(deviceRowId, target);
	}

	public void markPublished(String requestId) {
		mapper.update(null, new LambdaUpdateWrapper<FirmwareDeployTask>()
			.eq(FirmwareDeployTask::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
			.eq(FirmwareDeployTask::getRequestId, requestId)
			.eq(FirmwareDeployTask::getDeployStatus, "CREATED")
			.set(FirmwareDeployTask::getDeployStatus, "PUBLISHED")
			.set(FirmwareDeployTask::getPublishedAt, new Date())
			.set(FirmwareDeployTask::getUpdateTime, new Date()));
	}

	public void markPublishFailed(String requestId, String reason) {
		Date now = new Date();
		mapper.update(null, new LambdaUpdateWrapper<FirmwareDeployTask>()
			.eq(FirmwareDeployTask::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
			.eq(FirmwareDeployTask::getRequestId, requestId)
			.eq(FirmwareDeployTask::getDeployStatus, "CREATED")
			.set(FirmwareDeployTask::getDeployStatus, FAILED)
			.set(FirmwareDeployTask::getFailureReason, StringUtils.abbreviate(reason, 2000))
			.set(FirmwareDeployTask::getCompletedAt, now)
			.set(FirmwareDeployTask::getUpdateTime, now));
	}

	public boolean applyHardwareReply(String sourceMsgId, String requestId, String deviceId,
		String deviceModel, String target, String version, String fileSha256, String incomingStatus,
		String message, String rawPayload) {
		FirmwareDeployTask task = getByRequestId(requestId);
		if (task == null
			|| !StringUtils.equals(task.getMqttMessageId(), sourceMsgId)
			|| !StringUtils.equals(task.getDeviceId(), deviceId)
			|| !StringUtils.equals(task.getDeviceModel(), deviceModel)
			|| !StringUtils.equals(task.getTarget(), target)
			|| !StringUtils.equals(task.getTargetVersion(), version)) {
			return false;
		}
		String status = normalizeStatus(incomingStatus);
		if (status == null) {
			return false;
		}
		if (SUCCESS.equals(status)
			&& !StringUtils.equalsIgnoreCase(task.getSha256(), StringUtils.trimToEmpty(fileSha256))) {
			status = FAILED;
			message = "OTA package SHA-256 mismatch: expected=" + task.getSha256()
				+ ", actual=" + StringUtils.defaultIfBlank(fileSha256, "empty");
		}
		if (!canApply(task.getDeployStatus(), status)) {
			return true;
		}
		Date now = new Date();
		LambdaUpdateWrapper<FirmwareDeployTask> update = new LambdaUpdateWrapper<FirmwareDeployTask>()
			.eq(FirmwareDeployTask::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
			.eq(FirmwareDeployTask::getRequestId, requestId)
			.set(FirmwareDeployTask::getDeployStatus, status)
			.set(FirmwareDeployTask::getLastReplyAt, now)
			.set(FirmwareDeployTask::getReplyPayload, StringUtils.abbreviate(rawPayload, 8000))
			.set(FirmwareDeployTask::getFailureReason,
				FAILED.equals(status) ? StringUtils.abbreviate(message, 2000) : null)
			.set(FirmwareDeployTask::getUpdateTime, now);
		if (SUCCESS.equals(status) || FAILED.equals(status)) {
			update.set(FirmwareDeployTask::getCompletedAt, now);
		}
		if (FAILED.equals(status)) {
			update.ne(FirmwareDeployTask::getDeployStatus, SUCCESS);
		} else if (SUCCESS.equals(status)) {
			update.ne(FirmwareDeployTask::getDeployStatus, FAILED);
		} else {
			update.notIn(FirmwareDeployTask::getDeployStatus, SUCCESS, FAILED);
		}
		return mapper.update(null, update) > 0;
	}

	public FirmwareDeployTaskView toView(FirmwareDeployTask task) {
		if (task == null) {
			return null;
		}
		return FirmwareDeployTaskView.builder()
			.requestId(task.getRequestId())
			.target(task.getTarget())
			.currentVersion(task.getCurrentVersion())
			.targetVersion(task.getTargetVersion())
			.deployStatus(task.getDeployStatus())
			.publishedAt(task.getPublishedAt())
			.lastReplyAt(task.getLastReplyAt())
			.completedAt(task.getCompletedAt())
			.failureReason(task.getFailureReason())
			.build();
	}

	private String normalizeStatus(String status) {
		String normalized = StringUtils.upperCase(StringUtils.trimToEmpty(status));
		switch (normalized) {
			case "RECEIVED":
				return "ACCEPTED";
			case "DOWNLOADED":
				return "VERIFYING";
			case "DEPLOYING":
				return "INSTALLING";
			case "ACCEPTED":
			case "DOWNLOADING":
			case "VERIFYING":
			case "INSTALLING":
			case "REBOOTING":
			case SUCCESS:
			case FAILED:
				return normalized;
			default:
				return null;
		}
	}

	private boolean canApply(String currentStatus, String incomingStatus) {
		String current = StringUtils.upperCase(StringUtils.trimToEmpty(currentStatus));
		if (SUCCESS.equals(current) || FAILED.equals(current)) {
			return current.equals(incomingStatus);
		}
		if (CANCELLED.equals(current) || TIMED_OUT.equals(current)) {
			return false;
		}
		if (FAILED.equals(incomingStatus)) {
			return true;
		}
		return statusOrder(incomingStatus) >= statusOrder(current);
	}

	private int expireStaleTasks(Long deviceRowId, String target) {
		Date now = new Date();
		long nowEpochSeconds = now.getTime() / 1000L;
		int inactivityMinutes = properties.getOtaTaskInactivityTimeoutMinutes() == null
			? 30 : Math.max(5, properties.getOtaTaskInactivityTimeoutMinutes());
		Date inactiveBefore = new Date(now.getTime() - inactivityMinutes * 60L * 1000L);

		LambdaUpdateWrapper<FirmwareDeployTask> downloadTimeout =
			new LambdaUpdateWrapper<FirmwareDeployTask>()
				.eq(FirmwareDeployTask::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
				.eq(deviceRowId != null, FirmwareDeployTask::getDeviceRowId, deviceRowId)
				.eq(StringUtils.isNotBlank(target), FirmwareDeployTask::getTarget, target)
				.in(FirmwareDeployTask::getDeployStatus, DOWNLOAD_STATUSES)
				.lt(FirmwareDeployTask::getDownloadExpiresAt, nowEpochSeconds)
				.set(FirmwareDeployTask::getDeployStatus, TIMED_OUT)
				.set(FirmwareDeployTask::getFailureReason, "OTA 下载地址已过期，设备未进入安装阶段")
				.set(FirmwareDeployTask::getCompletedAt, now)
				.set(FirmwareDeployTask::getUpdateTime, now);

		LambdaUpdateWrapper<FirmwareDeployTask> executionTimeout =
			new LambdaUpdateWrapper<FirmwareDeployTask>()
				.eq(FirmwareDeployTask::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
				.eq(deviceRowId != null, FirmwareDeployTask::getDeviceRowId, deviceRowId)
				.eq(StringUtils.isNotBlank(target), FirmwareDeployTask::getTarget, target)
				.in(FirmwareDeployTask::getDeployStatus, EXECUTION_STATUSES)
				.lt(FirmwareDeployTask::getUpdateTime, inactiveBefore)
				.set(FirmwareDeployTask::getDeployStatus, TIMED_OUT)
				.set(FirmwareDeployTask::getFailureReason,
					"设备超过 " + inactivityMinutes + " 分钟未上报 OTA 进度")
				.set(FirmwareDeployTask::getCompletedAt, now)
				.set(FirmwareDeployTask::getUpdateTime, now);

		return mapper.update(null, downloadTimeout) + mapper.update(null, executionTimeout);
	}

	private int statusOrder(String status) {
		switch (StringUtils.upperCase(StringUtils.trimToEmpty(status))) {
			case "PUBLISHED": return 1;
			case "ACCEPTED": return 2;
			case "DOWNLOADING": return 3;
			case "VERIFYING": return 4;
			case "INSTALLING": return 5;
			case "REBOOTING": return 6;
			case SUCCESS: return 7;
			default: return 0;
		}
	}
}
