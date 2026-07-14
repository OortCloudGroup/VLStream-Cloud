/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.workflow.rule;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.excel.ExcelResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.rule.bo.RuleListBo;
import com.ruoyi.rule.domain.RuleList;
import com.ruoyi.rule.service.IRuleListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 规则列表
 */
@RestController
@RequestMapping("/rule/list")
@Slf4j
public class RuleListController {
    @Autowired
    private IRuleListService ruleListService;


    /**
     * 分页列表查询
     *
     * @param ruleList
     * @return
     */
    //@AutoLog(value = "规则列表-分页列表查询")
    @SaCheckPermission("rule:list:queryPageList")
    @GetMapping(value = "/list")
    public R<IPage<RuleList>> queryPageList(RuleList ruleList, PageQuery pageQuery) {
        LambdaQueryWrapper<RuleList> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(ruleList.getName()), RuleList::getName, ruleList.getName());
        queryWrapper.eq(StringUtils.isNotBlank(ruleList.getStatus()), RuleList::getStatus, ruleList.getStatus());
        queryWrapper.eq(StringUtils.isNotBlank(ruleList.getTreeId()), RuleList::getTreeId, ruleList.getTreeId());
        queryWrapper.eq(StringUtils.isNotBlank(ruleList.getType()), RuleList::getType, ruleList.getType());
        IPage<RuleList> pageList = ruleListService.page(pageQuery.build(), queryWrapper);
        return R.ok(pageList);
    }

    /**
     * 添加
     *
     * @param ruleListBo
     * @return
     */
    @SaCheckPermission("rule:list:add")
    @PostMapping(value = "/add")
    public R<String> add(@RequestBody RuleListBo ruleListBo) {
        RuleList ruleList = new RuleList();
        BeanUtils.copyProperties(ruleListBo, ruleList);
        ruleListService.saveMain(ruleList);
        return R.ok("添加成功");
    }

    /**
     * 编辑
     *
     * @param ruleListBo
     * @return
     */
    @SaCheckPermission("rule:list:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public R<String> edit(@RequestBody RuleListBo ruleListBo) {
        RuleList ruleList = new RuleList();
        BeanUtils.copyProperties(ruleListBo, ruleList);
        RuleList ruleListEntity = ruleListService.getById(ruleList.getId());
        if (ruleListEntity == null) {
            return R.fail("未找到对应数据");
        }
        ruleListService.updateMain(ruleList);
        return R.ok("编辑成功!");
    }

    /**
     * 通过id删除,同时删除子表数据
     *
     * @param id
     * @return
     */
    @SaCheckPermission("rule:list:delete")
    @DeleteMapping(value = "/delete/{id}")
    public R<String> delete(@PathVariable String id) {
        ruleListService.delMain(id);
        return R.ok("删除成功!");
    }

    /**
     * 批量删除,同时删除子表数据
     * @param ids
     * @return
     */
    @SaCheckPermission("rule:list:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public R<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.ruleListService.delBatchMain(Arrays.asList(ids.split(",")));
        return R.ok("批量删除成功");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @SaCheckPermission("rule:list:queryById")
    @GetMapping(value = "/{id}")
    public R<RuleList> queryById(@PathVariable String id) {
        RuleList ruleList = ruleListService.getById(id);
        if (ruleList == null) {
            return R.fail("未找到对应数据");
        }
        return R.ok(ruleList);
    }

    /**
     * 导出规则列列表
     */
    @SaCheckPermission("rule:list:export")
    @PostMapping("/export")
    public void export(@RequestBody RuleList ruleList, HttpServletResponse response) {
        LambdaQueryWrapper<RuleList> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(ruleList.getName()), RuleList::getName, ruleList.getName());
        queryWrapper.eq(StringUtils.isNotBlank(ruleList.getStatus()), RuleList::getStatus, ruleList.getStatus());
        queryWrapper.eq(StringUtils.isNotBlank(ruleList.getTreeId()), RuleList::getTreeId, ruleList.getTreeId());
        List<RuleList> list = ruleListService.list(queryWrapper);
        ExcelUtil.exportExcel(list, "规则列", RuleList.class, response);
    }

    /**
     * 导入规则列列表
     *
     * @param file          导入文件
     * @param updateSupport 是否更新已存在数据
     */
    @SaCheckPermission("rule:user:importData")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        ExcelResult<RuleList> result = ExcelUtil.importExcel(file.getInputStream(), RuleList.class, updateSupport);
        List<RuleList> volist = result.getList();
        List<RuleList> list = BeanUtil.copyToList(volist, RuleList.class);
        ruleListService.saveBatch(list);
        return R.ok(result.getAnalysis());
    }

    /**
     * 导出规则列列表模版
     */
    @SaCheckPermission("rule:list:exportTemplate")
    @PostMapping("/exportTemplate")
    public void exportTemplate(@RequestBody RuleList ruleList, HttpServletResponse response) {
        LambdaQueryWrapper<RuleList> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(ruleList.getTreeId()), RuleList::getTreeId, ruleList.getTreeId());
        queryWrapper.last("limit 1");
        List<RuleList> list = ruleListService.list(queryWrapper);
        for (RuleList ruleList1 : list) {
            ruleList1.setName("模版数据(可替换为需要数据)");
            ruleList1.setExpression("模版数据(可替换为需要数据也可为空)");
            ruleList1.setDescription("模版数据(可替换为需要数据也可为空)");
        }
        ExcelUtil.exportExcel(list, "规则列", RuleList.class, response);
    }
}
