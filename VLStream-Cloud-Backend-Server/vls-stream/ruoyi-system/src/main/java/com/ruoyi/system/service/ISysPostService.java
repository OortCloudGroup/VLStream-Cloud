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
import com.ruoyi.system.domain.SysPost;

import java.util.List;

/**
 * info service layer
 *
 * @author Lion Li
 */
public interface ISysPostService {


    TableDataInfo<SysPost> selectPagePostList(SysPost post, PageQuery pageQuery);

    /**
     * Query infocollection
     *
     * @param post info
     * @return
     */
    List<SysPost> selectPostList(SysPost post);

    /**
     * Query all
     *
     * @return
     */
    List<SysPost> selectPostAll();

    /**
     * IDQuery info
     *
     * @param postId ID
     * @return roleobjectinfo
     */
    SysPost selectPostById(Long postId);

    /**
     * user IDGet
     *
     * @param userId user ID
     * @return in ID
     */
    List<Long> selectPostListByUserId(String userId);

    /**
     * Validate
     *
     * @param post info
     * @return
     */
    boolean checkPostNameUnique(SysPost post);

    /**
     * Validate
     *
     * @param post info
     * @return
     */
    boolean checkPostCodeUnique(SysPost post);

    /**
     * IDQuery
     *
     * @param postId ID
     * @return
     */
    long countUserPostById(Long postId);

    /**
     * Delete info
     *
     * @param postId ID
     * @return
     */
    int deletePostById(Long postId);

    /**
     * Batch delete info
     *
     * @param postIds need to Delete ID
     * @return
     */
    int deletePostByIds(Long[] postIds);

    /**
     * Add info
     *
     * @param post info
     * @return
     */
    int insertPost(SysPost post);

    /**
     * Update info
     *
     * @param post info
     * @return
     */
    int updatePost(SysPost post);
}
