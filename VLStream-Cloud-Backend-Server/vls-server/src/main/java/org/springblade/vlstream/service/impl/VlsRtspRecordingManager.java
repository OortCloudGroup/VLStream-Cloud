package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springblade.modules.system.service.IFileUploadService;
import org.springblade.vlstream.config.VlsRtspRecordingProperties;
import org.springblade.vlstream.mapper.VlsRecordEventStrategyMapper;
import org.springblade.vlstream.mapper.VlsRecordingSessionMapper;
import org.springblade.vlstream.mapper.VlsTimeStrategyMapper;
import org.springblade.vlstream.pojo.dto.FileResponseDto;
import org.springblade.vlstream.pojo.entity.*;
import org.springblade.vlstream.service.IVlsDeviceInfoService;
import org.springblade.vlstream.service.IVlsVideoRecordService;
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
 * RTSP recording process manager
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
		log.info("Received RTSP recording refresh request, executing asynchronously");
		CompletableFuture.runAsync(this::refreshNow);
	}

	public void refreshNow() {
		if (Boolean.FALSE.equals(recordingProperties.getEnabled())) {
			log.warn("Skipped RTSP recording refresh: vlstream.rtsp-recording.enabled=false");
			stopAllSessions("RTSP recording function not enabled");
			return;
		}
		if (!refreshInProgress.compareAndSet(false, true)) {
			log.info("Skipped RTSP recording refresh: Another refresh task is already in progress");
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
			log.info("Execute RTSP recording refresh: timeStrategyCount={}, recordEventStrategyCount={}, activeSessionCount={}, segmentSeconds={}", activeTimeStrategyMap.size(), recordEventStrategyMap.size(), runningSessionMap.size(), resolveSegmentSeconds());

			stopRemovedSessions(activeTimeStrategyMap, runningSessionMap, currentDateTime);
			activeTimeStrategyMap.values().forEach(timeStrategy -> refreshTimeStrategySession(timeStrategy, recordEventStrategyMap, runningSessionMap, currentDateTime));
		} catch (Exception refreshException) {
			log.error("Failed to refresh RTSP recording task", refreshException);
		} finally {
			refreshInProgress.set(false);
		}
	}

	public void syncRecordsNow() {
		if (Boolean.FALSE.equals(recordingProperties.getEnabled())) {
			return;
		}
		if (!recordSyncInProgress.compareAndSet(false, true)) {
			log.info("Skipped recording storage scanning: Another scanning task is already in progress");
			return;
		}
		try {
			syncAllGeneratedRecords(LocalDateTime.now());
		} catch (Exception syncException) {
			log.error("Recording storage scan task failed", syncException);
		} finally {
			recordSyncInProgress.set(false);
		}
	}

	@PreDestroy
	public void shutdown() {
		stopAllSessions("System shutdown");
	}

	private void stopRemovedSessions(Map<Long, TimeStrategy> activeTimeStrategyMap, Map<Long, RecordingSession> runningSessionMap, LocalDateTime currentDateTime) {
		Set<Long> activeTimeStrategyIdSet = runningSessionMap.keySet();
		for (Long activeTimeStrategyId : activeTimeStrategyIdSet) {
			if (!activeTimeStrategyMap.containsKey(activeTimeStrategyId)) {
				stopSession(activeTimeStrategyId, "Event strategy or time strategy has been deleted/disabled", currentDateTime);
			}
		}
	}

	private void refreshTimeStrategySession(TimeStrategy timeStrategy, Map<String, RecordEventStrategy> recordEventStrategyMap, Map<Long, RecordingSession> runningSessionMap, LocalDateTime currentDateTime) {
		Long timeStrategyId = timeStrategy.getId();
		if (timeStrategyId == null) {
			log.info("Skipped time strategy refresh: strategyId is empty, strategyDeviceId={}", timeStrategy.getDeviceId());
			return;
		}
		DeviceInfo deviceInfo = resolveDeviceInfo(timeStrategy.getDeviceId());
		if (deviceInfo == null) {
			log.info("Stop session: device information not found, strategyId={}, strategyDeviceId={}", timeStrategyId, timeStrategy.getDeviceId());
			stopSession(timeStrategyId, "Device does not exist", currentDateTime);
			return;
		}
		RecordEventStrategy recordEventStrategy = resolveRecordEventStrategy(recordEventStrategyMap, timeStrategy, deviceInfo);
		if (!isEventRecordingEnabled(recordEventStrategy)) {
			log.info("Stop session: recording disabled by event strategy, strategyId={}, strategyDeviceId={}, deviceBusinessId={}, triggerAction={}", timeStrategyId, timeStrategy.getDeviceId(), deviceInfo.getDeviceId(), recordEventStrategy == null ? "<none>" : recordEventStrategy.getTriggerAction());
			stopSession(timeStrategyId, "No event policy found with recording action enabled", currentDateTime);
			return;
		}
		String streamUrl = deviceInfo.getStreamUrl();
		if (!isRtspAddress(streamUrl)) {
			log.info("Stop session: invalid device RTSP address, strategyId={}, deviceId={}, streamUrl={}", timeStrategyId, deviceInfo.getId(), deviceInfo.getStreamUrl());
			stopSession(timeStrategyId, "Device RTSP address not configured", currentDateTime);
			return;
		}
		boolean shouldRunNow = shouldRunNow(timeStrategy, currentDateTime);
		if (!shouldRunNow) {
			stopSession(timeStrategyId, "Current time is not within recording strategy", currentDateTime);
			return;
		}

		int segmentSeconds = resolveSegmentSeconds();
		Path outputDirectory = resolveOutputDirectory(timeStrategy, deviceInfo);
		String outputPattern = outputDirectory.resolve("rec_%Y%m%d_%H%M%S.mp4").toAbsolutePath().toString();
		String sessionSignature = buildSessionSignature(streamUrl, outputPattern, segmentSeconds);

		RecordingSession existingSession = runningSessionMap.get(timeStrategyId);
		if (existingSession != null) {
			if (isSessionProcessAlive(timeStrategyId) && StringUtils.equals(existingSession.getSessionSignature(), sessionSignature)) {
				log.debug("Reuse existing recording session: strategyId={}, signature={}", timeStrategyId, sessionSignature);
				return;
			}
			log.info("Recording session parameters changed, preparing to rebuild: strategyId={}, oldSignature={}, newSignature={}", timeStrategyId, existingSession.getSessionSignature(), sessionSignature);
			stopSession(timeStrategyId, "Recording parameters changed, rebuilding session", currentDateTime);
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
			log.info("Started RTSP recording successfully: strategyId={}, deviceId={}, stream={}", timeStrategy.getId(), timeStrategy.getDeviceId(), streamUrl);
		} catch (IOException ioException) {
			log.error("Failed to start RTSP recording: strategyId={}, deviceId={}, reason={}", timeStrategy.getId(), timeStrategy.getDeviceId(), ioException.getMessage(), ioException);
		}
	}

	private List<String> buildFfmpegCommand(String streamUrl, String outputPattern, int segmentSeconds) {
		String ffmpegPath = StringUtils.defaultIfBlank(recordingProperties.getFfmpegPath(), "ffmpeg");
		return List.of(ffmpegPath, "-hide_banner", "-loglevel", "error", "-rtsp_transport", "tcp", "-i", streamUrl, "-c", "copy", "-f", "segment", "-segment_time", String.valueOf(segmentSeconds), "-reset_timestamps", "1", "-strftime", "1", outputPattern);
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
		log.info("Stop RTSP recording: timeStrategyId={}, reason={}", timeStrategyId, reason);
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
			log.info("Skipped file synchronization: Session or directory is empty");
			return;
		}
		File outputDirectoryFile = Paths.get(recordingSession.getOutputDirectory()).toFile();
		File[] videoFileArray = outputDirectoryFile.listFiles(file -> file != null && file.isFile() && file.getName().endsWith(".mp4"));
		if (videoFileArray == null) {
			log.info("Skipped file synchronization: Directory inaccessible or file array is empty, strategyId={}, directory={}", recordingSession.getTimeStrategyId(), recordingSession.getOutputDirectory());
			return;
		}
		log.info("Start syncing video files: strategyId={}, deviceId={}, directory={}, fileCount={}", recordingSession.getTimeStrategyId(), recordingSession.getDeviceId(), recordingSession.getOutputDirectory(), videoFileArray.length);
		for (File videoFile : videoFileArray) {
			String absoluteFilePath = videoFile.getAbsolutePath();
			LocalDateTime fileModifyTime = LocalDateTime.ofInstant(videoFile.lastModified() <= 0 ? java.time.Instant.now() : java.time.Instant.ofEpochMilli(videoFile.lastModified()), ZoneId.systemDefault());
			if (fileModifyTime.isAfter(currentDateTime.minusSeconds(resolveStaleFileSeconds()))) {
				log.info("Skipped unstable file: path={}, modifyTime={}, staleThresholdSeconds={}", absoluteFilePath, fileModifyTime, resolveStaleFileSeconds());
				continue;
			}
			if (existsRecordByFilePath(absoluteFilePath)) {
				log.info("Skipped duplicate file storage (already exists in DB): {}", absoluteFilePath);
				continue;
			}
			VideoRecord videoRecord = buildVideoRecord(recordingSession, videoFile.toPath());
			boolean saveResult = videoRecordService.save(videoRecord);
			if (!saveResult) {
				log.error("Failed to store video record: path={}, strategyId={}, deviceId={}", absoluteFilePath, recordingSession.getTimeStrategyId(), recordingSession.getDeviceId());
			}
			if (saveResult && videoRecord.getId() != null) {
				VideoRecord updateVideoRecord = new VideoRecord();
				updateVideoRecord.setId(videoRecord.getId());
				updateVideoRecord.setUrl("/vlsVideoRecord/stream/" + videoRecord.getId());
				boolean updateResult = videoRecordService.updateById(updateVideoRecord);
				if (!updateResult) {
					log.warn("Failed to write back video record URL: recordId={}, path={}", videoRecord.getId(), absoluteFilePath);
				}
			}
			if (saveResult) {
				log.info("Video record stored successfully: recordId={}, path={}, startTime={}, endTime={}", videoRecord.getId(), videoRecord.getFilePath(), videoRecord.getRecordStartTime(), videoRecord.getRecordEndTime());
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
				log.warn("Thumbnail upload failed, returned URL is empty: videoPath={}, thumbnailPath={}", videoPath, thumbnailPath);
				return null;
			}
			return fileResponseDto.getUrl();
		} catch (Exception uploadException) {
			log.error("Failed to upload video thumbnail: videoPath={}, thumbnailPath={}", videoPath, thumbnailPath, uploadException);
			return null;
		} finally {
			try {
				Files.deleteIfExists(thumbnailPath);
			} catch (IOException deleteException) {
				log.warn("Failed to delete local thumbnail: thumbnailPath={}", thumbnailPath, deleteException);
			}
		}
	}

	private boolean captureVideoThumbnail(Path videoPath, Path thumbnailPath) {
		String ffmpegPath = StringUtils.defaultIfBlank(recordingProperties.getFfmpegPath(), "ffmpeg");
		List<String> command = List.of(ffmpegPath, "-hide_banner", "-loglevel", "error", "-y", "-i", videoPath.toAbsolutePath().toString(), "-vf", "select=eq(n\\,0)", "-vframes", "1", "-q:v", "2", thumbnailPath.toAbsolutePath().toString());
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
		try {
			Process process = processBuilder.start();
			boolean finished = process.waitFor(THUMBNAIL_CAPTURE_WAIT_SECONDS, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				log.warn("Video thumbnail generation timed out: videoPath={}", videoPath);
				return false;
			}
			if (process.exitValue() != 0) {
				log.warn("Failed to generate video thumbnail, abnormal ffmpeg exit code: videoPath={}, exitCode={}", videoPath, process.exitValue());
				return false;
			}
			if (!Files.exists(thumbnailPath) || Files.size(thumbnailPath) <= 0) {
				log.warn("Failed to generate video thumbnail, thumbnail file does not exist or size is 0: videoPath={}, thumbnailPath={}", videoPath, thumbnailPath);
				return false;
			}
			return true;
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			log.warn("Video thumbnail generation interrupted: videoPath={}", videoPath, interruptedException);
			return false;
		} catch (Exception captureException) {
			log.error("Exception generating video thumbnail: videoPath={}, thumbnailPath={}", videoPath, thumbnailPath, captureException);
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
				log.info("Failed to parse recording file start time: fileName={}", fileName);
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
			log.info("Event policy not configured, recording is enabled by default according to the time policy");
			return true;
		}
		String triggerAction = StringUtils.trimToEmpty(recordEventStrategy.getTriggerAction());
		if (StringUtils.isBlank(triggerAction)) {
			log.info("Event strategy does not configure triggerAction, enabling recording by default: strategyDeviceId={}", recordEventStrategy.getDeviceId());
			return true;
		}
		boolean enabled = StringUtils.containsIgnoreCase(triggerAction, TRIGGER_ACTION_RECORD);
		if (!enabled) {
			log.info("Event strategy explicitly disables recording (triggerAction does not contain record): strategyDeviceId={}, triggerAction={}", recordEventStrategy.getDeviceId(), triggerAction);
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
