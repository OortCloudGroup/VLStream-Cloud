/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:54:07
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:54:07
* @Copyright aPaaS-front-team. All rights reserved.
*/
export default {
  mounted: (el, binding) => {
    el.addEventListener('click', () => {
      if (!el.disabled) {
        if (el.style['pointer-events']) {
          el.style['pointer-events'] = 'none'
        }
        el.disabled = true
        setTimeout(() => {
          el.style['pointer-events'] = 'all'
          el.disabled = false
        }, binding.value || 2000)
      }
    })
  }
}
