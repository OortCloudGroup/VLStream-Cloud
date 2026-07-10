
export const containers = [
  {
    type: 'items',
    category: 'container',
    icon: 'grid',
    items: [],
    options: {
      name: '',
      customName: '',
      hidden: false,
      defaultValue: [],
      labelWidth: null,
      readonly: false,
      disabled: false,
      clearable: true,
      required: false,
      customClass: '' // 自定义css类名
    }
  },

  // {
  //   type: 'grid',
  //   category: 'container',
  //   icon: 'grid',
  //   cols: [],
  //   options: {
  //     name: '',
  //     hidden: false,
  //     gutter: 12,
  //     colHeight: null, // 栅格列统一高度属性，用于解决栅格列设置响应式布局浮动后被挂住的问题！！
  //     customClass: '' // 自定义css类名
  //   }
  // },

  // {
  //   type: 'table',
  //   category: 'container',
  //   icon: 'table',
  //   rows: [],
  //   options: {
  //     name: '',
  //     hidden: false,
  //     customClass: '' // 自定义css类名
  //   }
  // },

  // {
  //   type: 'tab',
  //   category: 'container',
  //   icon: 'tab',
  //   displayType: 'border-card',
  //   tabs: [],
  //   options: {
  //     name: '',
  //     hidden: false,
  //     customClass: '' // 自定义css类名
  //   }
  // },

  // {
  //   type: 'grid-col',
  //   category: 'container',
  //   icon: 'grid-col',
  //   internal: true,
  //   widgetList: [],
  //   options: {
  //     name: '',
  //     hidden: false,
  //     span: 12,
  //     offset: 0,
  //     push: 0,
  //     pull: 0,
  //     responsive: false, // 是否开启响应式布局
  //     md: 12,
  //     sm: 12,
  //     xs: 12,
  //     customClass: '' // 自定义css类名
  //   }
  // },
  {
    type: 'items-item',
    category: 'container',
    icon: 'grid-col',
    internal: true,
    widgetList: [],
    options: {
      name: '',
      customName: '',
      hidden: false,
      span: 12,
      offset: 0,
      push: 0,
      pull: 0,
      responsive: false, // 是否开启响应式布局
      md: 12,
      sm: 12,
      xs: 12,
      customClass: '' // 自定义css类名
    }
  }

  // {
  //   type: 'table-cell',
  //   category: 'container',
  //   icon: 'table-cell',
  //   internal: true,
  //   widgetList: [],
  //   merged: false,
  //   options: {
  //     name: '',
  //     cellWidth: '',
  //     cellHeight: '',
  //     colspan: 1,
  //     rowspan: 1,
  //     wordBreak: false, // 是否自动换行
  //     customClass: '' // 自定义css类名
  //   }
  // },

  // {
  //   type: 'tab-pane',
  //   category: 'container',
  //   icon: 'tab-pane',
  //   internal: true,
  //   widgetList: [],
  //   options: {
  //     name: '',
  //     label: '',
  //     hidden: false,
  //     active: false,
  //     disabled: false,
  //     customClass: '' // 自定义css类名
  //   }
  // }
]

