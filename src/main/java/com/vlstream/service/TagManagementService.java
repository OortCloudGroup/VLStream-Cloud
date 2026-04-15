package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.entity.TagManagement;

import java.util.List;

/**
 * 标签管理 Service 接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface TagManagementService extends IService<TagManagement> {

    /**
     * 获取标签树形结构
     *
     * @return 标签树
     */
    List<TagManagement> getTagTree();

    /**
     * 根据类型获取标签树
     *
     * @param tagType 标签类型(own-自有, public-公共)
     * @return 标签树
     */
    List<TagManagement> getTagTreeByType(String tagType);

    /**
     * 创建标签
     *
     * @param tagManagement 标签信息
     * @return 创建成功的标签
     */
    TagManagement createTag(TagManagement tagManagement);

    /**
     * 更新标签
     *
     * @param tagManagement 标签信息
     * @return 更新成功的标签
     */
    TagManagement updateTag(TagManagement tagManagement);

    /**
     * 删除标签（级联删除子标签）
     *
     * @param tagId 标签ID
     * @return 是否删除成功
     */
    boolean deleteTag(Long tagId);

    /**
     * 批量删除标签
     *
     * @param tagIds 标签ID列表
     * @return 是否删除成功
     */
    boolean deleteTags(List<Long> tagIds);

    /**
     * 移动标签（调整父级或排序）
     *
     * @param tagId 标签ID
     * @param targetParentId 目标父级ID
     * @param targetPosition 目标位置
     * @return 是否移动成功
     */
    boolean moveTag(Long tagId, Long targetParentId, Integer targetPosition);

    /**
     * 更新标签使用次数
     *
     * @param tagId 标签ID
     * @param increment 增加的次数
     */
    void updateUsageCount(Long tagId, Integer increment);

    /**
     * 检查标签名称是否重复（同级别下）
     *
     * @param tagName 标签名称
     * @param parentId 父级ID
     * @param excludeId 排除的ID（用于编辑时验证）
     * @return 是否重复
     */
    boolean isTagNameDuplicate(String tagName, Long parentId, Long excludeId);

    /**
     * 启用/禁用标签
     *
     * @param tagId 标签ID
     * @param isActive 是否启用
     * @return 是否操作成功
     */
    boolean toggleTagStatus(Long tagId, boolean isActive);

    /**
     * 获取标签的使用统计
     *
     * @param tagId 标签ID
     * @return 使用统计信息
     */
    TagManagement getTagUsageStats(Long tagId);

    /**
     * 根据分类类型获取标签
     *
     * @param categoryType 分类类型(own-自有, public-公共)
     * @return 标签列表
     */
    List<TagManagement> getTagsByCategory(String categoryType);

    /**
     * 分页查询标签管理
     *
     * @param page 分页参数
     * @param keyword 搜索关键字
     * @param categoryType 标签大类
     * @param level 标签层级
     * @param parentId 父级ID
     * @param tagId 标签ID
     * @return 分页结果
     */
    IPage<TagManagement> getTagManagementPage(Page<TagManagement> page, String keyword, String categoryType, Integer level, Long parentId, Long tagId);
} 