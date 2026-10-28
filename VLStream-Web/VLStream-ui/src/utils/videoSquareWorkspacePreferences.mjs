import { videoGridStyle } from './videoSquareLayout.mjs'

const key = 'vls.videoSquare.workspace.v1'
export function normalizeVideoSquarePreferences(value = {}) {
    const view = ['video', 'map', 'list'].includes(value.view) ? value.view : 'video'
    const presentation = ['default', 'bottom', 'grid'].includes(value.presentation) ? value.presentation : 'default'
    let layout = {count:1, variant:'grid'}
    try {
        videoGridStyle(value.layout?.count, value.layout?.variant)
        layout = {count:value.layout.count, variant:value.layout.variant || 'grid'}
    } catch { /* Invalid or outdated preferences keep the safe default. */ }
    return {view, presentation, layout, closed:value.closed === true}
}

// Tab-local display preferences only: no devices, coordinates, media or credentials.
export function readVideoSquarePreferences(storage) {
    try { return normalizeVideoSquarePreferences(JSON.parse((storage || globalThis.sessionStorage)?.getItem(key) || '{}')) }
    catch { return normalizeVideoSquarePreferences() }
}
export function saveVideoSquarePreferences(value, storage) {
    try { (storage || globalThis.sessionStorage)?.setItem(key, JSON.stringify(normalizeVideoSquarePreferences(value))) }
    catch { /* Storage restrictions must not prevent playback or layout changes. */ }
}
