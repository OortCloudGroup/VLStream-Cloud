package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import com.ruoyi.vlstream.test.vlstream.excel.VlsTagManagementExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TagManagementDTO;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TagManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TagManagementVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsTagManagementService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsTagManagementWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 标签管理表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsTagManagement")
@Tag(name = "标签管理表", description = "标签管理表接口")
public class VlsTagManagementController extends BladeController {

	private final IVlsTagManagementService vlsTagManagementService;

	/**
	 * 标签管理表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description  = "传入vlsTagManagement")
	public R<TagManagementVO> detail(TagManagement vlsTagManagement) {
		TagManagement detail = vlsTagManagementService.getOne(Condition.getQueryWrapper(vlsTagManagement));
		return R.data(VlsTagManagementWrapper.build().entityVO(detail));
	}

	/**
	 * 标签管理表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description  = "传入vlsTagManagement")
	public R<IPage<TagManagementVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsTagManagement, Query query) {
		IPage<TagManagement> pages = vlsTagManagementService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsTagManagement, TagManagement.class));
		return R.data(VlsTagManagementWrapper.build().pageVO(pages));
	}


	/**
	 * 标签管理表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description  = "传入vlsTagManagement")
	public R<IPage<TagManagementVO>> page(TagManagementVO vlsTagManagement, Query query) {
		IPage<TagManagementVO> pages = vlsTagManagementService.selectVlsTagManagementPage(Condition.getPage(query), vlsTagManagement);
		return R.data(pages);
	}

	/**
	 * 标签管理表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description  = "传入vlsTagManagement")
	public R save(@Valid @RequestBody TagManagement vlsTagManagement) {
		return R.status(vlsTagManagementService.save(vlsTagManagement));
	}

	/**
	 * 标签管理表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description  = "传入vlsTagManagement")
	public R update(@Valid @RequestBody TagManagement vlsTagManagement) {
		return R.status(vlsTagManagementService.updateById(vlsTagManagement));
	}

	/**
	 * 标签管理表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description  = "传入vlsTagManagement")
	public R submit(@Valid @RequestBody TagManagement vlsTagManagement) {
		return R.status(vlsTagManagementService.saveOrUpdate(vlsTagManagement));
	}

	/**
	 * 标签管理表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description  = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsTagManagementService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsTagManagement")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description  = "传入vlsTagManagement")
	public void exportVlsTagManagement(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsTagManagement, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<TagManagement> queryWrapper = Condition.getQueryWrapper(vlsTagManagement, TagManagement.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsTagManagementEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsTagManagementEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsTagManagementExcel> list = vlsTagManagementService.exportVlsTagManagement(queryWrapper);
		ExcelUtil.export(response, "标签管理表数据" + DateUtil.today(), "标签管理表数据表", list, VlsTagManagementExcel.class);
	}

	@GetMapping("/tree")
	@Operation(description = "获取标签树形结构（用于左侧导航）")
	public R<List<Map<String, Object>>> getTagTree() {
		try {
			// 获取自有标签和公共标签数据
			List<TagManagement> ownTags = vlsTagManagementService.getTagsByCategory("own");
			List<TagManagement> publicTags = vlsTagManagementService.getTagsByCategory("public");

			// 构建返回结构
			Map<String, Object> result = new HashMap<>();

			// 构建自有标签树形结构
			Map<String, Object> ownRoot = new HashMap<>();
			ownRoot.put("id", "own");
			ownRoot.put("tagName", "自有标签");
			ownRoot.put("categoryType", "own");
			ownRoot.put("level", 0);
			ownRoot.put("children", buildTreeStructure(ownTags));

			// 构建公共标签树形结构
			Map<String, Object> publicRoot = new HashMap<>();
			publicRoot.put("id", "public");
			publicRoot.put("tagName", "公共标签");
			publicRoot.put("categoryType", "public");
			publicRoot.put("level", 0);
			publicRoot.put("children", buildTreeStructure(publicTags));

			List<Map<String, Object>> data = new ArrayList<>();
			data.add(ownRoot);
			data.add(publicRoot);

			return R.data(data);
		} catch (Exception e) {
			log.error("获取标签树失败", e);
			return R.fail("获取标签树失败: " + e.getMessage());
		}
	}

	/**
	 * 构建树形结构
	 */
	private List<Map<String, Object>> buildTreeStructure(List<TagManagement> tags) {
		List<Map<String, Object>> result = new ArrayList<>();

		// 获取所有标签类型（level=1）
		List<TagManagement> categories = tags.stream()
			.filter(tag -> tag.getLevel() == 1)
			.sorted(Comparator.comparing(TagManagement::getSortOrder))
			.collect(Collectors.toList());

		// 为每个标签类型构建子标签
		for (TagManagement category : categories) {
			Map<String, Object> categoryNode = new HashMap<>();
			categoryNode.put("id", category.getId());
			categoryNode.put("tagName", category.getTagName());
			categoryNode.put("categoryType", category.getCategoryType());
			categoryNode.put("level", category.getLevel());
			categoryNode.put("parentId", category.getParentId());
			categoryNode.put("sortOrder", category.getSortOrder());
			categoryNode.put("tagColor", category.getTagColor());
			categoryNode.put("tagIcon", category.getTagIcon());
			categoryNode.put("description", category.getDescription());
			categoryNode.put("isActive", category.getIsActive());
			categoryNode.put("usageCount", category.getUsageCount());
			categoryNode.put("createTime", category.getCreateTime());
			categoryNode.put("updateTime", category.getUpdateTime());

			// 获取该类型下的所有具体标签
			List<TagManagement> subTags = tags.stream()
				.filter(tag -> tag.getLevel() == 2 && Objects.equals(tag.getParentId(), category.getId()))
				.sorted(Comparator.comparing(TagManagement::getSortOrder))
				.collect(Collectors.toList());

			List<Map<String, Object>> children = new ArrayList<>();
			for (TagManagement subTag : subTags) {
				Map<String, Object> subNode = new HashMap<>();
				subNode.put("id", subTag.getId());
				subNode.put("tagName", subTag.getTagName());
				subNode.put("categoryType", subTag.getCategoryType());
				subNode.put("level", subTag.getLevel());
				subNode.put("parentId", subTag.getParentId());
				subNode.put("sortOrder", subTag.getSortOrder());
				subNode.put("tagColor", subTag.getTagColor());
				subNode.put("tagIcon", subTag.getTagIcon());
				subNode.put("description", subTag.getDescription());
				subNode.put("isActive", subTag.getIsActive());
				subNode.put("usageCount", subTag.getUsageCount());
				subNode.put("createTime", subTag.getCreateTime());
				subNode.put("updateTime", subTag.getUpdateTime());
				subNode.put("parentName", category.getTagName());

				children.add(subNode);
			}

			categoryNode.put("children", children);
			result.add(categoryNode);
		}

		return result;
	}

