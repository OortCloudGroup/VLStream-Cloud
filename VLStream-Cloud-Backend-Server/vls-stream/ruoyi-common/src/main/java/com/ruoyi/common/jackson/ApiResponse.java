/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ApiResponse<T> {
  private int code;
  private String msg;
  private T data;

  // 工单类型
  // 用于 addBasicInfo 的数据结构
  @Data
  public static class DataItem {
    private String appId;
    private String applicationName;
    private String synthesisId;
    private String categoryName;
  }

  // 表单信息
  // 用于 addFormInfo 的数据结构
  @Data
  public static class FormResponse {
    private FormConfig formConfig;
    private List<Widget> widgetList;
  }

  @Data
  public static class FormConfig {
    private String title;
    private String formRef;
    private Object formModel;
    private String size;
    private String labelPosition;
    private int labelWidth;
    private Object formRules;
    private Object gutter;
    private boolean disabled;
    private Object span;
    private boolean formBtns;
  }

  @Data
  public static class Widget {
    private Options options;
  }

  @Data
  public static class Options {
    private String label;
    //    @JsonDeserialize(using = DefaultValueDeserializer.class)
    private Object defaultValue; // 支持 Integer/String
    private AreaOptionItems areaOptionItems;
    private List<OptionItems> optionItems;
  }

  @Data
  public static class OptionItems {
      private String label;
      private Object value;
  }

  @Data
  public static class AreaOptionItems {
    private List<OptionItem> optionItems;
  }

  @Data
  public static class OptionItem {
    private String label;
    private Object value;
  }

  // 项目名称
  @Data
  public static class DeptResponse {
    @JsonProperty("list")
    private List<Dept> deptList;
  }

  @Data
  public static class Dept {
    @JsonProperty("dept_id") // 明确映射JSON字段
    private String deptId;

    @JsonProperty("dept_name") // 明确映射JSON字段
    private String deptName;

    @JsonProperty("son_dept")
    private List<Dept> sonDept;
  }

  // 审批信息
  @Data
  public static class HistoryProcNodeList {
    private String assigneeName;
    private String activityName;
    private String endTime;
    private List<CommentList> commentList;
  }

  @Data
  public static class CommentList {
    private String time;
    private String fullMessage;
  }
}
