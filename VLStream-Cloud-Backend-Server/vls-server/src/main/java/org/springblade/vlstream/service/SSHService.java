package org.springblade.vlstream.service;

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
            // Create JSch instance
            JSch jsch = new JSch();

            // Create session
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            // Set connection properties
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            // Connection
            session.connect(CONNECT_TIMEOUT);
            log.info("SSH connection succeeded: {}@{}:{}", username, host, port);

            // Create execution channel
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            // Get output stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);

            // Get error stream
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            channel.setErrStream(errorStream);

            // Execute command
            channel.connect(SESSION_TIMEOUT);
            log.info("SSH command execution: {}", command);

            // Wait for command execution to complete
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw interruptedException;
                }
            }

            // Get execution result, use UTF-8 encoding to ensure correct display of Chinese
            String output = outputStream.toString("UTF-8");
            String error = errorStream.toString("UTF-8");

            int exitStatus = channel.getExitStatus();

            // Set Result
            result.setSuccess(exitStatus == 0 || exitStatus == -1);
            result.setOutput(output);
            result.setErrorMsg(error);

            log.info("SSH command execution completed, output length: {}, error length: {}", output.length(), error.length());
//            if (org.bytedeco.librealsense.error.length() > 0) {
//                log.warn("SSH command execution error info: {}", error);
//            }
//            if (output.length() > 0) {
//                log.info("SSH command execution output: {}", output);
//            }

        } catch (Exception e) {
            log.error("SSH command execution failed: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());
        } finally {
            // Close connection
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
            log.info("SSH connection test succeeded: {}@{}:{}", username, host, port);
            return true;
        } catch (Exception e) {
            log.error("SSH connection test failed: {}", e.getMessage(), e);
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
