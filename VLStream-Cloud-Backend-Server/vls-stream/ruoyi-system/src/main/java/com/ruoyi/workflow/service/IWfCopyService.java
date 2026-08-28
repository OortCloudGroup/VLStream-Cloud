/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.bo.WfCopyBo;
import com.ruoyi.workflow.domain.bo.WfTaskBo;
import com.ruoyi.workflow.domain.vo.WfCopyVo;

import java.util.List;

/**
 * workflow Serviceinterface
 *
 * @author KonBAI
 * @date 2022-05-19
 */
public interface IWfCopyService {

    /**
     * Query workflow
     *
     * @param copyId workflow primary key
     * @return workflow
     */
    WfCopyVo queryById(Long copyId);

    /**
     * Query workflow list
     *
     * @param wfCopy workflow
     * @param sysUser
     * @return workflow collection
     */
    TableDataInfo<WfCopyVo> selectPageList(WfCopyBo wfCopy, PageQuery pageQuery, SysUser sysUser);

    /**
     * Query workflow list
     *
     * @param wfCopy workflow
     * @return workflow collection
     */
    List<WfCopyVo> selectList(WfCopyBo wfCopy);

    /**
     *
     *
     * @param taskBo
     * @param sysUser
     * @return
     */
//    Boolean makeCopy(WfTaskBo taskBo, SysUser sysUser);

    /**
     *
     * @param taskBo
     * @return
     */
    Boolean makeCopy(WfTaskBo taskBo, SysUser sysUser);


    /**
     * taskIDQuery user ID
     * @param taskId
     * @return
     */
    List<String> selectCopyUserIdByTaskId(String taskId);
}
