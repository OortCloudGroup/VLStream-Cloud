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
 * 文件上传服务实现类
 * </p>
 *
 * @author OORT
 * @since 2025-04-07
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements IFileUploadService {

	//文件上传方法路径
	@Value("${apaas.fileUpload}")
	private String fileUpload;

	@Override
	public File multipartFileToFile(MultipartFile multiFile) {
		// 获取文件名
		String fileName = multiFile.getOriginalFilename();
		// 获取文件后缀
		String prefix = fileName.substring(fileName.lastIndexOf("."));
		// 若须要防止生成的临时文件重复,能够在文件名后添加随机码
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
			log.error("文件为空");
			return null;
		}

		try {

			log.info("开始上传文件: {}", file.getName());
			HashMap<String, Object> paramMap = new HashMap<>();
			//文件上传只需将参数中的键指定（默认file），值设为文件对象即可，对于使用者来说，文件上传与普通表单提交并无区别
			paramMap.put("file", file);

			String responseString = HttpRequest.post(fileUpload)
				.header("requestType", "app")
				.header("appId", appId)
				.header("secretKey", secretKey)
				.form(paramMap).timeout(5000).execute().body();
//			log.info("文件存储获取响应: {}", responseString);

			// 解析响应
			JSONObject jsonResponse = new JSONObject(responseString);
			if (jsonResponse.getInt("code") != 200) {
				String errorMsg = jsonResponse.getStr("msg");
				log.error("Upload failed: {}", errorMsg);
				return null;
			}
			// 获取文件路径
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
			log.error("文件上传失败:{}", file.getName());
			throw new RuntimeException("文件上传失败: " + e.getMessage());
		}
	}


}
