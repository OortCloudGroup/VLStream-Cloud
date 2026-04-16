package com.vlstream.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import com.vlstream.dto.FileResponseDto;
import com.vlstream.service.IFileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;

/**
 * <p>
 * File Upload Service Implementation Class
 * </p>
 *
 * @author OORT
 * @since 2025-04-07
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements IFileUploadService {

	// File upload method path
	@Value("${apaas.fileUpload}")
	private String fileUpload;

	@Override
	public File multipartFileToFile(MultipartFile multiFile) {
		// Get file name
		String fileName = multiFile.getOriginalFilename();
		// Get file extension
		String prefix = fileName.substring(fileName.lastIndexOf("."));
		// If you need to prevent duplicate temporary files, you can add a random code after the file name
		try {
			File file = File.createTempFile(fileName, prefix);
			multiFile.transferTo(file);
			return file;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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
			// For file upload, just specify the key in the parameter (default is file) and set the value to the file object. For users, file upload is no different from ordinary form submission
			paramMap.put("file", file);

			String responseString = HttpRequest.post(fileUpload)
				.header("requestType", "app")
				.header("appId", appId)
				.header("secretKey", secretKey)
				.form(paramMap).timeout(5000).execute().body();
//			log.info("File storage response: {}", responseString);

			// Parse response
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
			throw new RuntimeException("File upload failed: " + e.getMessage());
		}
	}


}
