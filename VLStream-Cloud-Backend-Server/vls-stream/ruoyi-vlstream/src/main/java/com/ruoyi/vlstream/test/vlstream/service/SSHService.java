/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

/**
 * SSH service
 */
@Slf4j
@Service
public class SSHService {

    /**
     * SSH configuration
     */
    private static final int CONNECT_TIMEOUT = 30000;
    private static final int SESSION_TIMEOUT = 30000;

    /**
     * Execute SSH
     */
    public SSHExecutionResult executeCommand(String host, int port, String username, String password, String command) {
        Session session = null;
        ChannelExec channel = null;
        SSHExecutionResult result = new SSHExecutionResult();

        try {
            // JSchinstance
            JSch jsch = new JSch();

            // will
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            // Set property
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            //
            session.connect(CONNECT_TIMEOUT);
            log.debug("SSH连接成功: {}@{}:{}", username, host, port);

            // Execute channel
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            // Get
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);

            // Get
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            channel.setErrStream(errorStream);

            // Execute
            channel.connect(SESSION_TIMEOUT);
            log.debug("SSH命令执行: {}", command);

            // etc. Execute
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw interruptedException;
                }
            }

            // Get Execute , UTF-8 in correct
            String output = outputStream.toString("UTF-8");
            String error = errorStream.toString("UTF-8");

            int exitStatus = channel.getExitStatus();

            // Set
            result.setSuccess(exitStatus == 0 || exitStatus == -1);
            result.setOutput(output);
            result.setErrorMsg(error);

            log.debug("SSH命令执行完成，输出长度: {}, 错误长度: {}", output.length(), error.length());
//            if (org.bytedeco.librealsense.error.length() > 0) {
// log.warn("SSH Execute info: {}", error);
//            }
//            if (output.length() > 0) {
// log.info("SSH Execute : {}", output);
//            }

        } catch (Exception e) {
            log.error("SSH命令执行失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());
        } finally {
            //
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
     * SSH
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
     * SSHExecute
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
