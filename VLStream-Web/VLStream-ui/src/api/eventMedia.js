import request from '@/utils/request'
import { ElMessage } from 'element-plus'

// API calls use the current business identity; storage links carry only their own signature.
export async function downloadEventVideo(event) {
  try {
    const response = event.videoMediaId
      ? await request({ url: `/vlsDeviceMedia/${encodeURIComponent(event.videoMediaId)}/download-url`, method: 'get' })
      : null
    const url = response?.data || event.videoUrl
    if (!/^https?:\/\//i.test(url || '')) throw new Error('视频下载地址不可用，请刷新后重试')
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'event.mp4'
    anchor.target = '_blank'
    anchor.rel = 'noopener noreferrer'
    anchor.click()
  } catch (error) {
    ElMessage.error(error.message || '视频下载失败')
  }
}
