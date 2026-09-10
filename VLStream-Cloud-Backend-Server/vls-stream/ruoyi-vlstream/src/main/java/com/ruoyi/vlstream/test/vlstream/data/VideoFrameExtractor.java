package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.function.IntConsumer;

/** Seeks to requested times and records decoded frame timestamps, not fabricated frame numbers. */
@Component
public class VideoFrameExtractor {
    @Getter @AllArgsConstructor
    public static class Extracted {
        private final Path path;
        private final long requestedMs;
        private final long timestampMs;
    }
    public List<Extracted> extract(Path video, Path directory, VideoFrameRequest request, IntConsumer progress) throws Exception {
        validate(request);
        List<Extracted> result = new ArrayList<>();
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(30);
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(video.toFile()); Java2DFrameConverter converter = new Java2DFrameConverter()) {
            grabber.setOption("protocol_whitelist", "file");
            grabber.setVideoOption("threads", "2");
            grabber.setTimeout(60000);
            grabber.start();
            if (!grabber.hasVideo()) throw new ServiceException("此文件没有可解码的视频轨道");
            if ((long) grabber.getImageWidth() * grabber.getImageHeight() > 40000000L) throw new ServiceException("视频分辨率超过4000万像素，无法安全切图");
            long duration = grabber.getLengthInTime();
            long start = micros(request.getStartSeconds());
            long end = request.getEndSeconds() == null ? duration : Math.min(duration, micros(request.getEndSeconds()));
            if (duration <= 0 || start >= end) throw new ServiceException("开始时间超出视频时长，或所选范围没有画面");
            long interval = micros(request.getIntervalSeconds());
            Set<Long> decoded = new HashSet<>();
            Files.createDirectories(directory);
            for (long position = start; position < end && result.size() < request.getMaxFrames(); position += interval) {
                if (Thread.currentThread().isInterrupted() || System.nanoTime() > deadline) throw new ServiceException("视频切图已中断或超过30分钟，请缩小时间范围");
                grabber.setVideoTimestamp(position);
                Frame frame = grabber.grabImage();
                if (frame == null) break;
                long timestamp = grabber.getTimestamp();
                // Seek selects the closest frame. Advance until it lies inside the requested interval.
                while (frame != null && timestamp < position) { frame = grabber.grabImage(); timestamp = grabber.getTimestamp(); }
                if (frame == null || timestamp >= end) break;
                if (!decoded.add(timestamp)) continue;
                BufferedImage image = converter.convert(frame);
                if (image == null) throw new ServiceException("视频画面解码失败");
                Path output = directory.resolve("frame_" + (result.size() + 1) + "_" + timestamp / 1000 + ".jpg");
                if (!ImageIO.write(image, "jpg", output.toFile())) throw new ServiceException("JPEG编码器不可用");
                result.add(new Extracted(output, position / 1000, timestamp / 1000));
                progress.accept(result.size());
            }
        } catch (LinkageError e) { throw new ServiceException("视频解码组件不可用，请安装对应平台的FFmpeg运行库"); }
        if (result.isEmpty()) throw new ServiceException("未能提取画面，请确认视频可播放并调整时间范围");
        return result;
    }
    public static void validate(VideoFrameRequest request) {
        if (request.getStartSeconds() == null || request.getStartSeconds().signum() < 0 || request.getStartSeconds().doubleValue() > 86400
            || request.getIntervalSeconds() == null || request.getIntervalSeconds().doubleValue() < 0.1 || request.getIntervalSeconds().doubleValue() > 3600
            || request.getMaxFrames() < 1 || request.getMaxFrames() > 1000)
            throw new ServiceException("切图参数无效：间隔0.1至3600秒，每次最多1000张");
        if (request.getEndSeconds() != null && (request.getEndSeconds().compareTo(request.getStartSeconds()) <= 0 || request.getEndSeconds().doubleValue() > 86400))
            throw new ServiceException("结束时间必须大于开始时间，且不超过24小时");
    }
    private static long micros(java.math.BigDecimal seconds) { return seconds.multiply(java.math.BigDecimal.valueOf(1000000)).longValue(); }
}
