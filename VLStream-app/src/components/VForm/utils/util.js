import axios from 'axios'

// 常见文件格式的 MIME 类型定义
export const mimeTypes = {
  '7z': 'application/x-7z-compressed',
  avi: 'video/x-msvideo',
  bmp: 'image/bmp',
  csv: 'text/csv',
  doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  flv: 'video/x-flv',
  gif: 'image/gif',
  jpeg: 'image/jpeg',
  jpg: 'image/jpeg',
  mkv: 'video/x-matroska',
  mov: 'video/quicktime',
  mp3: 'audio/mpeg',
  mp4: 'video/mp4',
  pdf: 'application/pdf',
  png: 'image/png',
  rar: 'application/x-rar-compressed',
  txt: 'text/plain',
  wmv: 'video/x-ms-wmv',
  xls: 'application/vnd.ms-excel',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  zip: 'application/zip'
}

export function isNull(value) {
  return value === null || value === undefined
}

export function isNotNull(value) {
  return value !== null && value !== undefined
}

export function isEmptyStr(str) {
  // return (str === undefined) || (!str) || (!/[^\s]/.test(str));
  return str === undefined || (!str && str !== 0 && str !== '0') || !/[^\s]/.test(str)
}

export const generateId = function() {
  return Math.floor(Math.random() * 100000 + Math.random() * 20000 + Math.random() * 5000)
}

export const deepClone = function(origin) {
  if (origin === undefined) {
    return undefined
  }

  return JSON.parse(JSON.stringify(origin))
}

export const evalFn = function(fn, DSV, VFR) {
  let Fn = Function // 一个变量指向Function，防止有些前端编译工具报错
  // console.log('evalFn值测试',fn,eval(fn),new Fn('return '+fun)())

  let f = new Fn(
    'DSV',
    'VFR',
    `
    const dsv = (name, value='') => {
      if (DSV[name] !== undefined && DSV[name] !== null) {
        return DSV[name];
      } else {
        return value;
      }
    };
    const vfr = (name, value='') => {
      if (VFR[name] !== undefined && VFR[name] !== null) {
        return VFR[name];
      } else {
        return value;
      }
    };
    return ` + fn
  )
  return f(DSV, VFR)
}

export const overwriteObj = function(obj1, obj2) {
  /* 浅拷贝对象属性，obj2覆盖obj1 */
  // for (let prop in obj2) {
  //   if (obj2.hasOwnProperty(prop)) {
  //     obj1[prop] = obj2[prop]
  //   }
  // }

  Object.keys(obj2).forEach(prop => {
    obj1[prop] = obj2[prop]
  })
}

export const addWindowResizeHandler = function(handler) {
  let oldHandler = window.onresize
  if (typeof window.onresize !== 'function') {
    window.onresize = handler
  } else {
    window.onresize = function() {
      oldHandler()
      handler()
    }
  }
}

// const createStyleSheet = function() {
//   let head = document.head || document.getElementsByTagName('head')[0]
//   let style = document.createElement('style')
//   style.type = 'text/css'
//   head.appendChild(style)
//   return style.sheet
// }

export const insertCustomCssToHead = function(cssCode, formId = '') {
  let head = document.getElementsByTagName('head')[0]
  let oldStyle = document.getElementById('vform-custom-css')
  if (oldStyle) {
    head.removeChild(oldStyle) // 先清除后插入！！
  }
  if (formId) {
    oldStyle = document.getElementById('vform-custom-css' + '-' + formId)
    !!oldStyle && head.removeChild(oldStyle) // 先清除后插入！！
  }

  let newStyle = document.createElement('style')
  newStyle.type = 'text/css'
  newStyle.rel = 'stylesheet'
  newStyle.id = formId ? 'vform-custom-css' + '-' + formId : 'vform-custom-css'
  try {
    newStyle.appendChild(document.createTextNode(cssCode))
  } catch (ex) {
    newStyle.styleSheet.cssText = cssCode
  }

  head.appendChild(newStyle)
}

