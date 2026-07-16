/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.config.VlsRtspRecordingProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsRecordEventStrategyMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsRecordingSessionMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsTimeStrategyMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FileResponseDto;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.IVlsDeviceInfoService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsVideoRecordService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RTSP录制进程管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "vlstream.rtsp-recording.enabled", havingValue = "true", matchIfMissing = true)
public class VlsRtspRecordingManager {

	private static final String TRIGGER_ACTION_RECORD = "record";
	private static final Pattern FILE_TIME_PATTERN = Pattern.compile("(\\d{8}_\\d{6})");
	private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
	private static final String FILE_UPLOAD_APP_ID = "818301f0e77f4cd8a117414cbeb32d9e";
	private static final String FILE_UPLOAD_SECRET_KEY = "5f0de11687d744bc95e84e207d319493";
	private static final long THUMBNAIL_CAPTURE_WAIT_SECONDS = 20L;

	private final VlsRtspRecordingProperties recordingProperties;
	private final VlsRecordEventStrategyMapper recordEventStrategyMapper;
	private final IVlsDeviceInfoService deviceInfoService;
	private final IVlsVideoRecordService videoRecordService;
	private final IFileUploadService fileUploadService;
	private final VlsTimeStrategyMapper timeStrategyMapper;
	private final VlsRecordingSessionMapper recordingSessionMapper;

	private final Map<Long, Process> recordingProcessMap = new ConcurrentHashMap<>();
	private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
	private final AtomicBoolean recordSyncInProgress = new AtomicBoolean(false);

//	@Scheduled(fixedDelayString = "${vlstream.rtsp-recording.refresh-interval-millis:10000}")
//	public void scheduledRefresh() {
//		refreshNow();
//	}

	@Scheduled(fixedDelayString = "${vlstream.rtsp-recording.record-sync-interval-millis:10000}")
	public void scheduledSyncRecords() {
		syncRecordsNow();
	}

	public void refreshNowAsync() {
		log.info("收到RTSP录制刷新请求，异步执行");
		CompletableFuture.runAsync(this::refreshNow);
	}

	public void refreshNow() {
		if (Boolean.FALSE.equals(recordingProperties.getEnabled())) {
			log.warn("跳过RTSP录制刷新：vlstream.rtsp-recording.enabled=false");
			stopAllSessions("RTSP录制功能未启用");
			return;
		}
		if (!refreshInProgress.compareAndSet(false, true)) {
			log.info("跳过RTSP录制刷新：已有刷新任务进行中");
			return;
		}
		try {
			LocalDateTime currentDateTime = LocalDateTime.now();
			List<RecordEventStrategy> recordEventStrategyList = recordEventStrategyMapper.selectList(new QueryWrapper<>());
			Map<String, RecordEventStrategy> recordEventStrategyMap = recordEventStrategyList.stream().filter(recordEventStrategy -> recordEventStrategy != null && StringUtils.isNotBlank(recordEventStrategy.getDeviceId())).collect(Collectors.toMap(RecordEventStrategy::getDeviceId, Function.identity(), (left, right) -> right));

			List<TimeStrategy> timeStrategyList = timeStrategyMapper.selectList(new QueryWrapper<>());
			Map<Long, TimeStrategy> activeTimeStrategyMap = timeStrategyList.stream().filter(timeStrategy -> timeStrategy != null && timeStrategy.getId() != null).filter(timeStrategy -> StringUtils.isNotBlank(timeStrategy.getDeviceId())).collect(Collectors.toMap(TimeStrategy::getId, Function.identity(), (left, right) -> right));
			List<RecordingSession> runningSessionList = recordingSessionMapper.selectList(Wrappers.<RecordingSession>lambdaQuery().eq(RecordingSession::getSessionStatus, "running"));
			Map<Long, RecordingSession> runningSessionMap = runningSessionList.stream().filter(recordingSession -> recordingSession != null && recordingSession.getTimeStrategyId() != null).collect(Collectors.toMap(RecordingSession::getTimeStrategyId, Function.identity(), (left, right) -> right));
			log.info("执行RTSP录制刷新：timeStrategyCount={}, recordEventStrategyCount={}, activeSessionCount={}, segmentSeconds={}", activeTimeStrategyMap.size(), recordEventStrategyMap.size(), runningSessionMap.size(), resolveSegmentSeconds());

			stopRemovedSessions(activeTimeStrategyMap, runningSessionMap, currentDateTime);
			activeTimeStrategyMap.values().forEach(timeStrategy -> refreshTimeStrategySession(timeStrategy, recordEventStrategyMap, runningSessionMap, currentDateTime));
		} catch (Exception refreshException) {
			log.error("刷新RTSP录制任务失败", refreshException);
		} finally {
			refreshInProgress.set(false);
		}
	}

