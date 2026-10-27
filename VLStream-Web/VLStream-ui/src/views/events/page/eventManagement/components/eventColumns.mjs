export const defaultEventColumns = [
    { key: 'created_at', label: '事件时间', width: 150 },
    { key: 'status', label: '状态', width: 120 },
    { key: 'position', label: '事件位置', width: 140 },
    { key: 'item', label: '事件类型', width: 130 },
    { key: 'pics', label: '抓拍照片', width: 120 },
    { key: 'video', label: '录制视频', width: 130 },
    { key: 'device_name', label: '设备名称', width: 150 },
    { key: 'device_id', label: '设备编号', width: 190 },
    { key: 'describe', label: '事件描述', width: 160 },
    { key: 'device_tag', label: '标签', width: 130 },
    { key: 'finish_at', label: '完成时间', width: 150 }
]

// Persist only known column keys/visibility, never event data or media URLs.
export function normalizeEventColumns(saved) {
    const known = new Map(defaultEventColumns.map(column => [column.key, column]))
    const result = []
    for (const entry of Array.isArray(saved) ? saved : []) {
        if (!entry || !known.has(entry.key)) continue
        result.push({ ...known.get(entry.key), visible: entry.visible !== false })
        known.delete(entry.key)
    }
    for (const column of known.values()) result.push({ ...column, visible: true })
    if (!result.some(column => column.visible)) result[0].visible = true
    return result
}

export function moveEventColumn(columns, sourceKey, targetKey) {
    const result = [...columns]
    const from = result.findIndex(column => column.key === sourceKey)
    const to = result.findIndex(column => column.key === targetKey)
    if (from < 0 || to < 0 || from === to) return result
    result.splice(to, 0, result.splice(from, 1)[0])
    return result
}
