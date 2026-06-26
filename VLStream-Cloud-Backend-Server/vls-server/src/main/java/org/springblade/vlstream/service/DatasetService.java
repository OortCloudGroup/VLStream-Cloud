package org.springblade.vlstream.service;


import jakarta.servlet.http.HttpServletResponse;

/**
 * Dataset service interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface DatasetService {

	/**
	 * Connect to remote server
	 *
	 * @param host     server address
	 * @param username username
	 * @param password password
	 * @param path     dataset path
	 * @return whether connected successfully
	 */
	boolean connectToServer(String host, String username, String password, String path);

	/**
	 * Get dataset file list
	 *
	 * @param host server address
	 * @param path dataset path
	 * @return file list
	 */
	Object getDatasetFiles(String host, String path);

	/**
	 * Get file content
	 *
	 * @param host     server address
	 * @param path     dataset path
	 * @param filename file name
	 * @return file content
	 */
	String getFileContent(String host, String path, String filename);

	/**
	 * Download file
	 *
	 * @param host     server address
	 * @param path     dataset path
	 * @param filename file name
	 * @param response HTTP response object
	 */
	void downloadFile(String host, String path, String filename, HttpServletResponse response);
}