	public void syncRecordsNow() {
		if (Boolean.FALSE.equals(recordingProperties.getEnabled())) {
			return;
		}
		if (!recordSyncInProgress.compareAndSet(false, true)) {
			log.info("跳过录像入库扫描：已有扫描任务进行中");
			return;
		}
		try {
			syncAllGeneratedRecords(LocalDateTime.now());
		} catch (Exception syncException) {
			log.error("录像入库扫描任务失败", syncException);
		} finally {
			recordSyncInProgress.set(false);
		}
	}

	@PreDestroy
	public void shutdown() {
		stopAllSessions("系统关闭");
	}

	private void stopRemovedSessions(Map<Long, TimeStrategy> activeTimeStrategyMap, Map<Long, RecordingSession> runningSessionMap, LocalDateTime currentDateTime) {
		Set<Long> activeTimeStrategyIdSet = runningSessionMap.keySet();
		for (Long activeTimeStrategyId : activeTimeStrategyIdSet) {
			if (!activeTimeStrategyMap.containsKey(activeTimeStrategyId)) {
				stopSession(activeTimeStrategyId, "事件策略或时间策略已删除/禁用", currentDateTime);
			}
		}
	}

	private void refreshTimeStrategySession(TimeStrategy timeStrategy, Map<String, RecordEventStrategy> recordEventStrategyMap, Map<Long, RecordingSession> runningSessionMap, LocalDateTime currentDateTime) {
		Long timeStrategyId = timeStrategy.getId();
		if (timeStrategyId == null) {
			log.info("跳过时间策略刷新：strategyId为空, strategyDeviceId={}", timeStrategy.getDeviceId());
			return;
		}
		DeviceInfo deviceInfo = resolveDeviceInfo(timeStrategy.getDeviceId());
		if (deviceInfo == null) {
			log.info("停止会话：未找到设备信息, strategyId={}, strategyDeviceId={}", timeStrategyId, timeStrategy.getDeviceId());
			stopSession(timeStrategyId, "设备不存在", currentDateTime);
			return;
		}
		RecordEventStrategy recordEventStrategy = resolveRecordEventStrategy(recordEventStrategyMap, timeStrategy, deviceInfo);
		if (!isEventRecordingEnabled(recordEventStrategy)) {
			log.info("停止会话：事件策略禁用录制, strategyId={}, strategyDeviceId={}, deviceBusinessId={}, triggerAction={}", timeStrategyId, timeStrategy.getDeviceId(), deviceInfo.getDeviceId(), recordEventStrategy == null ? "<none>" : recordEventStrategy.getTriggerAction());
			stopSession(timeStrategyId, "未找到启用录制动作的事件策略", currentDateTime);
			return;
		}
		String streamUrl = deviceInfo.getStreamUrl();
		if (!isRtspAddress(streamUrl)) {
			log.info("停止会话：设备RTSP地址无效, strategyId={}, deviceId={}, streamUrl={}", timeStrategyId, deviceInfo.getId(), deviceInfo.getStreamUrl());
			stopSession(timeStrategyId, "设备未配置RTSP地址", currentDateTime);
			return;
		}
		boolean shouldRunNow = shouldRunNow(timeStrategy, currentDateTime);
		if (!shouldRunNow) {
			stopSession(timeStrategyId, "当前时间不在录制策略内", currentDateTime);
			return;
		}

		int segmentSeconds = resolveSegmentSeconds();
		Path outputDirectory = resolveOutputDirectory(timeStrategy, deviceInfo);
		String outputPattern = outputDirectory.resolve("rec_%Y%m%d_%H%M%S.mp4").toAbsolutePath().toString();
		String sessionSignature = buildSessionSignature(streamUrl, outputPattern, segmentSeconds);

		RecordingSession existingSession = runningSessionMap.get(timeStrategyId);
		if (existingSession != null) {
			if (isSessionProcessAlive(timeStrategyId) && StringUtils.equals(existingSession.getSessionSignature(), sessionSignature)) {
				log.debug("复用已存在录制会话: strategyId={}, signature={}", timeStrategyId, sessionSignature);
				return;
			}
			log.info("录制会话参数发生变化，准备重建: strategyId={}, oldSignature={}, newSignature={}", timeStrategyId, existingSession.getSessionSignature(), sessionSignature);
			stopSession(timeStrategyId, "录制参数变更，重建会话", currentDateTime);
		}
		startSession(timeStrategy, deviceInfo, streamUrl, outputDirectory, outputPattern, segmentSeconds, sessionSignature);
	}

