export const VIDEO_PRESENTATIONS = ['default', 'bottom', 'grid']

export function applyVideoPresentation(dialog, mode) {
    if (!VIDEO_PRESENTATIONS.includes(mode)) throw new RangeError('Invalid video presentation')
    if (mode === 'default' && dialog.layoutCount > 1) {
        const preferred = dialog.activeWindowIndex || 0
        const selectedIndex = dialog.cameras[preferred] ? preferred : Math.max(0, dialog.cameras.findIndex(Boolean))
        const selected = dialog.cameras[selectedIndex] || null
        dialog.cameras = [selected]
        if (dialog.windowIds) dialog.windowIds = [dialog.windowIds[selectedIndex]]
        dialog.layoutCount = 1
        dialog.layoutVariant = 'grid'
        dialog.activeWindowIndex = 0
    }
    dialog.presentationMode = mode
}

export function captureThumbnailFrame(video, createCanvas = () => document.createElement('canvas')) {
    if (!video || video.readyState < 2 || !video.videoWidth || !video.videoHeight) return null
    try {
        const canvas = createCanvas()
        const scale = Math.min(280 / video.videoWidth, 280 / video.videoHeight)
        canvas.width = Math.max(1, Math.round(video.videoWidth * scale))
        canvas.height = Math.max(1, Math.round(video.videoHeight * scale))
        const context = canvas.getContext('2d')
        if (!context) return null
        context.drawImage(video, 0, 0, canvas.width, canvas.height)
        return canvas.toDataURL('image/jpeg', .75)
    } catch {
        // A cross-origin or unavailable frame keeps its real placeholder.
        return null
    }
}
