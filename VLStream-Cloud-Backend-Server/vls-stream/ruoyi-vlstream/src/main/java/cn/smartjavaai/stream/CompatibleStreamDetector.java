package cn.smartjavaai.stream;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SmartJavaAI 兼容视频流执行器。
 *
 * <p>官方 vision 模块目前只为普通目标检测提供流执行器，本类为其他模型复用同一套
 * FFmpeg 抓帧生命周期，避免业务会话依赖未发布的专用 stream 包。</p>
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
            throw new IllegalArgumentException("兼容执行器当前仅支持 STREAM 视频源");
        }
        if (streamUrl == null || streamUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("streamUrl 不能为空");
        }
        this.streamUrl = streamUrl;
        this.frameDetectionInterval = Math.max(1, frameDetectionInterval);
        this.frameProcessor = frameProcessor;
        this.listener = listener;
    }

    /** 启动抓帧和推理线程。 */
    public synchronized void startDetection() {
        if (running) {
            throw new IllegalStateException("当前正在运行中");
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
            throw new IllegalStateException("视频流检测启动失败", exception);
        }
        grabber = ffmpegGrabber;
        running = true;
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::processFrames);
    }

    /** 停止抓帧并释放 FFmpeg 资源。 */
    public synchronized void stopDetection() {
        running = false;
        FrameGrabber current = grabber;
        grabber = null;
        if (current != null) {
            try {
                current.stop();
            } catch (FrameGrabber.Exception ignored) {
                // release 会继续执行，避免停止异常导致原生资源泄漏。
            }
            try {
                current.release();
            } catch (FrameGrabber.Exception ignored) {
                // 调用方的会话级日志会记录停止结果。
            }
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
                    if (!detections.isEmpty() && listener != null) {
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
            if (listener != null) {
                if (disconnected) {
                    listener.onStreamDisconnected();
                } else {
                    listener.onStreamEnded();
                }
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

    /** 将一帧图片转换为统一检测信息。 */
    public interface FrameProcessor {
        List<DetectionInfo> process(Image image) throws Exception;
    }

    /** 与现有业务会话保持一致的流生命周期回调。 */
    public interface DetectionListener {
        void onObjectDetected(List<DetectionInfo> detectionInfoList, Image image);

        void onStreamEnded();

        void onStreamDisconnected();
    }
}
