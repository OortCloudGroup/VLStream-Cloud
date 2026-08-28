/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysPost;
import com.ruoyi.system.domain.SysUserPost;
import com.ruoyi.system.mapper.SysPostMapper;
import com.ruoyi.system.mapper.SysUserPostMapper;
import com.ruoyi.system.service.ISysPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * info service layer Process
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysPostServiceImpl implements ISysPostService {

    private final SysPostMapper baseMapper;
    private final SysUserPostMapper userPostMapper;

    @Override
    public TableDataInfo<SysPost> selectPagePostList(SysPost post, PageQuery pageQuery) {
        LambdaQueryWrapper<SysPost> lqw = new LambdaQueryWrapper<SysPost>()
            .like(StringUtils.isNotBlank(post.getPostCode()), SysPost::getPostCode, post.getPostCode())
            .eq(StringUtils.isNotBlank(post.getStatus()), SysPost::getStatus, post.getStatus())
            .like(StringUtils.isNotBlank(post.getPostName()), SysPost::getPostName, post.getPostName());
        Page<SysPost> page = baseMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * Query infocollection
     *
     * @param post info
     * @return infocollection
     */
    @Override
    public List<SysPost> selectPostList(SysPost post) {
        return baseMapper.selectList(new LambdaQueryWrapper<SysPost>()
            .like(StringUtils.isNotBlank(post.getPostCode()), SysPost::getPostCode, post.getPostCode())
            .eq(StringUtils.isNotBlank(post.getStatus()), SysPost::getStatus, post.getStatus())
            .like(StringUtils.isNotBlank(post.getPostName()), SysPost::getPostName, post.getPostName()));
    }

    /**
     * Query all
     *
     * @return
     */
    @Override
    public List<SysPost> selectPostAll() {
        return baseMapper.selectList();
    }

    /**
     * IDQuery info
     *
     * @param postId ID
     * @return roleobjectinfo
     */
    @Override
    public SysPost selectPostById(Long postId) {
        return baseMapper.selectById(postId);
    }

    /**
     * user IDGet
     *
     * @param userId user ID
     * @return in ID
     */
    @Override
    public List<Long> selectPostListByUserId(String userId) {
        return baseMapper.selectPostListByUserId(userId);
    }

    /**
     * Validate whether
     *
     * @param post info
     * @return
     */
    @Override
    public boolean checkPostNameUnique(SysPost post) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysPost>()
            .eq(SysPost::getPostName, post.getPostName())
            .ne(ObjectUtil.isNotNull(post.getPostId()), SysPost::getPostId, post.getPostId()));
        return !exist;
    }

    /**
     * Validate whether
     *
     * @param post info
     * @return
     */
    @Override
    public boolean checkPostCodeUnique(SysPost post) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysPost>()
            .eq(SysPost::getPostCode, post.getPostCode())
            .ne(ObjectUtil.isNotNull(post.getPostId()), SysPost::getPostId, post.getPostId()));
        return !exist;
    }

    /**
     * IDQuery
     *
     * @param postId ID
     * @return
     */
    @Override
    public long countUserPostById(Long postId) {
        return userPostMapper.selectCount(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getPostId, postId));
    }

    /**
     * Delete info
     *
     * @param postId ID
     * @return
     */
    @Override
    public int deletePostById(Long postId) {
        return baseMapper.deleteById(postId);
    }

    /**
     * Batch delete info
     *
     * @param postIds need to Delete ID
     * @return
     */
    @Override
    public int deletePostByIds(Long[] postIds) {
        for (Long postId : postIds) {
            SysPost post = selectPostById(postId);
            if (countUserPostById(postId) > 0) {
                throw new ServiceException(String.format("%1$s已分配,不能删除", post.getPostName()));
            }
        }
        return baseMapper.deleteBatchIds(Arrays.asList(postIds));
    }

    /**
     * Add info
     *
     * @param post info
     * @return
     */
    @Override
    public int insertPost(SysPost post) {
        return baseMapper.insert(post);
    }

    /**
     * Update info
     *
     * @param post info
     * @return
     */
    @Override
    public int updatePost(SysPost post) {
        return baseMapper.updateById(post);
    }
}