export const insertGlobalFunctionsToHtml = function(functionsCode, formId = '') {
  let bodyEle = document.getElementsByTagName('body')[0]
  let oldScriptEle = document.getElementById('v_form_global_functions')
  !!oldScriptEle && bodyEle.removeChild(oldScriptEle) // 先清除后插入！！
  if (formId) {
    oldScriptEle = document.getElementById('v_form_global_functions' + '-' + formId)
    !!oldScriptEle && bodyEle.removeChild(oldScriptEle) // 先清除后插入！！
  }

  let newScriptEle = document.createElement('script')
  newScriptEle.id = formId ? 'v_form_global_functions' + '-' + formId : 'v_form_global_functions'
  newScriptEle.type = 'text/javascript'
  newScriptEle.innerHTML = functionsCode
  bodyEle.appendChild(newScriptEle)
}

export const optionExists = function(optionsObj, optionName) {
  if (!optionsObj) {
    return false
  }

  return Object.keys(optionsObj).indexOf(optionName) > -1
}

export const loadRemoteScript = function(srcPath, callback) {
  /* 加载远程js，加载成功后执行回调函数 */
  let sid = encodeURIComponent(srcPath)
  let oldScriptEle = document.getElementById(sid)

  if (!oldScriptEle) {
    let s = document.createElement('script')
    s.src = srcPath
    s.id = sid
    document.body.appendChild(s)

    s.onload = s.onreadystatechange = function(_, isAbort) {
      /* 借鉴自ace.js */
      if (isAbort || !s.readyState || s.readyState === 'loaded' || s.readyState === 'complete') {
        s = s.onload = s.onreadystatechange = null
        if (!isAbort) {
          callback()
        }
      }
    }
  }
}

export function traverseFieldWidgets(widgetList, handler, parent = null) {
  widgetList.forEach(w => {
    if (w.formItemFlag) {
      handler(w, parent)
    } else if (w.type === 'm-grid') {
      w.cols.forEach(col => {
        traverseFieldWidgets(col.widgetList, handler, w)
      })
    } else if (w.type === 'm-tab') {
      w.tabs.forEach(tab => {
        traverseFieldWidgets(tab.widgetList, handler, w)
      })
    } else if (w.type === 'm-sub-form') {
      traverseFieldWidgets(w.widgetList, handler, w)
    } else if (w.type === 'm-items') {
      traverseFieldWidgets(w.items[0]?.widgetList || [], handler, w)
    } else if (w.category === 'container') {
      // 自定义容器
      // traverseFieldWidgets(w.widgetList, handler, w)
    }
  })
}

export function traverseContainWidgets(widgetList, handler) {
  widgetList.forEach(w => {
    // if (w.category === 'container') {
    //   handler(w)
    // }

    if (w.type === 'm-grid') {
      w.cols.forEach(col => {
        traverseContainWidgets(col.widgetList, handler)
      })
    } else if (w.type === 'm-table') {
      w.rows.forEach(row => {
        row.cols.forEach(cell => {
          traverseContainWidgets(cell.widgetList, handler)
        })
      })
    } else if (w.type === 'm-tab') {
      w.tabs.forEach(tab => {
        traverseContainWidgets(tab.widgetList, handler)
      })
    } else if (w.type === 'm-sub-form') {
      traverseContainWidgets(w.widgetList, handler)
    } else if (w.type === 'm-items') {
      traverseContainWidgets(w.items[0]?.widgetList || [], handler)
    } else if (w.category === 'container') {
      // 自定义容器
      // traverseContainWidgets(w.widgetList, handler)
    }
  })
}

export function traverseAllWidgets(widgetList, handler) {
  widgetList.forEach(w => {
    handler(w)

    if (w.type === 'm-grid') {
      w.cols.forEach(col => {
        handler(col)
        traverseAllWidgets(col.widgetList, handler)
      })
    } else if (w.type === 'm-table') {
      w.rows.forEach(row => {
        row.cols.forEach(cell => {
          handler(cell)
          traverseAllWidgets(cell.widgetList, handler)
        })
      })
    } else if (w.type === 'm-tab') {
      w.tabs.forEach(tab => {
        traverseAllWidgets(tab.widgetList, handler)
      })
    } else if (w.type === 'm-sub-form') {
      traverseAllWidgets(w.widgetList, handler)
    } else if (w.type === 'm-items') {
      traverseAllWidgets(w.items[0]?.widgetList || [], handler)
    } else if (w.category === 'container') {
      // 自定义容器
      // traverseAllWidgets(w.widgetList, handler)
    }
  })
}

