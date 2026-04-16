package com.vlstream.controller;

import com.vlstream.common.Result;
import com.vlstream.service.DatasetService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * Dataset Management Controller
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
@Api(tags = "Dataset Management")
public class DatasetController {

    private final DatasetService datasetService;

    /**
     * Connect to remote server
     */
    @PostMapping("/connect")
    @Operation(summary = "Connect to remote server", description = "Connect to remote server and verify connection status")
    public Result<String> connectToServer(
            @Parameter(description = "Server address") @RequestParam String host,
            @Parameter(description = "Username") @RequestParam String username,
            @Parameter(description = "Password") @RequestParam String password,
            @Parameter(description = "Dataset path") @RequestParam String path) {
        
        log.info("Connecting to remote server: host={}, username={}, path={}", host, username, path);
        
        try {
            boolean connected = datasetService.connectToServer(host, username, password, path);
            if (connected) {
                return Result.success("Server connection successful");
            } else {
                return Result.error("Server connection failed");
            }
        } catch (Exception e) {
            log.error("Failed to connect to server: ", e);
            return Result.error("Failed to connect to server: " + e.getMessage());
        }
    }

    /**
     * Get dataset file list
     */
    @GetMapping("/files")
    @Operation(summary = "Get file list", description = "Get file list from remote server at specified path")
    public Result<Object> getDatasetFiles(
            @Parameter(description = "Server address") @RequestParam String host,
            @Parameter(description = "Dataset path") @RequestParam String path) {
        
        log.info("Getting dataset file list: host={}, path={}", host, path);
        
        try {
            Object files = datasetService.getDatasetFiles(host, path);
            return Result.success(files);
        } catch (Exception e) {
            log.error("Failed to get file list: ", e);
            return Result.error("Failed to get file list: " + e.getMessage());
        }
    }

    /**
     * Get file content
     */
    @GetMapping("/file-content")
    @Operation(summary = "Get file content", description = "Get content of specified file from remote server")
    public Result<String> getFileContent(
            @Parameter(description = "Server address") @RequestParam String host,
            @Parameter(description = "Dataset path") @RequestParam String path,
            @Parameter(description = "Filename") @RequestParam String filename) {
        
        log.info("Getting file content: host={}, path={}, filename={}", host, path, filename);
        
        try {
            String content = datasetService.getFileContent(host, path, filename);
            return Result.success(content);
        } catch (Exception e) {
            log.error("Failed to get file content: ", e);
            return Result.error("Failed to get file content: " + e.getMessage());
        }
    }

    /**
     * Download file
     */
    @GetMapping("/download")
    @Operation(summary = "Download file", description = "Download specified file from remote server")
    public void downloadFile(
            @Parameter(description = "Server address") @RequestParam String host,
            @Parameter(description = "Dataset path") @RequestParam String path,
            @Parameter(description = "Filename") @RequestParam String filename,
            HttpServletResponse response) {
        
        log.info("Downloading file: host={}, path={}, filename={}", host, path, filename);
        
        try {
            datasetService.downloadFile(host, path, filename, response);
        } catch (Exception e) {
            log.error("Failed to download file: ", e);
            try {
                response.setStatus(500);
                response.getWriter().write("Failed to download file: " + e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to write error response: ", ex);
            }
        }
    }
} 