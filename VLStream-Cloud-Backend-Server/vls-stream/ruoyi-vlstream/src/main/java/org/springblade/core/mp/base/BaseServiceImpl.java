package org.springblade.core.mp.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * SpringBlade service implementation compatibility base backed by the real mapper.
 *
 * @param <M> mapper type
 * @param <T> entity type
 */
public class BaseServiceImpl<M extends BaseMapper<T>, T>
    extends ServiceImpl<M, T> implements BaseService<T> {
}
