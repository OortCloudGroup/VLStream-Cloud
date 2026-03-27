import { ref, computed } from 'vue'

/**
 * 要素数据处理 Hook
 * 根据 getBindPopupHtmlStr.ts 的逻辑，将要素数据转换为 label-value 格式
 * @param data 要素数据对象
 * @returns labelAndValue 响应式数组
 */
export function useFeatureData(data: any) {
  const labelAndValue = ref<Array<{ label: string; value: any }>>([])

  const initData = () => {
    labelAndValue.value = []
    if (!data.value) return

    const obj = data.value

    // 如果有 fieldList，优先使用 fieldList
    if (obj.fieldList && obj.fieldList.length > 0) {
      obj.fieldList.forEach((item: any) => {
        labelAndValue.value.push({
          label: item.fieldName,
          value: obj[item.colName]
        })
      })
    } else {
      // 如果没有 fieldsMap，直接返回
      if (!obj.fieldsMap) return

      // 遍历 fieldsMap，显示映射后的中文名
      for (let key of Object.keys(obj)) {
        if (!!obj.fieldsMap[key]) {
          labelAndValue.value.push({
            label: obj.fieldsMap[key],
            value: obj[key] || ''
          })
        }
      }
    }
  }

  // 使用 computed 自动响应数据变化
  const processedData = computed(() => {
    const result: Array<{ label: string; value: any }> = []
    if (!data.value) return result

    const obj = data.value

    // 如果有 fieldList，优先使用 fieldList
    if (obj.fieldList && obj.fieldList.length > 0) {
      obj.fieldList.forEach((item: any) => {
        result.push({
          label: item.fieldName,
          value: obj[item.colName]
        })
      })
    } else {
      // 如果没有 fieldsMap，直接返回
      if (!obj.fieldsMap) return result

      // 遍历 fieldsMap，显示映射后的中文名
      for (let key of Object.keys(obj)) {
        if (!!obj.fieldsMap[key]) {
          result.push({
            label: obj.fieldsMap[key],
            value: obj[key] || ''
          })
        }
      }
    }

    return result
  })

  return {
    labelAndValue,
    initData,
    processedData
  }
}
