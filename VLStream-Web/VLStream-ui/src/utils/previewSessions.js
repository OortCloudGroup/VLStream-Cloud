import { applyWvpHeaders, getWvpBaseURL } from '@/utils/wvpRequest'
import { createPreviewSessionRegistry } from './previewSessionRegistry.mjs'
import { createPreviewStartQueue } from './previewStartQueue.mjs'

const registry = createPreviewSessionRegistry(session => {
  const path = `${session.base}/${session.deviceId}/preview/${session.id}`
  // Same identity headers as every other WVP request; keepalive lets the DELETE outlive the unloading page.
  const config = applyWvpHeaders({ baseURL: getWvpBaseURL(), url: path, method: 'delete', headers: {} })
  fetch(`${getWvpBaseURL()}${path}`, { method: 'DELETE', headers: config.headers, keepalive: true, credentials: 'same-origin' })
    .catch(() => { /* The backend lease retries cleanup. */ })
})

if (typeof window !== 'undefined' && !window.__vlsPreviewPagehideRelease) {
  window.__vlsPreviewPagehideRelease = true
  window.addEventListener('pagehide', () => registry.releaseAll())
}

const startQueue = createPreviewStartQueue()

/** Track a live lease { base, deviceId, id }; call the returned function once it is released normally. */
export const registerPreviewSession = session => registry.register(session)

/** Run a compact (thumbnail) preview start through the shared queue; undefined means it was cancelled while waiting. */
export const schedulePreviewStart = (task, cancelled) => startQueue.schedule(task, cancelled)
