/*
* @Created by: 兰舰
* Email: lanjian@oortcloudsmart.com
* Phone: 16620805419
* @Date: 2025-12-11
* @Last Modified by: 兰舰
* @Last Modified time: 2025-12-11
* @Copyright aPaaS-front-team. All rights reserved.
* @Description: appendBody指令 - 将元素挂载到body下，类似el-dialog的appendToBody功能
*/

interface AppendBodyState {
  originalParent: HTMLElement | null
  placeholder: Comment | null
  mounted: boolean
}

const appendBody = {
  mounted(el: HTMLElement) {
    const state: AppendBodyState = {
      originalParent: el.parentElement,
      placeholder: null,
      mounted: false
    }

    // 创建占位符注释节点，用于记录原始位置
    const placeholder = document.createComment('appendBody placeholder')
    // 在原位置插入占位符
    if (el.parentElement) {
      el.parentElement.insertBefore(placeholder, el)
      state.placeholder = placeholder
    }

    // 将元素移动到body下
    document.body.appendChild(el)
    state.mounted = true

    // 保存状态到元素上，以便卸载时使用
    ;(el as any).__appendBodyState__ = state
  },

  unmounted(el: HTMLElement) {
    const state = (el as any).__appendBodyState__ as AppendBodyState | undefined
    if (state && state.mounted) {
      // 如果元素还在body下，将其移回原位置
      if (el.parentElement === document.body) {
        if (state.placeholder && state.placeholder.parentElement) {
          // 在占位符位置插入元素
          state.placeholder.parentElement.insertBefore(el, state.placeholder)
          // 移除占位符
          state.placeholder.parentElement.removeChild(state.placeholder)
        } else if (state.originalParent) {
          // 如果占位符不存在，尝试恢复到原父元素
          state.originalParent.appendChild(el)
        }
      }
      // 清理状态
      delete (el as any).__appendBodyState__
    }
  }
}

export default appendBody
