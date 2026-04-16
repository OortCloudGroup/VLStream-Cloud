package com.vlstream.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

/**
 * SSH connection service class
 */
@Slf4j
@Service
public class SSHService {
    
    /**
     * SSH connection configuration
     */
    private static final int CONNECT_TIMEOUT = 30000;
    private static final int SESSION_TIMEOUT = 30000;
    
    /**
     * Execute SSH command
     */
    public SSHExecutionResult executeCommand(String host, int port, String username, String password, String command) {
        Session session = null;
        ChannelExec channel = null;
        SSHExecutionResult result = new SSHExecutionResult();
        
        try {
            // 创建JSch实例
            JSch jsch = new JSch();
            
            // 创建会话
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            
            // 设置连接属性
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            
            // 连接
            session.connect(CONNECT_TIMEOUT);
            log.info("SSH连接成功: {}@{}:{}", username, host, port);
            
            // 创建执行通道
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            // 获取输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);
            
            // 获取错误流
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            channel.setErrStream(errorStream);
            
            // 执行命令
            channel.connect();
            log.info("SSH命令执行: {}", command);
            
            // 等待命令执行完成
            while (channel.isConnected()) {
                Thread.sleep(100);
            }
            
            // 获取执行结果，使用UTF-8编码确保中文正确显示
            String output = outputStream.toString("UTF-8");
            String error = errorStream.toString("UTF-8");
            
            // 设置结果
            result.setSuccess(true);
            result.setOutput(output);
            result.setErrorMsg(error);
            
            log.info("SSH命令执行完成，输出长度: {}, 错误长度: {}", output.length(), error.length());
//            if (org.bytedeco.librealsense.error.length() > 0) {
//                log.warn("SSH命令执行错误信息: {}", error);
//            }
//            if (output.length() > 0) {
//                log.info("SSH命令执行输出: {}", output);
//            }
            
        } catch (Exception e) {
            log.error("SSH命令执行失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());
        } finally {
            // 关闭连接
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        
        return result;
    }
    
    /**
     * Test SSH connection
     */
    public boolean testConnection(String host, int port, String username, String password) {
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            
            session.connect(CONNECT_TIMEOUT);
            log.info("SSH连接测试成功: {}@{}:{}", username, host, port);
            return true;
        } catch (Exception e) {
            log.error("SSH连接测试失败: {}", e.getMessage(), e);
            return false;
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }
    
    /**
     * SSH execution result class
     */
    public static class SSHExecutionResult {
        private boolean success;
        private String output;
        private String errorMsg;
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getOutput() {
            return output;
        }
        
        public void setOutput(String output) {
            this.output = output;
        }
        
        public String getErrorMsg() {
            return errorMsg;
        }
        
        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }
    }
} 