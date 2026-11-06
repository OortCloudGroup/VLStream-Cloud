import { onBeforeUnmount, watch } from 'vue'

// One retry per mounted preview. Closing or changing a source cancels its pending work.
export function usePreviewReconnect({ enabled, failed, busy, reconnect, media = () => null }) {
    let timer, attempts = 0, stopped = false, running = false
    let lastMedia, lastTime = -1, lastProgress = Date.now()
    const cancel = () => { clearTimeout(timer); timer = null }
    const schedule = () => {
        if (stopped || timer || running || !enabled()) return
        const delay = Math.min(30000, 5000 * 2 ** Math.min(attempts++, 3)) + Math.random() * 1000
        timer = setTimeout(async () => {
            timer = null
            if (stopped || !enabled() || busy()) return
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
    const watchdog = setInterval(() => {
        if (!enabled() || stopped || busy() || running) { lastProgress = Date.now(); return }
        if (failed()) { schedule(); return }
        const element = media()
        if (!element) return
        if (element !== lastMedia || element.currentTime !== lastTime) {
            lastMedia = element; lastTime = element.currentTime; lastProgress = Date.now()
            if (element.readyState >= 2) { attempts = 0; cancel() }
        } else if (Date.now() - lastProgress >= 30000) schedule()
    }, 5000)
    onBeforeUnmount(() => { stopped = true; cancel(); clearInterval(watchdog) })
}
