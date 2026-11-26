function interruptedSceneStream() {
    return Object.assign(new Error('生成连接已中断，请重新载入查看已保存的视图'), { code: 'SCENE_STREAM_INTERRUPTED' })
}

export async function readSceneUpdates(body, onPreview) {
    if (!body) throw interruptedSceneStream()
    const reader = body.getReader()
    const decoder = new TextDecoder()
    let pending = '', saved
    function consume(line) {
        if (!line.trim()) return
        let event
        try {
            event = JSON.parse(line)
        } catch {
            throw interruptedSceneStream()
        }
        if (!event || typeof event.type !== 'string') throw interruptedSceneStream()
        if (event.type === 'preview') onPreview(event.scene)
        if (event.type === 'saved') saved = event.value
        if (event.type === 'error') throw Object.assign(new Error(event.message), { code: event.code, reload: event.reload === true })
    }
    try {
        while (true) {
            const { value, done } = await reader.read()
            pending += decoder.decode(value, { stream: !done })
            const lines = pending.split('\n')
            pending = lines.pop()
            for (const line of lines) consume(line)
            if (done) break
        }
        consume(pending)
    } finally {
        await reader.cancel().catch(() => {})
        reader.releaseLock()
    }
    if (!saved?.scene) throw interruptedSceneStream()
    return saved
}

// Starting points for an empty conversation. Users can describe any other page or interaction.
export const sceneStarters = {
    topology: [
        '画一张设备链路图：摄像头 → 交换机 → 录像机，连线上有流动的数据光点，点击摄像头打开实时视频。',
        '设计深色交互式指挥屏：左侧设备列表，中间环形拓扑，右侧告警滚动，点击设备高亮它的全部连接。',
        '按楼层分组展示摄像头和网络设备，支持按名称搜索设备并定位高亮。',
    ],
    bim: [
        '生成可旋转的三维园区：三栋高度不同的楼、道路、绿化和摄像头点位，支持昼夜切换，点击点位看实时视频。',
        '绘制一栋五层办公楼的剖面，每层显示房间和摄像头，提供楼层切换按钮。',
        '做一个楼层监控台：左侧楼层列表，中间二层平面图，摄像头点位标在房间里，点击可看实时视频。',
    ],
}

// One-tap follow-ups that edit the current page.
export const sceneFollowUps = {
    topology: ['点击任一摄像头，仅高亮它到录像机的完整路径，同时打开实时预览', '增加按名称搜索设备并定位', '增加昼夜配色切换按钮'],
    bim: ['保留园区，加一个二层剖面', '点击楼栋时镜头拉近并显示楼层列表', '增加自动环绕巡航按钮'],
}

/** The last few lines of the streaming page, shown as a live code ticker. */
export function codeTail(html, lines = 5) {
    if (typeof html !== 'string' || !html) return ''
    return html.split('\n').map(line => line.trim()).filter(Boolean).slice(-lines)
        .map(line => line.length > 72 ? line.slice(0, 71) + '…' : line).join('\n')
}

/** A repair request that carries the runtime errors and the lines they point at. */
export function repairPrompt(errors, translate = text => text) {
    const lines = (errors || []).slice(0, 4).map((error, index) => {
        const code = error.code ? error.code.split('\n')[Math.min(1, error.code.split('\n').length - 1)] : ''
        const where = error.line ? translate('（第 {line} 行）', { line: error.line }) : ''
        return (index + 1) + '. ' + error.message + where + (code ? '：' + code : '')
    })
    return (translate('修复当前页面的运行错误，保留现有设计、镜头操作、昼夜切换、按钮和摄像头绑定：') + '\n' + lines.join('\n')).slice(0, 2000)
}

/** Rebuilds the conversation from the saved history so it survives refreshes and new sign-ins. */
export function restoredMessages(saved) {
    const scene = saved?.scene
    if (!scene) return []
    const log = Array.isArray(scene.log) ? scene.log : []
    if (!log.length) {
        return saved.prompt ? [{ role: 'user', text: saved.prompt, state: 'restored' },
            { role: 'assistant', state: 'saved', text: '已载入上次保存的视图。', title: scene.title || '' }] : []
    }
    return log.flatMap(entry => entry.kind === 'undo'
        ? [{ role: 'system', text: '已回退到上一版', title: entry.title || '' }]
        : [{ role: 'user', text: entry.prompt, state: 'restored' },
            { role: 'assistant', state: 'saved', text: '已生成并保存视图。', title: entry.title || '',
                meta: { edit: entry.edit, edits: entry.edits, model: entry.model } }])
}

