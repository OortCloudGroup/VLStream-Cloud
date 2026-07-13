package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.service.IVlsDatasetService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsDatasetControllerTest {

    @Test
    void exposesFrontendDatasetRoutes() throws Exception {
        Method connectToServer = VlsDatasetController.class.getDeclaredMethod(
            "connectToServer", String.class, String.class, String.class, String.class);
        Method getDatasetFiles = VlsDatasetController.class.getDeclaredMethod("getDatasetFiles", String.class, String.class);
        Method getFileContent = VlsDatasetController.class.getDeclaredMethod(
            "getFileContent", String.class, String.class, String.class);
        Method downloadFile = VlsDatasetController.class.getDeclaredMethod(
            "downloadFile", String.class, String.class, String.class);
        Method uploadFileToServer = VlsDatasetController.class.getDeclaredMethod(
            "uploadFileToServer", String.class, String.class, org.springframework.web.multipart.MultipartFile.class);
        Method createRemoteDirectory = VlsDatasetController.class.getDeclaredMethod("createRemoteDirectory", Map.class);
        Method deleteRemoteFile = VlsDatasetController.class.getDeclaredMethod("deleteRemoteFile", String.class, String.class);

        assertArrayEquals(new String[] {"/connect"}, connectToServer.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/files"}, getDatasetFiles.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/file-content"}, getFileContent.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/download"}, downloadFile.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/upload"}, uploadFileToServer.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/create-directory"}, createRemoteDirectory.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/delete"}, deleteRemoteFile.getAnnotation(DeleteMapping.class).value());
    }

    @Test
    void connectForwardsFrontendFormFields() {
        IVlsDatasetService service = mock(IVlsDatasetService.class);
        VlsDatasetController controller = new VlsDatasetController(service);
        Map<String, Object> serviceResult = new HashMap<String, Object>();
        serviceResult.put("connected", true);

        when(service.connectToServer("127.0.0.1", "root", "secret", "/datasets")).thenReturn(serviceResult);

        BladeResult<Map<String, Object>> result = controller.connectToServer("127.0.0.1", "root", "secret", "/datasets");

        assertEquals(200, result.getCode());
        assertEquals(serviceResult, result.getData());
        verify(service).connectToServer("127.0.0.1", "root", "secret", "/datasets");
    }

    @Test
    void createDirectoryReadsFrontendJsonBody() {
        IVlsDatasetService service = mock(IVlsDatasetService.class);
        VlsDatasetController controller = new VlsDatasetController(service);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("host", "127.0.0.1");
        body.put("path", "/datasets/new-dir");
        Map<String, Object> serviceResult = Collections.<String, Object>singletonMap("created", true);

        when(service.createDirectory("127.0.0.1", "/datasets/new-dir")).thenReturn(serviceResult);

        BladeResult<Map<String, Object>> result = controller.createRemoteDirectory(body);

        assertEquals(serviceResult, result.getData());
        verify(service).createDirectory("127.0.0.1", "/datasets/new-dir");
    }

    @Test
    void uploadForwardsMultipartFileAndPath() {
        IVlsDatasetService service = mock(IVlsDatasetService.class);
        VlsDatasetController controller = new VlsDatasetController(service);
        MockMultipartFile file = new MockMultipartFile("file", "labels.txt", "text/plain", "a".getBytes());
        Map<String, Object> serviceResult = Collections.<String, Object>singletonMap("filename", "labels.txt");

        when(service.uploadFile("127.0.0.1", "/datasets", file)).thenReturn(serviceResult);

        BladeResult<Map<String, Object>> result = controller.uploadFileToServer("127.0.0.1", "/datasets", file);

        assertEquals(serviceResult, result.getData());
        verify(service).uploadFile("127.0.0.1", "/datasets", file);
    }

    @Test
    void downloadReturnsServiceResponseForBlobClient() {
        IVlsDatasetService service = mock(IVlsDatasetService.class);
        VlsDatasetController controller = new VlsDatasetController(service);
        ResponseEntity<Object> notFound = ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        when(service.downloadFile("127.0.0.1", "/datasets", "missing.txt")).thenReturn(notFound);

        ResponseEntity<?> response = controller.downloadFile("127.0.0.1", "/datasets", "missing.txt");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(service).downloadFile("127.0.0.1", "/datasets", "missing.txt");
    }
}
