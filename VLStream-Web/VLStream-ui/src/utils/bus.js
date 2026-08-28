/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// to vue3.0
// from instance in $on, $off and $once method , $emit is API , to component eventProcess
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

// event
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
