import { computed, onBeforeUnmount, ref } from 'vue'
import { readWvpMediaAvailability } from './wvpResponse.mjs'

export function useWvpMediaStatus(fetchStatus) {
  const mediaAvailable = ref(null), mediaLoading = ref(false), mediaError = ref('')
  let generation = 0, disposed = false
  const mediaLabel = computed(() => mediaLoading.value ? '检测中' : mediaError.value ? '检测失败'
    : mediaAvailable.value === null ? '未知' : mediaAvailable.value ? '可用' : '不可用')
  const mediaTagType = computed(() => mediaAvailable.value === null ? 'info' : mediaAvailable.value ? 'success' : 'danger')
  async function loadMediaStatus() {
    const current = ++generation
    mediaLoading.value = true
    mediaAvailable.value = null
    mediaError.value = ''
    try {
      const available = readWvpMediaAvailability(await fetchStatus())
      if (!disposed && current === generation) mediaAvailable.value = available
    } catch (error) {
      if (!disposed && current === generation) mediaError.value = error?.message || 'WVP 媒体状态检测失败'
    } finally {
      if (!disposed && current === generation) mediaLoading.value = false
    }
  }
  onBeforeUnmount(() => { disposed = true; generation += 1 })
  return { mediaAvailable, mediaLabel, mediaTagType, mediaError, loadMediaStatus }
}
