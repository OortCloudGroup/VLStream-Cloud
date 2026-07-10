package com.ruoyi.flowable.core.domain;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.*;

/**
 * 流程查询实体对象
 *
 * @author KonBAI
 * @createTime 2022/6/11 01:15
 */
@Data
public class ProcessQuery {

    /**
     * 流程标识
     */
    private String processKey;

    /**
     * 流程名称
     */
    private String processName;

    /**
     * 流程分类
     */
    private String category;

    /**
     * 状态
     */
    private String state;

    /**
     * 手机端是否显示
     */
    private String showMobile;

    /**
     * 流程创建时间开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date proStartBeginTime;

    /**
     * 流程创建时间结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date proStartEndTime;

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 分类类型
     */
    private String categoryType;

    /**
     * 查询全部应用通用流程
     */
    private Boolean wfAppAll= false;
    /**
     * 查询全部综合通用流程
     */
    private Boolean wfSynthesisAll= false;
    /**
     * 查询全部应用工单流程
     */
    private Boolean WorkOrderAppAll= false;
    /**
     * 查询全部综合工单流程
     */
    private Boolean WorkOrderSynthesisAll= false;
    /**
     * 分类列表
     */
    List<String> categoryList = new ArrayList<>();
    /**
     * 访问的接口路径
     */
    private String apiPath;
}
