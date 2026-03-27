/*
 * @Author: lanjian
 * @Date: 2021-11-27 10:13:50
 * @LastEditors: lanjian
 * @LastEditTime: 2021-11-27 12:19:01
 * @FilePath: \cordava_utils\demo\src\util\bus.js
 * @Description: Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
 */
/**
 * @Author: lanjian
 * @Date: 2019-12-6 15:18
 * @LastEditors: lanjian
 * @LastEditTime: 2020-09-22 12:23:46
 * @Description: Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
 */

// 为了适应vue3.0的改版
// 我们整个从实例中移除了 $on，$off 和 $once 方法，$emit 仍然是现有 API 的一部分，因为它用于触发由父组件以声明方式附加的事件处理程序
import mitt from 'mitt'

const bus = {}

const emitter = mitt()

bus.$on = emitter.on
bus.$off = emitter.off
bus.$emit = emitter.emit

bus.$clear = function(types = []) {
  if (types) {
    if (typeof types === 'string') {
      types = [types]
    }
    if (types instanceof Array) {
      for (let type of types) {
        emitter.off(type)
      }
    }
  }
}

// 注册监听事件
bus.$register = function(type, callback, _t, field) {
  if (_t) {
    let types = field || 'types'
    if (!_t[types]) {
      _t[types] = {}
    }
    _t[types][type] = true
  }
  emitter.on(type, callback)
}

export default bus
