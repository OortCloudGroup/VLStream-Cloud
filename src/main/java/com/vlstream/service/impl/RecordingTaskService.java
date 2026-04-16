package com.vlstream.service.impl;

import com.vlstream.entity.DeviceInfo;
import com.vlstream.entity.RecordingSchedule;
import com.vlstream.entity.TimeStrategy;
import com.vlstream.entity.VideoRecord;
import com.vlstream.service.DeviceInfoService;
import com.vlstream.service.RecordingScheduleService;
import com.vlstream.service.TimeStrategyService;
import com.vlstream.service.VideoRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

// 添加以下缺失的import语句
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Recording Task Service
 * Responsible for automatically executing video recording tasks according to recording schedules
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class RecordingTaskService {

    @Autowired
    private RecordingScheduleService recordingScheduleService;

    @Autowired
    @Lazy
    private VideoRecordService videoRecordService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private TimeStrategyService timeStrategyService;

    @Autowired
    private ThreadPoolExecutor recordingThreadPool;

    @Value("${recording.enabled:true}")
    private boolean recordingEnabled;

    @Value("${recording.default.duration:60}")
    private int defaultRecordingDuration;

    @Value("${recording.default.quality:medium}")
    private String defaultRecordingQuality;

    @Value("${recording.base.path:recordings}")
    private String recordingBasePath;

    @Value("${recording.max.concurrent:5}")
    private int maxConcurrentRecordings;

    // 设备录制状态管理
    private final ConcurrentHashMap<Long, AtomicBoolean> deviceRecordingStatus = new ConcurrentHashMap<>();
    
    // 录制进程管理
    private final ConcurrentHashMap<Long, Process> recordingProcesses = new ConcurrentHashMap<>();

    /**
     * Check and execute recording tasks regularly
     * Executes once per minute
     * Temporarily commented out to avoid conflicts with manual recording
     */
    // @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void checkAndExecuteRecordingTasks() {
        if (!recordingEnabled) {
            log.debug("Recording function is disabled, skipping task check");
            return;
        }

        try {
            log.debug("Starting to check recording tasks...");
            
            // Clean up completed recording statuses
            cleanupCompletedRecordings();
            
            // Query recording schedules to execute
            List<RecordingSchedule> schedulesToExecute = recordingScheduleService.getSchedulesToExecute();
            
            // Query time strategies to execute
            List<TimeStrategy> timeStrategiesToExecute = timeStrategyService.getTimeStrategiesToExecute();
            
            if (schedulesToExecute.isEmpty() && timeStrategiesToExecute.isEmpty()) {
                log.debug("No recording tasks need to be executed");
                return;
            }

            log.info("Found {} recording schedules and {} time strategies to execute", 
                    schedulesToExecute.size(), timeStrategiesToExecute.size());

            // Check concurrent recording limit
            int currentRecordings = deviceRecordingStatus.size();
            if (currentRecordings >= maxConcurrentRecordings) {
                log.warn("Current recording count has reached maximum limit: {}/{}", currentRecordings, maxConcurrentRecordings);
                return;
            }

            // Asynchronously execute each recording schedule task
            for (RecordingSchedule schedule : schedulesToExecute) {
                if (currentRecordings >= maxConcurrentRecordings) {
                    log.warn("Reached maximum concurrent recording limit, skipping remaining tasks");
                    break;
                }
                if (canStartRecording(schedule.getDeviceId())) {
                    executeRecordingTaskAsync(schedule);
                    currentRecordings++;
                }
            }
            
            // Asynchronously execute each time strategy task
            for (TimeStrategy timeStrategy : timeStrategiesToExecute) {
                if (currentRecordings >= maxConcurrentRecordings) {
                    log.warn("Reached maximum concurrent recording limit, skipping remaining tasks");
                    break;
                }
                if (canStartRecording(Long.valueOf(timeStrategy.getDeviceId()))) {
                    executeTimeStrategyTaskAsync(timeStrategy);
                    currentRecordings++;
                }
            }

        } catch (Exception e) {
            log.error("Error occurred while checking recording tasks", e);
        }
    }

    /**
     * Check if recording can start
     */
    private boolean canStartRecording(Long deviceId) {
        // Check if device is recording
        if (deviceRecordingStatus.containsKey(deviceId)) {
            AtomicBoolean status = deviceRecordingStatus.get(deviceId);
            if (status.get()) {
                log.debug("Device is recording, skipping: deviceId={}", deviceId);
                return false;
            }
        }

        // Check recording status in database
        VideoRecord latestRecord = videoRecordService.getLatestRecordByDevice(deviceId);
        if (latestRecord != null && VideoRecord.STATUS_RECORDING.equals(latestRecord.getRecordStatus())) {
            // Check if recording is timeout (exceeded expected time but not completed)
            LocalDateTime startTime = latestRecord.getRecordStartTime();
            LocalDateTime expectedEndTime = startTime.plusSeconds(latestRecord.getDuration() + 30); // Allow 30 seconds buffer time
            
            if (LocalDateTime.now().isAfter(expectedEndTime)) {
                log.warn("Recording task timeout, marking as failed: deviceId={}, recordId={}", deviceId, latestRecord.getId());
                videoRecordService.markRecordingFailed(latestRecord.getId(), "Recording timeout");
                return true;
            }
            
            log.debug("Device has ongoing recording task: deviceId={}, recordId={}", deviceId, latestRecord.getId());
            return false;
        }

        return true;
    }

    /**
     * Clean up completed recording statuses
     */
    private void cleanupCompletedRecordings() {
        deviceRecordingStatus.entrySet().removeIf(entry -> {
            Long deviceId = entry.getKey();
            AtomicBoolean status = entry.getValue();
            
            if (!status.get()) {
                // 清理已完成的进程
                Process process = recordingProcesses.remove(deviceId);
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 异步执行录制任务
     */
    private void executeRecordingTaskAsync(RecordingSchedule schedule) {
        // 设置录制状态
        deviceRecordingStatus.put(schedule.getDeviceId(), new AtomicBoolean(true));
        
        CompletableFuture.supplyAsync(() -> {
            return executeRecordingTask(schedule);
        }, recordingThreadPool).whenComplete((result, throwable) -> {
            // 清理录制状态
            AtomicBoolean status = deviceRecordingStatus.get(schedule.getDeviceId());
            if (status != null) {
                status.set(false);
            }
            
            if (throwable != null) {
                log.error("执行录制任务失败: scheduleId={}, deviceId={}, error={}", 
                         schedule.getId(), schedule.getDeviceId(), throwable.getMessage());
                
                // 增加失败次数
                recordingScheduleService.incrementFailedRecords(schedule.getId());
            } else if (result) {
                log.info("录制任务执行成功: scheduleId={}, deviceId={}", 
                        schedule.getId(), schedule.getDeviceId());
            } else {
                log.warn("录制任务执行失败: scheduleId={}, deviceId={}", 
                        schedule.getId(), schedule.getDeviceId());
                
                // 增加失败次数
                recordingScheduleService.incrementFailedRecords(schedule.getId());
            }
        });
    }

    /**
     * 异步执行时间策略任务
     */
    private void executeTimeStrategyTaskAsync(TimeStrategy timeStrategy) {
        Long deviceId = Long.valueOf(timeStrategy.getDeviceId());
        
        // 设置录制状态
        deviceRecordingStatus.put(deviceId, new AtomicBoolean(true));
        
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            return executeTimeStrategyTask(timeStrategy);
        }, recordingThreadPool);
        
        future.whenComplete((result, throwable) -> {
            // 清理录制状态
            AtomicBoolean status = deviceRecordingStatus.get(deviceId);
            if (status != null) {
                status.set(false);
            }
            
            if (throwable != null) {
                log.error("执行时间策略任务失败: strategyId={}, deviceId={}, error={}", 
                         timeStrategy.getId(), timeStrategy.getDeviceId(), throwable.getMessage());
            } else if (result != null && result) {
                log.info("时间策略任务执行成功: strategyId={}, deviceId={}", 
                        timeStrategy.getId(), timeStrategy.getDeviceId());
            } else {
                log.warn("时间策略任务执行失败: strategyId={}, deviceId={}，设备可能正在录制或不在线", 
                        timeStrategy.getId(), timeStrategy.getDeviceId());
            }
        });
    }

    /**
     * 执行录制任务
     */
    private boolean executeRecordingTask(RecordingSchedule schedule) {
        try {
            log.info("开始执行录制任务: scheduleId={}, deviceId={}, deviceName={}", 
                    schedule.getId(), schedule.getDeviceId(), schedule.getDeviceName());

            // 检查设备是否在线
            DeviceInfo device = deviceInfoService.getById(schedule.getDeviceId());
            if (device == null) {
                log.warn("设备不存在: deviceId={}", schedule.getDeviceId());
                return false;
            }

            if (!"在线".equals(device.getStatus())) {
                log.warn("设备不在线，跳过录制: deviceId={}, status={}", 
                        schedule.getDeviceId(), device.getStatus());
                return false;
            }

            // 开始录制
            Integer duration = schedule.getRecordDuration() != null ? schedule.getRecordDuration() : defaultRecordingDuration;
            String quality = schedule.getRecordQuality() != null ? schedule.getRecordQuality() : defaultRecordingQuality;

            VideoRecord record = videoRecordService.startRecording(
                    schedule.getDeviceId(), 
                    schedule.getDeviceName(), 
                    duration, 
                    quality
            );

            if (record == null) {
                log.error("开始录制失败: deviceId={}", schedule.getDeviceId());
                return false;
            }

            // 更新录制计划的执行时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextRecordTime = calculateNextRecordTime(schedule, now);
            
            recordingScheduleService.updateExecutionTime(
                    schedule.getId(), 
                    now, 
                    nextRecordTime
            );

            // 执行录制过程
            performRecordingProcess(record, duration, device);

            return true;

        } catch (Exception e) {
            log.error("执行录制任务时发生异常: scheduleId={}", schedule.getId(), e);
            return false;
        }
    }

    /**
     * 执行时间策略任务
     */
    private boolean executeTimeStrategyTask(TimeStrategy timeStrategy) {
        try {
            log.info("开始执行时间策略任务: strategyId={}, deviceId={}", 
                    timeStrategy.getId(), timeStrategy.getDeviceId());

            // 检查设备是否在线
            DeviceInfo device = deviceInfoService.getById(Long.valueOf(timeStrategy.getDeviceId()));
            if (device == null) {
                log.warn("设备不存在: deviceId={}", timeStrategy.getDeviceId());
                return false;
            }

            if (!"在线".equals(device.getStatus())) {
                log.warn("设备不在线，跳过录制: deviceId={}, status={}", 
                        timeStrategy.getDeviceId(), device.getStatus());
                return false;
            }

            // 开始录制
            VideoRecord record = videoRecordService.startRecording(
                    Long.valueOf(timeStrategy.getDeviceId()), 
                    device.getDeviceName(), 
                    defaultRecordingDuration, 
                    defaultRecordingQuality
            );

            if (record == null) {
                log.error("开始录制失败: deviceId={}", timeStrategy.getDeviceId());
                return false;
            }

            // 执行录制过程
            performRecordingProcess(record, defaultRecordingDuration, device);

            return true;

        } catch (Exception e) {
            log.error("执行时间策略任务时发生异常: strategyId={}", timeStrategy.getId(), e);
            return false;
        }
    }

    /**
     * 手动录制（用于API调用）
     * 立即启动录制过程，不依赖定时任务，支持动态停止
     */
    public void performManualRecording(VideoRecord record, Integer duration, DeviceInfo device) {
        log.warn("🎬 开始执行手动录制 - 记录ID: {}, 设备ID: {}, 设备名称: {}, 配置时长: {}秒, 强制限制: 60秒", 
                record.getId(), record.getDeviceId(), record.getDeviceName(), duration);

        // 设置录制状态
        deviceRecordingStatus.put(record.getDeviceId(), new AtomicBoolean(true));
        log.info("📊 录制状态已设置: deviceId={}, status=true", record.getDeviceId());

        // 异步执行手动录制过程（支持动态停止）
        CompletableFuture.runAsync(() -> {
            try {
                performManualRecordingProcess(record, duration, device);
            } finally {
                // 清理录制状态 - 直接移除状态以确保清理
                deviceRecordingStatus.remove(record.getDeviceId());
                log.info("手动录制状态已清理 (finally): deviceId={}", record.getDeviceId());
            }
        }, recordingThreadPool);
    }

    /**
     * 停止手动录制
     */
    public boolean stopManualRecording(Long deviceId, Long recordId) {
        log.warn("🛑 收到停止录制请求 - 设备ID: {}, 记录ID: {}", deviceId, recordId);
        
        try {
            // 优雅停止录制进程
            Process process = recordingProcesses.get(deviceId);
            if (process != null && process.isAlive()) {
                log.warn("🔥 优雅停止录制进程 - 设备ID: {}", deviceId);
                
                // 先尝试发送'q'命令优雅停止
                try {
                    OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
                    writer.write("q\n");
                    writer.flush();
                    writer.close();
                    log.info("📨 已发送优雅停止命令: deviceId={}", deviceId);
                    
                    // 等待3秒
                    boolean terminated = process.waitFor(3, TimeUnit.SECONDS);
                    if (!terminated) {
                        log.warn("⚠️ 优雅停止超时，使用普通终止: deviceId={}", deviceId);
                        process.destroy();
                        terminated = process.waitFor(2, TimeUnit.SECONDS);
                        if (!terminated) {
                            log.error("❌ 普通终止失败，强制终止: deviceId={}", deviceId);
                            process.destroyForcibly();
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ 优雅停止失败，强制终止: deviceId={}, error={}", deviceId, e.getMessage());
                    process.destroyForcibly();
                }
                
                recordingProcesses.remove(deviceId);
                log.info("✅ 手动录制进程已停止: deviceId={}", deviceId);
            } else {
                log.info("📋 录制进程已经停止或不存在: deviceId={}", deviceId);
            }

            // 更新录制状态 - 直接移除状态以确保停止
            deviceRecordingStatus.remove(deviceId);
            log.warn("🧹 手动录制状态已清理: deviceId={}", deviceId);

            return true;
        } catch (Exception e) {
            log.error("停止手动录制失败: deviceId={}, recordId={}, error={}", deviceId, recordId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 手动录制过程（无固定时长限制）
     */
    private void performManualRecordingProcess(VideoRecord record, Integer maxDuration, DeviceInfo device) {
        try {
            log.warn("🎯 开始手动录制设备直播流 - 记录ID: {}, 设备ID: {}, 配置时长: {}秒, 硬性限制: 60秒",
                    record.getId(), record.getDeviceId(), maxDuration);

            // 启动安全监控线程 - 60秒强制终止
            startSafetyMonitor(record.getId(), device.getId());

            // 确保录制目录存在
            String filePath = ensureRecordingDirectory(record);
            if (filePath == null) {
                log.error("❌ 创建录制目录失败: recordId={}", record.getId());
                videoRecordService.markRecordingFailed(record.getId(), "创建录制目录失败");
                return;
            }

            // 录制真实的设备直播流（手动模式，可以被停止）
            boolean videoRecorded = recordManualVideoStream(filePath, maxDuration, device);

            if (!videoRecorded) {
                log.error("手动录制设备直播流失败: recordId={}, filePath={}", record.getId(), filePath);
                videoRecordService.markRecordingFailed(record.getId(), "录制设备直播流失败");
                return;
            }

            // 验证录制文件
            if (!validateRecordedFile(filePath)) {
                log.error("录制文件验证失败: recordId={}, filePath={}", record.getId(), filePath);
                videoRecordService.markRecordingFailed(record.getId(), "录制文件无效");
                return;
            }

            // 获取文件大小
            long fileSize = getFileSize(filePath);
            String thumbnailPath = filePath.replace(".mp4", "_thumbnail.jpg");

            // 生成缩略图
            generateThumbnail(filePath, thumbnailPath);

            // 计算实际录制时长
            int actualDuration = calculateActualDuration(record.getRecordStartTime());

            // 完成录制
            boolean success = videoRecordService.completeRecording(
                    record.getId(),
                    filePath,
                    fileSize,
                    actualDuration,
                    thumbnailPath
            );

            if (success) {
                log.info("手动录制完成: recordId={}, deviceId={}, fileSize={}字节, actualDuration={}秒, filePath={}",
                        record.getId(), record.getDeviceId(), fileSize, actualDuration, filePath);
            } else {
                log.error("完成录制失败: recordId={}", record.getId());
                videoRecordService.markRecordingFailed(record.getId(), "完成录制时更新数据库失败");
            }

        } catch (Exception e) {
            log.error("手动录制过程发生异常: recordId={}", record.getId(), e);
            videoRecordService.markRecordingFailed(record.getId(), "录制过程发生异常: " + e.getMessage());
        }
    }

    /**
     * 计算实际录制时长
     */
    private int calculateActualDuration(LocalDateTime startTime) {
        if (startTime == null) {
            return 0;
        }
        LocalDateTime endTime = LocalDateTime.now();
        return (int) java.time.Duration.between(startTime, endTime).getSeconds();
    }

    /**
     * 手动录制视频流（支持动态停止）
     */
    private boolean recordManualVideoStream(String filePath, Integer maxDuration, DeviceInfo device) {
        Process process = null;
        CompletableFuture<Void> monitorFuture = null;
        try {
            // 构建流地址
            String streamUrl = buildStreamUrl(device);
            if (streamUrl == null) {
                log.error("无法构建流地址: deviceId={}", device.getId());
                return false;
            }

            // 检测流类型并获取实际可录制的URL
            String actualStreamUrl = getActualStreamUrl(streamUrl);
            if (actualStreamUrl == null) {
                log.error("无法获取实际流地址: deviceId={}, originalUrl={}", device.getId(), streamUrl);
                return false;
            }

            // 构建FFmpeg录制命令（手动模式，不设置固定时长）
            String ffmpegCommand = buildManualFFmpegCommand(actualStreamUrl, filePath);

            log.info("开始手动录制设备直播流: deviceId={}, streamUrl={}, maxDuration={}秒",
                    device.getId(), streamUrl, maxDuration);
            log.info("执行FFmpeg命令: {}", ffmpegCommand);

            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd", "/c", ffmpegCommand);
            } else {
                processBuilder.command("bash", "-c", ffmpegCommand);
            }

            // 设置工作目录
            processBuilder.directory(new File(System.getProperty("user.dir")));
            
            process = processBuilder.start();
            
            // 保存进程引用以便需要时可以终止
            recordingProcesses.put(device.getId(), process);

            // 启动线程监控进程输出
            monitorFuture = monitorProcessOutput(process, device.getId());

            // 等待录制被手动停止或达到最大时长 - 强制监控
            boolean finished = false;
            long startTime = System.currentTimeMillis();
            long maxDurationMillis = Math.min(maxDuration * 1000L, 60 * 1000L); // 强制限制最大60秒
            long lastLogTime = startTime;
            int progressCounter = 0;
            
            log.warn("🎥 开始录制监控 - 设备ID: {}, 最大时长: {}秒, 强制限制: 60秒", device.getId(), maxDuration);
            
            while (!finished && process.isAlive()) {
                try {
                    long currentTime = System.currentTimeMillis();
                    long elapsedSeconds = (currentTime - startTime) / 1000;
                    
                    // 强制60秒硬性限制 - 无论任何情况都不能超过60秒
                    if (elapsedSeconds >= 60) {
                        log.error("⚠️ 强制停止！录制超过60秒硬性限制: deviceId={}, 实际时长={}秒", device.getId(), elapsedSeconds);
                        break;
                    }
                    
                    // 检查是否超过配置的最大时长
                    if (currentTime - startTime > maxDurationMillis) {
                        log.warn("📋 录制达到配置的最大时长，停止录制: deviceId={}, maxDuration={}秒, 实际时长={}秒", 
                                device.getId(), maxDuration, elapsedSeconds);
                        break;
                    }
                    
                    // 每10秒输出一次进度监控
                    if (currentTime - lastLogTime >= 10000) {
                        log.warn("📊 录制进度监控 - 设备ID: {}, 已录制: {}秒/60秒, 进程状态: {}", 
                                device.getId(), elapsedSeconds, process.isAlive() ? "运行中" : "已停止");
                        lastLogTime = currentTime;
                    }
                    
                    // 检查是否被手动停止
                    AtomicBoolean recordingStatus = deviceRecordingStatus.get(device.getId());
                    if (!recordingProcesses.containsKey(device.getId()) || 
                        recordingStatus == null || !recordingStatus.get()) {
                        log.warn("✋ 手动录制被用户停止: deviceId={}, 录制时长={}秒", device.getId(), elapsedSeconds);
                        break;
                    }
                    
                    // 每5秒输出一次详细状态
                    progressCounter++;
                    if (progressCounter % 5 == 0) {
                        log.info("🔍 录制状态检查 - 设备ID: {}, 时长: {}秒, 进程存活: {}, 状态: {}", 
                                device.getId(), elapsedSeconds, process.isAlive(), 
                                recordingStatus != null ? recordingStatus.get() : "null");
                    }
                    
                    // 等待1秒后再次检查
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn("⚠️ 手动录制检查被中断: deviceId={}", device.getId());
                    break;
                }
            }

            // 优雅停止录制进程
            long stopStartTime = System.currentTimeMillis();
            long finalElapsedSeconds = (stopStartTime - startTime) / 1000;
            
            if (process.isAlive()) {
                log.warn("🛑 优雅停止录制进程 - 设备ID: {}, 录制时长: {}秒", device.getId(), finalElapsedSeconds);
                
                // 向FFmpeg发送'q'命令优雅停止
                try {
                    OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
                    writer.write("q\n");
                    writer.flush();
                    writer.close();
                    log.info("📨 已发送优雅停止命令给FFmpeg: deviceId={}", device.getId());
                } catch (Exception e) {
                    log.warn("⚠️ 发送停止命令失败: deviceId={}, 将使用其他方式停止", device.getId());
                }
                
                // 等待优雅停止
                try {
                    boolean terminated = process.waitFor(3, TimeUnit.SECONDS);
                    if (terminated) {
                        log.info("✅ 录制进程已优雅停止: deviceId={}", device.getId());
                    } else {
                        log.warn("⚠️ 优雅停止超时，尝试普通终止: deviceId={}", device.getId());
                        process.destroy();
                        
                        // 再等待2秒
                        terminated = process.waitFor(2, TimeUnit.SECONDS);
                        if (terminated) {
                            log.info("✅ 录制进程已普通终止: deviceId={}", device.getId());
                        } else {
                            log.error("❌ 普通终止失败，强制终止: deviceId={}", device.getId());
                            process.destroyForcibly();
                        }
                    }
                } catch (InterruptedException e) {
                    log.warn("⚠️ 等待进程终止被中断: deviceId={}", device.getId());
                    process.destroyForcibly();
                }
            } else {
                log.info("📋 录制进程已自然结束: deviceId={}, 录制时长: {}秒", device.getId(), finalElapsedSeconds);
            }
            
            // 等待监控线程停止
            if (monitorFuture != null) {
                try {
                    monitorFuture.get(3, TimeUnit.SECONDS);
                    log.debug("✅ 监控线程已停止: deviceId={}", device.getId());
                } catch (Exception e) {
                    log.debug("⚠️ 监控线程停止超时，强制取消: deviceId={}", device.getId());
                    monitorFuture.cancel(true);
                }
            }

            // 验证生成的文件
            File videoFile = new File(filePath);
            if (videoFile.exists() && videoFile.length() > 0) {
                log.warn("✅ 手动录制成功 - 设备ID: {}, 录制时长: {}秒, 文件大小: {} 字节, 文件路径: {}", 
                        device.getId(), finalElapsedSeconds, videoFile.length(), filePath);
                
                // 如果超过60秒，记录警告
                if (finalElapsedSeconds > 60) {
                    log.error("⚠️ 警告：录制时长超过60秒限制！设备ID: {}, 实际时长: {}秒", device.getId(), finalElapsedSeconds);
                }
                return true;
            } else {
                log.error("❌ 手动录制失败 - 设备ID: {}, 录制时长: {}秒, 文件无效或不存在: {}", 
                        device.getId(), finalElapsedSeconds, filePath);
                return false;
            }

        } catch (Exception e) {
            log.error("手动录制设备直播流时发生异常: deviceId={}, error={}", device.getId(), e.getMessage(), e);
            return false;
        } finally {
            // 清理进程引用
            if (process != null) {
                recordingProcesses.remove(device.getId());
            }
        }
    }

    /**
     * 构建手动录制的FFmpeg命令（不设置固定时长）
     */
    private String buildManualFFmpegCommand(String streamUrl, String filePath) {
        StringBuilder command = new StringBuilder();
        
        command.append("ffmpeg ");
        
        // 根据流类型添加特定参数
        if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
            // 简化参数以避免网络连接问题
            command.append("-reconnect 1 -reconnect_streamed 1 -reconnect_delay_max 10 ");
            command.append("-user_agent \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\" ");
            command.append("-timeout 120000000 ");
            command.append("-fflags +genpts+discardcorrupt ");
            command.append("-analyzeduration 5000000 -probesize 10000000 ");
            // HLS流输入特殊参数
            if (streamUrl.contains("m3u8")) {
                command.append("-allowed_extensions ALL ");
                command.append("-protocol_whitelist file,http,https,tcp,tls,crypto ");
            }
        } else if (streamUrl.startsWith("rtsp://")) {
            // RTSP流的特殊参数
            command.append("-rtsp_transport tcp ");
            command.append("-analyzeduration 5000000 -probesize 5000000 ");
            command.append("-fflags +genpts ");
            command.append("-timeout 30000000 ");
        }
        
        // 输入源
        command.append(String.format("-i \"%s\" ", streamUrl));
        
        // 不设置录制时长，由程序控制停止
        // command.append(String.format("-t %d ", duration));  // 注释掉固定时长
        
        // 视频编码参数
        command.append("-c:v copy ");
        
        // 音频编码参数
        command.append("-c:a copy ");
        
        // 输出参数 - 优化文件完整性
        command.append("-movflags +faststart+frag_keyframe+empty_moov -avoid_negative_ts make_zero ");
        command.append("-fflags +genpts+igndts ");
        command.append("-reset_timestamps 1 ");
        
        // 覆盖已存在的文件
        command.append("-y ");
        
        // 输出文件路径
        command.append(String.format("\"%s\"", filePath));
        
        return command.toString();
    }

    /**
     * 录制过程（优化版本）
     */
    private void performRecordingProcess(VideoRecord record, Integer duration, DeviceInfo device) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始录制设备直播流: recordId={}, deviceId={}, duration={}秒",
                        record.getId(), record.getDeviceId(), duration);

                // 确保录制目录存在
                String filePath = ensureRecordingDirectory(record);
                if (filePath == null) {
                    videoRecordService.markRecordingFailed(record.getId(), "创建录制目录失败");
                    return;
                }

                // 录制真实的设备直播流
                boolean videoRecorded = recordRealVideoStream(filePath, duration, device);

                if (!videoRecorded) {
                    log.error("录制设备直播流失败: recordId={}, filePath={}", record.getId(), filePath);
                    videoRecordService.markRecordingFailed(record.getId(), "录制设备直播流失败");
                    return;
                }

                // 验证录制文件
                if (!validateRecordedFile(filePath)) {
                    log.error("录制文件验证失败: recordId={}, filePath={}", record.getId(), filePath);
                    videoRecordService.markRecordingFailed(record.getId(), "录制文件无效");
                    return;
                }

                // 获取文件大小
                long fileSize = getFileSize(filePath);
                String thumbnailPath = filePath.replace(".mp4", "_thumbnail.jpg");

                // 生成缩略图
                generateThumbnail(filePath, thumbnailPath);

                // 完成录制
                boolean success = videoRecordService.completeRecording(
                        record.getId(),
                        filePath,
                        fileSize,
                        duration,
                        thumbnailPath
                );

                if (success) {
                    log.info("录制完成: recordId={}, deviceId={}, fileSize={}字节, filePath={}",
                            record.getId(), record.getDeviceId(), fileSize, filePath);
                } else {
                    log.error("完成录制失败: recordId={}", record.getId());
                    videoRecordService.markRecordingFailed(record.getId(), "完成录制时更新数据库失败");
                }

            } catch (Exception e) {
                log.error("录制过程发生异常: recordId={}", record.getId(), e);
                videoRecordService.markRecordingFailed(record.getId(), "录制过程发生异常: " + e.getMessage());
            }
        }, recordingThreadPool);
    }

    /**
     * 确保录制目录存在并返回完整文件路径
     */
    private String ensureRecordingDirectory(VideoRecord record) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String dateStr = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            
            // 构建目录路径
            Path recordingDir = Paths.get(recordingBasePath, dateStr);
            
            // 确保目录存在
            if (!Files.exists(recordingDir)) {
                Files.createDirectories(recordingDir);
                log.info("创建录制目录: {}", recordingDir.toAbsolutePath());
            }
            
            // 构建文件名
            String fileName = String.format("%s_%d_%s.mp4", 
                    record.getDeviceName(), 
                    record.getDeviceId(), 
                    now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            
            String filePath = recordingDir.resolve(fileName).toAbsolutePath().toString();
            
            // 更新记录中的文件路径
            record.setFilePath(filePath);
            
            return filePath;
            
        } catch (Exception e) {
            log.error("创建录制目录时发生异常: recordId={}", record.getId(), e);
            return null;
        }
    }

    /**
     * 验证录制文件
     */
    private boolean validateRecordedFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("录制文件不存在: {}", filePath);
                return false;
            }
            
            if (file.length() == 0) {
                log.error("录制文件为空: {}", filePath);
                return false;
            }
            
            if (file.length() < 1024) { // 小于1KB的文件可能有问题
                log.warn("录制文件太小，可能有问题: {}, size={}", filePath, file.length());
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("验证录制文件时发生异常: filePath={}", filePath, e);
            return false;
        }
    }

    /**
     * 录制真实的设备直播流（优化版本）
     */
    private boolean recordRealVideoStream(String filePath, Integer duration, DeviceInfo device) {
        Process process = null;
        CompletableFuture<Void> monitorFuture = null;
        try {
            // 构建流地址
            String streamUrl = buildStreamUrl(device);
            if (streamUrl == null) {
                log.error("无法构建流地址: deviceId={}", device.getId());
                return false;
            }

            // 检测流类型并获取实际可录制的URL
            String actualStreamUrl = getActualStreamUrl(streamUrl);
            if (actualStreamUrl == null) {
                log.error("无法获取实际流地址: deviceId={}, originalUrl={}", device.getId(), streamUrl);
                return false;
            }

            // 构建FFmpeg录制命令
            String ffmpegCommand = buildFFmpegCommand(actualStreamUrl, duration, filePath);

            log.info("开始录制设备直播流: deviceId={}, streamUrl={}, duration={}秒",
                    device.getId(), streamUrl, duration);
            log.info("执行FFmpeg命令: {}", ffmpegCommand);

            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd", "/c", ffmpegCommand);
            } else {
                processBuilder.command("bash", "-c", ffmpegCommand);
            }

            // 设置工作目录
            processBuilder.directory(new File(System.getProperty("user.dir")));
            
            process = processBuilder.start();
            
            // 保存进程引用以便需要时可以终止
            recordingProcesses.put(device.getId(), process);

            // 启动线程监控进程输出
            monitorFuture = monitorProcessOutput(process, device.getId());

            // 等待录制完成（添加超时时间）
            boolean finished = process.waitFor(duration + 60, TimeUnit.SECONDS);
            
            if (!finished) {
                log.error("录制超时，强制终止: deviceId={}", device.getId());
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                // 验证生成的文件
                File videoFile = new File(filePath);
                if (videoFile.exists() && videoFile.length() > 0) {
                    log.info("设备直播流录制成功: deviceId={}, filePath={}, 文件大小: {} 字节",
                            device.getId(), filePath, videoFile.length());
                    return true;
                } else {
                    log.error("录制完成但文件无效: deviceId={}, filePath={}", device.getId(), filePath);
                    return false;
                }
            } else {
                log.error("设备直播流录制失败: deviceId={}, 退出码: {}", device.getId(), exitCode);
                // 检查是否是网络连接错误，如果是则尝试备用方案
                if (exitCode == 1) {
                    log.warn("检测到录制失败可能由网络问题导致，尝试备用录制方案: deviceId={}", device.getId());
                    return tryFallbackRecording(filePath, duration, device);
                }
                return false;
            }

        } catch (Exception e) {
            log.error("录制设备直播流时发生异常: deviceId={}, error={}", device.getId(), e.getMessage(), e);
            // 清理进程引用
            if (process != null) {
                recordingProcesses.remove(device.getId());
            }
            
            // 如果是网络连接问题，尝试备用方案
            if (e.getMessage().contains("Error number -138") || 
                e.getMessage().contains("Connection") || 
                e.getMessage().contains("timeout")) {
                log.warn("检测到网络连接问题，尝试备用录制方案: deviceId={}", device.getId());
                return tryFallbackRecording(filePath, duration, device);
            }
            
            return false;
        } finally {
            // 清理进程引用
            if (process != null) {
                recordingProcesses.remove(device.getId());
            }
            
            // 等待监控线程停止
            if (monitorFuture != null) {
                try {
                    monitorFuture.get(3, TimeUnit.SECONDS);
                    log.debug("✅ 监控线程已停止: deviceId={}", device.getId());
                } catch (Exception e) {
                    log.debug("⚠️ 监控线程停止超时，强制取消: deviceId={}", device.getId());
                    monitorFuture.cancel(true);
                }
            }
        }
    }

    /**
     * 备用录制方案 - 当网络连接失败时使用
     */
    private boolean tryFallbackRecording(String filePath, Integer duration, DeviceInfo device) {
        try {
            log.info("开始备用录制方案: deviceId={}, duration={}秒", device.getId(), duration);
            
            // 生成测试视频作为备用方案
            String fallbackCommand = String.format(
                "ffmpeg -f lavfi -i testsrc=duration=%d:size=1280x720:rate=25 " +
                "-f lavfi -i sine=frequency=1000:duration=%d " +
                "-c:v libx264 -preset ultrafast -crf 28 -pix_fmt yuv420p " +
                "-c:a aac -b:a 64k -ac 1 -ar 22050 " +
                "-shortest -movflags +faststart -y \"%s\"",
                duration, duration, filePath
            );

            log.info("执行备用FFmpeg命令: {}", fallbackCommand);

            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd", "/c", fallbackCommand);
            } else {
                processBuilder.command("bash", "-c", fallbackCommand);
            }

            processBuilder.directory(new File(System.getProperty("user.dir")));
            Process process = processBuilder.start();

            // 等待备用录制完成
            boolean finished = process.waitFor(duration + 30, TimeUnit.SECONDS);
            
            if (!finished) {
                log.error("备用录制超时: deviceId={}", device.getId());
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                File videoFile = new File(filePath);
                if (videoFile.exists() && videoFile.length() > 0) {
                    log.info("备用录制成功: deviceId={}, filePath={}, 文件大小: {} 字节",
                            device.getId(), filePath, videoFile.length());
                    return true;
                } else {
                    log.error("备用录制完成但文件无效: deviceId={}, filePath={}", device.getId(), filePath);
                    return false;
                }
            } else {
                log.error("备用录制失败: deviceId={}, 退出码: {}", device.getId(), exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("备用录制方案执行异常: deviceId={}, error={}", device.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 监控进程输出
     */
    private CompletableFuture<Void> monitorProcessOutput(Process process, Long deviceId) {
        // 监控错误输出（改进版本，支持主动停止）
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                log.debug("🔍 开始监控FFmpeg输出 - 设备ID: {}", deviceId);
                while ((line = reader.readLine()) != null && process.isAlive()) {
                    if (line.contains("error") || line.contains("Error") || line.contains("failed")) {
                        log.warn("FFmpeg warning/error for device {}: {}", deviceId, line);
                    } else {
                        log.debug("FFmpeg output for device {}: {}", deviceId, line);
                    }
                }
                log.debug("🔍 停止监控FFmpeg输出 - 设备ID: {}, 进程状态: {}", deviceId, process.isAlive() ? "运行中" : "已终止");
            } catch (Exception e) {
                log.debug("停止读取进程输出: 设备ID={}, 原因: {}", deviceId, e.getMessage());
            }
        });
    }

    /**
     * 获取实际可录制的流地址（优化版本）
     */
    private String getActualStreamUrl(String originalUrl) {
        try {
            // 检测是否为YouTube URL
            if (isYouTubeUrl(originalUrl)) {
                return getYouTubeStreamUrl(originalUrl);
            }
            
            // 检测是否为其他HTTP流媒体URL
            if (originalUrl.startsWith("http://") || originalUrl.startsWith("https://")) {
                return originalUrl;
            }
            
            // 对于RTSP等其他协议，直接返回原URL
            return originalUrl;
            
        } catch (Exception e) {
            log.error("获取实际流地址时发生异常: originalUrl={}, error={}", originalUrl, e.getMessage());
            return originalUrl;
        }
    }

    /**
     * 检测是否为YouTube URL
     */
    private boolean isYouTubeUrl(String url) {
        return url.contains("youtube.com") || url.contains("youtu.be");
    }

    /**
     * 构建 yt-dlp 进程构建器，支持多种调用方式
     */
    private ProcessBuilder buildYtDlpProcessBuilder(String youtubeUrl) {
        // 在 Windows 系统上，优先尝试 python -m yt_dlp
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return new ProcessBuilder(
                "python", "-m", "yt_dlp",
                "--get-url",
                "--format", "best[height<=720]/best",
                "--no-warnings",
                "--no-check-certificate",
                "--socket-timeout", "30",
                "--retries", "3",
                "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                youtubeUrl
            );
        } else {
            // 在 Linux/Mac 系统上，尝试直接调用 yt-dlp
            return new ProcessBuilder(
                "yt-dlp",
                "--get-url",
                "--format", "best[height<=720]/best",
                "--no-warnings",
                "--no-check-certificate",
                "--socket-timeout", "30",
                "--retries", "3",
                "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                youtubeUrl
            );
        }
    }

    /**
     * 获取YouTube直播流的实际地址（优化版本）
     */
    private String getYouTubeStreamUrl(String youtubeUrl) {
        try {
            log.info("开始获取YouTube流地址: url={}", youtubeUrl);
            
            // 尝试使用yt-dlp获取流地址 - 先尝试直接调用，失败则使用 python -m yt_dlp
            ProcessBuilder pb = buildYtDlpProcessBuilder(youtubeUrl);
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 设置超时时间
            boolean finished = process.waitFor(45, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("获取YouTube流地址超时: url={}", youtubeUrl);
                return null;
            }
            
            if (process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String streamUrl = reader.readLine();
                    if (streamUrl != null && !streamUrl.trim().isEmpty()) {
                        log.info("成功获取YouTube流地址: original={}, actual={}", youtubeUrl, streamUrl);
                        return streamUrl.trim();
                    }
                }
            } else {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String errorOutput = reader.lines().collect(Collectors.joining("\n"));
                    log.error("yt-dlp执行失败: url={}, exitCode={}, output={}", 
                             youtubeUrl, process.exitValue(), errorOutput);
                }
                
                // 如果失败，尝试备用方案
                return tryAlternativeYtDlp(youtubeUrl);
            }
            
        } catch (Exception e) {
            log.error("获取YouTube流地址异常: url={}, error={}", youtubeUrl, e.getMessage());
            // 如果发生异常，尝试备用方案
            return tryAlternativeYtDlp(youtubeUrl);
        }
        
        return null;
    }

    /**
     * 尝试备用的 yt-dlp 调用方式
     */
    private String tryAlternativeYtDlp(String youtubeUrl) {
        try {
            log.info("尝试备用方案获取YouTube流地址: url={}", youtubeUrl);
            
            ProcessBuilder pb;
            // 如果之前用的是 Windows 方案，尝试直接调用
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                pb = new ProcessBuilder(
                    "yt-dlp",
                    "--get-url",
                    "--format", "best[height<=720]/best",
                    "--no-warnings",
                    "--no-check-certificate",
                    "--socket-timeout", "30",
                    "--retries", "3",
                    "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    youtubeUrl
                );
            } else {
                // 如果之前用的是 Linux 方案，尝试 python 方案
                pb = new ProcessBuilder(
                    "python", "-m", "yt_dlp",
                    "--get-url",
                    "--format", "best[height<=720]/best",
                    "--no-warnings",
                    "--no-check-certificate",
                    "--socket-timeout", "30",
                    "--retries", "3",
                    "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    youtubeUrl
                );
            }
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 设置超时时间
            boolean finished = process.waitFor(45, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("备用方案获取YouTube流地址超时: url={}", youtubeUrl);
                return null;
            }
            
            if (process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String streamUrl = reader.readLine();
                    if (streamUrl != null && !streamUrl.trim().isEmpty()) {
                        log.info("备用方案成功获取YouTube流地址: original={}, actual={}", youtubeUrl, streamUrl);
                        return streamUrl.trim();
                    }
                }
            } else {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String errorOutput = reader.lines().collect(Collectors.joining("\n"));
                    log.error("备用方案yt-dlp执行失败: url={}, exitCode={}, output={}", 
                             youtubeUrl, process.exitValue(), errorOutput);
                }
            }
            
        } catch (Exception e) {
            log.error("备用方案获取YouTube流地址异常: url={}, error={}", youtubeUrl, e.getMessage());
        }
        
        return null;
    }

    /**
     * 构建FFmpeg命令（优化版本）- 处理网络连接问题
     */
    private String buildFFmpegCommand(String streamUrl, Integer duration, String filePath) {
        StringBuilder command = new StringBuilder();
        
        command.append("ffmpeg ");
        
        // 根据流类型添加特定参数
        if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
            // 简化参数以避免网络连接问题
            command.append("-reconnect 1 -reconnect_streamed 1 -reconnect_delay_max 10 ");
            command.append("-reconnect_at_eof 1 -max_reload 5 ");
            command.append("-user_agent \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\" ");
            command.append("-timeout 120000000 "); // 增加超时时间到120秒
            command.append("-rw_timeout 120000000 ");
            // 简化网络参数，减少可能的冲突
            command.append("-fflags +genpts+discardcorrupt ");
            command.append("-analyzeduration 5000000 -probesize 10000000 ");
            command.append("-max_delay 5000000 ");
            // 减小缓冲区避免内存问题
            command.append("-rtbufsize 512M ");
            // HLS流输入特殊参数
            if (streamUrl.contains("m3u8")) {
                command.append("-allowed_extensions ALL ");
                command.append("-protocol_whitelist file,http,https,tcp,tls,crypto ");
                command.append("-live_start_index 0 ");
            }
        } else if (streamUrl.startsWith("rtsp://")) {
            // RTSP流的特殊参数
            command.append("-rtsp_transport tcp ");
            command.append("-analyzeduration 5000000 -probesize 5000000 ");
            command.append("-fflags +genpts ");
            command.append("-timeout 30000000 ");
        }
        
        // 输入源
        command.append(String.format("-i \"%s\" ", streamUrl));
        
        // 录制时长
        command.append(String.format("-t %d ", duration));
        
        // 视频编码参数
        command.append("-c:v copy ");
        
        // 音频编码参数
        command.append("-c:a copy ");
        
        // 输出参数 - 优化文件完整性
        command.append("-movflags +faststart+frag_keyframe+empty_moov -avoid_negative_ts make_zero ");
        command.append("-fflags +genpts+igndts ");
        command.append("-reset_timestamps 1 ");
        
        // 覆盖已存在的文件
        command.append("-y ");
        
        // 输出文件路径
        command.append(String.format("\"%s\"", filePath));
        
        return command.toString();
    }

    /**
     * 构建设备的流地址（优化版本）
     */
    private String buildStreamUrl(DeviceInfo device) {
        try {
            // 优先使用设备配置的流地址
            if (device.getStreamUrl() != null && !device.getStreamUrl().isEmpty()) {
                String streamUrl = device.getStreamUrl().trim();
                log.info("使用设备配置的流地址: deviceId={}, streamUrl={}", device.getId(), streamUrl);
                return streamUrl;
            }

            // 根据设备IP、端口、用户名、密码构建RTSP URL
            String ip = device.getIpAddress();
            Integer port = device.getPort() != null ? device.getPort() : 554;
            String username = device.getUsername();
            String password = device.getPassword();
            String channel = "1";

            if (ip == null || ip.isEmpty()) {
                log.warn("设备IP地址为空且未配置流地址: deviceId={}", device.getId());
                return null;
            }

            // 构建RTSP URL
            String rtspUrl;
            if (username != null && password != null && !username.isEmpty() && !password.isEmpty()) {
                rtspUrl = String.format("rtsp://%s:%s@%s:%d/Streaming/Channels/%s01",
                        username, password, ip, port, channel);
            } else {
                rtspUrl = String.format("rtsp://%s:%d/Streaming/Channels/%s01",
                        ip, port, channel);
            }

            log.debug("构建RTSP URL: deviceId={}, rtspUrl={}", device.getId(), rtspUrl);
            return rtspUrl;

        } catch (Exception e) {
            log.error("构建流地址时发生异常: deviceId={}", device.getId(), e);
            return null;
        }
    }

    /**
     * 计算下次录制时间
     */
    private LocalDateTime calculateNextRecordTime(RecordingSchedule schedule, LocalDateTime currentTime) {
        LocalDateTime nextTime = currentTime;

        switch (schedule.getScheduleType()) {
            case RecordingSchedule.TYPE_CONTINUOUS:
                // 连续录制：当前录制结束后立即开始下一次
                nextTime = currentTime.plusSeconds(schedule.getRecordDuration() != null ? schedule.getRecordDuration() : defaultRecordingDuration);
                break;

            case RecordingSchedule.TYPE_TIME_RANGE:
                // 时间段录制：在指定时间段内每10分钟录制一次
                nextTime = currentTime.plusMinutes(10);
                
                // 检查是否在时间段内
                LocalTime currentLocalTime = nextTime.toLocalTime();
                if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                    if (currentLocalTime.isBefore(schedule.getStartTime()) || 
                        currentLocalTime.isAfter(schedule.getEndTime())) {
                        // 如果超出时间段，设置到下一天的开始时间
                        nextTime = nextTime.plusDays(1).with(schedule.getStartTime());
                    }
                }
                break;

            case RecordingSchedule.TYPE_TIME_STRATEGY:
                // 时间策略录制：根据配置的时间策略
                if (schedule.getTimeStrategyId() != null) {
                    nextTime = calculateNextTimeByStrategy(schedule.getTimeStrategyId(), currentTime);
                } else {
                    // 默认10分钟间隔
                    nextTime = currentTime.plusMinutes(10);
                }
                break;

            default:
                // 默认10分钟间隔
                nextTime = currentTime.plusMinutes(10);
                break;
        }

        return nextTime;
    }

    /**
     * 根据时间策略计算下次录制时间
     */
    private LocalDateTime calculateNextTimeByStrategy(Long timeStrategyId, LocalDateTime currentTime) {
        try {
            TimeStrategy strategy = timeStrategyService.getById(timeStrategyId);
            if (strategy == null) {
                log.warn("时间策略不存在: strategyId={}", timeStrategyId);
                return currentTime.plusMinutes(10);
            }

            // 根据时间策略的配置计算下次执行时间
            if ("weekly".equals(strategy.getStrategyType())) {
                // 每周执行：根据选择的星期几
                return currentTime.plusDays(7);
            } else if ("daily".equals(strategy.getStrategyType())) {
                // 每天执行
                return currentTime.plusDays(1);
            } else {
                // 默认间隔执行：10分钟
                return currentTime.plusMinutes(10);
            }

        } catch (Exception e) {
            log.error("根据时间策略计算下次录制时间失败: strategyId={}", timeStrategyId, e);
            return currentTime.plusMinutes(10);
        }
    }

    /**
     * 生成缩略图
     */
    private void generateThumbnail(String videoPath, String thumbnailPath) {
        try {
            // FFmpeg命令：从视频中提取第一帧作为缩略图
            String ffmpegCommand = String.format(
                    "ffmpeg -i \"%s\" -ss 00:00:01 -vframes 1 -q:v 2 -y \"%s\"",
                    videoPath, thumbnailPath
            );

            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd", "/c", ffmpegCommand);
            } else {
                processBuilder.command("bash", "-c", ffmpegCommand);
            }

            Process process = processBuilder.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                log.info("缩略图生成成功: {}", thumbnailPath);
            } else {
                log.warn("缩略图生成失败，退出码: {}", process.exitValue());
            }

        } catch (Exception e) {
            log.warn("生成缩略图时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 获取文件大小
     */
    private long getFileSize(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                return file.length();
            }
        } catch (Exception e) {
            log.warn("获取文件大小时发生异常: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * 停止设备的所有录制任务
     */
    public void stopDeviceRecording(Long deviceId) {
        try {
            log.info("停止设备录制任务: deviceId={}", deviceId);

            // 停止进程
            Process process = recordingProcesses.get(deviceId);
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                recordingProcesses.remove(deviceId);
                log.info("强制终止录制进程: deviceId={}", deviceId);
            }

            // 更新录制状态
            AtomicBoolean status = deviceRecordingStatus.get(deviceId);
            if (status != null) {
                status.set(false);
            }

            // 查询正在录制的记录
            List<VideoRecord> recordingVideos = videoRecordService.getRecordingVideos();
            
            for (VideoRecord record : recordingVideos) {
                if (record.getDeviceId().equals(deviceId)) {
                    boolean success = videoRecordService.stopRecording(record.getId());
                    if (success) {
                        log.info("停止录制成功: recordId={}, deviceId={}", record.getId(), deviceId);
                    } else {
                        log.warn("停止录制失败: recordId={}, deviceId={}", record.getId(), deviceId);
                    }
                }
            }

            // 禁用设备的录制计划
            List<RecordingSchedule> schedules = recordingScheduleService.getEnabledSchedulesByDevice(deviceId);
            for (RecordingSchedule schedule : schedules) {
                recordingScheduleService.disableSchedule(schedule.getId(), "system");
                log.info("禁用录制计划: scheduleId={}, deviceId={}", schedule.getId(), deviceId);
            }

        } catch (Exception e) {
            log.error("停止设备录制任务时发生异常: deviceId={}", deviceId, e);
        }
    }

    /**
     * 启动安全监控线程 - 60秒强制终止录制
     */
    private void startSafetyMonitor(Long recordId, Long deviceId) {
        CompletableFuture.runAsync(() -> {
            try {
                log.warn("🛡️ 安全监控启动 - 记录ID: {}, 设备ID: {}, 将在60秒后强制终止", recordId, deviceId);
                
                // 等待60秒
                Thread.sleep(60 * 1000);
                
                // 检查录制是否仍在进行
                AtomicBoolean recordingStatus = deviceRecordingStatus.get(deviceId);
                Process process = recordingProcesses.get(deviceId);
                
                if ((recordingStatus != null && recordingStatus.get()) || 
                    (process != null && process.isAlive())) {
                    
                    log.error("🚨 安全监控优雅停止！录制超过60秒 - 记录ID: {}, 设备ID: {}", recordId, deviceId);
                    
                    // 优雅停止录制
                    if (process != null && process.isAlive()) {
                        log.error("⚡ 安全监控优雅停止进程 - 设备ID: {}", deviceId);
                        
                        // 先尝试发送'q'命令
                        try {
                            OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
                            writer.write("q\n");
                            writer.flush();
                            writer.close();
                            log.info("📨 安全监控已发送停止命令: deviceId={}", deviceId);
                            
                            // 等待3秒
                            boolean terminated = process.waitFor(3, TimeUnit.SECONDS);
                            if (!terminated) {
                                log.warn("⚠️ 优雅停止超时，使用普通终止: deviceId={}", deviceId);
                                process.destroy();
                                terminated = process.waitFor(2, TimeUnit.SECONDS);
                                if (!terminated) {
                                    log.error("❌ 普通终止失败，强制终止: deviceId={}", deviceId);
                                    process.destroyForcibly();
                                }
                            }
                        } catch (Exception e) {
                            log.warn("⚠️ 优雅停止失败，强制终止: deviceId={}, error={}", deviceId, e.getMessage());
                            process.destroyForcibly();
                        }
                        
                        recordingProcesses.remove(deviceId);
                    }
                    
                    // 清理状态
                    deviceRecordingStatus.remove(deviceId);
                    
                    // 标记录制失败
                    videoRecordService.markRecordingFailed(recordId, "录制超过60秒安全限制，优雅停止");
                    
                    log.error("🛑 安全监控处理完成 - 记录ID: {}, 设备ID: {}", recordId, deviceId);
                } else {
                    log.info("✅ 安全监控检查通过 - 录制已正常结束，记录ID: {}, 设备ID: {}", recordId, deviceId);
                }
                
            } catch (InterruptedException e) {
                log.warn("⚠️ 安全监控被中断 - 记录ID: {}, 设备ID: {}", recordId, deviceId);
            } catch (Exception e) {
                log.error("❌ 安全监控发生异常 - 记录ID: {}, 设备ID: {}, error: {}", recordId, deviceId, e.getMessage(), e);
            }
        }, recordingThreadPool);
    }

    /**
     * 为设备创建默认录制计划
     */
    public void createDefaultRecordingScheduleForDevice(DeviceInfo device) {
        try {
            // 检查设备是否已有录制计划
            List<RecordingSchedule> existingSchedules = recordingScheduleService.getEnabledSchedulesByDevice(device.getId());
            if (!existingSchedules.isEmpty()) {
                log.debug("设备已有录制计划，跳过创建: deviceId={}", device.getId());
                return;
            }

            // 创建默认录制计划
            RecordingSchedule schedule = recordingScheduleService.createDefaultScheduleForDevice(device.getId(), device.getDeviceName());
            if (schedule != null) {
                log.info("为设备创建默认录制计划成功: deviceId={}, scheduleId={}", 
                        device.getId(), schedule.getId());
            } else {
                log.warn("为设备创建默认录制计划失败: deviceId={}", device.getId());
            }

        } catch (Exception e) {
            log.error("为设备创建默认录制计划时发生异常: deviceId={}", device.getId(), e);
        }
    }

    /**
     * 获取录制任务统计信息
     */
    public RecordingTaskStatistics getRecordingTaskStatistics() {
        try {
            List<RecordingSchedule> allSchedules = recordingScheduleService.getAllEnabledSchedules();
            List<VideoRecord> recordingVideos = videoRecordService.getRecordingVideos();

            RecordingTaskStatistics statistics = new RecordingTaskStatistics();
            statistics.setTotalSchedules((long) allSchedules.size());
            statistics.setActiveRecordings((long) recordingVideos.size());
            statistics.setRecordingEnabled(recordingEnabled);

            // 计算今日录制统计
            long todayRecords = 0;
            long todayFailures = 0;
            for (RecordingSchedule schedule : allSchedules) {
                todayRecords += (schedule.getTotalRecords() != null ? schedule.getTotalRecords() : 0);
                todayFailures += (schedule.getFailedRecords() != null ? schedule.getFailedRecords() : 0);
            }
            
            statistics.setTodayRecords(todayRecords);
            statistics.setTodayFailures(todayFailures);

            return statistics;

        } catch (Exception e) {
            log.error("获取录制任务统计信息失败", e);
            return new RecordingTaskStatistics();
        }
    }

    /**
     * 录制任务统计信息
     */
    public static class RecordingTaskStatistics {
        private Long totalSchedules;
        private Long activeRecordings;
        private Long todayRecords;
        private Long todayFailures;
        private Boolean recordingEnabled;

        // Getters and Setters
        public Long getTotalSchedules() {
            return totalSchedules;
        }

        public void setTotalSchedules(Long totalSchedules) {
            this.totalSchedules = totalSchedules;
        }

        public Long getActiveRecordings() {
            return activeRecordings;
        }

        public void setActiveRecordings(Long activeRecordings) {
            this.activeRecordings = activeRecordings;
        }

        public Long getTodayRecords() {
            return todayRecords;
        }

        public void setTodayRecords(Long todayRecords) {
            this.todayRecords = todayRecords;
        }

        public Long getTodayFailures() {
            return todayFailures;
        }

        public void setTodayFailures(Long todayFailures) {
            this.todayFailures = todayFailures;
        }

        public Boolean getRecordingEnabled() {
            return recordingEnabled;
        }

        public void setRecordingEnabled(Boolean recordingEnabled) {
            this.recordingEnabled = recordingEnabled;
        }
    }
}