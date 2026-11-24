import { computed, shallowRef, watch, onBeforeUnmount } from 'vue'
import { platformRecording } from '@/api/platformRecording'
import { authManager } from '@/utils/auth'

const RECORDING_POLL_MS = 5000
const IDLE_POLL_MS = 15000
const FAILED_POLL_MS = 30000
const MAX_REQUESTS = 3
const sources = new Map()
let timer
let pending = 0

function schedule() {
  clearTimeout(timer)
  if (document.hidden || pending >= MAX_REQUESTS) return
  let nextCheck = Infinity
  for (const source of sources.values()) {
    if (!source.request) nextCheck = Math.min(nextCheck, source.nextCheck)
  }
  if (Number.isFinite(nextCheck)) timer = setTimeout(poll, Math.max(0, nextCheck - Date.now()))
}

function poll() {
  if (document.hidden) return
  const now = Date.now()
  const due = [...sources.values()]
    .filter(source => !source.request && source.nextCheck <= now)
    .sort((left, right) => left.nextCheck - right.nextCheck)
  for (const source of due.slice(0, MAX_REQUESTS - pending)) refresh(source)
  schedule()
}

async function refresh(source) {
  const controller = new AbortController()
  const identity = authManager.getIdentitySnapshot()
  source.request = controller
  pending += 1
  if (source.confirmed.value && !authManager.isIdentityCurrent(source.confirmed.value)) source.confirmed.value = null
  try {
    const recording = await platformRecording.status(source.protocol, source.rowId, controller.signal)
    if (controller.signal.aborted) return
    const active = recording === true && authManager.isIdentityCurrent(identity)
    source.confirmed.value = active ? identity : null
    source.nextCheck = Date.now() + (active ? RECORDING_POLL_MS : IDLE_POLL_MS)
  } catch {
    if (controller.signal.aborted) return
    source.confirmed.value = null
    source.nextCheck = Date.now() + FAILED_POLL_MS
  } finally {
    source.request = null
    pending -= 1
    schedule()
  }
}

function onVisibilityChange() {
  for (const source of sources.values()) {
    source.request?.abort()
    source.confirmed.value = null
    source.nextCheck = 0
  }
  schedule()
}

function subscribe(key) {
  let source = sources.get(key)
  if (!source) {
    const [protocol, rowId] = JSON.parse(key)
    if (!sources.size) document.addEventListener('visibilitychange', onVisibilityChange)
    source = { protocol, rowId, subscribers: 0, confirmed: shallowRef(null), nextCheck: 0, request: null }
    sources.set(key, source)
    schedule()
  }
  source.subscribers += 1
  return {
    confirmed: source.confirmed,
    release() {
      source.subscribers -= 1
      if (source.subscribers) return
      sources.delete(key)
      source.request?.abort()
      if (!sources.size) document.removeEventListener('visibilitychange', onVisibilityChange)
      schedule()
    }
  }
}

export function useDeviceRecording(device) {
  const subscriptions = shallowRef([])
  const stop = watch(() => [...new Set((device()?.sources || []).map(
    ({ protocol, rowId }) => JSON.stringify([protocol, String(rowId)])
  ))], keys => {
    const next = keys.map(subscribe)
    for (const item of subscriptions.value) item.release()
    subscriptions.value = next
  }, { immediate: true })
  onBeforeUnmount(() => {
    stop()
    for (const item of subscriptions.value) item.release()
  })
  return computed(() => subscriptions.value.some(({ confirmed }) => confirmed.value && authManager.isIdentityCurrent(confirmed.value)))
}
