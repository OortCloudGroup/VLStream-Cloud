/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 11:04:44
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 11:04:44
* @Copyright aPaaS-front-team. All rights reserved.
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
