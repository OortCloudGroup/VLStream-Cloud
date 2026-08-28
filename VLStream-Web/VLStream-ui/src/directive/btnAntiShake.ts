/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
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
