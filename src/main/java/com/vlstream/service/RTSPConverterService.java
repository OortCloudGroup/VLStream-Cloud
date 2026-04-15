package com.vlstream.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RTSP流转换服务
 * 负责将RTSP流转换为HLS格式以便浏览器播放
 */
@Service
public class RTSPConverterService {
    
    private static final Logger logger = LoggerFactory.getLogger(RTSPConverterService.class);
    
    private final Map<String, Process> activeStreams = new ConcurrentHashMap<>();
    private final Map<String, String> streamUrls = new ConcurrentHashMap<>();
    
    @Value("${stream.hls.output-dir:static/hls/}")
    private String hlsOutputDir;
    
    @Value("${stream.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;
    
    /**
     * 启动HLS流转换
     */
    public String startHLSStream(String deviceId, String rtspUrl, String quality) {
        logger.info("启动HLS转换: deviceId={}, rtspUrl={}, quality={}", deviceId, rtspUrl, quality);
        
        // 如果该设备已经在转换中，先停止
        if (activeStreams.containsKey(deviceId)) {
            logger.info("设备 {} 已在转换中，先停止旧转换", deviceId);
            stopHLSStream(deviceId);
        }
        
        try {
            String outputPath = hlsOutputDir + deviceId + "/";
            String playlistFile = outputPath + "playlist.m3u8";
            
            // 创建输出目录
            File outputDir = new File(outputPath);
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                logger.info("创建输出目录: {} - {}", outputPath, created ? "成功" : "失败");
            }
            
            // 构建FFmpeg命令
            List<String> command = buildFFmpegCommand(rtspUrl, playlistFile, quality);
            logger.info("FFmpeg命令: {}", String.join(" ", command));
            
            // 启动FFmpeg进程
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取FFmpeg输出
            startOutputReader(deviceId, process);
            
            // 存储进程引用
            activeStreams.put(deviceId, process);
            
            // 生成HLS播放URL
            String hlsUrl = "/hls/" + deviceId + "/playlist.m3u8";
            streamUrls.put(deviceId, hlsUrl);
            
            // 启动监控线程
            startProcessMonitor(deviceId, process);
            
            logger.info("HLS转换启动成功: deviceId={}, hlsUrl={}", deviceId, hlsUrl);
            return hlsUrl;
            
        } catch (IOException e) {
            logger.error("启动HLS转换失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            throw new RuntimeException("启动HLS转换失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 停止HLS流转换
     */
    public void stopHLSStream(String deviceId) {
        logger.info("停止HLS转换: deviceId={}", deviceId);
        
        Process process = activeStreams.remove(deviceId);
        streamUrls.remove(deviceId);
        
        if (process != null) {
            try {
                // 优雅关闭
                process.destroy();
                
                // 等待一段时间
                if (!process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                    // 强制关闭
                    process.destroyForcibly();
                    logger.warn("强制关闭FFmpeg进程: deviceId={}", deviceId);
                } else {
                    logger.info("FFmpeg进程正常关闭: deviceId={}", deviceId);
                }
            } catch (InterruptedException e) {
                logger.error("停止FFmpeg进程时被中断: deviceId={}", deviceId, e);
                process.destroyForcibly();
            }
            
            // 清理输出文件
            cleanupOutputFiles(deviceId);
        } else {
            logger.warn("未找到设备的转换进程: deviceId={}", deviceId);
        }
    }
    
    /**
     * 获取活跃的流信息
     */
    public Map<String, String> getActiveStreams() {
        return new ConcurrentHashMap<>(streamUrls);
    }
    
    /**
     * 检查设备是否在转换中
     */
    public boolean isStreamActive(String deviceId) {
        return activeStreams.containsKey(deviceId) && streamUrls.containsKey(deviceId);
    }
    
    /**
     * 构建FFmpeg转换命令
     */
    private List<String> buildFFmpegCommand(String rtspUrl, String playlistFile, String quality) {
        List<String> command = new ArrayList<>();
        
        command.add(ffmpegPath);
        command.add("-i");
        command.add(rtspUrl);
        
        // 视频编码设置
        command.add("-c:v");
        command.add("libx264");
        
        // 音频编码设置
        command.add("-c:a");
        command.add("aac");
        
        // 根据质量设置参数
        switch (quality.toLowerCase()) {
            case "low":
                command.add("-s");
                command.add("640x480");
                command.add("-b:v");
                command.add("500k");
                break;
            case "high":
                command.add("-s");
                command.add("1920x1080");
                command.add("-b:v");
                command.add("2000k");
                break;
            case "medium":
            default:
                command.add("-s");
                command.add("1280x720");
                command.add("-b:v");
                command.add("1000k");
                break;
        }
        
        // HLS格式设置
        command.add("-f");
        command.add("hls");
        command.add("-hls_time");
        command.add("2");
        command.add("-hls_list_size");
        command.add("5");
        command.add("-hls_flags");
        command.add("delete_segments");
        command.add("-hls_allow_cache");
        command.add("0");
        
        // 输出文件
        command.add(playlistFile);
        
        return command;
    }
    
    /**
     * 启动进程监控线程
     */
    private void startProcessMonitor(String deviceId, Process process) {
        Thread monitorThread = new Thread(() -> {
            try {
                int exitCode = process.waitFor();
                logger.info("FFmpeg进程结束: deviceId={}, exitCode={}", deviceId, exitCode);
                
                // 清理资源
                activeStreams.remove(deviceId);
                streamUrls.remove(deviceId);
                cleanupOutputFiles(deviceId);
                
            } catch (InterruptedException e) {
                logger.debug("监控线程被中断: deviceId={}", deviceId);
            }
        });
        
        monitorThread.setName("FFmpeg-Monitor-" + deviceId);
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * 启动输出读取线程
     */
    private void startOutputReader(String deviceId, Process process) {
        Thread outputThread = new Thread(() -> {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("FFmpeg[{}]: {}", deviceId, line);
                }
            } catch (java.io.IOException e) {
                logger.error("读取FFmpeg输出失败: deviceId={}", deviceId, e);
            }
        });
        
        outputThread.setName("FFmpeg-Output-" + deviceId);
        outputThread.setDaemon(true);
        outputThread.start();
    }
    
    /**
     * 清理输出文件
     */
    private void cleanupOutputFiles(String deviceId) {
        try {
            String outputPath = hlsOutputDir + deviceId + "/";
            File outputDir = new File(outputPath);
            
            if (outputDir.exists() && outputDir.isDirectory()) {
                File[] files = outputDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.delete()) {
                            logger.debug("删除文件: {}", file.getPath());
                        }
                    }
                }
                
                if (outputDir.delete()) {
                    logger.info("清理输出目录: {}", outputPath);
                }
            }
        } catch (Exception e) {
            logger.error("清理输出文件失败: deviceId={}", deviceId, e);
        }
    }
    
    /**
     * 停止所有转换
     */
    public void stopAllStreams() {
        logger.info("停止所有HLS转换，当前活跃数量: {}", activeStreams.size());
        
        for (String deviceId : new ArrayList<>(activeStreams.keySet())) {
            stopHLSStream(deviceId);
        }
    }
} 