export function videoGridStyle(count, variant = 'grid') {
  if (!Number.isInteger(count) || count < 1 || count > 25) throw new RangeError('Invalid video window count')
  if ((variant === 'two-overlay' && count !== 2) || (['three-top', 'three-left'].includes(variant) && count !== 3)) throw new RangeError('Layout variant does not match window count')
  if (!['grid', 'two-overlay', 'three-top', 'three-left'].includes(variant)) throw new RangeError('Invalid video layout variant')
  if (variant === 'two-overlay') return { gridTemplateColumns: 'minmax(0, 1fr)', gridTemplateRows: 'minmax(0, 1fr)' }
  if (variant.startsWith('three-')) return { gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gridTemplateRows: 'repeat(2, minmax(0, 1fr))' }
  const columns = count === 2 ? 2 : Math.ceil(Math.sqrt(count))
  return { gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))`, gridTemplateRows: `repeat(${Math.ceil(count / columns)}, minmax(0, 1fr))` }
}

// 画面排布以接入手册第 80 页的布局表为准：16:9 画布按 cols × rows 等分，
// 每个画面占据整数个格子，cells 顺序即画面序号（0 起）。
const cell = (col, row, colSpan = 1, rowSpan = 1) => ({col, row, colSpan, rowSpan})
const uniformCells = (cols, rows) => Array.from({length: cols * rows}, (_, index) => cell(index % cols, Math.floor(index / cols)))

export const VIDEO_LAYOUT_SPECS = {
  '1': {cols: 1, rows: 1, cells: [cell(0, 0)]},
  '2A': {cols: 2, rows: 4, cells: [cell(0, 1, 1, 2), cell(1, 1, 1, 2)]},
  '2B': {cols: 3, rows: 3, cells: [cell(0, 0, 3, 3), cell(2, 2)]},
  '3A': {cols: 4, rows: 4, cells: [cell(1, 0, 2, 2), cell(0, 2, 2, 2), cell(2, 2, 2, 2)]},
  '3B': {cols: 4, rows: 4, cells: [cell(0, 1, 2, 2), cell(2, 0, 2, 2), cell(2, 2, 2, 2)]},
  '4': {cols: 2, rows: 2, cells: uniformCells(2, 2)},
  '5': {cols: 8, rows: 4, cells: [cell(1, 0, 6, 3), cell(0, 3, 2), cell(2, 3, 2), cell(4, 3, 2), cell(6, 3, 2)]},
  '6': {cols: 3, rows: 3, cells: [cell(0, 0, 2, 2), cell(2, 0), cell(2, 1), cell(2, 2), cell(1, 2), cell(0, 2)]},
  '7': {cols: 4, rows: 4, cells: [cell(0, 0, 2, 2), cell(2, 0, 2, 2), cell(0, 2, 2, 2), cell(2, 2), cell(3, 2), cell(2, 3), cell(3, 3)]},
  '8': {cols: 4, rows: 4, cells: [cell(0, 0, 3, 3), cell(3, 0), cell(3, 1), cell(3, 2), cell(3, 3), cell(2, 3), cell(1, 3), cell(0, 3)]},
  '9': {cols: 3, rows: 3, cells: uniformCells(3, 3)},
  '10': {cols: 4, rows: 4, cells: [cell(0, 1, 2, 2), cell(2, 1, 2, 2), cell(0, 0), cell(1, 0), cell(2, 0), cell(3, 0), cell(0, 3), cell(1, 3), cell(2, 3), cell(3, 3)]},
  '11': {cols: 5, rows: 5, cells: [cell(1, 0, 3, 3), cell(0, 3), cell(1, 3), cell(2, 3), cell(3, 3), cell(4, 3), cell(0, 4), cell(1, 4), cell(2, 4), cell(3, 4), cell(4, 4)]},
  '12': {cols: 6, rows: 6, cells: [cell(0, 0, 3, 3), cell(3, 0, 3, 3), cell(0, 3, 3, 3), cell(3, 3), cell(4, 3), cell(5, 3), cell(3, 4), cell(4, 4), cell(5, 4), cell(3, 5), cell(4, 5), cell(5, 5)]},
  '13': {cols: 4, rows: 4, cells: [cell(1, 1, 2, 2), cell(0, 0), cell(1, 0), cell(2, 0), cell(3, 0), cell(0, 1), cell(3, 1), cell(0, 2), cell(3, 2), cell(0, 3), cell(1, 3), cell(2, 3), cell(3, 3)]},
  '14': {cols: 5, rows: 4, cells: [cell(0, 0, 2, 2), cell(2, 0, 2, 2), cell(4, 0), cell(4, 1), cell(0, 2), cell(1, 2), cell(2, 2), cell(3, 2), cell(4, 2), cell(0, 3), cell(1, 3), cell(2, 3), cell(3, 3), cell(4, 3)]},
  '15': {cols: 6, rows: 4, cells: [cell(0, 0, 2, 2), cell(2, 0, 2, 2), cell(4, 0, 2, 2), cell(0, 2), cell(1, 2), cell(2, 2), cell(3, 2), cell(4, 2), cell(5, 2), cell(0, 3), cell(1, 3), cell(2, 3), cell(3, 3), cell(4, 3), cell(5, 3)]},
  '16': {cols: 4, rows: 4, cells: uniformCells(4, 4)},
  '17': {cols: 5, rows: 5, cells: [cell(0, 0, 3, 3), cell(3, 0), cell(4, 0), cell(3, 1), cell(4, 1), cell(3, 2), cell(4, 2), cell(0, 3), cell(1, 3), cell(2, 3), cell(3, 3), cell(4, 3), cell(0, 4), cell(1, 4), cell(2, 4), cell(3, 4), cell(4, 4)]},
  '18': {cols: 6, rows: 6, cells: [cell(0, 1, 2, 2), cell(2, 1, 2, 2), cell(4, 1, 2, 2), cell(0, 3, 2, 2), cell(2, 3, 2, 2), cell(4, 3, 2, 2), cell(0, 0), cell(1, 0), cell(2, 0), cell(3, 0), cell(4, 0), cell(5, 0), cell(0, 5), cell(1, 5), cell(2, 5), cell(3, 5), cell(4, 5), cell(5, 5)]},
  '19': {cols: 5, rows: 5, cells: [cell(0, 0, 2, 2), cell(2, 0), cell(3, 0), cell(4, 0), cell(2, 1), cell(3, 1), cell(4, 1), cell(0, 2), cell(1, 2), cell(2, 2), cell(3, 2), cell(4, 2), cell(0, 3), cell(1, 3), cell(2, 3), cell(3, 3), cell(4, 3), cell(0, 4), cell(1, 4), cell(2, 4), cell(3, 4), cell(4, 4)]},
  '20': {cols: 6, rows: 6, cells: [cell(0, 0, 3, 3), cell(3, 0, 3, 3), cell(0, 3), cell(1, 3), cell(2, 3), cell(3, 3), cell(4, 3), cell(5, 3), cell(0, 4), cell(1, 4), cell(2, 4), cell(3, 4), cell(4, 4), cell(5, 4), cell(0, 5), cell(1, 5), cell(2, 5), cell(3, 5), cell(4, 5), cell(5, 5)]},
  '21': {cols: 6, rows: 6, cells: [cell(1, 1, 4, 4), cell(0, 0), cell(1, 0), cell(2, 0), cell(3, 0), cell(4, 0), cell(5, 0), cell(0, 1), cell(5, 1), cell(0, 2), cell(5, 2), cell(0, 3), cell(5, 3), cell(0, 4), cell(5, 4), cell(0, 5), cell(1, 5), cell(2, 5), cell(3, 5), cell(4, 5), cell(5, 5)]},
  '22': {cols: 5, rows: 5, cells: [cell(0, 0, 2, 2), cell(2, 0), cell(3, 0), cell(4, 0), cell(2, 1), cell(3, 1), cell(4, 1), cell(0, 2), cell(1, 2), cell(2, 2), cell(3, 2), cell(4, 2), cell(0, 3), cell(1, 3), cell(2, 3), cell(3, 3), cell(4, 3), cell(0, 4), cell(1, 4), cell(2, 4), cell(3, 4), cell(4, 4)]},
  // 手册未定义本项；格位按 MasterGo 设计稿图标反推：左列两个大块（3×3、3×2），右侧 3×5，底行 6。
  '23': {cols: 6, rows: 6, cells: [cell(0, 0, 3, 3), cell(3, 0), cell(4, 0), cell(5, 0), cell(3, 1), cell(4, 1), cell(5, 1), cell(3, 2), cell(4, 2), cell(5, 2), cell(0, 3, 3, 2), cell(3, 3), cell(4, 3), cell(5, 3), cell(3, 4), cell(4, 4), cell(5, 4), cell(0, 5), cell(1, 5), cell(2, 5), cell(3, 5), cell(4, 5), cell(5, 5)]},
  '24': {cols: 6, rows: 6, cells: [cell(1, 1, 2, 2), cell(3, 1, 2, 2), cell(1, 3, 2, 2), cell(3, 3, 2, 2), cell(0, 0), cell(1, 0), cell(2, 0), cell(3, 0), cell(4, 0), cell(5, 0), cell(0, 1), cell(5, 1), cell(0, 2), cell(5, 2), cell(0, 3), cell(5, 3), cell(0, 4), cell(5, 4), cell(0, 5), cell(1, 5), cell(2, 5), cell(3, 5), cell(4, 5), cell(5, 5)]},
  '25': {cols: 5, rows: 5, cells: uniformCells(5, 5)},
}

const DEFAULT_LAYOUT_KEY = count => count === 2 ? '2A' : count === 3 ? '3A' : String(count)
const VARIANT_LAYOUT_KEY = { 'two-overlay': '2B', 'three-left': '3B', 'three-top': '3A' }

// 手册未定义且设计稿也没有的数量才回退到按行铺满的通用宫格，避免出现无法排布的画面。
const fallbackLayoutSpec = count => {
  const cols = Math.max(1, Math.ceil(Math.sqrt(count)))
  return {cols, rows: Math.ceil(count / cols), cells: Array.from({length: count}, (_, index) => cell(index % cols, Math.floor(index / cols)))}
}

export function resolveLayoutSpec(count, variant = 'grid') {
  const cells = VIDEO_LAYOUT_SPECS[VARIANT_LAYOUT_KEY[variant] || DEFAULT_LAYOUT_KEY(count)]
  return cells && cells.cells.length === count ? cells : fallbackLayoutSpec(count)
}

const numerals = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十', '十一', '十二', '十三', '十四', '十五', '十六', '十七', '十八', '十九', '二十', '二十一', '二十二', '二十三', '二十四', '二十五']
const layoutOption = (key, count, variant, label) => ({key, count, variant, label, ...VIDEO_LAYOUT_SPECS[key]})

// 顺序以设计稿/手册布局表为准（含二十三画面，排在二十四画面之前）。
export const VIDEO_LAYOUT_OPTIONS = [
  layoutOption('1', 1, 'grid', '一画面'),
  layoutOption('2A', 2, 'grid', '二画面（等大）'),
  layoutOption('2B', 2, 'two-overlay', '二画面（一大一小右下）'),
  layoutOption('3A', 3, 'three-top', '三画面（一上二下）'),
  layoutOption('3B', 3, 'three-left', '三画面（一左二右）'),
  ...numerals.slice(3).map((numeral, index) => {
    const count = index + 4
    return layoutOption(String(count), count, 'grid', `${numeral}画面`)
  }),
]

let nextWindowId = 1
export function ensureVideoWindowIds(dialog) {
  const ids = (dialog.windowIds || []).slice(0, dialog.layoutCount)
  while (ids.length < dialog.layoutCount) ids.push(`${dialog.id}:window:${nextWindowId++}`)
  dialog.windowIds = ids
  return ids
}

export function resizeVideoLayout(dialog, count, variant = 'grid', {resetPositions = true} = {}) {
  videoGridStyle(count, variant)
  const cameras = dialog.cameras.slice(0, count)
  while (cameras.length < count) cameras.push(null)
  dialog.cameras = cameras
  dialog.layoutCount = count
  dialog.layoutVariant = variant
  ensureVideoWindowIds(dialog)
  if (resetPositions) dialog.layoutRevision = (dialog.layoutRevision || 0) + 1
}

// A layout is display state. Device binding happens only through an explicit user selection.
export function createEmptyVideoLayout({id, count = 1, variant = 'grid', zIndex = 3000, closed = false}) {
  videoGridStyle(count, variant)
  const dialog = {id, cameras:Array.from({length:closed ? 0 : count}, () => null), layoutCount:closed ? 0 : count, layoutVariant:variant,
    activeWindowIndex:0, position:{x:0,y:0}, minimized:false, zIndex,
    recording:{isRecording:false,recordId:null,startTime:null,timer:null,currentRecordTime:'00:00'}}
  ensureVideoWindowIds(dialog)
  return dialog
}

// Zero is a deliberate closed layout, not a request to create another window.
export function closeVideoWindow(dialog, index, windowId) {
  if (!dialog || !Number.isInteger(index) || index < 0 || index >= dialog.layoutCount) return false
  ensureVideoWindowIds(dialog)
  if (windowId && windowId !== dialog.windowIds[index]) return false
  const active = dialog.activeWindowIndex || 0
  dialog.cameras.splice(index, 1)
  dialog.windowIds.splice(index, 1)
  dialog.layoutCount--
  dialog.layoutVariant = 'grid'
  let nextActive = index < active ? active - 1 : active
  if (index === active && dialog.layoutCount) {
    const nextPlaying = dialog.cameras.findIndex((camera, candidate) => candidate >= index && camera)
    const previousPlaying = dialog.cameras.reduce((last, camera, candidate) => camera ? candidate : last, -1)
    if (nextPlaying >= 0) nextActive = nextPlaying
    else if (previousPlaying >= 0) nextActive = previousPlaying
  }
  dialog.activeWindowIndex = Math.max(0, Math.min(nextActive, dialog.layoutCount - 1))
  return true
}

export function catalogVideoCamera(device) {
  return { id: device.id, name: device.deviceName, deviceData: { ...device } }
}

// 底部排列默认窗口：保留已有画面，空窗口补齐到目标数量，空位优先填在线设备。
export function fillLayoutWindows(dialog, devices, targetCount) {
  if (!dialog || !Number.isInteger(targetCount) || targetCount < 1) return 0
  const hasCamera = dialog.cameras.some(Boolean)
  const target = hasCamera ? Math.max(targetCount, dialog.layoutCount) : targetCount
  if (dialog.layoutCount !== target || dialog.cameras.length !== target) {
    const cameras = dialog.cameras.slice(0, target)
    while (cameras.length < target) cameras.push(null)
    dialog.cameras = cameras
    dialog.layoutCount = target
    dialog.layoutVariant = 'grid'
    dialog.activeWindowIndex = Math.max(0, Math.min(dialog.activeWindowIndex || 0, target - 1))
    ensureVideoWindowIds(dialog)
    dialog.layoutRevision = (dialog.layoutRevision || 0) + 1
  }
  const used = new Set(dialog.cameras.filter(Boolean).map(camera => camera.id))
  const candidates = (devices || [])
    .filter(device => device && !used.has(device.id))
    .sort((left, right) => Number(right.status === 'ONLINE') - Number(left.status === 'ONLINE'))
  let filled = 0
  for (let index = 0; index < dialog.cameras.length && candidates.length; index++) {
    if (dialog.cameras[index]) continue
    dialog.cameras[index] = catalogVideoCamera(candidates.shift())
    filled++
  }
  return filled
}

export function assignVideoWindow(dialogs, target, device, authorizedDevices) {
  if (!target || !authorizedDevices.some(item => item.id === device.id)) return false
  const dialog = dialogs.find(item => item.id === target.dialogId)
  if (!dialog || !Number.isInteger(target.windowIndex) || target.windowIndex < 0 || target.windowIndex >= dialog.layoutCount) return false
  if (target.windowId && dialog.windowIds?.[target.windowIndex] !== target.windowId) return false
  dialog.cameras[target.windowIndex] = catalogVideoCamera(device)
  return true
}
