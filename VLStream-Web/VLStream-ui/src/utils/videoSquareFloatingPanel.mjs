const finiteSize = value => Number.isFinite(value) ? Math.max(0, value) : 0

export function clampFloatingPanelPosition(position, bounds, margin = 8) {
    const availableX = Math.max(0, finiteSize(bounds.width) - finiteSize(bounds.panelWidth))
    const availableY = Math.max(0, finiteSize(bounds.height) - finiteSize(bounds.panelHeight))
    const gapX = Math.min(finiteSize(margin), availableX / 2)
    const gapY = Math.min(finiteSize(margin), availableY / 2)
    const clamp = (value, lower, upper) => Math.min(upper, Math.max(lower, Number.isFinite(value) ? value : lower))
    return {
        x: clamp(position.x, gapX, availableX - gapX),
        y: clamp(position.y, gapY, availableY - gapY),
    }
}

// The handle owns drag listeners; no events or commands are sent to the PTZ body.
export function createFloatingPanelDrag({ readBounds, readPosition, writePosition, eventTarget, onDragChange = () => {}, margin = 8 }) {
    let active = null
    let disposed = false
    const fit = () => {
        if (!disposed) writePosition(clampFloatingPanelPosition(readPosition(), readBounds(), margin))
    }
    const stop = event => {
        if (!active || (event?.pointerId != null && event.pointerId !== active.pointerId)) return
        const current = active
        active = null
        eventTarget.removeEventListener('pointermove', move)
        eventTarget.removeEventListener('pointerup', stop)
        eventTarget.removeEventListener('pointercancel', stop)
        eventTarget.removeEventListener('blur', blur)
        try {
            if (current.handle?.hasPointerCapture?.(current.pointerId)) current.handle.releasePointerCapture(current.pointerId)
        } catch { /* The browser may already have released capture. */ }
        onDragChange(false)
    }
    const blur = () => stop()
    const move = event => {
        if (!active || event.pointerId !== active.pointerId || !Number.isFinite(event.clientX) || !Number.isFinite(event.clientY)) return
        const position = readPosition()
        const next = { x: position.x + event.clientX - active.x, y: position.y + event.clientY - active.y }
        active.x = event.clientX
        active.y = event.clientY
        writePosition(clampFloatingPanelPosition(next, readBounds(), margin))
        event.preventDefault?.()
    }
    const start = event => {
        if (disposed || active || event.button !== 0 || event.isPrimary === false || !Number.isFinite(event.pointerId) || !Number.isFinite(event.clientX) || !Number.isFinite(event.clientY)) return false
        fit()
        active = { pointerId: event.pointerId, x: event.clientX, y: event.clientY, handle: event.currentTarget }
        try { active.handle?.setPointerCapture?.(active.pointerId) } catch { /* Window listeners still cover uncaptured pointers. */ }
        eventTarget.addEventListener('pointermove', move, { passive: false })
        eventTarget.addEventListener('pointerup', stop)
        eventTarget.addEventListener('pointercancel', stop)
        eventTarget.addEventListener('blur', blur)
        onDragChange(true)
        event.preventDefault?.()
        return true
    }
    const moveByKey = event => {
        if (disposed || active) return
        const delta = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] }[event.key]
        if (!delta) return
        const step = event.shiftKey ? 20 : 10
        const position = readPosition()
        writePosition(clampFloatingPanelPosition({ x: position.x + delta[0] * step, y: position.y + delta[1] * step }, readBounds(), margin))
        event.preventDefault?.()
        event.stopPropagation?.()
    }
    return {
        start, stop, fit, moveByKey,
        destroy() { stop(); disposed = true },
        isDragging: () => active !== null,
    }
}
