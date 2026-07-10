package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.VideoRecord;
import com.ruoyi.vlstream.service.IVlsVideoRecordService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsVideoRecordControllerTest {

    @Test
    void exposesFrontendVideoRecordRoutes() throws Exception {
        Method page = VlsVideoRecordController.class.getDeclaredMethod(
            "page", Long.class, Long.class, Long.class, Long.class, Long.class,
            String.class, String.class, String.class, String.class, String.class);
        Method getRecord = VlsVideoRecordController.class.getDeclaredMethod("getRecord", Long.class);
        Method createRecord = VlsVideoRecordController.class.getDeclaredMethod("createRecord", VideoRecord.class);
        Method updateRecord = VlsVideoRecordController.class.getDeclaredMethod("updateRecord", Long.class, VideoRecord.class);
        Method deleteRecord = VlsVideoRecordController.class.getDeclaredMethod("deleteRecord", Long.class);
        Method deleteRecords = VlsVideoRecordController.class.getDeclaredMethod("deleteRecords", List.class);
        Method startRecording = VlsVideoRecordController.class.getDeclaredMethod(
            "startRecording", Long.class, String.class, Integer.class, String.class);
        Method stopRecording = VlsVideoRecordController.class.getDeclaredMethod("stopRecording", Long.class);
        Method recordingStatus = VlsVideoRecordController.class.getDeclaredMethod("recordingStatus", Long.class);
        Method statistics = VlsVideoRecordController.class.getDeclaredMethod("statistics");
        Method deviceRecords = VlsVideoRecordController.class.getDeclaredMethod(
            "deviceRecords", Long.class, String.class, Long.class, Long.class);
        Method download = VlsVideoRecordController.class.getDeclaredMethod("download", Long.class);
        Method preview = VlsVideoRecordController.class.getDeclaredMethod("preview", Long.class);
        Method file = VlsVideoRecordController.class.getDeclaredMethod("file", String.class);
        Method thumbnail = VlsVideoRecordController.class.getDeclaredMethod("thumbnail", String.class);

        assertArrayEquals(new String[] {"/page"}, page.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, getRecord.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {}, createRecord.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, updateRecord.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, deleteRecord.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/batch"}, deleteRecords.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/start"}, startRecording.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/stop/{recordId}"}, stopRecording.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/status/{deviceId}"}, recordingStatus.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/statistics"}, statistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/device/{deviceId}"}, deviceRecords.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/download"}, download.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/preview"}, preview.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/file/{filePath}"}, file.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/thumbnail/{filePath}"}, thumbnail.getAnnotation(GetMapping.class).value());
    }

    @Test
    void pageResolvesFrontendPaginationAliases() {
        IVlsVideoRecordService service = mock(IVlsVideoRecordService.class);
        VlsVideoRecordController controller = new VlsVideoRecordController(service);
        VideoRecord record = new VideoRecord();
        record.setId(9L);
        record.setFileName("rec.mp4");
        BladePage<VideoRecord> page = BladePage.of(Collections.singletonList(record), 1L, 20L, 2L);

        when(service.getRecordPage(2L, 20L, 7L, "camera", "rec", "recording", "2026-02-28")).thenReturn(page);

        BladeResult<BladePage<VideoRecord>> result = controller.page(
            null, 2L, null, 20L, 7L, "camera", "rec", null, "recording", "2026-02-28");

        assertEquals(200, result.getCode());
        assertEquals(page, result.getData());
        verify(service).getRecordPage(2L, 20L, 7L, "camera", "rec", "recording", "2026-02-28");
    }

    @Test
    void startRecordingReturnsRecordPayloadForFrontendTimer() {
        IVlsVideoRecordService service = mock(IVlsVideoRecordService.class);
        VlsVideoRecordController controller = new VlsVideoRecordController(service);
        VideoRecord record = new VideoRecord();
        record.setId(12L);
        record.setRecordId(12L);

        when(service.startRecording(5L, "camera-5", 60, "medium")).thenReturn(record);

        BladeResult<VideoRecord> result = controller.startRecording(5L, "camera-5", 60, "medium");

        assertEquals(200, result.getCode());
        assertEquals(12L, result.getData().getId());
        assertEquals(12L, result.getData().getRecordId());
        verify(service).startRecording(5L, "camera-5", 60, "medium");
    }

    @Test
    void deviceRecordsReturnsArrayPayloadExpectedByPlaybackView() {
        IVlsVideoRecordService service = mock(IVlsVideoRecordService.class);
        VlsVideoRecordController controller = new VlsVideoRecordController(service);
        VideoRecord first = new VideoRecord();
        first.setId(1L);
        VideoRecord second = new VideoRecord();
        second.setId(2L);
        List<VideoRecord> records = Arrays.asList(first, second);

        when(service.getDeviceRecords(8L, "2026-02-28", 1L, 100L)).thenReturn(records);

        BladeResult<List<VideoRecord>> result = controller.deviceRecords(8L, "2026-02-28", 1L, 100L);

        assertEquals(records, result.getData());
        verify(service).getDeviceRecords(8L, "2026-02-28", 1L, 100L);
    }

    @Test
    void downloadReturnsNotFoundWhenRecordHasNoReadableFile() {
        IVlsVideoRecordService service = mock(IVlsVideoRecordService.class);
        VlsVideoRecordController controller = new VlsVideoRecordController(service);

        when(service.getRecord(77L)).thenReturn(null);

        ResponseEntity<?> response = controller.download(77L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(service).getRecord(77L);
    }
}
