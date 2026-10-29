package com.ruoyi.vlstream.test.vlstream.compute;

import com.jcraft.jsch.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Each session belongs to one persisted node; never falls back to the global GPU host. */
@Service
@RequiredArgsConstructor
public class ComputeSsh {
    private final ComputeCredentialCipher cipher;
    private final ComputeTrustedSelfPolicy trustedSelfPolicy;
    @Autowired(required = false)
    private ComputeDeploymentSshIdentity deploymentIdentity;
    @Autowired(required = false)
    private ComputeApprovedAutoDlSshIdentity approvedAutoDlIdentity;
    @Autowired(required = false)
    private ConfiguredOfficialSshIdentity officialIdentity;

    /** Validate the administrator-owned key reference without reading private material. */
    public void requireDeploymentIdentity(ComputeNode node) throws IOException {
        if ("autodl".equals(node.getProvider())) {
            if(node.isConfigurationManaged()) {
                if(officialIdentity==null)throw new IOException("官方算力SSH部署身份未配置");
                officialIdentity.resolve(node);return;
            }
            if (approvedAutoDlIdentity == null) throw new IOException("AutoDL本地部署身份未配置");
            approvedAutoDlIdentity.requireConfigured(node);
            return;
        }
        if (deploymentIdentity == null) throw new IOException("后台部署SSH身份未配置");
        deploymentIdentity.requireConfigured(node);
    }

    public Connection open(ComputeNode node) throws IOException {
        Session session = null;
        JSch jsch = null;
        try {
            ComputeTrustedSelfPolicy.Target target = trustedSelfPolicy.resolve(node);
            boolean keyMode = ComputeDeploymentSshIdentity.KEY.equals(node.getAuthMode());
            ComputeApprovedAutoDlSshIdentity.Reference autoDlReference = null;
            ConfiguredOfficialSshIdentity.Reference officialReference = null;
            if (keyMode && "autodl".equals(node.getProvider())) {
                if(node.isConfigurationManaged()) {
                    if(officialIdentity==null)throw new IOException("官方算力SSH部署身份未配置");
                    officialReference=officialIdentity.resolve(node);target=officialReference.bind(target);
                } else {
                if (approvedAutoDlIdentity == null) throw new IOException("AutoDL本地部署身份未配置");
                autoDlReference = approvedAutoDlIdentity.resolve(node);
                target = autoDlReference.bind(target);
                }
            }
            InetAddress[] addresses = target.addresses();
            jsch = new JSch();
            String alias = node.getHost() + ":" + node.getPort();
            boolean pinned = target.hostKey() != null && !target.hostKey().isEmpty();
            if (pinned) jsch.getHostKeyRepository().add(new HostKey(alias, Base64.getDecoder().decode(target.hostKey())), null);
            if (keyMode) {
                requireDeploymentIdentity(node);
                if (!pinned) throw new IOException("后台部署SSH身份必须绑定已核验主机公钥");
                if (officialReference != null) officialReference.addIdentity(jsch);
                else if (autoDlReference != null) autoDlReference.addIdentity(jsch);
                else deploymentIdentity.addIdentity(node, jsch);
            } else if (!ComputeDeploymentSshIdentity.PASSWORD.equals(node.getAuthMode())) {
                throw new IOException("SSH认证模式无效");
            }
            session = jsch.getSession(node.getUsername(), node.getHost(), node.getPort());
            session.setSocketFactory(verifiedSockets(addresses));
            session.setHostKeyAlias(alias);
            session.setConfig("StrictHostKeyChecking", pinned ? "yes" : "no");
            if (target.requiredAlgorithm() != null) session.setConfig("server_host_key", target.requiredAlgorithm());
            session.setConfig("PreferredAuthentications", keyMode ? "publickey" : "password,keyboard-interactive");
            if (!keyMode) session.setPassword(cipher.decrypt(node.getPasswordCipher()));
            session.setTimeout(30000);
            session.connect(20000);
            return new Connection(session, jsch);
        } catch (Exception e) {
            if (session != null) session.disconnect();
            if (jsch != null) try { jsch.removeAllIdentity(); } catch (Exception ignored) { }
            // An exception cause from a key parser may contain a private file path.
            throw new IOException("SSH 连接失败，请检查实例、认证模式和主机指纹");
        }
    }

    public static String quote(String text) { return "'" + text.replace("'", "'\"'\"'") + "'"; }