function handleWidgetForTraverse(widget, handler) {
  if (widget.category) {
    traverseFieldWidgetsOfContainer(widget, handler)
  } else if (widget.formItemFlag) {
    handler(widget)
  }
}

/**
 * 遍历容器内的字段组件
 * @param con
 * @param handler
 */
export function traverseFieldWidgetsOfContainer(con, handler) {
  if (con.type === 'm-grid') {
    con.cols.forEach(col => {
      col.widgetList.forEach(cw => {
        handleWidgetForTraverse(cw, handler)
      })
    })
  } else if (con.type === 'm-table') {
    con.rows.forEach(row => {
      row.cols.forEach(cell => {
        cell.widgetList.forEach(cw => {
          handleWidgetForTraverse(cw, handler)
        })
      })
    })
  } else if (con.type === 'm-tab') {
    con.tabs.forEach(tab => {
      tab.widgetList.forEach(cw => {
        handleWidgetForTraverse(cw, handler)
      })
    })
  } else if (con.type === 'm-sub-form') {
    con.widgetList.forEach(cw => {
      handleWidgetForTraverse(cw, handler)
    })
  // eslint-disable-next-line no-dupe-else-if
  } else if (con.type === 'm-grid') {
    con.cols.forEach(cw => {
      handleWidgetForTraverse(cw, handler)
    })
  } else if (con.type === 'm-items') {
    con.items[0]?.widgetList.forEach(cw => {
      handleWidgetForTraverse(cw, handler)
    })
  } else if (con.category === 'container') {
    // // 自定义容器
    // con.widgetList.forEach(cw => {
    //   handleWidgetForTraverse(cw, handler)
    // })
  }
}

function handleContainerTraverse(widget, fieldHandler, containerHandler) {
  if (!!widget.category && widget.category === 'container') {
    traverseWidgetsOfContainer(widget, fieldHandler, containerHandler)
  } else if (widget.formItemFlag) {
    fieldHandler(widget)
  }
}

/**
 * 遍历容器内部的字段组件和容器组件
 * @param con
 * @param fieldHandler
 * @param containerHandler
 */
export function traverseWidgetsOfContainer(con, fieldHandler, containerHandler) {
  if (con.type === 'm-grid') {
    con.cols.forEach(col => {
      col.widgetList.forEach(cw => {
        handleContainerTraverse(cw, fieldHandler, containerHandler)
      })
    })
  } else if (con.type === 'm-table') {
    con.rows.forEach(row => {
      row.cols.forEach(cell => {
        cell.widgetList.forEach(cw => {
          handleContainerTraverse(cw, fieldHandler, containerHandler)
        })
      })
    })
  } else if (con.type === 'm-tab') {
    con.tabs.forEach(tab => {
      tab.widgetList.forEach(cw => {
        handleContainerTraverse(cw, fieldHandler, containerHandler)
      })
    })
  } else if (con.type === 'm-sub-form') {
    con.widgetList.forEach(cw => {
      handleContainerTraverse(cw, fieldHandler, containerHandler)
    })
  } else if (con.type === 'm-items') {
    con.items[0]?.widgetList.forEach(cw => {
      handleContainerTraverse(cw, fieldHandler, containerHandler)
    })
  } else if (con.category === 'container') {
    // 自定义容器
    // con.widgetList.forEach(cw => {
    //   handleContainerTraverse(cw, fieldHandler, containerHandler)
    // })
  }
}

/**
 * 获取所有字段组件
 * @param widgetList
 * @returns {[]}
 */
export function getAllFieldWidgets(widgetList) {
  let result = []
  let handlerFn = w => {
    result.push({
      field: w,
      name: w.options.name,
      type: w.type
    })
  }
  traverseFieldWidgets(widgetList, handlerFn)

  return result
}

/**
 * 获取所有容器组件
 * @param widgetList
 * @returns {[]}
 */
export function getAllContainerWidgets(widgetList) {
  let result = []
  let handlerFn = w => {
    result.push({
      container: w,
      name: w.options.name,
      type: w.type
    })
  }
  traverseContainWidgets(widgetList, handlerFn)

  return result
}

