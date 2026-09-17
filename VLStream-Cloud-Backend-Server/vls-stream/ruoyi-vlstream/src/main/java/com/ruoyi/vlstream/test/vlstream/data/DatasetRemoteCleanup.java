package com.ruoyi.vlstream.test.vlstream.data;

import com.jcraft.jsch.*;
import com.ruoyi.vlstream.test.common.constant.CommonConstant;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

/** Only the generated directory for one numeric project ID is eligible. Never follows links. */
@Component
@RequiredArgsConstructor
public class DatasetRemoteCleanup {
    private final VlsSshProperties ssh;

    public String identity() { return ssh.getHost() + ":" + ssh.getPort() + ":" + ssh.getUsername() + ":" + CommonConstant.BASE_DATASETS_PATH; }

    static String root(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("无效的数据集编号");
        return CommonConstant.BASE_DATASETS_PATH + "vls/annotation_" + id;
    }

    public void remove(Long id) throws Exception {
        String root = root(id);
        Session session = null;
        ChannelSftp sftp = null;
        try {
            session = new JSch().getSession(ssh.getUsername(), ssh.getHost(), ssh.getPort());
            session.setPassword(ssh.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(20000);
            session.connect(15000);
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(15000);
            removeDirectory(sftp, id);
        } finally {
            if (sftp != null) sftp.disconnect();
            if (session != null) session.disconnect();
        }
    }

    void removeDirectory(ChannelSftp sftp, Long id) throws SftpException {
            String root = root(id);
            try { sftp.lstat(root); }
            catch (SftpException e) { if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) return; throw e; }
            if (!root.equals(sftp.realpath(root)) || sftp.lstat(root).isLink()) {
                throw new IllegalStateException("训练目录包含链接或不属于当前项目，已停止清理");
            }
            List<String> files = new ArrayList<>();
            List<String> directories = new ArrayList<>();
            collect(sftp, root, files, directories);
            // Complete the safety inventory before the first mutation.
            for (String file : files) sftp.rm(file);
            for (String directory : directories) sftp.rmdir(directory);
    }

    static boolean datasetFile(String name) {
        return name.matches("(?i).+\\.(jpg|jpeg|png|bmp|gif|webp|txt|yaml|yml|json|cache|npy)");
    }

    private void collect(ChannelSftp sftp, String directory, List<String> files, List<String> directories) throws SftpException {
        for (Object value : sftp.ls(directory)) {
            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) value;
            String name = entry.getFilename();
            if (".".equals(name) || "..".equals(name)) continue;
            if (name.contains("/") || entry.getAttrs().isLink()) throw new IllegalStateException("训练目录包含不安全路径，已停止清理");
            String path = directory + "/" + name;
            if (entry.getAttrs().isDir()) collect(sftp, path, files, directories);
            else {
                if (!entry.getAttrs().isReg() || !datasetFile(name)) throw new IllegalStateException("训练目录包含模型或未知文件，已停止清理");
                files.add(path);
            }
        }
        directories.add(directory);
    }
}
