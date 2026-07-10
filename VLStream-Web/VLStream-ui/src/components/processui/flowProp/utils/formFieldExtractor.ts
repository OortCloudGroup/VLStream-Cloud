/**
 * 表单字段提取工具函数
 * 用于从VForm设计器生成的JSON结构中提取所有嵌套的表单字段
 *
 * 特点：
 * 1. 支持多层嵌套结构（grid、table、tab、items等容器）
 * 2. 自动排除容器组件本身，只收集实际的表单字段
 * 3. 提取每个字段的id、name、readonly、hidden、required属性
 */

/**
 * 容器组件类型集合
 * 这些类型的组件仅作为布局容器，不应被添加到formProperties中
 */
const CONTAINER_TYPES = new Set([
  'grid', // 栅格布局容器
  'table', // 表格容器
  'tab', // 标签页容器
  'items', // 明细行容器
  'card', // 卡片容器
  'div', // div 容器
  'panel' // 面板容器
])

/**
 * 表单字段信息接口
 */
export interface FormFieldInfo {
  id: string
  name: string
  readonly: boolean
  hidden: boolean
  required: boolean
}

/**
 * 递归遍历并收集表单字段
 * @param widgetList - 要遍历的组件数组
 * @param formProperties - 存储收集结果的数组
 */
const collectFormFields = (widgetList: any[], formProperties: FormFieldInfo[]): void => {
  if (!Array.isArray(widgetList)) {
    return
  }

  widgetList.forEach(widget => {
    // 判断当前组件是否为容器类型
    const isContainerWidget = CONTAINER_TYPES.has(widget.type)

    // 仅当非容器组件且拥有 id 和 options 时，才添加到 formProperties
    if (!isContainerWidget && widget.id && widget.options) {
      formProperties.push({
        id: widget.id,
        name: widget.options.label || '未命名',
        readonly: widget.readonly || false,
        hidden: widget.hidden || false,
        required: widget.required || false
      })
    }

    // 递归处理嵌套的 widgetList（无论是否为容器组件）
    // Grid 容器：cols[].widgetList
    if (widget.cols && Array.isArray(widget.cols)) {
      widget.cols.forEach(col => {
        if (col.widgetList && Array.isArray(col.widgetList)) {
          collectFormFields(col.widgetList, formProperties)
        }
      })
    }

    // Table 容器：rows[].cols[].widgetList
    if (widget.rows && Array.isArray(widget.rows)) {
      widget.rows.forEach(row => {
        if (row.cols && Array.isArray(row.cols)) {
          row.cols.forEach(col => {
            if (col.widgetList && Array.isArray(col.widgetList)) {
              collectFormFields(col.widgetList, formProperties)
            }
          })
        }
      })
    }

    // Tab 容器：tabs[].widgetList
    if (widget.tabs && Array.isArray(widget.tabs)) {
      widget.tabs.forEach(tab => {
        if (tab.widgetList && Array.isArray(tab.widgetList)) {
          collectFormFields(tab.widgetList, formProperties)
        }
      })
    }

    // Items 容器：items[].widgetList
    if (widget.items && Array.isArray(widget.items)) {
      widget.items.forEach(item => {
        if (item.widgetList && Array.isArray(item.widgetList)) {
          collectFormFields(item.widgetList, formProperties)
        }
      })
    }
  })
}

/**
 * 从VForm JSON内容中提取表单字段
 * @param jsonContent - VForm设计器导出的JSON对象或字符串
 * @returns 表单字段数组
 *
 * @example
 * const formJson = JSON.parse(res.data.content)
 * const fields = extractFormFields(formJson)
 */
export const extractFormFields = (jsonContent: any): FormFieldInfo[] => {
  const formProperties: FormFieldInfo[] = []

  try {
    let formJson = jsonContent

    // 如果输入是字符串，需要先解析
    if (typeof jsonContent === 'string') {
      formJson = JSON.parse(jsonContent)
    }

    // 检查是否为有效的表单JSON
    if (!formJson || !Array.isArray(formJson.widgetList)) {
      console.warn('Invalid form JSON structure: missing widgetList')
      return formProperties
    }

    // 执行递归遍历收集所有表单字段
    collectFormFields(formJson.widgetList, formProperties)
  } catch (error) {
    console.error('Error extracting form fields:', error)
  }

  return formProperties
}

/**
 * 批量提取多个表单的字段
 * @param formJsonList - 表单JSON数组
 * @returns 包含所有字段的数组
 */
export const extractFormFieldsBatch = (formJsonList: any[]): FormFieldInfo[] => {
  const allFields: FormFieldInfo[] = []

  if (Array.isArray(formJsonList)) {
    formJsonList.forEach(formJson => {
      const fields = extractFormFields(formJson)
      allFields.push(...fields)
    })
  }

  return allFields
}
