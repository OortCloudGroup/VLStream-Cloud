export function isVideoFullscreen(target) {
    const current = target?.ownerDocument?.fullscreenElement
    return !!(current && (target.contains(current) || current.contains(target)))
}

export function videoFullscreenContext(target) {
    const document = target?.ownerDocument
    const window = document?.defaultView
    const policy = document?.permissionsPolicy || document?.featurePolicy
    let allowed = null
    try { allowed = policy?.allowsFeature('fullscreen') ?? null } catch { /* Optional browser API. */ }
    return {
        targetTag: target?.tagName || null,
        targetConnected: target?.isConnected ?? false,
        fullscreenEnabled: document?.fullscreenEnabled ?? null,
        fullscreenTag: document?.fullscreenElement?.tagName || null,
        policyAllowsFullscreen: allowed,
        topLevel: window ? window === window.top : null,
        focused: document?.hasFocus?.() ?? null,
        visibility: document?.visibilityState || null,
        userActivation: window?.navigator?.userActivation?.isActive ?? null,
    }
}

// Invoke the browser API synchronously in the click stack, before any await.
export function toggleVideoFullscreen(target) {
    const document = target?.ownerDocument
    if (!document || !target.isConnected) return Promise.reject(new Error('Video fullscreen target is not connected'))
    try {
        if (isVideoFullscreen(target)) return Promise.resolve(document.exitFullscreen())
        if (typeof target.requestFullscreen !== 'function') return Promise.reject(new Error('Fullscreen API is unavailable'))
        return Promise.resolve(target.requestFullscreen())
    } catch (error) {
        return Promise.reject(error)
    }
}
