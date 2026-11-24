import { onBeforeUnmount, watch } from 'vue'

const monitors = new Set()
const pageVisible = () => typeof document === 'undefined' || !document.hidden
let watchdog

function updateWatchdog() {
    clearInterval(watchdog)
    watchdog = null
    const visible = pageVisible()
    for (const monitor of monitors) monitor.visibilityChanged(visible)
    if (visible && monitors.size) {
        watchdog = setInterval(() => {
            for (const monitor of monitors) monitor.check()
        }, 5000)
    }
}

function registerMonitor(monitor) {
    monitors.add(monitor)
    if (monitors.size === 1) {
        if (typeof document !== 'undefined') document.addEventListener('visibilitychange', updateWatchdog)
        updateWatchdog()
    }
    return () => {
        monitors.delete(monitor)
        if (monitors.size > 0) return
        if (typeof document !== 'undefined') document.removeEventListener('visibilitychange', updateWatchdog)
        updateWatchdog()
    }
}

// One retry per mounted preview. Closing or changing a source cancels its pending work.
export function usePreviewReconnect({ enabled, failed, busy, reconnect, media = () => null }) {
    let timer, attempts = 0, stopped = false, running = false
    let lastMedia, lastTime = -1, lastProgress = Date.now()
    const cancel = () => { clearTimeout(timer); timer = null }
    const schedule = () => {
        if (stopped || timer || running || !enabled() || !pageVisible()) return
        const delay = Math.min(30000, 5000 * 2 ** Math.min(attempts, 3)) + Math.random() * 1000
        timer = setTimeout(async () => {
            timer = null
            if (stopped || !enabled() || !pageVisible() || busy()) return
            attempts++
            running = true
            try { await reconnect() } catch { /* Player owns the failure message. */ }
            finally {
                running = false
                lastProgress = Date.now()
                if (!stopped && failed()) schedule()
            }
        }, delay)
    }
    watch(() => [enabled(), failed()], ([active, failure]) => {
        if (active && failure) schedule()
        else cancel()
    }, { immediate: true })
    const unregister = registerMonitor({
        visibilityChanged(visible) {
            lastProgress = Date.now()
            if (visible && failed()) schedule()
            else cancel()
        },
        check() {
            if (!enabled() || busy() || running) { lastProgress = Date.now(); return }
            if (failed()) { schedule(); return }
            const element = media()
            if (!element || (element.paused && element.readyState >= 2)) {
                lastProgress = Date.now()
                cancel()
                return
            }
            if (element !== lastMedia || element.currentTime !== lastTime) {
                lastMedia = element
                lastTime = element.currentTime
                lastProgress = Date.now()
                if (element.readyState >= 2) { attempts = 0; cancel() }
            } else if (Date.now() - lastProgress >= 30000) schedule()
        }
    })
    onBeforeUnmount(() => { stopped = true; cancel(); unregister() })
}
