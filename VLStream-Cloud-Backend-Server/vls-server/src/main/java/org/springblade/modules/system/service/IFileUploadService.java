package org.springblade.modules.system.service;

import org.springblade.vlstream.pojo.dto.FileResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * <p>
 * File upload service interface
 * </p>
 *
 * @author Liu Xin
 * @since 2025-04-01
 */
public interface IFileUploadService {

	/**
	 * Convert file
	 * @param multiFile
	 * @return
	 */
	public File multipartFileToFile(MultipartFile multiFile);

	/**
     * Upload file to file storage service
     *
     * @param file file
     * @return file response information
     */
    FileResponseDto uploadFile(String appId, String secretKey, File file);

}