// Legacy structured scenes (before generated pages) still render through the old components.
export const sceneSamples = {
    topology: {
        title: '园区监控链路',
        nodes: [
            { id: 'east', label: '东门摄像头', kind: 'camera', column: 0 },
            { id: 'west', label: '西门球机', kind: 'dome', column: 0 },
            { id: 'core', label: '汇聚交换机', kind: 'switch', column: 1 },
            { id: 'record', label: '录像服务器', kind: 'storage', column: 2 },
            { id: 'screen', label: '监控大屏', kind: 'display', column: 2 },
        ],
        edges: [{ from: 'east', to: 'core' }, { from: 'west', to: 'core' }, { from: 'core', to: 'record' }, { from: 'core', to: 'screen' }],
    },
    bim: {
        title: '办公楼剖面',
        buildings: [{ name: '办公楼', floors: [
            { name: '1F', rooms: [{ name: '入口', cameras: [] }, { name: '大厅', cameras: [{ label: '大厅摄像头' }] }] },
            { name: '2F', rooms: [{ name: '会议室', cameras: [{ label: '会议室摄像头' }] }, { name: '办公区', cameras: [] }] },
            { name: '3F', rooms: [{ name: '办公室', cameras: [] }, { name: '休息区', cameras: [] }] },
        ] }],
    },
}

// Without an explicit "all cameras" request, a page binds a handful of cameras (online first): dozens of
// nodes crowd generated layouts. Named and already bound cameras always stay.
const ALL_CAMERAS = /全部|所有|全体|每一?台|\ball\b|\bevery\b/i
export function sceneDeviceIds(devices, prompt, previous) {
    const mentioned = []
    const retained = []
    const others = []
    const previousIds = new Set((previous?.nodes || []).map(node => node.deviceId))
    for (const id of Object.values(previous?.cameras || {})) previousIds.add(id)
    for (const building of previous?.buildings || []) for (const floor of building.floors) {
        for (const room of floor.rooms) for (const camera of room.cameras) previousIds.add(camera.deviceId)
    }
    const query = prompt.toLocaleLowerCase()
    for (const device of devices) {
        const name = device.deviceName?.trim().toLocaleLowerCase()
        const group = name && query.includes(name) ? mentioned : previousIds.has(device.id) ? retained : others
        group.push(device)
    }
    const rank = device => device.status === 'ONLINE' ? 0 : device.status === 'OFFLINE' ? 2 : 1
    others.sort((a, b) => rank(a) - rank(b))
    const limit = ALL_CAMERAS.test(prompt) ? 80 : Math.max(8, mentioned.length + retained.length)
    return [...new Set([...mentioned, ...retained, ...others].map(device => device.id))].slice(0, Math.min(80, limit))
}

export function topologyLayout(scene) {
    const columns = [...new Set(scene.nodes.map(node => node.column))].sort((a, b) => a - b)
        .map(index => scene.nodes.filter(node => node.column === index))
    const width = columns.length * 204 + (columns.length - 1) * 82 + 48
    const height = Math.max(230, Math.max(...columns.map(column => column.length)) * 94 + 32)
    const nodes = columns.flatMap((column, x) => column.map((node, y) => ({
        ...node, x: 24 + x * 286, y: (height - (column.length - 1) * 94 - 65) / 2 + y * 94,
    })))
    const byId = new Map(nodes.map(node => [node.id, node]))
    const edges = scene.edges.map(edge => {
        const from = byId.get(edge.from), to = byId.get(edge.to)
        if (from.x === to.x) {
            const x = from.x + 204
            return `M ${x} ${from.y + 32} C ${x + 27} ${from.y + 32}, ${x + 27} ${to.y + 32}, ${x} ${to.y + 32}`
        }
        const forward = to.x > from.x
        const start = from.x + (forward ? 204 : 0), end = to.x + (forward ? 0 : 204)
        const middle = (start + end) / 2
        return `M ${start} ${from.y + 32} C ${middle} ${from.y + 32}, ${middle} ${to.y + 32}, ${end} ${to.y + 32}`
    })
    return { width, height, nodes, edges }
}

