/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:54:17
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:54:17
* @Copyright aPaaS-front-team. All rights reserved.
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
