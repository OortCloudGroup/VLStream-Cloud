package org.springblade.core.mp.support;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity-to-VO wrapper compatible with SpringBlade generated code.
 */
public abstract class BaseEntityWrapper<E, V> {

    /**
     * Converts one entity to its view object.
     */
    public abstract V entityVO(E entity);

    /**
     * Converts an entity list while preserving its order.
     */
    public List<V> listVO(List<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<V> result = new ArrayList<>(entities.size());
        for (E entity : entities) {
            result.add(entityVO(entity));
        }
        return result;
    }

    /**
     * Converts page records and preserves pagination metadata.
     */
    public IPage<V> pageVO(IPage<E> page) {
        Page<V> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setPages(page.getPages());
        result.setRecords(listVO(page.getRecords()));
        return result;
    }
}
