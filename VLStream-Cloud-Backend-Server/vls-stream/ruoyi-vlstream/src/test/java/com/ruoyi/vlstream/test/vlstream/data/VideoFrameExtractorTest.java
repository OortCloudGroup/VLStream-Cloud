package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class VideoFrameExtractorTest {
    static Path artifacts() throws Exception {return Files.createDirectories(DataTestConfiguration.root().resolve("codex/video-frames"));}
    static Path playable() throws Exception {
        Path file=artifacts().resolve("playable.mp4");
        try(FFmpegFrameRecorder recorder=new FFmpegFrameRecorder(file.toFile(),160,96);Java2DFrameConverter converter=new Java2DFrameConverter()) {
            recorder.setFormat("mp4");recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MPEG4);recorder.setFrameRate(10);recorder.setVideoBitrate(300000);recorder.start();
            for(int i=0;i<50;i++) {
                BufferedImage image=new BufferedImage(160,96,BufferedImage.TYPE_3BYTE_BGR);Graphics2D graphics=image.createGraphics();
                graphics.setColor(new Color((i*5)%255,(i*11)%255,(i*17)%255));graphics.fillRect(0,0,160,96);
                graphics.setColor(Color.WHITE);graphics.fillRect((i*3)%130,25,25,25);graphics.drawString("Frame "+i,10,80);graphics.dispose();
                recorder.record(converter.convert(image));
            }
            recorder.stop();
        }
        return file;
    }
    @Test void extractsActualFramesWithinRangeAtRequestedIntervals() throws Exception {
        VideoFrameRequest request=new VideoFrameRequest();request.setStartSeconds(BigDecimal.ONE);request.setEndSeconds(new BigDecimal("4"));
        List<VideoFrameExtractor.Extracted> frames=new VideoFrameExtractor().extract(playable(),artifacts().resolve("range"),request,count->{});
        assertEquals(3,frames.size());
        for(int i=0;i<frames.size();i++) {
            assertEquals((i+1)*1000L,frames.get(i).getRequestedMs());assertTrue(frames.get(i).getTimestampMs()>=frames.get(i).getRequestedMs());
            assertTrue(frames.get(i).getTimestampMs()<frames.get(i).getRequestedMs()+200);
            BufferedImage image=ImageIO.read(frames.get(i).getPath().toFile());assertNotNull(image);assertEquals(160,image.getWidth());assertEquals(96,image.getHeight());
        }
    }
    @Test void respectsFrameCapAndRejectsInvalidOrEmptyRanges() throws Exception {
        VideoFrameRequest request=new VideoFrameRequest();request.setMaxFrames(2);
        assertEquals(2,new VideoFrameExtractor().extract(playable(),artifacts().resolve("capped"),request,count->{}).size());
        request.setStartSeconds(new BigDecimal("99"));
        assertThrows(ServiceException.class,()->new VideoFrameExtractor().extract(playable(),artifacts().resolve("outside"),request,count->{}));
        request.setIntervalSeconds(BigDecimal.ZERO);assertThrows(ServiceException.class,()->VideoFrameExtractor.validate(request));
    }
    @Test void corruptVideoDoesNotCreateSuccessFrames() throws Exception {
        Path file=artifacts().resolve("corrupt.mp4");Files.write(file,new byte[]{1,2,3,4});
        assertThrows(Exception.class,()->new VideoFrameExtractor().extract(file,artifacts().resolve("corrupt-frames"),new VideoFrameRequest(),count->{}));
    }
}
