package com.ruoyi.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.workflow.domain.WfUserInterfaceField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WfUserInterfaceFieldMapper extends BaseMapper<WfUserInterfaceField> {
    /**
     * 查询某用户、某接口的 field_codes JSON
     */
    String selectFieldCodes(@Param("userId") String userId,
                            @Param("interfacePath") String interfacePath);

    /**
     * 插入或更新一条配置
     */
    int upsert(@Param("userId") String userId,
               @Param("interfacePath") String interfacePath,
               @Param("fieldCodes")String fieldCodes);
}
