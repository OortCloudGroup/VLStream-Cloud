import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const SCROLL_PIXELS_PER_MS = 0.028
const RESUME_DELAY_MS = 650

export function videoStripLoopWidth(count, itemWidth, viewportWidth, gap = 0) {
    const itemRun = count * (itemWidth + gap)
    if (!viewportWidth || !itemWidth || count < 2 || itemRun - gap <= viewportWidth + 1) return 0
    // Leave enough room to move a fully clipped item to the far edge without cloning its player.
    return Math.ceil(Math.max(itemRun, viewportWidth + itemWidth))
}

export function videoStripWrappedCount(itemWidth, offset, loopWidth, gap = 0) {
    return loopWidth ? Math.floor((offset + gap) / (itemWidth + gap)) : 0
}

export function useVideoMarquee(element, loopWidth) {
    const offset = ref(0)
    const reduceMotion = ref(true)
    const period = computed(() => reduceMotion.value ? 0 : loopWidth.value)
    const holds = new Set()
    let frame = 0
    let timer
    let previousTime = 0
    let position = 0
    let lastScroll = 0
    let intersecting = false
    let motionPreference
    let observer

    const syncOffset = () => { offset.value = element.value?.scrollLeft || 0 }
    const canRun = () => period.value > 0 && intersecting && !document.hidden && !holds.size && element.value?.isConnected

    const stop = () => {
        clearTimeout(timer)
        if (frame) cancelAnimationFrame(frame)
        frame = 0
        previousTime = 0
    }

    const step = time => {
        if (!canRun()) return stop()
        const target = element.value
        if (!previousTime || target.scrollLeft !== lastScroll) position = target.scrollLeft
        const elapsed = previousTime ? Math.min(time - previousTime, 40) : 0
        position = (position + elapsed * SCROLL_PIXELS_PER_MS) % period.value
        previousTime = time
        target.scrollLeft = position
        lastScroll = target.scrollLeft
        syncOffset()
        frame = requestAnimationFrame(step)
    }

    const schedule = () => {
        stop()
        if (canRun()) timer = setTimeout(() => { frame = requestAnimationFrame(step) }, RESUME_DELAY_MS)
    }

    const setPaused = (reason, paused) => {
        if (paused === holds.has(reason)) return
        if (paused) holds.add(reason)
        else holds.delete(reason)
        schedule()
    }

    const trackFocus = event => {
        const next = event.type === 'focusout' ? event.relatedTarget : event.target
        setPaused('keyboard', !!next?.matches?.(':focus-visible') && event.currentTarget.contains(next))
    }

    const syncMotionPreference = () => { reduceMotion.value = motionPreference?.matches ?? false }
    const observe = () => {
        observer?.disconnect()
        intersecting = !!element.value && !observer
        if (element.value) observer?.observe(element.value)
        syncOffset()
        schedule()
    }

    watch(element, () => {
        holds.clear()
        observe()
    }, { flush: 'post' })
    watch(period, schedule, { flush: 'post' })
    onMounted(() => {
        motionPreference = window.matchMedia?.('(prefers-reduced-motion: reduce)')
        motionPreference?.addEventListener('change', syncMotionPreference)
        syncMotionPreference()
        if (typeof IntersectionObserver !== 'undefined') {
            observer = new IntersectionObserver(entries => {
                const entry = entries.find(candidate => candidate.target === element.value)
                if (!entry) return
                intersecting = entry.isIntersecting
                schedule()
            })
        }
        observe()
        document.addEventListener('visibilitychange', schedule)
    })
    onBeforeUnmount(() => {
        stop()
        observer?.disconnect()
        motionPreference?.removeEventListener('change', syncMotionPreference)
        document.removeEventListener('visibilitychange', schedule)
    })

    return { offset, period, setPaused, trackFocus, syncOffset, schedule }
}
