/* eslint-disable no-useless-call */
function _broadcast(componentName, eventName, params) {
  this.$children.forEach(function(child) {
    let name = child.$options.componentName
    if (name === componentName) {
      // child.$emit.apply(child, [eventName].concat(params));
      if (!!child.emit$) {
        // eslint-disable-next-line no-useless-call
        child.emit$.call(child, eventName, params)
      }
    } else {
      _broadcast.apply(child, [componentName, eventName].concat([params]))
    }
  })
}

import { getCurrentInstance } from 'vue'
export function useEmitter(data = {}, _proxy) {
  // const { proxy } = getCurrentInstance();
  let proxy
  if (_proxy) {
    // 在插件property-editor-factory.jsx中无法获取proxy，故此处传入
    proxy = _proxy
  } else {
    proxy = getCurrentInstance().proxy
  }

  data.vfEvents = {}

  const methods = {
    broadcast: function broadcast(componentName, eventName, params) {
      /* Vue3移除了$children属性，_broadcast方法已不能使用！！ */
      // _broadcast.call(this, componentName, eventName, params);

      if (!!proxy.widgetRefList) {
        // FormRender只需遍历自身的widgetRefList属性
        Object.keys(proxy.widgetRefList).forEach(refName => {
          let cmpName = proxy.widgetRefList[refName].$options.componentName
          if (cmpName === componentName) {
            let foundRef = proxy.widgetRefList[refName]
            foundRef.emit$.call(foundRef, eventName, params)
          }
        })
      }

      if (!!proxy.refList) {
        // 其他组件遍历inject的refList属性
        Object.keys(proxy.refList).forEach(refName => {
          let cmpName = proxy.refList[refName].$options.componentName
          if (cmpName === componentName) {
            let foundRef = proxy.refList[refName]
            foundRef.emit$.call(foundRef, eventName, params)
          }
        })
      }
    },

    dispatch: function dispatch(componentName, eventName, params) {
      let parent = proxy.$parent || proxy.$root
      let name = parent.$options.componentName

      while (parent && (!name || name !== componentName)) {
        parent = parent.$parent

        if (parent) {
          name = parent.$options.componentName
        }
      }
      if (parent) {
        if (!!parent.emit$) {
          parent.emit$.call(parent, eventName, params)

          if (componentName === 'VmFormRender') {
            parent.$emit(eventName, ...params) // 执行原生$emit，以便可以用@进行声明式事件处理！！
          }
        }
      }
    },

    emit$(eventName, eventData) {
      if (data.vfEvents[eventName]) {
        data.vfEvents[eventName].forEach(fn => {
          fn(eventData)
        })
      }
    },

    off$(eventName, fn) {
      if (data.vfEvents[eventName]) {
        if (fn === undefined || fn === null) {
          data.vfEvents[eventName].length = 0
          return
        }

        for (let i = 0; i < data.vfEvents[eventName].length; i++) {
          if (data.vfEvents[eventName][i] === fn) {
            data.vfEvents[eventName].splice(i, 1)
            break
          }
        }
      }
    },

    on$(eventName, fn) {
      data.vfEvents[eventName] = data.vfEvents[eventName] || []
      data.vfEvents[eventName].push(fn)
    }
  }

  return {
    ...methods
  }
}
