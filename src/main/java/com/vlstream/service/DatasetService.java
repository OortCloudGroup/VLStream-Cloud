package com.vlstream.service;

import javax.servlet.http.HttpServletResponse;

/**
 * 数据集服务接口
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
public interface DatasetService {

    /**
     * 连接远程服务器
     * 
     * @param host 服务器地址
     * @param username 用户名
     * @param password 密码
     * @param path 数据集路径
     * @return 是否连接成功
     */
    boolean connectToServer(String host, String username, String password, String path);

    /**
     * 获取数据集文件列表
     * 
     * @param host 服务器地址
     * @param path 数据集路径
     * @return 文件列表
     */
    Object getDatasetFiles(String host, String path);

    /**
     * 获取文件内容
     * 
     * @param host 服务器地址
     * @param path 数据集路径
     * @param filename 文件名
     * @return 文件内容
     */
    String getFileContent(String host, String path, String filename);

    /**
     * 下载文件
     * 
     * @param host 服务器地址
     * @param path 数据集路径
     * @param filename 文件名
     * @param response HTTP响应对象
     */
    void downloadFile(String host, String path, String filename, HttpServletResponse response);
} 