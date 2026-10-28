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

// Pack the picture rectangles, rather than stretching cells and leaving contain bars between them.
// Rows may have different column widths for mixed sources; the six/eight-window main+side shape stays.
export function packVideoWindows({width, height, count, variant = 'grid', aspects = [], insets = []}) {
    const total = Math.max(1, Math.min(25, Math.floor(count) || 1))
    const fallback = aspects.find(value => Number.isFinite(value) && value > 0) || 16 / 9
    const ratio = index => Number.isFinite(aspects[index]) && aspects[index] > 0 ? aspects[index] : fallback
    const inset = index => Math.max(0, Number(insets[index]) || 0)
    const atWidth = width => {
        const boxes = Array(total)
        const row = (indices, y, slots = indices.length) => {
            const entries = Array.from({length:slots}, (_, offset) => indices[offset] ?? -1)
            const sum = entries.reduce((value, index) => value + ratio(index), 0)
            const rowHeight = (width + entries.reduce((value, index) => value + ratio(index) * inset(index), 0)) / sum
            let x = 0
            for (const index of entries) {
                const cellWidth = Math.max(0, (rowHeight - inset(index)) * ratio(index))
                if (index >= 0) boxes[index] = {x, y, width:cellWidth, height:rowHeight}
                x += cellWidth
            }
            return rowHeight
        }
        let layoutHeight
        if (variant === 'two-overlay' && total === 2) {
            layoutHeight = width / ratio(0) + inset(0)
            boxes[0] = {x:0, y:0, width, height:layoutHeight}
            const picture = fitVideoPicture(width * .3, Math.max(1, layoutHeight * .3 - inset(1)), ratio(1))
            const overlayHeight = picture.height + inset(1)
            boxes[1] = {x:width - picture.width, y:layoutHeight - overlayHeight, width:picture.width, height:overlayHeight}
        } else if (variant === 'three-top' && total === 3) {
            const mainHeight = width / ratio(0) + inset(0)
            boxes[0] = {x:0, y:0, width, height:mainHeight}
            layoutHeight = mainHeight + row([1, 2], mainHeight)
        } else if ((variant === 'three-left' && total === 3) || (variant === 'grid' && [6, 8].includes(total))) {
            const sideCount = total === 8 ? 3 : 2
            const side = Array.from({length:sideCount}, (_, index) => index + 1)
            const inverse = side.reduce((value, index) => value + 1 / ratio(index), 0)
            const chrome = side.reduce((value, index) => value + inset(index), 0)
            const mainHeight = (width + ratio(0) * inset(0) + chrome / inverse) / (ratio(0) + 1 / inverse)
            const mainWidth = (mainHeight - inset(0)) * ratio(0)
            const sideWidth = width - mainWidth
            boxes[0] = {x:0, y:0, width:mainWidth, height:mainHeight}
            let y = 0
            for (const index of side) {
                const cellHeight = sideWidth / ratio(index) + inset(index)
                boxes[index] = {x:mainWidth, y, width:sideWidth, height:cellHeight}
                y += cellHeight
            }
            layoutHeight = mainHeight
            if (total > 3) {
                // Existing L layouts: the last right-column window belongs to the bottom row.
                const bottom = [...Array.from({length:sideCount}, (_, index) => sideCount + 2 + index), sideCount + 1]
                layoutHeight += row(bottom, mainHeight)
            }
        } else {
            const columns = total === 2 ? 2 : Math.ceil(Math.sqrt(total))
            layoutHeight = 0
            for (let start = 0; start < total; start += columns) {
                const indices = Array.from({length:Math.min(columns, total - start)}, (_, offset) => start + offset)
                layoutHeight += row(indices, layoutHeight, columns)
            }
        }
        return {width, height:layoutHeight, boxes}
    }
    const maximum = Math.max(1, Number(width) || 1)
    const available = Math.max(1, Number(height) || 1)
    let result = atWidth(maximum)
    if (result.height > available) {
        let low = 0, high = maximum
        for (let step = 0; step < 40; step++) {
            const middle = (low + high) / 2
            if (atWidth(middle).height > available) high = middle
            else low = middle
        }
        result = atWidth(Math.max(.01, low))
    }
    return result
}
