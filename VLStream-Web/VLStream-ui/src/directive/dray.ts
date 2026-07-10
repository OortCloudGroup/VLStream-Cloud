/*
* @Created by: 兰舰
* Email: lanjian@oortcloudsmart.com
* Phone: 16620805419
* @Date: 2025-12-10
* @Last Modified by: 兰舰
* @Last Modified time: 2025-12-10
* @Copyright aPaaS-front-team. All rights reserved.
* @Description: 拖拽指令 - 使元素可拖动
* @Usage:
*   v-dray                              - 默认限制在父元素范围内，整个元素可拖拽
*   v-dray="{ noLimit: true }"          - 限制在 body 元素范围内
*   v-dray="{ handle: '.header' }"      - 指定拖拽手柄（CSS选择器）
*   v-dray="{ handle: headerRef }"      - 指定拖拽手柄（HTMLElement）
*/

import type { DirectiveBinding } from 'vue'

interface DragOptions {
  noLimit?: boolean // 是否不限制于父元素，改为限制在 body 内，默认 false
  handle?: string | HTMLElement // 拖拽手柄，可以是选择器或 DOM 元素，不指定则整个元素可拖拽
}

interface DragState {
  isDragging: boolean
  startX: number
  startY: number
  offsetX: number
  offsetY: number
  currentHandleEl: HTMLElement | null // 当前拖拽的手柄元素
}

/**
 * 检查事件目标是否在手柄元素内
 * @param target 事件目标元素
 * @param el 指令绑定的元素
 * @param handle 手柄选择器或元素
 * @returns 匹配到的手柄元素，未匹配则返回 null
 */
function matchHandle(
  target: EventTarget | null,
  el: HTMLElement,
  handle?: string | HTMLElement
): HTMLElement | null {
  if (!target || !(target instanceof HTMLElement)) return null

  // 没有指定 handle，整个元素都可拖拽
  if (!handle) {
    return el.contains(target) ? el : null
  }

  // handle 是字符串选择器
  if (typeof handle === 'string') {
    // 从事件目标开始向上查找，看是否匹配手柄选择器
    let current: HTMLElement | null = target
    while (current && current !== el) {
      if (current.matches(handle)) {
        return current
      }
      current = current.parentElement
    }
    // 检查 el 本身是否匹配
    if (current === el && el.matches(handle)) {
      return el
    }
    return null
  }

  // handle 是 HTMLElement
  if (handle instanceof HTMLElement) {
    return handle.contains(target) ? handle : null
  }

  return null
}

