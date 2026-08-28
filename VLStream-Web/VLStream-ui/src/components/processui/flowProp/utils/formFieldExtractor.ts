/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

/**
 * formfield
 * from VForm Generate JSON in all formfield
 *
 * :
 * 1. layer (grid、table、tab、items etc. )
 * 2. component , only formfield
 * 3. each field id、name、readonly、hidden、requiredproperty
 */

/**
 * component collection
 * component to , formProperties in
 */
const CONTAINER_TYPES = new Set([
  'grid', //
  'table', // table
  'tab', //
  'items', //
  'card', //
  'div', // div
  'panel' //
])

/**
 * formfieldinfointerface
 */
export interface FormFieldInfo {
  id: string
  name: string
  readonly: boolean
  hidden: boolean
  required: boolean
}

/**
 * formfield
 * @param widgetList - need to componentarray
 * @param formProperties - array
 */
const collectFormFields = (widgetList: any[], formProperties: FormFieldInfo[]): void => {
  if (!Array.isArray(widgetList)) {
    return
  }

  widgetList.forEach(widget => {
    // Check current componentwhether to
    const isContainerWidget = CONTAINER_TYPES.has(widget.type)

    // non- component id and options , formProperties
    if (!isContainerWidget && widget.id && widget.options) {
      formProperties.push({
        id: widget.id,
        name: widget.options.label || '未命名',
        readonly: widget.readonly || false,
        hidden: widget.hidden || false,
        required: widget.required || false
      })
    }

    // Process widgetList ( whether to component)
    // Grid : cols[].widgetList
    if (widget.cols && Array.isArray(widget.cols)) {
      widget.cols.forEach(col => {
        if (col.widgetList && Array.isArray(col.widgetList)) {
          collectFormFields(col.widgetList, formProperties)
        }
      })
    }

    // Table : rows[].cols[].widgetList
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

    // Tab : tabs[].widgetList
    if (widget.tabs && Array.isArray(widget.tabs)) {
      widget.tabs.forEach(tab => {
        if (tab.widgetList && Array.isArray(tab.widgetList)) {
          collectFormFields(tab.widgetList, formProperties)
        }
      })
    }

    // Items : items[].widgetList
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
 * from VForm JSON in formfield
 * @param jsonContent - VForm Export JSONobject
 * @return s formfieldarray
 *
 * @example
 * const formJson = JSON.parse(res.data.content)
 * const fields = extractFormFields(formJson)
 */
export const extractFormFields = (jsonContent: any): FormFieldInfo[] => {
  const formProperties: FormFieldInfo[] = []

  try {
    let formJson = jsonContent

    // if is , need to Parse
    if (typeof jsonContent === 'string') {
      formJson = JSON.parse(jsonContent)
    }

    // whether to formJSON
    if (!formJson || !Array.isArray(formJson.widgetList)) {
      console.warn('Invalid form JSON structure: missing widgetList')
      return formProperties
    }

    // Execute all formfield
    collectFormFields(formJson.widgetList, formProperties)
  } catch (error) {
    console.error('Error extracting form fields:', error)
  }

  return formProperties
}

/**
 * form field
 * @param formJsonList - formJSONarray
 * @return s all field array
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
