/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

interface ResizeState {
  isResizing: boolean
  direction: string
  startX: number
  startY: number
  startWidth: number
  startHeight: number
  startLeft: number
  startTop: number
  minWidth: number
  minHeight: number
}

// configuration
const createDirection = (name: string, cursor: string, position: any) => ({ name, cursor, position })

const DIRECTIONS = [
  createDirection('top', 'n-resize', { top: '-4px', left: '0', right: '0', height: '8px' }),
  createDirection('bottom', 's-resize', { bottom: '-4px', left: '0', right: '0', height: '8px' }),
  createDirection('left', 'w-resize', { left: '-4px', top: '0', bottom: '0', width: '8px' }),
  createDirection('right', 'e-resize', { right: '-4px', top: '0', bottom: '0', width: '8px' }),
  createDirection('top-left', 'nw-resize', { top: '-4px', left: '-4px', width: '12px', height: '12px' }),
  createDirection('top-right', 'ne-resize', { top: '-4px', right: '-4px', width: '12px', height: '12px' }),
  createDirection('bottom-left', 'sw-resize', { bottom: '-4px', left: '-4px', width: '12px', height: '12px' }),
  createDirection('bottom-right', 'se-resize', { bottom: '-4px', right: '-4px', width: '12px', height: '12px' })
]

const resize = {
  mounted(el: HTMLElement, binding: any) {
    // Get configuration item
    const options = binding.value || {}
    const minWidth = options.minWidth || 100
    const minHeight = options.minHeight || 100

    // element is
    if (getComputedStyle(el).position === 'static') {
      el.style.position = 'relative'
    }

    const state: ResizeState = {
      isResizing: false,
      direction: '',
      startX: 0,
      startY: 0,
      startWidth: 0,
      startHeight: 0,
      startLeft: 0,
      startTop: 0,
      minWidth,
      minHeight
    }

    //
    const resizers: HTMLElement[] = []

    DIRECTIONS.forEach(dir => {
      const resizer = document.createElement('div')
      resizer.className = `resize-handle resize-${dir.name}`
      resizer.dataset.direction = dir.name

      Object.assign(resizer.style, {
        position: 'absolute',
        backgroundColor: 'transparent',
        cursor: dir.cursor,
        zIndex: '1000',
        transition: 'background-color 0.2s',
        ...dir.position
      })

      //
      resizer.addEventListener('mouseenter', () => {
        resizer.style.backgroundColor = 'rgba(58, 142, 230, 0.3)'
      })
      resizer.addEventListener('mouseleave', () => {
        if (!state.isResizing) {
          resizer.style.backgroundColor = 'transparent'
        }
      })

      resizer.addEventListener('mousedown', handleMouseDown)
      el.appendChild(resizer)
      resizers.push(resizer)
    })

    // event
    function handleMouseDown(e: MouseEvent) {
      if (e.button !== 0) return

      const target = e.target as HTMLElement
      state.direction = target.dataset.direction || ''
      state.isResizing = true
      state.startX = e.clientX
      state.startY = e.clientY

      const rect = el.getBoundingClientRect()
      state.startWidth = rect.width
      state.startHeight = rect.height
      state.startLeft = el.offsetLeft
      state.startTop = el.offsetTop

      document.body.style.cursor = target.style.cursor
      document.body.style.userSelect = 'none'

      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)

      e.preventDefault()
      e.stopPropagation()
    }

    // event
    function handleMouseMove(e: MouseEvent) {
      if (!state.isResizing) return

      const deltaX = e.clientX - state.startX
      const deltaY = e.clientY - state.startY

      let newWidth = state.startWidth
      let newHeight = state.startHeight
      let newLeft = state.startLeft
      let newTop = state.startTop

      // and
      switch (state.direction) {
        case 'right':
          newWidth = Math.max(state.minWidth, state.startWidth + deltaX)
          break
        case 'left':
          newWidth = Math.max(state.minWidth, state.startWidth - deltaX)
          if (newWidth > state.minWidth) {
            newLeft = state.startLeft + deltaX
          }
          break
        case 'bottom':
          newHeight = Math.max(state.minHeight, state.startHeight + deltaY)
          break
        case 'top':
          newHeight = Math.max(state.minHeight, state.startHeight - deltaY)
          if (newHeight > state.minHeight) {
            newTop = state.startTop + deltaY
          }
          break
        case 'top-left':
          newWidth = Math.max(state.minWidth, state.startWidth - deltaX)
          newHeight = Math.max(state.minHeight, state.startHeight - deltaY)
          if (newWidth > state.minWidth) {
            newLeft = state.startLeft + deltaX
          }
          if (newHeight > state.minHeight) {
            newTop = state.startTop + deltaY
          }
          break
        case 'top-right':
          newWidth = Math.max(state.minWidth, state.startWidth + deltaX)
          newHeight = Math.max(state.minHeight, state.startHeight - deltaY)
          if (newHeight > state.minHeight) {
            newTop = state.startTop + deltaY
          }
          break
        case 'bottom-left':
          newWidth = Math.max(state.minWidth, state.startWidth - deltaX)
          newHeight = Math.max(state.minHeight, state.startHeight + deltaY)
          if (newWidth > state.minWidth) {
            newLeft = state.startLeft + deltaX
          }
          break
        case 'bottom-right':
          newWidth = Math.max(state.minWidth, state.startWidth + deltaX)
          newHeight = Math.max(state.minHeight, state.startHeight + deltaY)
          break
      }

      // new and
      el.style.width = `${newWidth}px`
      el.style.height = `${newHeight}px`

      // if need to ( )
      if (state.direction.includes('left') || state.direction.includes('top')) {
        el.style.left = `${newLeft}px`
        el.style.top = `${newTop}px`
      }

      e.preventDefault()
    }

    // event
    function handleMouseUp() {
      if (state.isResizing) {
        state.isResizing = false
        state.direction = ''
        document.body.style.cursor = ''
        document.body.style.userSelect = ''

        // all
        resizers.forEach(r => {
          r.style.backgroundColor = 'transparent'
        })

        document.removeEventListener('mousemove', handleMouseMove)
        document.removeEventListener('mouseup', handleMouseUp)
      }
    }

    // and
    (el as any).__resizeState__ = {
      state,
      resizers,
      handleMouseMove,
      handleMouseUp
    }
  },

  unmounted(el: HTMLElement) {
    const resizeState = (el as any).__resizeState__
    if (!resizeState) return

    // eventlistener
    document.removeEventListener('mousemove', resizeState.handleMouseMove)
    document.removeEventListener('mouseup', resizeState.handleMouseUp)

    // all
    resizeState.resizers.forEach((resizer: HTMLElement) => {
      if (resizer.parentNode === el) {
        el.removeChild(resizer)
      }
    })

    // Delete
    delete (el as any).__resizeState__
  }
}

export default resize
