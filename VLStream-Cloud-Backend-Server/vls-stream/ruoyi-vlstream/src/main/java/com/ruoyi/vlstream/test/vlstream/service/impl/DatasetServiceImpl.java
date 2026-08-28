/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.jcraft.jsch.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.service.DatasetService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * datasetservice
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
			log.info("尝试连接服务器：{}@{}", username, host);

			// SSH will
			session = jsch.getSession(username, host, sshProperties.getPort());
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30000); // 30

			log.info("SSH连接成功：{}@{}", username, host);

			// SFTP
			Channel channel = session.openChannel("sftp");
			channel.connect(30000);
			ChannelSftp sftp = (ChannelSftp) channel;

			//
			try {
				sftp.cd(path);
				log.info("路径访问成功：{}", path);
			} catch (SftpException e) {
				log.warn("路径不存在，尝试创建：{}", path);
				createRemoteDirectory(sftp, path);
			}

			sftp.disconnect();
			return true;

		} catch (Exception e) {
			log.error("连接服务器失败：{}", e.getMessage());
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
			// SSH will
			session = jsch.getSession(sshProperties.getUsername(), host, sshProperties.getPort());
			session.setPassword(sshProperties.getPassword());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30000);

			// SFTPchannel
			Channel channel = session.openChannel("sftp");
			channel.connect(30000);
			sftp = (ChannelSftp) channel;

			//
			sftp.cd(path);

			// Get
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

			log.info("获取到 {} 个文件", fileList.size());
			return fileList;

		} catch (Exception e) {
			log.error("获取文件列表失败：{}", e.getMessage());
			throw new RuntimeException("获取文件列表失败：" + e.getMessage());
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
			// SSH will
			session = jsch.getSession(sshProperties.getUsername(), host, sshProperties.getPort());
			session.setPassword(sshProperties.getPassword());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30000);

			// SFTPchannel
			Channel channel = session.openChannel("sftp");
			channel.connect(30000);
			sftp = (ChannelSftp) channel;

			//
			sftp.cd(path);

			//
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

			log.info("成功读取文件内容：{}", filename);
			return content;

		} catch (Exception e) {
			log.error("读取文件内容失败：{}", e.getMessage());
			throw new RuntimeException("读取文件内容失败：" + e.getMessage());
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
			// SSH will
			session = jsch.getSession(sshProperties.getUsername(), host, sshProperties.getPort());
			session.setPassword(sshProperties.getPassword());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30000);

			// SFTPchannel
			Channel channel = session.openChannel("sftp");
			channel.connect(30000);
			sftp = (ChannelSftp) channel;

			//
			sftp.cd(path);

			// Set
			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

			//
			InputStream inputStream = sftp.get(filename);
			OutputStream outputStream = response.getOutputStream();

			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			inputStream.close();
			outputStream.flush();

			log.info("文件下载成功：{}", filename);

		} catch (Exception e) {
			log.error("文件下载失败：{}", e.getMessage());
			throw new RuntimeException("文件下载失败：" + e.getMessage());
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
	 *
	 *
	 * @param sftp SFTPchannel
	 * @param path
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
				log.debug("目录已存在：{}", currentPath);
			} catch (SftpException e) {
				// in ,
				sftp.mkdir(currentPath);
				log.info("创建远程目录：{}", currentPath);
			}
		}
	}

	/**
	 * Format
	 *
	 * @param size ( )
	 * @return Format after
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
