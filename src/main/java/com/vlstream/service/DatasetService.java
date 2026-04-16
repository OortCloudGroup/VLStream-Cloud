package com.vlstream.service;

import javax.servlet.http.HttpServletResponse;

/**
 * Dataset Service Interface
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
public interface DatasetService {

    /**
     * Connect to remote server
     * 
     * @param host Server address
     * @param username Username
     * @param password Password
     * @param path Dataset path
     * @return Whether connection successful
     */
    boolean connectToServer(String host, String username, String password, String path);

    /**
     * Get dataset file list
     * 
     * @param host Server address
     * @param path Dataset path
     * @return File list
     */
    Object getDatasetFiles(String host, String path);

    /**
     * Get file content
     * 
     * @param host Server address
     * @param path Dataset path
     * @param filename Filename
     * @return File content
     */
    String getFileContent(String host, String path, String filename);

    /**
     * Download file
     * 
     * @param host Server address
     * @param path Dataset path
     * @param filename Filename
     * @param response HTTP response object
     */
    void downloadFile(String host, String path, String filename, HttpServletResponse response);
} 