export function getFieldWidgetByName(widgetList, fieldName) {
  let foundWidget = null
  let handlerFn = widget => {
    if (widget.options.name === fieldName) {
      foundWidget = widget
    }
  }

  traverseFieldWidgets(widgetList, handlerFn)
  return foundWidget
}

export function getContainerWidgetByName(widgetList, containerName) {
  let foundContainer = null
  let handlerFn = con => {
    if (con.options.name === containerName) {
      foundContainer = con
    }
  }

  traverseContainerWidgets(widgetList, handlerFn)
  return foundContainer
}

export function getContainerWidgetById(widgetList, containerId) {
  let foundContainer = null
  let handlerFn = con => {
    if (con.id === containerId) {
      foundContainer = con
    }
  }

  traverseContainerWidgets(widgetList, handlerFn)
  return foundContainer
}

export function getQueryParam(variable) {
  let query = window.location.search.substring(1)
  let vars = query.split('&')
  for (let i = 0; i < vars.length; i++) {
    let pair = vars[i].split('=')
    if (pair[0] === variable) {
      return pair[1]
    }
  }

  return undefined
}

export function getDefaultFormConfig() {
  return {
    background: '#EFEFEF',
    colon: false,
    cssCode: '',
    customClass: '',
    dataSources: [], // 数据源集合
    functions: '', // 全局函数
    inputAlign: 'left',
    inputBorder: false,
    jsonVersion: -3, // -3代表VForm 3 Mobile
    labelAlign: 'left',
    labelPosition: 'left',
    labelWidth: 80,
    layoutType: 'H5',
    modelName: 'formData',
    onFormCreated: '',
    onFormDataChange: '',
    onFormMounted: '',
    onFormValidate: '',
    padding: 5,

    popupZIndex: 3000,
    radius: 4,

    refName: 'vForm',
    rulesName: 'rules',
    size: ''
  }
}

export function buildDefaultFormJson() {
  return {
    formConfig: deepClone(getDefaultFormConfig()),
    widgetList: []
  }
}

export function cloneFormConfigWithoutEventHandler(formConfig) {
  let newFC = deepClone(formConfig)
  newFC.onFormCreated = ''
  newFC.onFormMounted = ''
  newFC.onFormDataChange = ''
  newFC.onFormValidate = ''

  return newFC
}

/**
 * 转译选择项数据
 * @param rawData
 * @param widgetType
 * @param labelKey
 * @param valueKey
 * @returns {[]}
 */
export function translateOptionItems(rawData, widgetType, labelKey, valueKey) {
  if (widgetType === 'm-cascader') {
    // 级联选择不转译
    return deepClone(rawData)
  }

  let result = []
  if (!!rawData && rawData.length > 0) {
    rawData.forEach(ri => {
      result.push({
        label: ri[labelKey],
        value: ri[valueKey]
      })
    })
  }

  return result
}

/**
 * 组装axios请求配置参数
 * @param arrayObj
 * @param DSV
 * @param VFR
 * @returns {{}}
 */
export function assembleAxiosConfig(arrayObj, DSV, VFR) {
  let result = {}
  if (!arrayObj || arrayObj.length <= 0) {
    return result
  }

  arrayObj.map(ai => {
    if (ai.type === 'String') {
      result[ai.name] = String(ai.value)
    } else if (ai.type === 'Number') {
      result[ai.name] = Number(ai.value)
    } else if (ai.type === 'Boolean') {
      if (ai.value.toLowerCase() === 'false' || ai.value === '0') {
        result[ai.name] = false
      } else if (ai.value.toLowerCase() === 'true' || ai.value === '1') {
        result[ai.name] = true
      } else {
        result[ai.name] = null
      }
    } else if (ai.type === 'Variable') {
      result[ai.name] = evalFn(ai.value, DSV, VFR)
    }
  })

  /* 需要注意：VFR.getWidgetRef()可能无法获取组件，因为组件尚未创建完成，跟数据源执行时机有关！！ */

  /* 加入如下两行日志打印代码，是为了防止编译打包时DSV、VFR参数被剔除！！ begin */
  /* DSV、VFR入参没有在本函数中直接使用到，但在eval表达式中可能被使用到，故需确保DSV、VFR参数始终存在！！ */
  console.log('test DSV: ', DSV)
  console.log('test VFR: ', VFR)
  /* 加入如下两行日志打印代码，是为了防止编译打包时DSV、VFR入参会被剔除！！ end */

  return result
}

