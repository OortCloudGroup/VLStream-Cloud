// Coordinates belong to the video workspace, never the page or viewport.
export const BOTTOM_WINDOWS_PER_ROW = 4
export const BOTTOM_WINDOW_ASPECT = 4 / 3
export const BOTTOM_WINDOW_HEIGHT_SCALE = 1.15
export const BOTTOM_WINDOW_MIN_WIDTH = 220

// 底部排列：所有窗口同尺寸，按一行 4 个槽位计算格子宽度并与工作区宽度等分，
// 外框基准 4:3 再放高 15%；窗口少于 4 个时保持格子大小靠左排列，超过 4 个横向轮播。
export function uniformWindowSize(width, height, ratio = BOTTOM_WINDOW_ASPECT) {
    const available = Math.max(1, Number(width) || 1)
    const workspaceHeight = Math.max(1, Number(height) || 1)
    const aspect = Number.isFinite(ratio) && ratio > 0 ? ratio : BOTTOM_WINDOW_ASPECT
    const heightLimited = Math.max(1, workspaceHeight * aspect / BOTTOM_WINDOW_HEIGHT_SCALE)
    const share = available / BOTTOM_WINDOWS_PER_ROW
    const windowWidth = Math.max(1, Math.min(heightLimited, available, Math.max(share, Math.min(BOTTOM_WINDOW_MIN_WIDTH, heightLimited))))
    return { width: windowWidth, height: Math.min(workspaceHeight, windowWidth / aspect * BOTTOM_WINDOW_HEIGHT_SCALE) }
}

export function clampVideoWindow(position, size, bounds) {
    return {
        x: Math.max(0, Math.min(position.x, Math.max(0, bounds.width - size.width))),
        y: Math.max(0, Math.min(position.y, Math.max(0, bounds.height - size.height))),
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

// 手册布局定义在 16:9 画布上按 cols × rows 等分，每个画面占据整数格；
// 画布整体等比缩放居中放入可用区域，画布内未被画面占用的格子就是手册示意图里的空白区域。
export const LAYOUT_CANVAS_ASPECT = 16 / 9

export function packVideoWindows({width, height, cols, rows, cells = []}) {
    const availableWidth = Math.max(1, Number(width) || 1)
    const availableHeight = Math.max(1, Number(height) || 1)
    const columns = Math.max(1, Math.floor(cols) || 1)
    const layoutRows = Math.max(1, Math.floor(rows) || 1)
    const layoutWidth = Math.max(1, Math.min(availableWidth, availableHeight * LAYOUT_CANVAS_ASPECT))
    const layoutHeight = layoutWidth / LAYOUT_CANVAS_ASPECT
    const unitWidth = layoutWidth / columns
    const unitHeight = layoutHeight / layoutRows
    const boxes = cells.map(({col = 0, row = 0, colSpan = 1, rowSpan = 1}) => ({
        x: col * unitWidth,
        y: row * unitHeight,
        width: colSpan * unitWidth,
        height: rowSpan * unitHeight,
    }))
    return {width: layoutWidth, height: layoutHeight, boxes}
}
