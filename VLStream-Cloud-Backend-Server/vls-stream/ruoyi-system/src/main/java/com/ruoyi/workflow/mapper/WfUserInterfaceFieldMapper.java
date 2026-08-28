/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.workflow.domain.WfUserInterfaceField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WfUserInterfaceFieldMapper extends BaseMapper<WfUserInterfaceField> {
    /**
     * Query user、 interface field_codes JSON
     */
    String selectFieldCodes(@Param("userId") String userId,
                            @Param("interfacePath") String interfacePath);

    /**
     * new configuration
     */
    int upsert(@Param("userId") String userId,
               @Param("interfacePath") String interfacePath,
               @Param("fieldCodes")String fieldCodes);
}
