// @ts-ignore
import Clipboard from 'clipboard'
import { ElMessage } from 'element-plus'

const copy = {
  mounted(el, { value }) {
    const clip = new Clipboard(el)
    el.dataset.clipboardText = value
    clip.on('success', e => {
      if (!value) {
        // 值为空
        ElMessage.warning('无复制内容')
        return
      }
      ElMessage.success('复制成功')
      e.clearSelection()
    })
    clip.on('error', e => {
      console.log(e)
      ElMessage.warning('复制失败')
    })
  },
  beforeUpdate(el, { value }) {
    el.dataset.clipboardText = value
  }
}

export default copy
