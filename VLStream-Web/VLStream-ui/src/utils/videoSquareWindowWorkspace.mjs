// Coordinates belong to the video workspace, never the page or viewport.
export function carouselWindowSize(width, height) {
    const windowWidth = Math.max(1, Math.min(300, width))
    return { width: windowWidth, height: Math.max(1, Math.min(height - 18, (windowWidth < 240 ? 90 : 32) + windowWidth * 9 / 16)) }
}

export function clampVideoWindow(position, size, bounds) {
    return {
        x: Math.max(0, Math.min(position.x, Math.max(0, bounds.width - size.width))),
        y: Math.max(0, Math.min(position.y, Math.max(0, bounds.height - size.height - 18))),
    }
}

export function isWindowDragGesture(start, event) {
    return Math.hypot(event.clientX - start.x, event.clientY - start.y) >= 5
}

export function fitVideoPicture(width, height, ratio = 16 / 9) {
    const aspect = Number.isFinite(ratio) && ratio > 0 ? ratio : 16 / 9
    const fittedWidth = Math.max(1, Math.min(width, height * aspect))
    return {width:fittedWidth, height:fittedWidth / aspect}
}
