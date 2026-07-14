/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.WfFormApp;
import com.ruoyi.workflow.domain.bo.WfFormAppBo;
import com.ruoyi.workflow.domain.vo.WfFormAppVo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 表单应用分类Service接口
 *
 * @author 雷超群
 * @date 2025-04-26
 */
public interface IWfFormAppService extends IService<WfFormApp> {

    /**
     * 查询表单应用分类
     */
    WfFormAppVo queryById(String categoryId);

    /**
     * 查询表单应用分类列表
     */
    TableDataInfo<WfFormAppVo> queryPageList(WfFormAppBo bo, PageQuery pageQuery);

    /**
     * 查询表单应用分类列表
     */
    Optional<List<WfFormAppVo>> queryList(WfFormAppBo bo);

    /**
     * 新增表单应用分类
     */
    WfFormApp insertByBo(WfFormAppBo bo);

    /**
     * 修改表单应用分类
     */
    Boolean updateByBo(WfFormAppBo bo);

    /**
     * 校验并批量删除表单应用分类信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
