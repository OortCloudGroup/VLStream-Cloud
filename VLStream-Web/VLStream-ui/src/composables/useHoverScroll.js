import { onBeforeUnmount, onMounted } from 'vue'

// Move only while the pointer stays on an edge. Stop at bounds and when the page loses focus.
export function useHoverScroll(element) {
    let frame = 0
    let previous = 0
    const stop = () => {
        cancelAnimationFrame(frame)
        frame = 0
        previous = 0
    }
    const start = direction => {
        stop()
        const step = time => {
            const target = element.value
            if (!target || !target.isConnected || document.hidden) return stop()
            const distance = Math.min(32, previous ? time - previous : 16) * 0.36
            previous = time
            const before = target.scrollLeft
            target.scrollLeft += direction * distance
            if (Math.abs(target.scrollLeft - before) < 0.1) return stop()
            frame = requestAnimationFrame(step)
        }
        frame = requestAnimationFrame(step)
    }
    onMounted(() => {
        window.addEventListener('blur', stop)
        document.addEventListener('visibilitychange', stop)
    })
    onBeforeUnmount(() => {
        stop()
        window.removeEventListener('blur', stop)
        document.removeEventListener('visibilitychange', stop)
    })
    return { start, stop }
}
