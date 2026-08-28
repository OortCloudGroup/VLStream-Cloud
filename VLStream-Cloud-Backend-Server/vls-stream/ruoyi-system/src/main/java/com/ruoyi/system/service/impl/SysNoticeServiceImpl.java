/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.mapper.SysNoticeMapper;
import com.ruoyi.system.service.ISysNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * service layer
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysNoticeServiceImpl implements ISysNoticeService {

    private final SysNoticeMapper baseMapper;

    @Override
    public TableDataInfo<SysNotice> selectPageNoticeList(SysNotice notice, PageQuery pageQuery) {
        LambdaQueryWrapper<SysNotice> lqw = new LambdaQueryWrapper<SysNotice>()
            .like(StringUtils.isNotBlank(notice.getNoticeTitle()), SysNotice::getNoticeTitle, notice.getNoticeTitle())
            .eq(StringUtils.isNotBlank(notice.getNoticeType()), SysNotice::getNoticeType, notice.getNoticeType())
            .like(StringUtils.isNotBlank(notice.getCreateBy()), SysNotice::getCreateBy, notice.getCreateBy());
        Page<SysNotice> page = baseMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * Query info
     *
     * @param noticeId ID
     * @return info
     */
    @Override
    public SysNotice selectNoticeById(Long noticeId) {
        return baseMapper.selectById(noticeId);
    }

    /**
     * Query list
     *
     * @param notice info
     * @return collection
     */
    @Override
    public List<SysNotice> selectNoticeList(SysNotice notice) {
        return baseMapper.selectList(new LambdaQueryWrapper<SysNotice>()
            .like(StringUtils.isNotBlank(notice.getNoticeTitle()), SysNotice::getNoticeTitle, notice.getNoticeTitle())
            .eq(StringUtils.isNotBlank(notice.getNoticeType()), SysNotice::getNoticeType, notice.getNoticeType())
            .like(StringUtils.isNotBlank(notice.getCreateBy()), SysNotice::getCreateBy, notice.getCreateBy()));
    }

    /**
     * Add
     *
     * @param notice info
     * @return
     */
    @Override
    public int insertNotice(SysNotice notice) {
        return baseMapper.insert(notice);
    }

    /**
     * Update
     *
     * @param notice info
     * @return
     */
    @Override
    public int updateNotice(SysNotice notice) {
        return baseMapper.updateById(notice);
    }

    /**
     * Delete object
     *
     * @param noticeId ID
     * @return
     */
    @Override
    public int deleteNoticeById(Long noticeId) {
        return baseMapper.deleteById(noticeId);
    }

    /**
     * Batch delete info
     *
     * @param noticeIds need to Delete ID
     * @return
     */
    @Override
    public int deleteNoticeByIds(Long[] noticeIds) {
        return baseMapper.deleteBatchIds(Arrays.asList(noticeIds));
    }
}
