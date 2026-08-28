/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import { ElMessage } from 'element-plus' // message

let messageDom = null
const resetMessage = (options) => {
  if (messageDom) messageDom.close() // Check dialogwhether already in , in
  messageDom = ElMessage(options)
}
const typeArr = ['success', 'error', 'warning', 'info']
typeArr.forEach(type => {
  resetMessage[type] = options => {
    if (typeof options === 'string') options = { message: options }
    options.type = type
    return resetMessage(options)
  }
})

export const message = resetMessage