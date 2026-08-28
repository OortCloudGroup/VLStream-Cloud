/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.SysNotice;

import java.util.List;

/**
 * service layer
 *
 * @author Lion Li
 */
public interface ISysNoticeService {


    TableDataInfo<SysNotice> selectPageNoticeList(SysNotice notice, PageQuery pageQuery);

    /**
     * Query info
     *
     * @param noticeId ID
     * @return info
     */
    SysNotice selectNoticeById(Long noticeId);

    /**
     * Query list
     *
     * @param notice info
     * @return collection
     */
    List<SysNotice> selectNoticeList(SysNotice notice);

    /**
     * Add
     *
     * @param notice info
     * @return
     */
    int insertNotice(SysNotice notice);

    /**
     * Update
     *
     * @param notice info
     * @return
     */
    int updateNotice(SysNotice notice);

    /**
     * Delete info
     *
     * @param noticeId ID
     * @return
     */
    int deleteNoticeById(Long noticeId);

    /**
     * Batch delete info
     *
     * @param noticeIds need to Delete ID
     * @return
     */
    int deleteNoticeByIds(Long[] noticeIds);
}
