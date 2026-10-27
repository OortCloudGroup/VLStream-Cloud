// Consume only the authorized detail response. Never merge another row's media
// or retain a list URL after a failed/forbidden detail refresh.
export function hasEventVideoReferences(detail) {
    return Array.isArray(detail?.video)
        ? detail.video.length > 0
        : Boolean(detail?.videoUrl || detail?.videoMediaId)
}

export function eventVideoSources(detail) {
    const sources = Array.isArray(detail?.video)
        ? detail.video
        : detail?.videoUrl ? [detail.videoUrl] : []
    const seen = new Set()
    const videos = []
    sources.forEach((source, sourceIndex) => {
        if (typeof source !== 'string') return
        const url = source.trim()
        try {
            const parsed = new URL(url)
            if (!['http:', 'https:'].includes(parsed.protocol) || parsed.username || parsed.password || seen.has(url)) return
        } catch { return }
        seen.add(url)
        videos.push({
            key: `video-${sourceIndex}`,
            url,
            // The compatibility API exposes a stable ID for the first video only.
            videoMediaId: sourceIndex === 0 && url === detail.videoUrl ? detail.videoMediaId || undefined : undefined
        })
    })
    return videos
}

// Isolated loader for the event player. Its caller supplies the current identity
// and the existing authenticated eventDetail API; no separate permission path.
export function createEventVideoLoader(fetchDetail, getIdentity, onState) {
    let generation = 0
    return {
        invalidate() {
            generation++
            onState({ status: 'idle', eventId: '', videos: [] })
        },
        async load(value) {
            const eventId = String(value ?? '')
            const requestGeneration = ++generation
            const identity = getIdentity()
            const current = () => requestGeneration === generation && identity === getIdentity()
            onState({ status: 'loading', eventId, videos: [] })
            try {
                if (!eventId) throw new Error('missing event')
                const response = await fetchDetail({ id: eventId })
                if (!current()) return
                if (response?.code !== 200 || String(response.data?.id ?? '') !== eventId) throw new Error('invalid event detail')
                const videos = eventVideoSources(response.data)
                onState({ status: !videos.length && hasEventVideoReferences(response.data) ? 'error' : 'ready', eventId, videos })
            } catch {
                if (current()) onState({ status: 'error', eventId, videos: [] })
            }
        }
    }
}

export function releaseEventVideo(video) {
    if (!video) return
    video.pause()
    video.removeAttribute('src')
    video.srcObject = null
    video.load()
}
