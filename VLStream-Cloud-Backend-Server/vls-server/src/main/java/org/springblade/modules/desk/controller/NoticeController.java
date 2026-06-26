/**
 * Copyright (c) 2018-2099, Chill Zhuang (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.modules.desk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.CacheNames;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.entity.Notice;
import org.springblade.modules.desk.service.INoticeService;
import org.springblade.modules.desk.vo.NoticeVO;
import org.springblade.modules.desk.wrapper.NoticeWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller
 *
 * @author Chill
 */
@RestController
@RequestMapping(AppConstant.APPLICATION_DESK_NAME + "/notice")
@AllArgsConstructor
@ApiSort(2)
@Tag(name = "User blog", description = "Blog interface")
public class NoticeController extends BladeController implements CacheNames {

	private INoticeService noticeService;

	/**
	 * Details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in notice")
	public R<NoticeVO> detail(Notice notice) {
		Notice detail = noticeService.getOne(Condition.getQueryWrapper(notice));
		return R.data(NoticeWrapper.build().entityVO(detail));
	}

	/**
	 * Pagination
	 */
	@GetMapping("/list")
	@Parameters({
		@Parameter(name = "category", description = "Announcement type", in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
		@Parameter(name = "title", description = "Announcement title", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in notice")
	public R<IPage<NoticeVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> notice, Query query) {
		IPage<Notice> pages = noticeService.page(Condition.getPage(query), Condition.getQueryWrapper(notice, Notice.class));
		return R.data(NoticeWrapper.build().pageVO(pages));
	}

	/**
	 * Add
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Add", description = "Pass in notice")
	public R save(@RequestBody Notice notice) {
		return R.status(noticeService.save(notice));
	}

	/**
	 * Modify
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Modify", description = "Pass in notice")
	public R update(@RequestBody Notice notice) {
		return R.status(noticeService.updateById(notice));
	}

	/**
	 * Add or modify
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Add or modify", description = "Pass in notice")
	public R submit(@RequestBody Notice notice) {
		return R.status(noticeService.saveOrUpdate(notice));
	}

	/**
	 * Delete
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Logical delete", description = "Pass in notice")
	public R remove(@Parameter(description = "Primary key collection") @RequestParam String ids) {
		boolean temp = noticeService.deleteLogic(Func.toLongList(ids));
		return R.status(temp);
	}

}
