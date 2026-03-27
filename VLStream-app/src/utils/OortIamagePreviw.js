/**
 Created by  lanjian   on 2020/7/28  20:40
 Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
 */

import { ImagePreview } from 'vant'
import bus from '@/utils/bus'
let instance = null
export function OortImagePreview(data) {
  bus.$emit('popShow', true)
  document.addEventListener('backbutton', ImagePreviewClose, false)
  // 监听键盘返回键
  const option = {
    onClose() {
      bus.$emit('popShow', false)
      document.removeEventListener('backbutton', ImagePreviewClose, false)
    }
  }
  instance = ImagePreview({ ...option, ...data })
}

function ImagePreviewClose() {
  if (instance) {
    instance.close()
  }
}
