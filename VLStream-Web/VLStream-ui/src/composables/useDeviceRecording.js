import { ref, watch, onBeforeUnmount } from 'vue'
import { platformRecording } from '@/api/platformRecording'

// A status failure must clear the indicator, never imply continued recording.
export function useDeviceRecording(device) {
  const recording = ref(false)
  let generation = 0, timer
  watch(() => JSON.stringify((device()?.sources || []).map(s => [s.protocol, s.rowId])), () => {
    const ticket = ++generation
    clearTimeout(timer); recording.value = false
    const sources = [...(device()?.sources || [])]
    async function refresh() {
      try {
        const states = await Promise.all(sources.map(s => platformRecording.status(s.protocol, s.rowId).catch(() => false)))
        if (ticket === generation) recording.value = states.some(value => value === true)
      } catch { if (ticket === generation) recording.value = false }
      finally { if (ticket === generation) timer = setTimeout(refresh, 5000) }
    }
    if (sources.length) refresh()
  }, { immediate: true })
  onBeforeUnmount(() => { generation++; clearTimeout(timer) })
  return recording
}
