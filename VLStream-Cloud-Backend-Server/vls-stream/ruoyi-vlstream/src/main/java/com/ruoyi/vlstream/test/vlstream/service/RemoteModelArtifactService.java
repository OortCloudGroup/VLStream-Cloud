/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Resolves, inspects and streams model artifacts stored on the GPU training server.
 */
@Service
public class RemoteModelArtifactService {

	@Resource
	private SSHService sshService;

	@Resource
	private VlsSshProperties sshProperties;

	public String normalizeType(String type) {
		String normalized = StringUtils.defaultIfBlank(type, "om").trim().toLowerCase(Locale.ROOT);
		if (!"pt".equals(normalized) && !"onnx".equals(normalized)
			&& !"rknn".equals(normalized) && !"int8-rknn".equals(normalized)
			&& !"om".equals(normalized)) {
			throw new IllegalArgumentException("Unsupported model type: " + type);
		}
		return normalized;
	}

	public String resolvePath(AlgorithmTraining training, String type) throws FileNotFoundException {
		if (training == null) {
			throw new FileNotFoundException("Training task does not exist");
		}
		String normalizedType = normalizeType(type);
		String path;
		switch (normalizedType) {
			case "onnx":
				path = training.getOnnxModelOutputPath();
				break;
			case "rknn":
				path = training.getRknnModelOutputPath();
				break;
			case "int8-rknn":
				path = training.getInt8RknnModelOutputPath();
				break;
			case "om":
				path = training.getOmModelOutputPath();
				break;
			case "pt":
			default:
				path = training.getModelOutputPath();
				break;
		}
		if (StringUtils.isBlank(path)) {
			throw new FileNotFoundException("Model artifact is not available for type: " + normalizedType);
		}
		return path.trim();
	}

	public ArtifactMetadata inspect(String remotePath) throws IOException {
		String command = "if [ ! -f " + quote(remotePath) + " ]; then exit 44; fi; "
			+ "stat -c '%s' -- " + quote(remotePath) + "; "
			+ "sha256sum -- " + quote(remotePath) + " | awk '{print $1}'";
		SSHService.SSHExecutionResult result = sshService.executeCommand(
			sshProperties.getHost(),
			sshProperties.getPort(),
			sshProperties.getUsername(),
			sshProperties.getPassword(),
			command
		);
		if (!result.isSuccess() || StringUtils.isBlank(result.getOutput())) {
			throw new FileNotFoundException("Remote model file does not exist: " + remotePath);
		}
		String[] lines = result.getOutput().trim().split("\\R");
		if (lines.length < 2) {
			throw new IOException("Cannot read remote model metadata: " + remotePath);
		}
		try {
			long fileSize = Long.parseLong(lines[0].trim());
			String sha256 = lines[lines.length - 1].trim().toLowerCase(Locale.ROOT);
			if (!sha256.matches("[0-9a-f]{64}")) {
				throw new IOException("Remote model SHA-256 is invalid: " + remotePath);
			}
			return new ArtifactMetadata(fileName(remotePath), fileSize, sha256);
		} catch (NumberFormatException ex) {
			throw new IOException("Remote model size is invalid: " + remotePath, ex);
		}
	}

	/**
	 * Streams the remote file through SFTP without buffering the complete model in JVM memory.
	 */
	public void stream(String remotePath, OutputStream outputStream) throws IOException {
		Session session = null;
		Channel channel = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(sshProperties.getUsername(), sshProperties.getHost(), sshProperties.getPort());
			session.setPassword(sshProperties.getPassword());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30000);

			channel = session.openChannel("sftp");
			channel.connect(30000);
			((ChannelSftp) channel).get(remotePath, outputStream);
			outputStream.flush();
		} catch (Exception ex) {
			throw new IOException("Failed to stream remote model file: " + remotePath, ex);
		} finally {
			if (channel != null && channel.isConnected()) {
				channel.disconnect();
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
	}

	private String quote(String value) {
		return "'" + value.replace("'", "'\\\"'\\\"'") + "'";
	}

	private String fileName(String path) {
		int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return slash >= 0 ? path.substring(slash + 1) : path;
	}

	@Data
	@AllArgsConstructor
	public static class ArtifactMetadata {
		private String fileName;
		private long fileSize;
		private String sha256;
	}
}