// Orthographic projection of the generated floors. Everything, including pins, uses the same room coordinates.
export function project(x, y, z) {
    return { x: (x - y) * 0.9, y: (x + y) * 0.37 - z }
}
function polygon(corners) {
    return corners.map(point => { const { x, y } = project(...point); return `${x},${y}` }).join(' ')
}
const deck = (x, y, w, d, z) => polygon([[x, y, z], [x + w, y, z], [x + w, y + d, z], [x, y + d, z]])
const wall = (x, y, dx, dy, z, h) => polygon([[x, y, z], [x + dx, y + dy, z], [x + dx, y + dy, z + h], [x, y, z + h]])

// Illustrative furniture is drawn in the same coordinates as its room and camera pins.
function roomFixtures(name, x, y, width, depth, z) {
    const fixtures = []
    function box(left, back, w, d, height, tone) {
        const sx = x + width * left, sy = y + depth * back
        const sw = width * w, sd = depth * d
        fixtures.push({
            top: deck(sx, sy, sw, sd, z + height),
            front: wall(sx, sy + sd, sw, 0, z, height),
            side: wall(sx + sw, sy, 0, sd, z, height),
            tone,
        })
    }
    if (/会议|洽谈|培训|conference|meeting|training/i.test(name)) {
        for (const seat of [0.21, 0.46, 0.71]) box(seat, 0.22, 0.1, 0.12, 3, 'upholstery')
        box(0.16, 0.39, 0.68, 0.25, 6, 'wood')
        for (const seat of [0.21, 0.46, 0.71]) box(seat, 0.69, 0.1, 0.12, 3, 'upholstery')
    } else if (/办公室?|工位|财务|工作区|office|workspace|workstation/i.test(name)) {
        for (const row of [0.18, 0.59]) {
            for (const col of [0.16, 0.55]) {
                box(col, row, 0.26, 0.13, 5, 'wood')
                box(col + 0.06, row + 0.15, 0.12, 0.1, 3, 'upholstery')
            }
        }
    } else if (/大厅|门厅|前台|休息|lobby|reception|lounge/i.test(name)) {
        box(0.12, 0.18, 0.32, 0.13, 7, 'wood')
        box(0.65, 0.25, 0.15, 0.35, 4, 'upholstery')
        box(0.43, 0.36, 0.13, 0.18, 3, 'wood')
        box(0.15, 0.72, 0.08, 0.1, 5, 'plant')
    } else if (/仓|机房|设备|server|storage|plant room/i.test(name)) {
        for (const col of [0.17, 0.43, 0.69]) box(col, 0.18, 0.12, 0.53, 10, 'metal')
    }
    return fixtures
}

export function buildingGeometry(building, floorIndex) {
    const floors = building.floors.map((floor, index) => ({ ...floor, index }))
        .filter(floor => floorIndex === null || floor.index === floorIndex)
    const w = 216, d = 134, rise = 50
    const layers = floors.map((floor, tier) => {
        const z = tier * rise, cols = Math.min(floor.rooms.length, Math.ceil(Math.sqrt(floor.rooms.length * 1.25)))
        const rows = Math.ceil(floor.rooms.length / cols)
        const rw = (w - 14) / cols, rd = (d - 14) / rows
        const rooms = floor.rooms.map((room, index) => {
            const x = 7 + (index % cols) * rw, y = 7 + Math.floor(index / cols) * rd
            const pins = room.cameras.map((camera, i) => ({
                ...camera, ...project(x + rw * (i + 1) / (room.cameras.length + 1), y + rd * 0.63, z + 6),
                key: `${floor.index}-${index}-${i}`, location: [building.name, floor.name, room.name].join(' / '),
            }))
            return {
                name: room.name, pins, plan: deck(x, y, rw - 3, rd - 3, z + 1),
                divider: wall(x, y, rw - 3, 0, z + 1, 9), side: wall(x, y, 0, rd - 3, z + 1, 9),
                fixtures: roomFixtures(room.name, x, y, rw - 3, rd - 3, z + 1),
            }
        })
        return {
            ...floor, rooms, slab: deck(0, 0, w, d, z), front: wall(0, d, w, 0, z - 4, 4),
            side: wall(w, 0, 0, d, z - 4, 4), rearGlass: wall(0, 0, w, 0, z, 23),
            westGlass: wall(0, 0, 0, d, z, 23),
            posts: [0, 0.25, 0.5, 0.75, 1].map(f => wall(w * f, 0, 2, 0, z, 23)),
            nameAt: project(0, d + 6, z),
        }
    })
    return {
        layers, ground: deck(-12, -12, w + 24, d + 24, -9),
        viewBox: `-161 ${-50 - (layers.length - 1) * rise} 388 ${210 + (layers.length - 1) * rise}`,
    }
}
