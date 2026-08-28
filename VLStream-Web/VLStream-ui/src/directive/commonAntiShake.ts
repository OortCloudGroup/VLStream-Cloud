/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

export default {
  mounted: (el, binding) => {
    let throttleTime = binding.value //
    if (!throttleTime) { // user Set , 2s
      throttleTime = 2000
    }
    let cbFun
    el.addEventListener('click', event => {
      if (!cbFun) { // Execute
        cbFun = setTimeout(() => {
          cbFun = null
        }, throttleTime)
      } else {
        event && event.stopImmediatePropagation()
      }
    }, true)
  }
}