export const basicFields = [
  {
    type: 'input',
    icon: 'text-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '单行输入',
      labelAlign: '',
      type: 'text',
      defaultValue: '',
      placeholder: '',
      columnWidth: '200px',
      size: '',
      labelWidth: null,
      labelHidden: false,
      readonly: false,
      disabled: false,
      hidden: false,
      clearable: true,
      showPassword: false,
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null,
      minLength: null,
      maxLength: null,
      showWordLimit: false,
      prefixIcon: '',
      suffixIcon: '',
      appendButton: false,
      appendButtonDisabled: false,
      buttonIcon: 'custom-search'
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onInput: '',
      // onChange: '',
      // onFocus: '',
      // onBlur: '',
      // onValidate: '',
      // onAppendButtonClick: ''
    }
  },

  {
    type: 'textarea',
    icon: 'textarea-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '多行输入',
      labelAlign: '',
      rows: 3,
      defaultValue: '',
      placeholder: '',
      columnWidth: '200px',
      size: '',
      labelWidth: null,
      labelHidden: false,
      readonly: false,
      disabled: false,
      hidden: false,
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null,
      minLength: null,
      maxLength: null,
      showWordLimit: false
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onInput: '',
      // onChange: '',
      // onFocus: '',
      // onBlur: '',
      // onValidate: ''
    }
  },

  {
    type: 'number',
    icon: 'number-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '计数器',
      labelAlign: '',
      defaultValue: 0,
      placeholder: '',
      columnWidth: '200px',
      size: '',
      labelWidth: null,
      labelHidden: false,
      disabled: false,
      hidden: false,
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null,
      min: -100000000000,
      max: 100000000000,
      precision: 0,
      step: 1,
      controlsPosition: 'right'
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onFocus: '',
      // onBlur: '',
      // onValidate: ''
    }
  },

  {
    type: 'radio',
    icon: 'radio-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '单选项',
      labelAlign: '',
      defaultValue: null,
      columnWidth: '200px',
      size: '',
      displayStyle: 'inline',
      buttonStyle: false,
      border: false,
      labelWidth: null,
      labelHidden: false,
      disabled: false,
      hidden: false,
      optionItems: [
        { label: 'radio 1', value: 1 },
        { label: 'radio 2', value: 2 },
        { label: 'radio 3', value: 3 }
      ],
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onValidate: ''
    }
  },

  {
    type: 'checkbox',
    icon: 'checkbox-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '多选项',
      labelAlign: '',
      defaultValue: [],
      columnWidth: '200px',
      size: '',
      displayStyle: 'inline',
      buttonStyle: false,
      border: false,
      labelWidth: null,
      labelHidden: false,
      disabled: false,
      hidden: false,
      optionItems: [
        { label: 'check 1', value: 1 },
        { label: 'check 2', value: 2 },
        { label: 'check 3', value: 3 }
      ],
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onValidate: ''
    }
  },

  {
    type: 'select',
    icon: 'select-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '下拉选项',
      labelAlign: '',
      defaultValue: '',
      placeholder: '',
      columnWidth: '200px',
      size: '',
      labelWidth: null,
      labelHidden: false,
      disabled: false,
      hidden: false,
      clearable: true,
      filterable: false,
      allowCreate: false,
      remote: false,
      automaticDropdown: false, // 自动下拉
      multiple: false,
      multipleLimit: 0,
      optionItems: [
        { label: 'select 1', value: 1 },
        { label: 'select 2', value: 2 },
        { label: 'select 3', value: 3 }
      ],
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null,
      // -------------------
      // onCreated: '',
      // onMounted: '',
      onRemoteQuery: ''
      // onChange: '',
      // onFocus: '',
      // onBlur: '',
      // onValidate: ''
    }
  },

  {
    type: 'time',
    icon: 'time-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '时间',
      labelAlign: '',
      defaultValue: null,
      placeholder: '',
      columnWidth: '200px',
      size: '',
      autoFullWidth: true,
      labelWidth: null,
      labelHidden: false,
      readonly: false,
      disabled: false,
      hidden: false,
      clearable: true,
      editable: false,
      format: 'HH:mm:ss', // 时间格式
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onFocus: '',
      // onBlur: '',
      // onValidate: ''
    }
  },

  // {
  //   type: 'time-range',
  //   icon: 'time-range-field',
  //   formItemFlag: true,
  //   options: {
  //     name: '',
  //     label: '时间范围',
  //     labelAlign: '',
  //     defaultValue: null,
  //     startPlaceholder: '',
  //     endPlaceholder: '',
  //     columnWidth: '200px',
  //     size: '',
  //     autoFullWidth: true,
  //     labelWidth: null,
  //     labelHidden: false,
  //     readonly: false,
  //     disabled: false,
  //     hidden: false,
  //     clearable: true,
  //     editable: false,
  //     format: 'HH:mm:ss', // 时间格式
  //     required: false,
  //     requiredHint: '',
  //     validation: '',
  //     validationHint: '',
  //     // -------------------
  //     customClass: '', // 自定义css类名
  //     labelIconClass: null,
  //     labelIconPosition: 'rear',
  //     labelTooltip: null
  //     // -------------------
  //     // onCreated: '',
  //     // onMounted: '',
  //     // onChange: '',
  //     // onFocus: '',
  //     // onBlur: '',
  //     // onValidate: ''
  //   }
  // },

  {
    type: 'date',
    icon: 'date-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '日期',
      labelAlign: '',
      datetype: 'date', // date datetime
      defaultValue: null,
      placeholder: '',
      columnWidth: '200px',
      size: '',
      autoFullWidth: true,
      labelWidth: null,
      labelHidden: false,
      readonly: false,
      disabled: false,
      hidden: false,
      clearable: true,
      editable: false,
      format: 'YYYY-MM-DD HH:mm:ss', // 日期显示格式
      valueFormat: 'YYYY-MM-DD HH:mm:ss', // 日期对象格式
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null
      // // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onFocus: '',
      // onBlur: '',
      // onValidate: ''
    }
  },

  {
    type: 'date-range',
    icon: 'date-range-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '日期范围',
      labelAlign: '',
      type: 'daterange',
      defaultValue: null,
      startPlaceholder: '',
      endPlaceholder: '',
      columnWidth: '200px',
      size: '',
      autoFullWidth: true,
      labelWidth: null,
      labelHidden: false,
      readonly: false,
      disabled: false,
      hidden: false,
      clearable: true,
      editable: false,
      format: 'YYYY-MM-DD', // 日期显示格式
      valueFormat: 'YYYY-MM-DD', // 日期对象格式
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onFocus: '',
      // onBlur: '',
      // onValidate: ''
    }
  },

  {
    type: 'switch',
    icon: 'switch-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '开关',
      labelAlign: '',
      defaultValue: null,
      columnWidth: '200px',
      labelWidth: null,
      labelHidden: false,
      disabled: false,
      hidden: false,
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null,
      switchWidth: 40,
      activeText: '',
      inactiveText: '',
      activeColor: null,
      inactiveColor: null
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onValidate: ''
    }
  },

  {
    type: 'rate',
    icon: 'rate-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '评分',
      labelAlign: '',
      defaultValue: null,
      columnWidth: '200px',
      labelWidth: null,
      labelHidden: false,
      disabled: false,
      hidden: false,
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null,
      max: 5,
      lowThreshold: 2,
      highThreshold: 4,
      allowHalf: false,
      showText: false,
      showScore: false
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onValidate: ''
    }
  },

  // {
  //   type: 'color',
  //   icon: 'color-field',
  //   formItemFlag: true,
  //   options: {
  //     name: '',
  //     label: '颜色选择器',
  //     labelAlign: '',
  //     defaultValue: null,
  //     columnWidth: '200px',
  //     size: '',
  //     labelWidth: null,
  //     labelHidden: false,
  //     disabled: false,
  //     hidden: false,
  //     required: false,
  //     requiredHint: '',
  //     validation: '',
  //     validationHint: '',
  //     // -------------------
  //     customClass: '', // 自定义css类名
  //     labelIconClass: null,
  //     labelIconPosition: 'rear',
  //     labelTooltip: null
  //     // -------------------
  //     // onCreated: '',
  //     // onMounted: '',
  //     // onChange: '',
  //     // onValidate: ''
  //   }
  // },

  {
    type: 'slider',
    icon: 'slider-field',
    formItemFlag: true,
    options: {
      name: '滑块',
      customName: '',
      label: '',
      labelAlign: '',
      columnWidth: '200px',
      showStops: true,
      size: '',
      labelWidth: null,
      labelHidden: false,
      disabled: false,
      hidden: false,
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null,
      min: 0,
      max: 100,
      step: 10,
      range: false,
      // vertical: false,
      height: null
      // // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onValidate: ''
    }
  },

  {
    type: 'static-text',
    icon: 'static-text',
    formItemFlag: false,
    options: {
      name: '静态文字',
      customName: '',
      hidden: false,
      textContent: '文本',
      textAlign: 'left',
      height: null,
      fontSize: '13px',
      fontWeight: 'normal',
      color: '#333',
      preWrap: false, // 是否自动换行
      // -------------------
      customClass: '' // 自定义css类名
      // // -------------------
      // onCreated: '',
      // onMounted: ''
    }
  },

  {
    type: 'html-text',
    icon: 'html-text',
    formItemFlag: false,
    options: {
      name: '',
      customName: '',
      columnWidth: '200px',
      hidden: false,
      htmlContent: '<b>html text</b>',
      // -------------------
      customClass: '' // 自定义css类名
      // -------------------
      // onCreated: '',
      // onMounted: ''
    }
  },

  // {
  //   type: 'button',
  //   icon: 'button',
  //   formItemFlag: false,
  //   options: {
  //     name: '',
  //     label: '按钮',
  //     columnWidth: '200px',
  //     size: '',
  //     displayStyle: 'block',
  //     disabled: false,
  //     hidden: false,
  //     type: '',
  //     plain: false,
  //     round: false,
  //     circle: false,
  //     icon: null,
  //     // -------------------
  //     customClass: '' // 自定义css类名
  //     // -------------------
  //     // onCreated: '',
  //     // onMounted: '',
  //     // onClick: ''
  //   }
  // },

  {
    type: 'divider',
    icon: 'divider',
    formItemFlag: false,
    options: {
      name: '',
      customName: '',
      label: '分隔线',
      columnWidth: '200px',
      direction: 'horizontal',
      contentPosition: 'center',
      hidden: false,
      // -------------------
      customClass: '' // 自定义css类名
      // -------------------
      // onCreated: '',
      // onMounted: '
    }
  },
  {
    type: 'money',
    icon: 'text-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '金额输入',
      labelAlign: '',
      type: 'text',
      defaultValue: '',
      placeholder: '',
      columnWidth: '200px',
      size: '',
      labelWidth: null,
      labelHidden: false,
      readonly: false,
      disabled: false,
      hidden: false,
      clearable: true,
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      moneyType: '￥', // 货币类型
      decimalLength: 2,
      maxLength: 10
    }
  },
  {
    type: 'hyperlink',
    icon: 'html-text',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '链接组件',
      labelAlign: '',
      defaultValue: '',
      placeholder: '',
      columnWidth: '200px',
      size: '',
      labelWidth: null,
      labelHidden: false,
      hidden: false,
      hypeLinkOptions: {
        linkType: 'defalut',
        linkUrl: 'http://www.test.com',
        linkText: '示例链接'
      }
    }
  },
  {
    type: 'numberComp',
    icon: 'number-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '数值组件',
      labelAlign: '',
      type: 'text',
      defaultValue: '',
      placeholder: '',
      columnWidth: '200px',
      size: '',
      labelWidth: null,
      labelHidden: false,
      readonly: false,
      disabled: false,
      hidden: false,
      clearable: true,
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      maxLength: 10,
      numberOptions: {
        type: 1,
        moneyType: '￥', // 货币类型
        decimalLength: 2
      }
    }
  },
  {
    type: 'picture-upload',
    icon: 'picture-upload-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '图片上传',
      labelAlign: '',
      labelWidth: null,
      labelHidden: false,
      columnWidth: '200px',
      disabled: false,
      hidden: false,
      required: false,
      requiredHint: '',
      customRule: '',
      customRuleHint: '',
      // -------------------
      uploadURL: '',
      uploadTip: '',
      withCredentials: false,
      multipleSelect: false,
      showFileList: true,
      limit: 3,
      fileMaxSize: 5, // MB
      fileTypes: ['jpg', 'jpeg', 'png'],
      // headers: [],
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onBeforeUpload: '',
      // onUploadSuccess: '',
      // onUploadError: '',
      // onFileRemove: '',
      // onValidate: ''
      // onFileChange: '',
    }
  },
  {
    type: 'file-upload',
    icon: 'file-upload-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '文件上传',
      labelAlign: '',
      labelWidth: null,
      labelHidden: false,
      columnWidth: '200px',
      disabled: false,
      hidden: false,
      required: false,
      requiredHint: '',
      customRule: '',
      customRuleHint: '',
      // -------------------
      uploadURL: '',
      uploadTip: '',
      withCredentials: false,
      multipleSelect: false,
      showFileList: true,
      limit: 3,
      fileMaxSize: 5, // MB
      fileTypes: ['doc', 'docx', 'xls', 'xlsx', 'txt'],
      // headers: [],
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onBeforeUpload: '',
      // onUploadSuccess: '',
      // onUploadError: '',
      // onFileRemove: '',
      // onValidate: ''
      // onFileChange: '',
    }
  }
]

