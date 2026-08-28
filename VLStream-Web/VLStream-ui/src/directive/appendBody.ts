/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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

    // node, record
    const placeholder = document.createComment('appendBody placeholder')
    // in
    if (el.parentElement) {
      el.parentElement.insertBefore(placeholder, el)
      state.placeholder = placeholder
    }

    // element body
    document.body.appendChild(el)
    state.mounted = true

    // element ,
    ;(el as any).__appendBodyState__ = state
  },

  unmounted(el: HTMLElement) {
    const state = (el as any).__appendBodyState__ as AppendBodyState | undefined
    if (state && state.mounted) {
      // if element in body ,
      if (el.parentElement === document.body) {
        if (state.placeholder && state.placeholder.parentElement) {
          // in element
          state.placeholder.parentElement.insertBefore(el, state.placeholder)
          //
          state.placeholder.parentElement.removeChild(state.placeholder)
        } else if (state.originalParent) {
          // if in , element
          state.originalParent.appendChild(el)
        }
      }
      //
      delete (el as any).__appendBodyState__
    }
  }
}

export default appendBody
