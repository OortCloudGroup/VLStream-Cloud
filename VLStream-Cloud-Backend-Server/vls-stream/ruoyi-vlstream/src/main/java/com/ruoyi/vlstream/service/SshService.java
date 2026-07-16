/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

/**
 * SSH command executor used for remote VLS model training and model file access.
 */
@Slf4j
@Service
public class SshService {

    private static final int CONNECT_TIMEOUT = 30000;
    private static final int SESSION_TIMEOUT = 30000;

    public SshExecutionResult executeCommand(String host, Integer port, String username, String password, String command) {
        SshExecutionResult result = new SshExecutionResult();
        if (isBlank(host) || port == null || isBlank(username) || isBlank(password) || isBlank(command)) {
            result.setSuccess(false);
            result.setErrorMsg("SSH host, port, username, password and command are required");
            return result;
        }

        Session session = null;
        ChannelExec channel = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(CONNECT_TIMEOUT);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);
            channel.setErrStream(errorStream);
            channel.connect(SESSION_TIMEOUT);

            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw interruptedException;
                }
            }

            String output = outputStream.toString("UTF-8");
            String error = errorStream.toString("UTF-8");
            int exitStatus = channel.getExitStatus();
            result.setSuccess(exitStatus == 0);
            result.setOutput(output);
            result.setErrorMsg(error);
            result.setExitStatus(exitStatus);
            log.info("SSH command completed: host={}, exitStatus={}, outputLength={}, errorLength={}",
                host, exitStatus, output.length(), error.length());
        } catch (Exception ex) {
            log.error("SSH command failed: host={}, error={}", host, ex.getMessage(), ex);
            result.setSuccess(false);
            result.setErrorMsg(ex.getMessage());
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        return result;
    }

    public boolean testConnection(String host, Integer port, String username, String password) {
        SshExecutionResult result = executeCommand(host, port, username, password, "echo OK");
        return result.isSuccess();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Data
    public static class SshExecutionResult {
        private boolean success;
        private String output;
        private String errorMsg;
        private Integer exitStatus;
    }
}
