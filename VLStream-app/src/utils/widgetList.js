export function traverseContainerWidgets(widgetList, handler) {
  if (!widgetList) {
    return
  }
  widgetList.map(w => {
    w.type = 'm-' + w.type
    if (w.category === 'container') {
      handler(w)
    }
    if (w.type === 'm-grid') {
      w.cols.map(col => {
        traverseContainerWidgets(col.widgetList, handler)
      })
    } else if (w.type === 'm-table') {
      w.rows.map(row => {
        row.cols.map(cell => {
          traverseContainerWidgets(cell.widgetList, handler)
        })
      })
    } else if (w.type === 'm-tab') {
      w.tabs.map(tab => {
        traverseContainerWidgets(tab.widgetList, handler)
      })
    } else if (w.type === 'm-sub-form') {
      traverseContainerWidgets(w.widgetList, handler)
    } else if (w.category === 'container') { // 自定义容器
      traverseContainerWidgets(w.widgetList, handler)
    }
  })
}

/**
 * 获取所有容器组件
 * @param widgetList
 * @returns {[]}
 */
export function getAllContainerWidgets(widgetList) {
  if (!widgetList) {
    return []
  }

  let result = []
  let handlerFn = (w) => {
    result.push({
      type: w.type,
      name: w.options.name,
      container: w
    })
  }
  traverseContainerWidgets(widgetList, handlerFn)
  return result
}
