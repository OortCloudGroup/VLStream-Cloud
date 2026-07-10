/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:54:11
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:54:11
* @Copyright aPaaS-front-team. All rights reserved.
*/
export default {
  mounted: (el, binding) => {
    let throttleTime = binding.value // 防抖时间
    if (!throttleTime) { // 用户若不设置防抖时间，则默认2s
      throttleTime = 2000
    }
    let cbFun
    el.addEventListener('click', event => {
      if (!cbFun) { // 第一次执行
        cbFun = setTimeout(() => {
          cbFun = null
        }, throttleTime)
      } else {
        event && event.stopImmediatePropagation()
      }
    }, true)
  }
}