    /** Preserve hostname routing (including local DNS proxies), checking the connected address before SSH sends credentials. */
    public static SocketFactory verifiedSockets(InetAddress[] validated) {
        final InetAddress[] allowed = validated.clone();
        return new SocketFactory() {
            @Override public Socket createSocket(String host, int port) throws IOException {
                AtomicBoolean cancelled = new AtomicBoolean();
                AtomicReference<Socket> opened = new AtomicReference<>();
                FutureTask<Socket> task = new FutureTask<>(() -> {
                    Socket socket = new Socket(host, port);
                    opened.set(socket);
                    if (cancelled.get() || !Arrays.asList(allowed).contains(socket.getInetAddress())) {
                        socket.close();
                        throw new IOException("SSH 连接地址与已校验的 DNS 地址不一致或连接已取消");
                    }
                    return socket;
                });
                Thread opener = new Thread(task, "vls-compute-connect");
                opener.setDaemon(true); opener.start();
                try { return task.get(20, TimeUnit.SECONDS); }
                catch (Exception e) {
                    cancelled.set(true); task.cancel(true);
                    Socket socket = opened.get();
                    if (socket != null) try { socket.close(); } catch (IOException ignored) { }
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    throw new IOException("SSH 网络连接失败或超时", e);
                }
            }
            @Override public InputStream getInputStream(Socket socket) throws IOException { return socket.getInputStream(); }
            @Override public OutputStream getOutputStream(Socket socket) throws IOException { return socket.getOutputStream(); }
        };
    }

    public static class Connection implements AutoCloseable {
        private final Session session;
        private final JSch identityOwner;
        private ChannelSftp sftp;
        public Connection(Session session) { this(session, null); }
        Connection(Session session, JSch identityOwner) { this.session = session; this.identityOwner = identityOwner; }
        public String hostKey() { return session.getHostKey().getKey(); }

        public String execute(String command, int timeoutSeconds) throws IOException {
            ChannelExec channel = null;
            try {
                channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(command);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                OutputStream bounded = new OutputStream() {
                    @Override public void write(int value) throws IOException {
                        if (bytes.size() >= 1024 * 1024) throw new IOException("远程命令输出过大");
                        bytes.write(value);
                    }
                    @Override public void write(byte[] value, int off, int len) throws IOException {
                        if (bytes.size() + len > 1024 * 1024) throw new IOException("远程命令输出过大");
                        bytes.write(value, off, len);
                    }
                };
                channel.setOutputStream(bounded);
                channel.setErrStream(bounded);
                channel.connect(20000);
                long deadline = System.nanoTime() + timeoutSeconds * 1000000000L;
                while (!channel.isClosed()) {
                    if (System.nanoTime() > deadline) throw new IOException("远程命令超时");
                    Thread.sleep(50);
                }
                if (channel.getExitStatus() != 0) throw new IOException("远程命令执行失败，请检查实例环境或训练日志");
                return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); throw new IOException("远程操作已中断", e);
            } catch (JSchException e) { throw new IOException("SSH 命令通道不可用", e); }
            finally { if (channel != null) channel.disconnect(); }
        }

        private ChannelSftp sftp() throws JSchException {
            if (sftp == null) { sftp = (ChannelSftp) session.openChannel("sftp"); sftp.connect(20000); }
            return sftp;
        }
        public void mkdir(String path) throws IOException {
            try {
                String current = "";
                for (String part : path.split("/")) {
                    if (part.isEmpty()) continue;
                    current += "/" + part;
                    try { sftp().stat(current); }
                    catch (SftpException e) { if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) throw e; sftp().mkdir(current); }
                }
            } catch (Exception e) { throw new IOException("无法创建实例工作目录", e); }
        }
        public void put(String path, InputStream input) throws IOException {
            try { sftp().put(input, path); } catch (Exception e) { throw new IOException("实例文件上传失败", e); }
        }
        public void putText(String path, String text) throws IOException {
            put(path, new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        }
        public void download(String path, Path target) throws IOException {
            try (OutputStream output = Files.newOutputStream(target)) { sftp().get(path, output); }
            catch (Exception e) { throw new IOException("实例产物下载失败", e); }
        }
        @Override public void close() { if (sftp != null) sftp.disconnect(); session.disconnect(); if (identityOwner != null) try { identityOwner.removeAllIdentity(); } catch (Exception ignored) { } }
    }
}