const dray = {
  mounted(el: HTMLElement, binding: DirectiveBinding<DragOptions>) {
    const options: DragOptions = binding.value || {}
    const noLimit = options.noLimit ?? false

    // 确保元素是可定位的
    if (getComputedStyle(el).position === 'static') {
      el.style.position = 'relative'
    }

    const state: DragState = {
      isDragging: false,
      startX: 0,
      startY: 0,
      offsetX: 0,
      offsetY: 0,
      currentHandleEl: null
    }

    // 鼠标按下事件 - 使用事件委托检查是否点击在手柄上
    const handleMouseDown = (e: MouseEvent) => {
      // 只响应左键
      if (e.button !== 0) return

      // 使用事件委托检查是否点击在手柄上
      const matchedHandle = matchHandle(e.target, el, options.handle)
      if (!matchedHandle) return

      state.currentHandleEl = matchedHandle
      state.isDragging = true
      state.startX = e.clientX
      state.startY = e.clientY

      // 获取当前的 transform 或 left/top 值
      const computedStyle = getComputedStyle(el)
      if (computedStyle.position === 'absolute' || computedStyle.position === 'fixed') {
        state.offsetX = parseFloat(computedStyle.left) || 0
        state.offsetY = parseFloat(computedStyle.top) || 0
      } else {
        const transform = computedStyle.transform
        if (transform && transform !== 'none') {
          const matrix = new DOMMatrix(transform)
          state.offsetX = matrix.m41
          state.offsetY = matrix.m42
        } else {
          state.offsetX = 0
          state.offsetY = 0
        }
      }

      matchedHandle.style.cursor = 'move'
      el.style.userSelect = 'none'

      e.preventDefault()
    }

    // 鼠标移动事件
    const handleMouseMove = (e: MouseEvent) => {
      if (!state.isDragging) return

      const deltaX = e.clientX - state.startX
      const deltaY = e.clientY - state.startY

      let newX = state.offsetX + deltaX
      let newY = state.offsetY + deltaY

      // 计算边界限制
      let minX = 0
      let minY = 0
      let maxX = 0
      let maxY = 0

      const elRect = el.getBoundingClientRect()
      const computedStyle = getComputedStyle(el)

      if (noLimit) {
        // 限制在 body 元素范围内
        const bodyWidth = document.body.clientWidth
        const bodyHeight = document.body.clientHeight

        if (computedStyle.position === 'fixed') {
          // fixed 定位直接相对于视口
          minX = 0
          minY = 0
          maxX = bodyWidth - elRect.width
          maxY = bodyHeight - elRect.height
        } else if (computedStyle.position === 'absolute') {
          // absolute 需要计算定位上下文的偏移
          const currentLeft = parseFloat(computedStyle.left) || 0
          const currentTop = parseFloat(computedStyle.top) || 0
          const contextOffsetX = elRect.left - currentLeft
          const contextOffsetY = elRect.top - currentTop

          minX = -contextOffsetX
          minY = -contextOffsetY
          maxX = bodyWidth - elRect.width - contextOffsetX
          maxY = bodyHeight - elRect.height - contextOffsetY
        } else {
          // relative/static 使用 transform
          const transform = computedStyle.transform
          let transformX = 0
          let transformY = 0
          if (transform && transform !== 'none') {
            const matrix = new DOMMatrix(transform)
            transformX = matrix.m41
            transformY = matrix.m42
          }
          const originalX = elRect.left - transformX
          const originalY = elRect.top - transformY

          minX = -originalX
          minY = -originalY
          maxX = bodyWidth - elRect.width - originalX
          maxY = bodyHeight - elRect.height - originalY
        }
      } else {
        // 默认限制在父元素范围内
        const parent = el.parentElement
        if (parent) {
          const parentRect = parent.getBoundingClientRect()
          minX = 0
          minY = 0
          maxX = parentRect.width - elRect.width
          maxY = parentRect.height - elRect.height
        }
      }

      // 限制在边界范围内
      newX = Math.max(minX, Math.min(newX, maxX))
      newY = Math.max(minY, Math.min(newY, maxY))

      // 应用新位置
      if (computedStyle.position === 'absolute' || computedStyle.position === 'fixed') {
        el.style.left = `${newX}px`
        el.style.top = `${newY}px`
        el.style.bottom = 'inherit'
        el.style.right = 'inherit'
      } else {
        el.style.transform = `translate(${newX}px, ${newY}px)`
      }

      e.preventDefault()
    }

    // 鼠标释放事件
    const handleMouseUp = () => {
      if (state.isDragging) {
        state.isDragging = false
        if (state.currentHandleEl) {
          state.currentHandleEl.style.cursor = ''
        }
        el.style.userSelect = ''
        state.currentHandleEl = null
      }
    }

    // 绑定事件到元素本身（事件委托）
    el.addEventListener('mousedown', handleMouseDown)
    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)

    // 保存事件处理器以便卸载时清理
    const elAny = el as any
    elAny.__dragHandlers__ = {
      handleMouseDown,
      handleMouseMove,
      handleMouseUp
    }
  },

  unmounted(el: HTMLElement) {
    // 清理事件监听器
    const handlers = (el as any).__dragHandlers__
    if (handlers) {
      el.removeEventListener('mousedown', handlers.handleMouseDown)
      document.removeEventListener('mousemove', handlers.handleMouseMove)
      document.removeEventListener('mouseup', handlers.handleMouseUp)
      delete (el as any).__dragHandlers__
    }
  }
}

export default dray
