package org.springblade.core.mp.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.core.tool.utils.BeanUtil;

import java.util.Map;

/**
 * Minimal query and page adapter for the original SpringBlade controller code.
 */
public final class Condition {

    private Condition() {
    }

    /**
     * Creates a MyBatis-Plus page using the requested page number and size.
     */
    public static <T> IPage<T> getPage(Query query) {
        Query safeQuery = query == null ? new Query() : query;
        Page<T> page = new Page<>(safeQuery.getCurrent(), safeQuery.getSize());
        applyOrders(page, safeQuery.getAscs(), true);
        applyOrders(page, safeQuery.getDescs(), false);
        return page;
    }

    /**
     * Creates an equality wrapper from a populated entity.
     */
    public static <T> QueryWrapper<T> getQueryWrapper(T entity) {
        return entity == null ? new QueryWrapper<>() : new QueryWrapper<>(entity);
    }

    /**
     * Converts request parameters to an entity and then creates an equality wrapper.
     */
    public static <T> QueryWrapper<T> getQueryWrapper(Map<String, Object> params, Class<T> entityType) {
        if (params == null || params.isEmpty()) {
            return new QueryWrapper<>();
        }
        T entity = BeanUtil.copyProperties(params, entityType);
        return getQueryWrapper(entity);
    }

    /**
     * Adds comma-separated order columns to a MyBatis-Plus page.
     */
    private static void applyOrders(Page<?> page, String columns, boolean ascending) {
        if (columns == null || columns.trim().isEmpty()) {
            return;
        }
        for (String column : columns.split(",")) {
            String safeColumn = column.trim();
            if (!safeColumn.matches("[A-Za-z0-9_]+")) {
                continue;
            }
            if (ascending) {
                page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.asc(safeColumn));
            } else {
                page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc(safeColumn));
            }
        }
    }
}
