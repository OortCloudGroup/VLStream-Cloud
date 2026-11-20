package com.ruoyi.framework.config;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleImmutableEntry;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.util.ConcurrentLruCache;

/** Reuse the rewritten SQL only. MyBatis still binds the current viewer and filters on every call. */
final class TrainingPageTenantInterceptor extends TenantLineInnerInterceptor {
    private static final String PAGE = "com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmTrainingMapper.selectVlsAlgorithmTrainingPage";
    private final CustomTenantLineHandler handler;
    private final ConcurrentLruCache<SimpleImmutableEntry<String, String>, String> statements =
        new ConcurrentLruCache<>(128, key -> parserSingle(key.getValue(), null));

    TrainingPageTenantInterceptor(CustomTenantLineHandler handler) {
        super(handler);
        this.handler = handler;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement statement, Object parameter, RowBounds bounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (!PAGE.equals(statement.getId()) || InterceptorIgnoreHelper.willIgnoreTenantLine(statement.getId())) {
            super.beforeQuery(executor, statement, parameter, bounds, resultHandler, boundSql);
            return;
        }
        PluginUtils.MPBoundSql sql = PluginUtils.mpBoundSql(boundSql);
        sql.sql(statements.get(new SimpleImmutableEntry<>(handler.resolveTenantId(), sql.sql())));
    }
}
