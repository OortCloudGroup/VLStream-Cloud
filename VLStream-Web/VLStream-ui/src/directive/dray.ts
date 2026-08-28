/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import type { DirectiveBinding } from 'vue'

interface DragOptions {
  noLimit?: boolean // whether element, to in body , false
  handle?: string | HTMLElement // , is DOM element, element
}

interface DragState {
  isDragging: boolean
  startX: number
  startY: number
  offsetX: number
  offsetY: number
  currentHandleEl: HTMLElement | null // current element
}

/**
 * event whether in element
 * @param target event element
 * @param el element
 * @param handle element
 * @return s element, not null
 */
function matchHandle(
  target: EventTarget | null,
  el: HTMLElement,
  handle?: string | HTMLElement
): HTMLElement | null {
  if (!target || !(target instanceof HTMLElement)) return null

  // handle, element
  if (!handle) {
    return el.contains(target) ? el : null
  }

  // handle is
  if (typeof handle === 'string') {
    // from event start find , whether
    let current: HTMLElement | null = target
    while (current && current !== el) {
      if (current.matches(handle)) {
        return current
      }
      current = current.parentElement
    }
    // el whether
    if (current === el && el.matches(handle)) {
      return el
    }
    return null
  }

  // handle is HTMLElement
  if (handle instanceof HTMLElement) {
    return handle.contains(target) ? handle : null
  }

  return null
}

const dray = {
  mounted(el: HTMLElement, binding: DirectiveBinding<DragOptions>) {
    const options: DragOptions = binding.value || {}
    const noLimit = options.noLimit ?? false

    // element is
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

    // event - event whether in
    const handleMouseDown = (e: MouseEvent) => {
      // only
      if (e.button !== 0) return

      // event whether in
      const matchedHandle = matchHandle(e.target, el, options.handle)
      if (!matchedHandle) return

      state.currentHandleEl = matchedHandle
      state.isDragging = true
      state.startX = e.clientX
      state.startY = e.clientY

      // Get current transform left/top value
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

    // event
    const handleMouseMove = (e: MouseEvent) => {
      if (!state.isDragging) return

      const deltaX = e.clientX - state.startX
      const deltaY = e.clientY - state.startY

      let newX = state.offsetX + deltaX
      let newY = state.offsetY + deltaY

      //
      let minX = 0
      let minY = 0
      let maxX = 0
      let maxY = 0

      const elRect = el.getBoundingClientRect()
      const computedStyle = getComputedStyle(el)

      if (noLimit) {
        // in body element
        const bodyWidth = document.body.clientWidth
        const bodyHeight = document.body.clientHeight

        if (computedStyle.position === 'fixed') {
          // fixed
          minX = 0
          minY = 0
          maxX = bodyWidth - elRect.width
          maxY = bodyHeight - elRect.height
        } else if (computedStyle.position === 'absolute') {
          // absolute need to
          const currentLeft = parseFloat(computedStyle.left) || 0
          const currentTop = parseFloat(computedStyle.top) || 0
          const contextOffsetX = elRect.left - currentLeft
          const contextOffsetY = elRect.top - currentTop

          minX = -contextOffsetX
          minY = -contextOffsetY
          maxX = bodyWidth - elRect.width - contextOffsetX
          maxY = bodyHeight - elRect.height - contextOffsetY
        } else {
          // relative/static transform
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
        // in element
        const parent = el.parentElement
        if (parent) {
          const parentRect = parent.getBoundingClientRect()
          minX = 0
          minY = 0
          maxX = parentRect.width - elRect.width
          maxY = parentRect.height - elRect.height
        }
      }

      // in
      newX = Math.max(minX, Math.min(newX, maxX))
      newY = Math.max(minY, Math.min(newY, maxY))

      // new
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

    // event
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

    // event element (event )
    el.addEventListener('mousedown', handleMouseDown)
    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)

    // eventProcess
    const elAny = el as any
    elAny.__dragHandlers__ = {
      handleMouseDown,
      handleMouseMove,
      handleMouseUp
    }
  },

  unmounted(el: HTMLElement) {
    // eventlistener
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
