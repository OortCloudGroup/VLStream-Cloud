/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import preReClick from './btnAntiShake'
import debounceClick from './commonAntiShake'
import copy from './copy'
import yResize from './yResize'
import dray from './dray'
import resize from './resize'
import appendBody from './appendBody'
import watermark from './watermark'

const directives = {
  preReClick,
  debounceClick,
  copy,
  yResize,
  dray,
  resize,
  appendBody,
  watermark
}
export default {
  install(Vue) {
    Object.keys(directives).forEach(key => {
      Vue.directive(key, directives[key])
    })
  }
}
