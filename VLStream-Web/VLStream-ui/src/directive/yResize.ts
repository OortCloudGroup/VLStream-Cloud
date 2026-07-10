export default {
  mounted(el) {
    // 创建状态对象并附加到元素上
    const state = {
      isResizing: false,
      resizer: null,
      initResize: (e) => {
        e.preventDefault()
        state.isResizing = true
        document.addEventListener('mousemove', state.resize)
        document.addEventListener('mouseup', state.stopResize)
      },
      resize: (e) => {
        if (!state.isResizing) return
        const containerRect = el.getBoundingClientRect()
        const newWidth = e.clientX - containerRect.left
        el.style.width = `${newWidth}px`
      },
      stopResize: () => {
        state.isResizing = false
        document.removeEventListener('mousemove', state.resize)
        document.removeEventListener('mouseup', state.stopResize)
      }
    }

    // 存储状态到元素上
    el.__resizeState__ = state

    // 设置元素样式
    el.style.position = 'relative'
    el.style.overflow = 'hidden' // 防止内容溢出

    // 创建调整手柄
    const resizer = document.createElement('div')
    Object.assign(resizer.style, {
      position: 'absolute',
      right: '0',
      top: '0',
      bottom: '0',
      width: '2px', // 稍微加宽以便操作
      backgroundColor: '#66666610',
      cursor: 'ew-resize',
      zIndex: '100',
      transition: 'background-color 0.2s'
    })

    // 悬停效果
    resizer.addEventListener('mouseenter', () => {
      resizer.style.backgroundColor = '#3a8ee6'
      resizer.style.width = '4px'
    })
    resizer.addEventListener('mouseleave', () => {
      resizer.style.backgroundColor = '#66666610'
      resizer.style.width = '2px'
    })

    // 绑定事件
    resizer.addEventListener('mousedown', state.initResize)
    el.appendChild(resizer)
    state.resizer = resizer
  },

  unmounted(el) {
    const state = el.__resizeState__
    if (!state) return

    // 清理事件监听器
    document.removeEventListener('mousemove', state.resize)
    document.removeEventListener('mouseup', state.stopResize)

    // 移除DOM元素
    if (state.resizer && state.resizer.parentNode === el) {
      el.removeChild(state.resizer)
    }

    // 删除状态引用
    delete el.__resizeState__
  }
}
