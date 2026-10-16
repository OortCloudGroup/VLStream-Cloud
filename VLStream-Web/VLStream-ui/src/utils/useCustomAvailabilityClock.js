import { ref, watch, onBeforeUnmount } from 'vue'
import { nextCustomStatusExpiry } from './customDeviceState'

// One timer per page, at the next evidence expiry; no per-second list/map rebuild.
export function useCustomAvailabilityClock(getDevices) {
  const clock = ref(Date.now())
  let timer
  const schedule = () => {
    clearTimeout(timer)
    clock.value = Date.now()
    const next = nextCustomStatusExpiry(getDevices(), clock.value)
    if (Number.isFinite(next)) timer = setTimeout(schedule, Math.min(2147483647, Math.max(1, next - clock.value + 1)))
  }
  watch(getDevices, schedule, { immediate: true })
  onBeforeUnmount(() => clearTimeout(timer))
  return clock
}
