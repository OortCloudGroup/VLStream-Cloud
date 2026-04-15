package com.vlstream.service;

import com.vlstream.dto.FileResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * <p>
 * 文件上传服务接口
 * </p>
 *
 * @author 刘鑫
 * @since 2025-04-01
 */
public interface IFileUploadService {

    /**
     * 转换文件
     * @param multiFile
     * @return
     */
    public File multipartFileToFile(MultipartFile multiFile);

    /**
     * 上传文件到文件存储服务
     *
     * @param file 文件
     * @return 文件响应信息
     */
    FileResponseDto uploadFile(String appId, String secretKey, File file);

}
