package org.springblade.vlstream.service.impl;

import com.jcraft.jsch.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springblade.vlstream.config.VlsSshProperties;
import org.springblade.vlstream.service.DatasetService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Dataset service implementation class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class DatasetServiceImpl implements DatasetService {

	@Resource
	private VlsSshProperties sshProperties;

	@Override
	public boolean connectToServer(String host, String username, String password, String path) {
		JSch jsch = new JSch();
		Session session = null;

		try {
			log.info("Try to connect to server: {}@{}", username, host);

			// Create SSH session
			session = jsch.getSession(username, host, sshProperties.getPort());
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30000); // 30 seconds timeout

			log.info("SSH connection succeeded: {}@{}", username, host);

			// Test SFTP connection
			Channel channel = session.openChannel("sftp");
			channel.connect(30000);
			ChannelSftp sftp = (ChannelSftp) channel;

			// Try to access designated path
			try {
				sftp.cd(path);
				log.info("Path accessed successfully: {}", path);
			} catch (SftpException e) {
				log.warn("Path does not exist, trying to create: {}", path);
				createRemoteDirectory(sftp, path);
			}

			sftp.disconnect();
			return true;

		} catch (Exception e) {
			log.error("Failed to connect to server: {}", e.getMessage());
			return false;
		} finally {
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
	}

	@Override
	public Object getDatasetFiles(String host, String path) {
		JSch jsch = new JSch();
		Session session = null;
		ChannelSftp sftp = null;

		try {
			// Create SSH session
			session = jsch.getSession(sshProperties.getUsername(), host, sshProperties.getPort());
			session.setPassword(sshProperties.getPassword());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30000);

			// Create SFTP channel
			Channel channel = session.openChannel("sftp");
			channel.connect(30000);
			sftp = (ChannelSftp) channel;

			// Switch to specified path
			sftp.cd(path);

			// Get file list
			Vector<ChannelSftp.LsEntry> files = sftp.ls("*");
			List<Map<String, Object>> fileList = new ArrayList<>();

			for (ChannelSftp.LsEntry entry : files) {
				if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
					Map<String, Object> fileInfo = new HashMap<>();
					fileInfo.put("name", entry.getFilename());
					fileInfo.put("type", entry.getAttrs().isDir() ? "directory" : "file");
					fileInfo.put("size", formatFileSize(entry.getAttrs().getSize()));
					fileInfo.put("modifiedTime", new Date(entry.getAttrs().getMTime() * 1000L));
					fileList.add(fileInfo);
				}
			}

			log.info("Got {} files", fileList.size());
			return fileList;

		} catch (Exception e) {
			log.error("Failed to get file list: {}", e.getMessage());
			throw new RuntimeException("Failed to get file list:" + e.getMessage());
		} finally {
			if (sftp != null && sftp.isConnected()) {
				sftp.disconnect();
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
	}

	@Override
	public String getFileContent(String host, String path, String filename) {
		JSch jsch = new JSch();
		Session session = null;
		ChannelSftp sftp = null;

		try {
			// Create SSH session
			session = jsch.getSession(sshProperties.getUsername(), host, sshProperties.getPort());
			session.setPassword(sshProperties.getPassword());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30000);

			// Create SFTP channel
			Channel channel = session.openChannel("sftp");
			channel.connect(30000);
			sftp = (ChannelSftp) channel;

			// Switch to specified path
			sftp.cd(path);

			// Read file content
			InputStream inputStream = sftp.get(filename);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			String content = outputStream.toString("UTF-8");
			inputStream.close();
			outputStream.close();

			log.info("Successfully read file content: {}", filename);
			return content;

		} catch (Exception e) {
			log.error("Failed to read file content: {}", e.getMessage());
			throw new RuntimeException("Failed to read file content:" + e.getMessage());
		} finally {
			if (sftp != null && sftp.isConnected()) {
				sftp.disconnect();
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
	}

	@Override
	public void downloadFile(String host, String path, String filename, HttpServletResponse response) {
		JSch jsch = new JSch();
		Session session = null;
		ChannelSftp sftp = null;

		try {
			// Create SSH session
			session = jsch.getSession(sshProperties.getUsername(), host, sshProperties.getPort());
			session.setPassword(sshProperties.getPassword());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30000);

			// Create SFTP channel
			Channel channel = session.openChannel("sftp");
			channel.connect(30000);
			sftp = (ChannelSftp) channel;

			// Switch to specified path
			sftp.cd(path);

			// Set response headers
			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

			// Download file
			InputStream inputStream = sftp.get(filename);
			OutputStream outputStream = response.getOutputStream();

			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			inputStream.close();
			outputStream.flush();

			log.info("File download succeeded: {}", filename);

		} catch (Exception e) {
			log.error("File download failed: {}", e.getMessage());
			throw new RuntimeException("File download failed:" + e.getMessage());
		} finally {
			if (sftp != null && sftp.isConnected()) {
				sftp.disconnect();
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
	}

	/**
	 * Create remote directory
	 *
	 * @param sftp SFTP channel
	 * @param path directory path
	 */
	private void createRemoteDirectory(ChannelSftp sftp, String path) throws SftpException {
		String[] dirs = path.split("/");
		String currentPath = "";

		for (String dir : dirs) {
			if (dir.isEmpty()) {
				continue;
			}

			currentPath += "/" + dir;

			try {
				sftp.cd(currentPath);
				log.debug("Directory already exists: {}", currentPath);
			} catch (SftpException e) {
				// Directory does not exist, creating it
				sftp.mkdir(currentPath);
				log.info("Create remote directory: {}", currentPath);
			}
		}
	}

	/**
	 * Format file size
	 *
	 * @param size file size (bytes)
	 * @return formatted file size
	 */
	private String formatFileSize(long size) {
		if (size < 1024) {
			return size + " B";
		} else if (size < 1024 * 1024) {
			return String.format("%.1f KB", size / 1024.0);
		} else if (size < 1024 * 1024 * 1024) {
			return String.format("%.1f MB", size / (1024.0 * 1024.0));
		} else {
			return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
		}
	}
}
