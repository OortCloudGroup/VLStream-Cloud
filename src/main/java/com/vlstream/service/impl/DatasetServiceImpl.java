package com.vlstream.service.impl;

import com.vlstream.service.DatasetService;
import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * 数据集服务实现类
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class DatasetServiceImpl implements DatasetService {

    @Value("${vlstream.ssh.username}")
    private String sshUsername;

    @Value("${vlstream.ssh.password}")
    private String sshPassword;

    @Value("${vlstream.ssh.port}")
    private Integer sshPort;

    @Override
    public boolean connectToServer(String host, String username, String password, String path) {
        JSch jsch = new JSch();
        Session session = null;
        
        try {
            log.info("尝试连接服务器：{}@{}", username, host);
            
            // 创建SSH会话
            session = jsch.getSession(username, host, sshPort);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000); // 30秒超时
            
            log.info("SSH连接成功：{}@{}", username, host);
            
            // 测试SFTP连接
            Channel channel = session.openChannel("sftp");
            channel.connect(30000);
            ChannelSftp sftp = (ChannelSftp) channel;
            
            // 尝试访问指定路径
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
            // 创建SSH会话
            session = jsch.getSession(sshUsername, host, sshPort);
            session.setPassword(sshPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);
            
            // 创建SFTP通道
            Channel channel = session.openChannel("sftp");
            channel.connect(30000);
            sftp = (ChannelSftp) channel;
            
            // 切换到指定路径
            sftp.cd(path);
            
            // 获取文件列表
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
            // 创建SSH会话
            session = jsch.getSession(sshUsername, host, sshPort);
            session.setPassword(sshPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);
            
            // 创建SFTP通道
            Channel channel = session.openChannel("sftp");
            channel.connect(30000);
            sftp = (ChannelSftp) channel;
            
            // 切换到指定路径
            sftp.cd(path);
            
            // 读取文件内容
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
            // 创建SSH会话
            session = jsch.getSession(sshUsername, host, sshPort);
            session.setPassword(sshPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);
            
            // 创建SFTP通道
            Channel channel = session.openChannel("sftp");
            channel.connect(30000);
            sftp = (ChannelSftp) channel;
            
            // 切换到指定路径
            sftp.cd(path);
            
            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            
            // 下载文件
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
     * 创建远程目录
     * 
     * @param sftp SFTP通道
     * @param path 目录路径
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
                // 目录不存在，创建它
                sftp.mkdir(currentPath);
                log.info("创建远程目录：{}", currentPath);
            }
        }
    }

    /**
     * 格式化文件大小
     * 
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小
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
