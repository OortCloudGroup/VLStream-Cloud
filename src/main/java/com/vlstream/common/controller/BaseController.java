package com.vlstream.common.controller;

import com.vlstream.common.Result;
import com.vlstream.common.ResultCode;
import com.vlstream.common.utils.DateUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.util.Date;

/**
 * Web layer common data processing
 */
public class BaseController {
    
    /**
     * Convert date format string passed from frontend to Date type
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Date type conversion
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(DateUtils.parseDate(text));
            }
        });
    }
    
    /**
     * Response return result
     *
     * @param rows Number of affected rows
     * @return Operation result
     */
    protected Result toAjax(int rows) {
        return rows > 0 ? Result.success() : Result.error();
    }
    
    /**
     * Response return result
     *
     * @param result Result
     * @return Operation result
     */
    protected Result toAjax(boolean result) {
        return result ? Result.success() : Result.error();
    }
    
    /**
     * Return success
     */
    protected Result success() {
        return Result.success();
    }
    
    /**
     * Return error message
     */
    protected Result error() {
        return Result.error();
    }
    
    /**
     * Return success message
     */
    protected Result success(String message) {
        return Result.success(message);
    }
    
    /**
     * Return error message
     */
    protected Result error(String message) {
        return Result.error(message);
    }
    
    /**
     * Return success data
     */
    protected Result success(Object data) {
        return Result.success(data);
    }
} 