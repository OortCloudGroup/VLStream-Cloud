package com.vlstream.common.controller;

import com.vlstream.common.Result;
import com.vlstream.common.ResultCode;
import com.vlstream.common.utils.DateUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.util.Date;

/**
 * web层通用数据处理
 */
public class BaseController {
    
    /**
     * 将前台传递过来的日期格式的字符串转换成Date类型
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Date 类型转换
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(DateUtils.parseDate(text));
            }
        });
    }
    
    /**
     * 响应返回结果
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected Result toAjax(int rows) {
        return rows > 0 ? Result.success() : Result.error();
    }
    
    /**
     * 响应返回结果
     *
     * @param result 结果
     * @return 操作结果
     */
    protected Result toAjax(boolean result) {
        return result ? Result.success() : Result.error();
    }
    
    /**
     * 返回成功
     */
    protected Result success() {
        return Result.success();
    }
    
    /**
     * 返回失败消息
     */
    protected Result error() {
        return Result.error();
    }
    
    /**
     * 返回成功消息
     */
    protected Result success(String message) {
        return Result.success(message);
    }
    
    /**
     * 返回失败消息
     */
    protected Result error(String message) {
        return Result.error(message);
    }
    
    /**
     * 返回成功数据
     */
    protected Result success(Object data) {
        return Result.success(data);
    }
} 