	private void startSession(TimeStrategy timeStrategy, DeviceInfo deviceInfo, String streamUrl, Path outputDirectory, String outputPattern, int segmentSeconds, String sessionSignature) {
		try {
			Files.createDirectories(outputDirectory);
			Path ffmpegLogPath = outputDirectory.resolve("ffmpeg.log");
			ProcessBuilder processBuilder = new ProcessBuilder(buildFfmpegCommand(streamUrl, outputPattern, segmentSeconds));
			processBuilder.redirectErrorStream(true);
			processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(ffmpegLogPath.toFile()));
			Process ffmpegProcess = processBuilder.start();

			recordingProcessMap.put(timeStrategy.getId(), ffmpegProcess);

			RecordingSession recordingSession = new RecordingSession();
			recordingSession.setTimeStrategyId(timeStrategy.getId());
			recordingSession.setDeviceId(deviceInfo.getId());
			recordingSession.setDeviceName(deviceInfo.getDeviceName());
			recordingSession.setStreamUrl(streamUrl);
			recordingSession.setOutputDirectory(outputDirectory.toAbsolutePath().toString());
			recordingSession.setOutputPattern(outputPattern);
			recordingSession.setSegmentSeconds(segmentSeconds);
			recordingSession.setSessionSignature(sessionSignature);
			recordingSession.setSessionStatus("running");
			recordingSession.setSessionStartTime(LocalDateTime.now());
			recordingSession.setSessionStopTime(null);
			recordingSession.setStopReason(null);
			recordingSession.setLastSyncTime(LocalDateTime.now());
			saveOrUpdateSession(recordingSession);
			log.info("启动RTSP录制成功: strategyId={}, deviceId={}, stream={}", timeStrategy.getId(), timeStrategy.getDeviceId(), streamUrl);
		} catch (IOException ioException) {
			log.error("启动RTSP录制失败: strategyId={}, deviceId={}, reason={}", timeStrategy.getId(), timeStrategy.getDeviceId(), ioException.getMessage(), ioException);
		}
	}

	private List<String> buildFfmpegCommand(String streamUrl, String outputPattern, int segmentSeconds) {
		String ffmpegPath = StringUtils.defaultIfBlank(recordingProperties.getFfmpegPath(), "ffmpeg");
		return java.util.Arrays.asList(ffmpegPath, "-hide_banner", "-loglevel", "error", "-rtsp_transport", "tcp", "-i", streamUrl, "-c", "copy", "-f", "segment", "-segment_time", String.valueOf(segmentSeconds), "-reset_timestamps", "1", "-strftime", "1", outputPattern);
	}

	private void stopSession(Long timeStrategyId, String reason, LocalDateTime currentDateTime) {
		RecordingSession recordingSession = getSessionByTimeStrategyId(timeStrategyId);
		if (recordingSession == null) {
			return;
		}
		Process recordingProcess = recordingProcessMap.remove(timeStrategyId);
		if (recordingProcess != null && recordingProcess.isAlive()) {
			recordingProcess.destroy();
			boolean terminated;
			try {
				terminated = recordingProcess.waitFor(resolveProcessStopWaitSeconds(), TimeUnit.SECONDS);
			} catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				terminated = false;
			}
			if (!terminated) {
				recordingProcess.destroyForcibly();
			}
		}
		recordingSession.setSessionStatus("stopped");
		recordingSession.setStopReason(reason);
		recordingSession.setSessionStopTime(LocalDateTime.now());
		recordingSession.setLastSyncTime(LocalDateTime.now());
		recordingSessionMapper.updateById(recordingSession);
		log.info("停止RTSP录制: timeStrategyId={}, reason={}", timeStrategyId, reason);
	}

	private void stopAllSessions(String reason) {
		LocalDateTime currentDateTime = LocalDateTime.now();
		List<RecordingSession> recordingSessionList = recordingSessionMapper.selectList(Wrappers.<RecordingSession>lambdaQuery().eq(RecordingSession::getSessionStatus, "running"));
		Set<Long> timeStrategyIdSet = recordingSessionList.stream().filter(recordingSession -> recordingSession != null && recordingSession.getTimeStrategyId() != null).map(RecordingSession::getTimeStrategyId).collect(Collectors.toSet());
		for (Long timeStrategyId : timeStrategyIdSet) {
			stopSession(timeStrategyId, reason, currentDateTime);
		}
	}

	private void syncAllGeneratedRecords(LocalDateTime currentDateTime) {
		List<RecordingSession> recordingSessionList = recordingSessionMapper.selectList(Wrappers.<RecordingSession>lambdaQuery().isNotNull(RecordingSession::getOutputDirectory));
		recordingSessionList.forEach(recordingSession -> syncGeneratedRecords(recordingSession, currentDateTime));
	}

	private void syncGeneratedRecords(RecordingSession recordingSession, LocalDateTime currentDateTime) {
		if (recordingSession == null || StringUtils.isBlank(recordingSession.getOutputDirectory())) {
			log.info("跳过文件同步：会话或目录为空");
			return;
		}
		File outputDirectoryFile = Paths.get(recordingSession.getOutputDirectory()).toFile();
		File[] videoFileArray = outputDirectoryFile.listFiles(file -> file != null && file.isFile() && file.getName().endsWith(".mp4"));
		if (videoFileArray == null) {
			log.info("跳过文件同步：目录不可访问或无文件数组, strategyId={}, directory={}", recordingSession.getTimeStrategyId(), recordingSession.getOutputDirectory());
			return;
		}
		log.info("开始同步录像文件: strategyId={}, deviceId={}, directory={}, fileCount={}", recordingSession.getTimeStrategyId(), recordingSession.getDeviceId(), recordingSession.getOutputDirectory(), videoFileArray.length);
		for (File videoFile : videoFileArray) {
			String absoluteFilePath = videoFile.getAbsolutePath();
			LocalDateTime fileModifyTime = LocalDateTime.ofInstant(videoFile.lastModified() <= 0 ? java.time.Instant.now() : java.time.Instant.ofEpochMilli(videoFile.lastModified()), ZoneId.systemDefault());
			if (fileModifyTime.isAfter(currentDateTime.minusSeconds(resolveStaleFileSeconds()))) {
				log.info("跳过未稳定文件: path={}, modifyTime={}, staleThresholdSeconds={}", absoluteFilePath, fileModifyTime, resolveStaleFileSeconds());
				continue;
			}
			if (existsRecordByFilePath(absoluteFilePath)) {
				log.info("跳过重复入库文件(DB已存在): {}", absoluteFilePath);
				continue;
			}
			VideoRecord videoRecord = buildVideoRecord(recordingSession, videoFile.toPath());
			boolean saveResult = videoRecordService.save(videoRecord);
			if (!saveResult) {
				log.error("录像记录入库失败: path={}, strategyId={}, deviceId={}", absoluteFilePath, recordingSession.getTimeStrategyId(), recordingSession.getDeviceId());
			}
			if (saveResult && videoRecord.getId() != null) {
				VideoRecord updateVideoRecord = new VideoRecord();
				updateVideoRecord.setId(videoRecord.getId());
				updateVideoRecord.setUrl("/vlsVideoRecord/stream/" + videoRecord.getId());
				boolean updateResult = videoRecordService.updateById(updateVideoRecord);
				if (!updateResult) {
					log.warn("录像记录URL回填失败: recordId={}, path={}", videoRecord.getId(), absoluteFilePath);
				}
			}
			if (saveResult) {
				log.info("录像记录入库成功: recordId={}, path={}, startTime={}, endTime={}", videoRecord.getId(), videoRecord.getFilePath(), videoRecord.getRecordStartTime(), videoRecord.getRecordEndTime());
			}
		}
		recordingSession.setLastSyncTime(LocalDateTime.now());
		recordingSessionMapper.updateById(recordingSession);
	}

	private boolean existsRecordByFilePath(String filePath) {
		long count = videoRecordService.count(Wrappers.<VideoRecord>lambdaQuery().eq(VideoRecord::getFilePath, filePath));
		return count > 0;
	}

	private VideoRecord buildVideoRecord(RecordingSession session, Path videoPath) {
		String fileName = videoPath.getFileName().toString();
		long fileSize;
		try {
			fileSize = Files.size(videoPath);
		} catch (IOException ioException) {
			fileSize = 0L;
		}
		LocalDateTime recordStartTime = resolveRecordStartTime(fileName, videoPath, session.getSegmentSeconds());
		LocalDateTime recordEndTime = recordStartTime.plusSeconds(session.getSegmentSeconds());
		String thumbnailUrl = generateAndUploadThumbnail(videoPath);

		VideoRecord videoRecord = new VideoRecord();
		videoRecord.setDeviceId(session.getDeviceId());
		videoRecord.setDeviceName(session.getDeviceName());
		videoRecord.setStream(session.getDeviceId() == null ? null : String.valueOf(session.getDeviceId()));
		videoRecord.setFileName(fileName);
		videoRecord.setFilePath(videoPath.toAbsolutePath().toString());
		videoRecord.setFileSize(fileSize);
		videoRecord.setDuration(session.getSegmentSeconds());
		videoRecord.setFormat("mp4");
		videoRecord.setRecordStartTime(recordStartTime);
		videoRecord.setRecordEndTime(recordEndTime);
		videoRecord.setRecordDate(recordStartTime.toLocalDate());
		videoRecord.setRecordStatus("completed");
		videoRecord.setThumbnailPath(thumbnailUrl);
		videoRecord.setTenantId("0e391fd7-1033-4f09-88c0-187582fee462");
		return videoRecord;
	}

	private String generateAndUploadThumbnail(Path videoPath) {
		if (videoPath == null || !Files.exists(videoPath)) {
			return null;
		}
		Path thumbnailPath = buildThumbnailPath(videoPath);
		if (!captureVideoThumbnail(videoPath, thumbnailPath)) {
			return null;
		}
		try {
			FileResponseDto fileResponseDto = fileUploadService.uploadFile(FILE_UPLOAD_APP_ID, FILE_UPLOAD_SECRET_KEY, thumbnailPath.toFile());
			if (fileResponseDto == null || StringUtils.isBlank(fileResponseDto.getUrl())) {
				log.warn("缩略图上传失败，返回URL为空: videoPath={}, thumbnailPath={}", videoPath, thumbnailPath);
				return null;
			}
			return fileResponseDto.getUrl();
		} catch (Exception uploadException) {
			log.error("上传视频缩略图失败: videoPath={}, thumbnailPath={}", videoPath, thumbnailPath, uploadException);
			return null;
		} finally {
			try {
				Files.deleteIfExists(thumbnailPath);
			} catch (IOException deleteException) {
				log.warn("删除本地缩略图失败: thumbnailPath={}", thumbnailPath, deleteException);
			}
		}
	}

	private boolean captureVideoThumbnail(Path videoPath, Path thumbnailPath) {
		String ffmpegPath = StringUtils.defaultIfBlank(recordingProperties.getFfmpegPath(), "ffmpeg");
		List<String> command = java.util.Arrays.asList(ffmpegPath, "-hide_banner", "-loglevel", "error", "-y", "-i", videoPath.toAbsolutePath().toString(), "-vf", "select=eq(n\\,0)", "-vframes", "1", "-q:v", "2", thumbnailPath.toAbsolutePath().toString());
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
		try {
			Process process = processBuilder.start();
			boolean finished = process.waitFor(THUMBNAIL_CAPTURE_WAIT_SECONDS, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				log.warn("生成视频缩略图超时: videoPath={}", videoPath);
				return false;
			}
			if (process.exitValue() != 0) {
				log.warn("生成视频缩略图失败，ffmpeg退出码异常: videoPath={}, exitCode={}", videoPath, process.exitValue());
				return false;
			}
			if (!Files.exists(thumbnailPath) || Files.size(thumbnailPath) <= 0) {
				log.warn("生成视频缩略图失败，缩略图文件不存在或大小为0: videoPath={}, thumbnailPath={}", videoPath, thumbnailPath);
				return false;
			}
			return true;
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			log.warn("生成视频缩略图被中断: videoPath={}", videoPath, interruptedException);
			return false;
		} catch (Exception captureException) {
			log.error("生成视频缩略图异常: videoPath={}, thumbnailPath={}", videoPath, thumbnailPath, captureException);
			return false;
		}
	}

	private Path buildThumbnailPath(Path videoPath) {
		String fileName = videoPath.getFileName().toString();
		int extensionIndex = fileName.lastIndexOf('.');
		String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
		return videoPath.resolveSibling(baseName + "_thumb.jpg");
	}

	private LocalDateTime resolveRecordStartTime(String fileName, Path videoPath, int segmentSeconds) {
		Matcher fileTimeMatcher = FILE_TIME_PATTERN.matcher(fileName);
		if (fileTimeMatcher.find()) {
			String dateText = fileTimeMatcher.group(1);
			try {
				return LocalDateTime.parse(dateText, FILE_TIME_FORMATTER);
			} catch (DateTimeParseException parseException) {
				log.info("解析录像文件开始时间失败: fileName={}", fileName);
			}
		}
		long fileModifyEpochMilli = videoPath.toFile().lastModified();
		if (fileModifyEpochMilli <= 0) {
			return LocalDateTime.now().minusSeconds(segmentSeconds);
		}
		return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(fileModifyEpochMilli), ZoneId.systemDefault()).minusSeconds(segmentSeconds);
	}

	private boolean shouldRunNow(TimeStrategy timeStrategy, LocalDateTime currentDateTime) {
		return matchTimeStrategy(timeStrategy, currentDateTime);
	}

	private boolean matchTimeStrategy(TimeStrategy timeStrategy, LocalDateTime currentDateTime) {
		if (timeStrategy == null) {
			return false;
		}
		return false;
	}

	private Path resolveOutputDirectory(TimeStrategy timeStrategy, DeviceInfo deviceInfo) {
		String rootStoragePath = StringUtils.defaultIfBlank(recordingProperties.getDefaultStoragePath(), "recordings");
		String deviceKey = StringUtils.defaultIfBlank(deviceInfo.getDeviceId(), String.valueOf(deviceInfo.getId()));
		return Paths.get(rootStoragePath, "device-" + deviceKey, "strategy-" + timeStrategy.getId());
	}

	private int resolveSegmentSeconds() {
		if (recordingProperties.getSegmentSeconds() != null && recordingProperties.getSegmentSeconds() > 0) {
			return recordingProperties.getSegmentSeconds();
		}
		return 60;
	}

	private boolean isEventRecordingEnabled(RecordEventStrategy recordEventStrategy) {
		if (recordEventStrategy == null || StringUtils.isBlank(recordEventStrategy.getDeviceId())) {
			log.info("未配置事件策略，按时间策略默认启用录制");
			return true;
		}
		String triggerAction = StringUtils.trimToEmpty(recordEventStrategy.getTriggerAction());
		if (StringUtils.isBlank(triggerAction)) {
			log.info("事件策略未配置triggerAction，按默认启用录制: strategyDeviceId={}", recordEventStrategy.getDeviceId());
			return true;
		}
		boolean enabled = StringUtils.containsIgnoreCase(triggerAction, TRIGGER_ACTION_RECORD);
		if (!enabled) {
			log.info("事件策略明确禁用录制(triggerAction不含record): strategyDeviceId={}, triggerAction={}", recordEventStrategy.getDeviceId(), triggerAction);
		}
		return enabled;
	}

	private RecordEventStrategy resolveRecordEventStrategy(Map<String, RecordEventStrategy> recordEventStrategyMap, TimeStrategy timeStrategy, DeviceInfo deviceInfo) {
		if (recordEventStrategyMap == null || recordEventStrategyMap.isEmpty()) {
			return null;
		}
		String strategyDeviceId = StringUtils.trimToEmpty(timeStrategy.getDeviceId());
		if (StringUtils.isNotBlank(strategyDeviceId)) {
			RecordEventStrategy byStrategyDeviceId = recordEventStrategyMap.get(strategyDeviceId);
			if (byStrategyDeviceId != null) {
				return byStrategyDeviceId;
			}
		}
		if (deviceInfo == null) {
			return null;
		}
		String businessDeviceId = StringUtils.trimToEmpty(deviceInfo.getDeviceId());
		if (StringUtils.isNotBlank(businessDeviceId)) {
			RecordEventStrategy byBusinessDeviceId = recordEventStrategyMap.get(businessDeviceId);
			if (byBusinessDeviceId != null) {
				return byBusinessDeviceId;
			}
		}
		if (deviceInfo.getId() == null) {
			return null;
		}
		return recordEventStrategyMap.get(String.valueOf(deviceInfo.getId()));
	}

	private DeviceInfo resolveDeviceInfo(String strategyDeviceId) {
		if (StringUtils.isBlank(strategyDeviceId)) {
			return null;
		}
		DeviceInfo byBusinessDeviceId = deviceInfoService.getByDeviceId(strategyDeviceId);
		if (byBusinessDeviceId != null) {
			return byBusinessDeviceId;
		}
		if (!StringUtils.isNumeric(strategyDeviceId)) {
			return null;
		}
		return deviceInfoService.getById(Long.valueOf(strategyDeviceId));
	}

	private boolean isRtspAddress(String streamUrl) {
		return StringUtils.isNotBlank(streamUrl) && StringUtils.startsWithIgnoreCase(streamUrl, "rtsp://");
	}

	private long resolveProcessStopWaitSeconds() {
		Integer processStopWaitSeconds = recordingProperties.getProcessStopWaitSeconds();
		if (processStopWaitSeconds == null || processStopWaitSeconds <= 0) {
			return 5L;
		}
		return processStopWaitSeconds.longValue();
	}

	private long resolveStaleFileSeconds() {
		Integer staleFileSeconds = recordingProperties.getStaleFileSeconds();
		if (staleFileSeconds == null || staleFileSeconds < 0) {
			return 5L;
		}
		return staleFileSeconds.longValue();
	}

	private String buildSessionSignature(String streamUrl, String outputPattern, int segmentSeconds) {
		return streamUrl + "|" + outputPattern + "|" + segmentSeconds;
	}

	private boolean isSessionProcessAlive(Long timeStrategyId) {
		Process process = recordingProcessMap.get(timeStrategyId);
		return process != null && process.isAlive();
	}

	private RecordingSession getSessionByTimeStrategyId(Long timeStrategyId) {
		return recordingSessionMapper.selectOne(Wrappers.<RecordingSession>lambdaQuery().eq(RecordingSession::getTimeStrategyId, timeStrategyId).last("limit 1"));
	}

	private void saveOrUpdateSession(RecordingSession recordingSession) {
		RecordingSession existingSession = getSessionByTimeStrategyId(recordingSession.getTimeStrategyId());
		if (existingSession == null) {
			recordingSessionMapper.insert(recordingSession);
			return;
		}
		recordingSession.setId(existingSession.getId());
		recordingSessionMapper.updateById(recordingSession);
	}

}
