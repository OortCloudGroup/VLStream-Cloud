package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RTSP录制配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.rtsp-recording")
public class VlsRtspRecordingProperties {

	private Boolean enabled = Boolean.TRUE;

	private String ffmpegPath = "ffmpeg";

	private String defaultStoragePath = "recordings";

	private Integer segmentSeconds = 60;

	private Integer processStopWaitSeconds = 5;

	private Integer staleFileSeconds = 5;

}
