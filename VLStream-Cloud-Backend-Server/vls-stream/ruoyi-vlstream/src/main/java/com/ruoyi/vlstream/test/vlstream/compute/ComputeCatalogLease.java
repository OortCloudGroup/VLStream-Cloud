package com.ruoyi.vlstream.test.vlstream.compute;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

/** Serializes target replacement with committed admission across VLS instances sharing a database. */
final class ComputeCatalogLease implements AutoCloseable {
    private final Connection connection;
    private ComputeCatalogLease(Connection connection) { this.connection = connection; }

    static ComputeCatalogLease acquire(DataSource source) {
        Connection connection = null;
        try {
            connection = source.getConnection();
            connection.setAutoCommit(true);
            try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?,15)")) {
                statement.setString(1, "vls-compute-catalog");
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next() || result.getInt(1) != 1) throw new IllegalStateException("Compute catalog lock unavailable");
                }
            }
            return new ComputeCatalogLease(connection);
        } catch (Exception unavailable) {
            if (connection != null) try { new ComputeCatalogLease(connection).close(); } catch (Exception ignored) { }
            throw ConfiguredComputeRegistry.unavailable("算力目录与任务提交正在同步，请稍后重试");
        }
    }

    org.springframework.jdbc.core.JdbcTemplate currentReads() {
        return new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true));
    }

    @Override public void close() {
        boolean reusable = false;
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, "vls-compute-catalog");
            try (ResultSet result = statement.executeQuery()) { reusable = result.next() && result.getInt(1) == 1; }
        } catch (Exception ignored) { }
        if (!reusable && !discardDruidConnection()) try { connection.abort(Runnable::run); }
        catch (Exception failedAbort) { throw new IllegalStateException("Could not destroy the compute catalog lock connection"); }
        try { connection.close(); } catch (Exception ignored) { }
    }

    /** The business module does not depend on a pool implementation. Druid needs its holder discarded. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean discardDruidConnection() {
        try {
            Class type = Class.forName("com.alibaba.druid.pool.DruidPooledConnection");
            Object pooled = type.isInstance(connection) ? connection : connection.unwrap(type);
            Object holder = type.getMethod("getConnectionHolder").invoke(pooled);
            Object source = holder.getClass().getMethod("getDataSource").invoke(holder);
            source.getClass().getMethod("discardConnection", holder.getClass()).invoke(source, holder);
            type.getMethod("disable").invoke(pooled); return true;
        } catch (Exception unavailable) { return false; }
    }
}