export const advancedFields = [
  {
    type: 'basic-data',
    icon: 'select-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '基础数据',
      labelAlign: '',
      type: 'text',
      defaultValue: '',
      placeholder: '',
      columnWidth: '200px',
      size: '',
      labelWidth: null,
      labelHidden: false,
      readonly: false,
      disabled: false,
      hidden: false,
      clearable: true,
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      maxLength: 10,
      basicOptions: {
        dataTag: '', // 数据标签
        showType: 1 // 显示方式
      }
    }
  },
  // {
  //   type: 'rich-editor',
  //   icon: 'rich-editor-field',
  //   formItemFlag: true,
  //   options: {
  //     name: '',
  //     label: '',
  //     labelAlign: '',
  //     placeholder: '',
  //     labelWidth: null,
  //     labelHidden: false,
  //     columnWidth: '200px',
  //     contentHeight: '200px',
  //     disabled: false,
  //     hidden: false,
  //     required: false,
  //     requiredHint: '',
  //     customRule: '',
  //     customRuleHint: '',
  //     //-------------------
  //     customClass: '',  //自定义css类名
  //     labelIconClass: null,
  //     labelIconPosition: 'rear',
  //     labelTooltip: null,
  //     minLength: null,
  //     maxLength: null,
  //     showWordLimit: false,
  //     //-------------------
  //     onCreated: '',
  //     onMounted: '',
  //     onValidate: '',
  //   },
  // },

  {
    type: 'cascader',
    icon: 'cascader-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '级联选择',
      labelAlign: '',
      defaultValue: '',
      placeholder: '',
      size: '',
      labelWidth: null,
      labelHidden: false,
      columnWidth: '200px',
      disabled: false,
      hidden: false,
      clearable: true,
      filterable: false,
      multiple: false,
      checkStrictly: false, // 可选择任意一级选项，默认不开启
      showAllLevels: true, // 显示完整路径
      optionItems: [
        { label: 'select 1', value: 1, children: [{ label: 'child 1', value: 11 }] },
        { label: 'select 2', value: 2 },
        { label: 'select 3', value: 3 }
      ],
      required: false,
      requiredHint: '',
      customRule: '',
      customRuleHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onFocus: '',
      // onBlur: '',
      // onValidate: ''
    }
  },
  {
    type: 'cascader-area',
    icon: 'cascader-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '地区组件',
      labelAlign: '',
      defaultValue: '',
      placeholder: '',
      size: '',
      labelWidth: null,
      labelHidden: false,
      columnWidth: '200px',
      disabled: false,
      hidden: false,
      clearable: true,
      filterable: false,
      multiple: false,
      checkStrictly: false, // 可选择任意一级选项，默认不开启
      showAllLevels: true, // 显示完整路径
      optionItems: [],
      required: false,
      requiredHint: '',
      customRule: '',
      customRuleHint: '',
      mapCenter: '',
      areaOptionItems: {
        optionItems: [],
        areaType: 1
      }
      // -------------------
      // customClass: '', // 自定义css类名
      // labelIconClass: null,
      // labelIconPosition: 'rear',
      // labelTooltip: null
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onFocus: '',
      // onBlur: '',
      // onValidate: ''
    }
  },

  {
    type: 'serial-number',
    icon: 'cascader-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '流水号',
      labelAlign: '',
      defaultValue: '00001111',
      placeholder: '',
      size: '',
      labelWidth: null,
      labelHidden: false,
      columnWidth: '200px',
      hidden: false,
      textContent: new Date().getTime(),
      textAlign: 'left',
      fontSize: '13px',
      color: '#333',
      serialOptions: [{
        type: 1, // 类型 1，自动计数 ，2 提交日期， 3，固定字符 ， 4，表单字段
        initNumber: 0,
        countCycle: 0, // 自定重复周期
        dateFormat: 'YYYY',
        content: '000001111',
        formFiled: ''
      }],
      preWrap: false, // 是否自动换行
      // -------------------
      customClass: '' // 自定义css类名
      // -------------------
      // onCreated: '',
      // onMounted: ''
    }
  },

  {
    type: 'address-book',
    icon: 'cascader-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '地址本',
      labelAlign: '',
      defaultValue: '',
      size: '',
      labelWidth: null,
      labelHidden: false,
      columnWidth: '200px',
      disabled: false,
      hidden: false,
      multiple: true,
      required: false,
      requiredHint: '',
      customRule: '',
      customRuleHint: '',
      addressSetting: {
        selectMod: 1, // 选择模式 1, 部门和人都可以选， 2 只选部门  3 只选人
        selectScope: 1, // 选择范围  1,全部  2 本部门 3 自定义
        defaultValueType: 1, // 1，无， 2 固定值， 3 提交人， 4 提交部门， 5 创建人， 6 创建人部门
        scopeValue: '', // 自定义范围
        defaultValue: '' // 固定值
      }
      // -------------------
      // customClass: '', // 自定义css类名
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onFocus: '',
      // onBlur: '',
      // onValidate: ''
    }
  },

  {
    type: 'link-form',
    icon: 'cascader-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '子表单',
      labelAlign: '',
      defaultValue: '',
      placeholder: '',
      size: '',
      labelWidth: null,
      labelHidden: false,
      columnWidth: '200px',
      disabled: false,
      hidden: false,
      buttonIcon: 'cascader-field',
      required: false,
      requiredHint: '',
      customRule: '',
      customRuleHint: '',
      buttonText: '子表单',
      linkFormID: ''
      // -------------------
      // labelIconClass: null,
      // labelIconPosition: 'rear',
      // labelTooltip: null,
      // -------------------
      // onCreated: '',
      // onMounted: '',
      // onChange: '',
      // onFocus: '',
      // onBlur: '',
      // onValidate: ''
    }
  },

  {
    type: 'calc-method',
    icon: 'cascader-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '计算公式',
      labelAlign: '',
      defaultValue: '',
      placeholder: '',
      size: '',
      labelWidth: null,
      labelHidden: false,
      columnWidth: '200px',
      color: '#333',
      hidden: false,
      calcOptions: { // 计算规则
        rules: ''
      }
    }
  },

  {
    type: 'link-search',
    icon: 'cascader-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '关联查询',
      labelAlign: '',
      defaultValue: '',
      placeholder: '',
      size: '',
      labelWidth: null,
      labelHidden: false,
      columnWidth: '200px',
      disabled: false,
      hidden: false,
      clearable: true,
      filterable: false,
      multiple: false,
      checkStrictly: false, // 可选择任意一级选项，默认不开启
      showAllLevels: true, // 显示完整路径
      optionItems: [],
      linkQuery: ''
    }
  },
  {
    type: 'scan-input',
    icon: 'cascader-field',
    formItemFlag: true,
    options: {
      name: '',
      customName: '',
      label: '扫码录入',
      labelAlign: '',
      type: 'text',
      defaultValue: '',
      placeholder: '',
      columnWidth: '200px',
      size: '',
      labelWidth: null,
      labelHidden: false,
      readonly: false,
      disabled: false,
      hidden: false,
      clearable: true,
      showPassword: false,
      required: false,
      requiredHint: '',
      validation: '',
      validationHint: '',
      // -------------------
      customClass: '', // 自定义css类名
      labelIconClass: null,
      labelIconPosition: 'rear',
      labelTooltip: null,
      minLength: null,
      maxLength: null,
      showWordLimit: false,
      prefixIcon: '',
      suffixIcon: '',
      appendButton: false,
      appendButtonDisabled: false,
      buttonIcon: 'custom-search',
      scanType: 1 // 1 二维码qr码，2，条形码,
    }
  }

  // {
  //   type: 'link-data',
  //   icon: 'cascader-field',
  //   formItemFlag: true,
  //   options: {
  //     name: '',
  //     label: '关联数据',
  //     labelAlign: '',
  //     defaultValue: '',
  //     placeholder: '',
  //     size: '',
  //     labelWidth: null,
  //     labelHidden: false,
  //     columnWidth: '200px',
  //     disabled: false,
  //     hidden: false,
  //     clearable: true,
  //     filterable: false,
  //     multiple: false,
  //     checkStrictly: false, // 可选择任意一级选项，默认不开启
  //     showAllLevels: true, // 显示完整路径
  //     optionItems: [
  //     ],
  //     required: false,
  //     requiredHint: '',
  //     customRule: '',
  //     customRuleHint: '',
  //     // -------------------
  //     customClass: '', // 自定义css类名
  //     labelIconClass: null,
  //     labelIconPosition: 'rear',
  //     labelTooltip: null,
  //     // -------------------
  //     onCreated: '',
  //     onMounted: '',
  //     onChange: '',
  //     onFocus: '',
  //     onBlur: '',
  //     onValidate: ''
  //   }
  // },
]

export const customFields = [

]

export const customTemplate = {
  type: 'grid',
  category: 'container',
  icon: 'grid',
  id: '',
  key: '',
  cols: [
    {
      category: 'container',
      icon: 'grid-col',
      internal: true,
      options: {
        name: '',
        hidden: false,
        md: 12,
        span: 24,
        xs: 12,
        pull: 0,
        push: 0,
        customClass: ''
      },
      widgetList: [],
      type: 'grid-col'
    }
  ],
  options: {
    name: '',
    hidden: false,
    gutter: 12,
    colHeight: null,
    customClass: ''
  }
}

export function addContainerWidgetSchema(containerSchema) {
  containers.push(containerSchema)
}

export function addBasicFieldSchema(fieldSchema) {
  basicFields.push(fieldSchema)
}

export function addAdvancedFieldSchema(fieldSchema) {
  advancedFields.push(fieldSchema)
}

export function addCustomWidgetSchema(widgetSchema) {
  customFields.push(widgetSchema)
}