	@GetMapping("/tree/{tagType}")
	public R<List<TagManagementDTO>> getTagTreeByType(@PathVariable String tagType) {
		try {
			List<TagManagementDTO> tagTree = vlsTagManagementService.getTagTreeByType(tagType);
			return R.data(tagTree);
		} catch (Exception e) {
			log.error("获取标签树失败，类型: {}", tagType, e);
			return R.fail("获取标签树失败: " + e.getMessage());
		}
	}

	@PostMapping
	public R<TagManagement> createTag(@RequestBody TagManagement tagManagement) {
		try {
			TagManagement createdTag = vlsTagManagementService.createTag(tagManagement);
			return R.data(createdTag);
		} catch (Exception e) {
			log.error("创建标签失败", e);
			return R.fail("创建标签失败: " + e.getMessage());
		}
	}

	@PutMapping("/{id}")
	public R<TagManagement> updateTag(@PathVariable Long id,
		@RequestBody TagManagement tagManagement) {
		try {
			tagManagement.setId(id);
			TagManagement updatedTag = vlsTagManagementService.updateTag(tagManagement);
			return R.data(updatedTag);
		} catch (Exception e) {
			log.error("更新标签失败，ID: {}", id, e);
			return R.fail("更新标签失败: " + e.getMessage());
		}
	}

	@DeleteMapping("/{id}")
	public R<Void> deleteTag(@PathVariable Long id) {
		try {
			boolean success = vlsTagManagementService.deleteTag(id);
			if (success) {
				return R.success();
			} else {
				return R.fail("删除标签失败");
			}
		} catch (Exception e) {
			log.error("删除标签失败，ID: {}", id, e);
			return R.fail("删除标签失败: " + e.getMessage());
		}
	}

	@DeleteMapping("/batch")
	public R<Void> deleteTags(@RequestBody List<Long> tagIds) {
		try {
			boolean success = vlsTagManagementService.deleteTags(tagIds);
			if (success) {
				return R.success();
			} else {
				return R.fail("批量删除标签失败");
			}
		} catch (Exception e) {
			log.error("批量删除标签失败，IDs: {}", tagIds, e);
			return R.fail("批量删除标签失败: " + e.getMessage());
		}
	}

	@PutMapping("/{id}/move")
	public R<Void> moveTag(@PathVariable Long id, @RequestParam(required = false) Long targetParentId, @RequestParam(required = false) Integer targetPosition) {
		try {
			boolean success = vlsTagManagementService.moveTag(id, targetParentId, targetPosition);
			if (success) {
				return R.success();
			} else {
				return R.fail("移动标签失败");
			}
		} catch (Exception e) {
			log.error("移动标签失败，ID: {}", id, e);
			return R.fail("移动标签失败: " + e.getMessage());
		}
	}

	@PutMapping("/{id}/toggle-status")
	public R<Void> toggleTagStatus(@PathVariable Long id, @RequestParam boolean isActive) {
		try {
			boolean success = vlsTagManagementService.toggleTagStatus(id, isActive);
			if (success) {
				return R.success();
			} else {
				return R.fail("切换标签状态失败");
			}
		} catch (Exception e) {
			log.error("切换标签状态失败，ID: {}", id, e);
			return R.fail("切换标签状态失败: " + e.getMessage());
		}
	}

	@GetMapping("/{id}/stats")
	public R<TagManagement> getTagUsageStats(@PathVariable Long id) {
		try {
			TagManagement tagStats = vlsTagManagementService.getTagUsageStats(id);
			return R.data(tagStats);
		} catch (Exception e) {
			log.error("获取标签统计失败，ID: {}", id, e);
			return R.fail("获取标签统计失败: " + e.getMessage());
		}
	}

	@GetMapping("/check-name")
	public R<Boolean> checkTagName(@RequestParam String tagName, @RequestParam(required = false) Long parentId, @RequestParam(required = false) Long excludeId) {
		try {
			boolean isDuplicate = vlsTagManagementService.isTagNameDuplicate(tagName, parentId, excludeId);
			return R.data(isDuplicate);
		} catch (Exception e) {
			log.error("检查标签名称失败", e);
			return R.fail("检查标签名称失败: " + e.getMessage());
		}
	}
}
