/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.controller;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmLlmReviewConfig;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmReviewTask;
import com.ruoyi.vlstream.test.vlstream.service.LlmReviewManagementService;
import com.ruoyi.vlstream.test.vlstream.service.LlmReviewTaskService;
import com.ruoyi.vlstream.test.vlstream.service.OpenAiVisionClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsLlmReview")
@Tag(name = "大模型事件复核")
public class VlsLlmReviewController {

	private final LlmReviewManagementService managementService;
	private final LlmReviewTaskService taskService;

	@GetMapping("/providers")
	@Operation(summary = "查询当前租户的大模型配置")
	public R<List<LlmProvider>> providers() {
		return R.data(managementService.listProviders());
	}

	@PostMapping("/providers")
	@Operation(summary = "新增大模型配置")
	public R<LlmProvider> createProvider(@RequestBody LlmProvider provider) {
		provider.setId(null);
		return R.data(managementService.saveProvider(provider));
	}

	@PutMapping("/providers/{id}")
	@Operation(summary = "修改大模型配置")
	public R<LlmProvider> updateProvider(@PathVariable Long id, @RequestBody LlmProvider provider) {
		provider.setId(id);
		return R.data(managementService.saveProvider(provider));
	}

	@DeleteMapping("/providers/{id}")
	@Operation(summary = "删除大模型配置")
	public R<String> deleteProvider(@PathVariable Long id) {
		managementService.deleteProvider(id);
		return R.success("删除成功");
	}

	@PostMapping("/providers/{id}/test")
	@Operation(summary = "使用一张图片测试视觉模型")
	public R<OpenAiVisionClient.VisionDecision> testProvider(@PathVariable Long id,
											 @RequestBody Map<String, String> body) {
		String imageBase64 = body.get("imageBase64");
		String encoded = StringUtils.contains(imageBase64, "base64,")
			? StringUtils.substringAfter(imageBase64, "base64,") : imageBase64;
		if (StringUtils.isBlank(encoded)) {
			throw new ServiceException("测试图片不能为空");
		}
		try {
			return R.data(taskService.testProvider(id, body.get("prompt"), Base64.getDecoder().decode(encoded)));
		} catch (IllegalArgumentException exception) {
			throw new ServiceException("测试图片 Base64 格式不正确");
		}
	}

	@GetMapping("/algorithms/{algorithmId}/config")
	@Operation(summary = "查询算法的大模型复核配置")
	public R<AlgorithmLlmReviewConfig> algorithmConfig(@PathVariable Long algorithmId) {
		return R.data(managementService.getAlgorithmConfig(algorithmId));
	}

	@PutMapping("/algorithms/{algorithmId}/config")
	@Operation(summary = "保存算法的大模型复核配置")
	public R<AlgorithmLlmReviewConfig> saveAlgorithmConfig(@PathVariable Long algorithmId,
														@RequestBody AlgorithmLlmReviewConfig config) {
		return R.data(managementService.saveAlgorithmConfig(algorithmId, config));
	}

	@GetMapping("/tasks")
	@Operation(summary = "分页查询大模型复核任务")
	public R<IPage<LlmReviewTask>> tasks(@RequestParam(defaultValue = "1") int current,
										 @RequestParam(defaultValue = "10") int size,
										 @RequestParam(required = false) String status,
										 @RequestParam(required = false) Long algorithmId) {
		return R.data(taskService.page(current, size, status, algorithmId));
	}

	@GetMapping("/tasks/{id}")
	@Operation(summary = "查询复核任务详情")
	public R<LlmReviewTask> task(@PathVariable Long id) {
		return R.data(taskService.detail(id));
	}

	@PostMapping("/tasks/{id}/manual-decision")
	@Operation(summary = "人工确认事件或误报")
	public R<LlmReviewTask> manualDecision(@PathVariable Long id, @RequestBody Map<String, String> body) {
		return R.data(taskService.manualDecision(id, body.get("decision")));
	}
}
