/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.compat.smartjavaai.stream;

import ai.djl.modality.cv.Image;
import cn.smartjavaai.common.cv.SmartImageFactory;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.enums.VideoSourceType;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reuses one FFmpeg frame-grabbing lifecycle for SmartJavaAI models whose public artifacts do not provide stream APIs.
 */
public final class CompatibleStreamDetector implements AutoCloseable {

    private static final int MAX_NULL_FRAMES = 10;

    private final String streamUrl;
    private final int frameDetectionInterval;
    private final FrameProcessor frameProcessor;
    private final DetectionListener listener;

    private volatile boolean running;
    private volatile FrameGrabber grabber;
    private ExecutorService executor;

    public CompatibleStreamDetector(VideoSourceType sourceType,
                                    String streamUrl,
                                    int frameDetectionInterval,
                                    FrameProcessor frameProcessor,
                                    DetectionListener listener) {
        if (sourceType != VideoSourceType.STREAM) {
            throw new IllegalArgumentException("Only STREAM video sources are supported");
        }
        if (streamUrl == null || streamUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("streamUrl must not be blank");
        }
        this.streamUrl = streamUrl;
        this.frameDetectionInterval = Math.max(1, frameDetectionInterval);
        this.frameProcessor = Objects.requireNonNull(frameProcessor, "frameProcessor must not be null");
        this.listener = Objects.requireNonNull(listener, "listener must not be null");
    }

    public synchronized void startDetection() {
        if (running) {
            throw new IllegalStateException("Stream detection is already running");
        }
        FFmpegFrameGrabber ffmpegGrabber = new FFmpegFrameGrabber(streamUrl);
        ffmpegGrabber.setOption("rtsp_transport", "tcp");
        ffmpegGrabber.setOption("buffer_size", "1024000");
        ffmpegGrabber.setOption("stimeout", "2000000");
        ffmpegGrabber.setOption("rw_timeout", "2000000");
        ffmpegGrabber.setOption("max_delay", "5000000");
        ffmpegGrabber.setOption("timeout", "2000000");
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        try {
            ffmpegGrabber.start();
        } catch (FrameGrabber.Exception exception) {
            throw new IllegalStateException("Failed to start stream detection", exception);
        }
        grabber = ffmpegGrabber;
        running = true;
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "smartjavaai-compatible-stream-detector");
            thread.setDaemon(true);
            return thread;
        });
        executor.submit(this::processFrames);
    }

    public synchronized void stopDetection() {
        running = false;
        FrameGrabber current = grabber;
        grabber = null;
        if (current == null) {
            return;
        }
        try {
            current.stop();
        } catch (FrameGrabber.Exception ignored) {
            // Continue with release so a stop failure does not leak native resources.
        }
        try {
            current.release();
        } catch (FrameGrabber.Exception ignored) {
            // Session-level callers log the final stop result.
        }
    }

    private void processFrames() {
        OpenCVFrameConverter.ToOrgOpenCvCoreMat converter = new OpenCVFrameConverter.ToOrgOpenCvCoreMat();
        long frameCounter = 0;
        int nullFrameCount = 0;
        boolean disconnected = false;
        try {
            while (running) {
                FrameGrabber current = grabber;
                if (current == null) {
                    break;
                }
                Frame frame = current.grabFrame();
                if (frame == null) {
                    if (++nullFrameCount > MAX_NULL_FRAMES) {
                        disconnected = true;
                        break;
                    }
                    continue;
                }
                nullFrameCount = 0;
                if (frame.type != Frame.Type.VIDEO || ++frameCounter % frameDetectionInterval != 0) {
                    continue;
                }
                Mat mat = converter.convert(frame);
                if (mat == null) {
                    continue;
                }
                try {
                    Image image = SmartImageFactory.getInstance().fromMat(mat);
                    List<DetectionInfo> detections = frameProcessor.process(image);
                    if (detections == null) {
                        detections = Collections.emptyList();
                    }
                    if (!detections.isEmpty()) {
                        listener.onObjectDetected(detections, image);
                    }
                } finally {
                    mat.release();
                }
            }
        } catch (Exception exception) {
            disconnected = true;
        } finally {
            stopDetection();
            ExecutorService currentExecutor = executor;
            if (currentExecutor != null) {
                currentExecutor.shutdown();
            }
            if (disconnected) {
                listener.onStreamDisconnected();
            } else {
                listener.onStreamEnded();
            }
        }
    }

    @Override
    public synchronized void close() {
        stopDetection();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public interface FrameProcessor {
        List<DetectionInfo> process(Image image) throws Exception;
    }

    public interface DetectionListener {
        void onObjectDetected(List<DetectionInfo> detectionInfoList, Image image);

        void onStreamEnded();

        void onStreamDisconnected();
    }
}