function buildRequestConfig(dataSource, DSV, VFR, isSandbox) {
  let config = {}
  if (dataSource.requestURLType === 'String') {
    config.url = dataSource.requestURL
  } else {
    config.url = evalFn(dataSource.requestURL, DSV, VFR)
  }
  config.method = dataSource.requestMethod

  config.headers = assembleAxiosConfig(dataSource.headers, DSV, VFR)
  config.params = assembleAxiosConfig(dataSource.params, DSV, VFR)
  config.data = assembleAxiosConfig(dataSource.data, DSV, VFR)

  let chFn = new Function('config', 'isSandbox', 'DSV', 'VFR', dataSource.configHandlerCode)
  // eslint-disable-next-line no-useless-call
  return chFn.call(null, config, isSandbox, DSV, VFR)
}

export async function runDataSourceRequest(dataSource, DSV, VFR, isSandbox, $message) {
  try {
    let requestConfig = buildRequestConfig(dataSource, DSV, VFR, isSandbox)
    let result = await axios.request(requestConfig)
    let dhFn = new Function('result', 'isSandbox', 'DSV', 'VFR', dataSource.dataHandlerCode)
    // eslint-disable-next-line no-useless-call
    return dhFn.call(null, result, isSandbox, DSV, VFR)
  } catch (err) {
    let ehFn = new Function('error', 'isSandbox', 'DSV', '$message', 'VFR', dataSource.errorHandlerCode)
    // eslint-disable-next-line no-useless-call
    ehFn.call(null, err, isSandbox, DSV, $message, VFR)
    console.error(err)
  }
}

export function getDSByName(formConfig, dsName) {
  let resultDS = null
  if (!!dsName && !!formConfig.dataSources) {
    formConfig.dataSources.forEach(ds => {
      if (ds.uniqueName === dsName) {
        resultDS = ds
      }
    })
  }

  return resultDS
}

function getRealUploadURL(uploadURL, DSV) {
  if (!!uploadURL && (uploadURL.indexOf('DSV.') > -1 || uploadURL.indexOf('DSV[') > -1)) {
    console.log('test DSV: ', DSV) // 为防止编译打包时DSV参数被剔除，本行代码不可注释，不可删除！！
    let url = evalFn(uploadURL, DSV)
    return url
  }

  return uploadURL
}

/* 异步上传文件或图片 */
export async function asyncUploadFile(file, uploadURL, DSV, uploadHeader = {}, uploadData = {}) {
  let formData = new FormData()

  if (Array.isArray(file)) {
    file.forEach((item, i) => {
      formData.append('files[' + i + ']', item.file)
    })
  } else {
    formData.append('file', file.file)
    uploadData.key = file.file.name
  }

  Object.keys(uploadData).forEach(dataKey => {
    formData.append(dataKey, uploadData[dataKey])
  })

  let uploadConfig = {
    headers: {
      'Content-Type': 'multipart/form-data',
      ...uploadHeader
    }
  }

  const realUploadURL = getRealUploadURL(uploadURL, DSV)
  return new Promise((resolve, reject) => {
    axios
      .post(realUploadURL, formData, uploadConfig)
      .then(res => {
        if (res && res.data) {
          resolve(res.data)
        } else {
          reject(new Error('No upload result'))
        }
      })
      .catch(err => {
        reject(err)
      })
  })
}

export function px2rem(px) {
  return px

  // 开启使用 rem 取消注释
  // let numPx;
  // if(typeof(px)=='string'){
  //   numPx=parseFloat(px.replace('px',''));
  // }else{
  //   numPx=px;
  // }

  // let em=numPx/37.5
  // return em+'rem'
}

export function findParentByComponentName(sourceRef, componentName) {
  // 递归查找父组件
  let parentRef = sourceRef.$parent
  while (parentRef) {
    if (parentRef.$ && parentRef.$.type.componentName === componentName) {
      // 找到父组件
      return parentRef
    }
    parentRef = parentRef.$parent
  }
  return null
}
