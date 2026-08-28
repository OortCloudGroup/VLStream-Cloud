/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.SysOssConfigBo;
import com.ruoyi.system.domain.vo.SysOssConfigVo;

import java.util.Collection;

/**
 * object configurationServiceinterface
 *
 * @author Lion Li
 * @author
 * @date 2021-08-13
 */
public interface ISysOssConfigService {

    /**
     * Initialize OSSconfiguration
     */
    void init();

    /**
     * Query
     */
    SysOssConfigVo queryById(Long ossConfigId);

    /**
     * Query list
     */
    TableDataInfo<SysOssConfigVo> queryPageList(SysOssConfigBo bo, PageQuery pageQuery);


    /**
     * Add object object configuration
     *
     * @param bo object configurationAdd object
     * @return
     */
    Boolean insertByBo(SysOssConfigBo bo);

    /**
     * objectUpdate object configuration
     *
     * @param bo object configuration object
     * @return
     */
    Boolean updateByBo(SysOssConfigBo bo);

    /**
     * Validate Delete data
     *
     * @param ids primary keycollection
     * @param isValid whether Validate ,true-Delete beforeValidate ,false- Validate
     * @return
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     *
     */
    int updateOssConfigStatus(SysOssConfigBo bo);

}
