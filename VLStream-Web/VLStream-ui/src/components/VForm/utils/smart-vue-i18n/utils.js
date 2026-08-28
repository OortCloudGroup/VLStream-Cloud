/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// value in
export function isDef(value) {
  return value !== undefined && value !== null
}

// object 'a.b' {a: {b: 'val'}}
export function get(object, path) {
  const keys = path.split('.')
  let result = object

  keys.forEach(key => {
    result = isDef(result) && isDef(result[key]) ? result[key] : null
  })

  return result
}

// whether is object
export function isObj(x) {
  const type = typeof x
  return x !== null && (type === 'object' || type === 'function')
}

//
const { hasOwnProperty } = Object.prototype

function assignKey(to, from, key) {
  const val = from[key]

  if (!isDef(val)) {
    return
  }

  if (!hasOwnProperty.call(to, key) || !isObj(val)) {
    to[key] = val
  } else {
    to[key] = deepAssign(Object(to[key]), from[key])
  }
}

export function deepAssign(to, from) {
  Object.keys(from).forEach(key => {
    assignKey(to, from, key)
  })

  return to
}
