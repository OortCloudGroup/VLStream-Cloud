package com.ruoyi.vlstream.test.vlstream.compute;

import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Removes this public instance's recorded training run directories before the claim is released. */
@Service
@RequiredArgsConstructor
public class ComputeWorkspaceCleanupService {
    private final JdbcTemplate jdbc;
    private final ComputeSsh ssh;

    /**
     * Only paths recorded by this instance's training jobs and inside a registered work directory
     * are removed. Any unverifiable path or failed deletion aborts the release, so an unverified
     * workspace is never handed to another user.
     */
    public int clean(ComputeNode node, String instanceKey) throws IOException {
        if (instanceKey == null || instanceKey.isEmpty()) return 0;
        List<String> roots = new java.util.ArrayList<>(jdbc.queryForList(
            "SELECT DISTINCT work_dir FROM vls_compute_node WHERE public_instance_key=? AND work_dir IS NOT NULL",
            String.class, instanceKey));
        if (node.getWorkDir() != null && !node.getWorkDir().trim().isEmpty() && !roots.contains(node.getWorkDir()))
            roots.add(node.getWorkDir());
        List<String> dirs = jdbc.queryForList(
            "SELECT DISTINCT run_dir FROM vls_cloud_training_job WHERE public_instance_key=? AND run_dir IS NOT NULL",
            String.class, instanceKey);
        if (dirs.isEmpty()) return 0;
        for (String dir : dirs) {
            if (!inside(dir, roots)) throw new IOException("任务目录不在已登记工作目录内，拒绝清理");
        }
        try (ComputeSsh.Connection connection = ssh.open(node)) {
            int cleaned = 0;
            for (String dir : dirs) {
                connection.execute("rm -rf " + ComputeSsh.quote(dir) + " && test ! -e " + ComputeSsh.quote(dir), 60);
                cleaned++;
            }
            return cleaned;
        }
    }

    private static boolean inside(String dir, List<String> roots) {
        if (dir == null || dir.trim().isEmpty() || dir.contains("..")
            || dir.indexOf('\n') >= 0 || dir.indexOf('\r') >= 0) return false;
        for (String root : roots) {
            if (root == null || root.trim().isEmpty()) continue;
            String prefix = root.endsWith("/") ? root : root + "/";
            if (dir.startsWith(prefix) && dir.length() > prefix.length()) return true;
        }
        return false;
    }
}
