package org.springblade.core.mp.base;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;

/**
 * SpringBlade service API compatibility contract backed by MyBatis-Plus.
 *
 * @param <T> entity type
 */
public interface BaseService<T> extends IService<T> {

    /**
     * Performs MyBatis-Plus batch deletion, including configured logical deletion.
     */
    default boolean deleteLogic(Collection<Long> ids) {
        return removeByIds(ids);
    }

    /**
     * Queries one row using the non-null properties of the supplied entity.
     */
    default T queryDetail(T entity) {
        return getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(entity));
    }
}
