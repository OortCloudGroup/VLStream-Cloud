package org.springblade.modules.system.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springblade.modules.system.service.IFileUploadService;
import org.springblade.vlstream.pojo.dto.FileResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * <p>
 * File upload service implementation class
 * </p>
 *
 * @author OORT
 * @since 2025-04-07
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements IFileUploadService {

	//File upload method path
	@Value("${apaas.fileUpload}")
	private String fileUpload;

	@Override
	public File multipartFileToFile(MultipartFile multiFile) {
		if (multiFile == null) {
			return null;
		}
		String originalFileName = multiFile.getOriginalFilename();
		if (originalFileName == null || originalFileName.isBlank()) {
			originalFileName = multiFile.getName();
		}
		String safeFileName = originalFileName;
		int lastUnixSeparatorIndex = safeFileName.lastIndexOf('/');
		int lastWindowsSeparatorIndex = safeFileName.lastIndexOf('\\');
		int lastSeparatorIndex = Math.max(lastUnixSeparatorIndex, lastWindowsSeparatorIndex);
		if (lastSeparatorIndex >= 0 && lastSeparatorIndex < safeFileName.length() - 1) {
			safeFileName = safeFileName.substring(lastSeparatorIndex + 1);
		}
		try {
			Path tempDirectory = Files.createTempDirectory("upload-");
			File file = tempDirectory.resolve(safeFileName).toFile();
			multiFile.transferTo(file);
			return file;
		} catch (Exception exception) {
			log.error("multipartFileToFile failed: {}", safeFileName, exception);
			return null;
		}
	}

	@Override
	public FileResponseDto uploadFile(String appId, String secretKey, File file) {
		if (file == null) {
			log.error("File is empty");
			return null;
		}

		try {

			log.info("Start uploading file: {}", file.getName());
			HashMap<String, Object> paramMap = new HashMap<>();
			// File upload only requires specifying the key in the parameters (default file) and setting the value as the file object. For users, file upload is no different from ordinary form submission
			paramMap.put("file", file);

			String responseString = HttpRequest.post(fileUpload)
				.header("requestType", "app")
				.header("appId", appId)
				.header("secretKey", secretKey)
				.form(paramMap).timeout(5000).execute().body();
			log.info("File storage get response: {}", responseString);

			// Parse Response
			JSONObject jsonResponse = new JSONObject(responseString);
			if (jsonResponse.getInt("code") != 200) {
				String errorMsg = jsonResponse.getStr("msg");
				log.error("Upload failed: {}", errorMsg);
				return null;
			}
			// Get file path
			String data = jsonResponse.getStr("data");
			JSONObject dataObj = new JSONObject(data);
			FileResponseDto fileResponseDto = new FileResponseDto();
			fileResponseDto.setUrl(dataObj.getStr("url"));
			fileResponseDto.setMd5(dataObj.getStr("md5"));
			fileResponseDto.setPath(dataObj.getStr("path"));
			fileResponseDto.setDomain(dataObj.getStr("domain"));
			fileResponseDto.setScene(dataObj.getStr("scene"));
			fileResponseDto.setSize(dataObj.getLong("size"));
			fileResponseDto.setMtime(dataObj.getLong("mtime"));
			fileResponseDto.setScenes(dataObj.getStr("scenes"));
			fileResponseDto.setRetmsg(dataObj.getStr("retmsg"));
			fileResponseDto.setRetcode(dataObj.getInt("retcode"));
			fileResponseDto.setSrc(dataObj.getStr("src"));
			fileResponseDto.setDuration(dataObj.getInt("duration"));
			return fileResponseDto;
		} catch (Exception e) {
			log.error("File upload failed:{}", file.getName());
			throw new RuntimeException("File upload failed:" + e.getMessage());
		}
	}


}
