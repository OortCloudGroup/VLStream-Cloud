export function videoGridStyle(count, variant = 'grid') {
  if (!Number.isInteger(count) || count < 1 || count > 25) throw new RangeError('Invalid video window count')
  if ((variant === 'two-overlay' && count !== 2) || (['three-top', 'three-left'].includes(variant) && count !== 3)) throw new RangeError('Layout variant does not match window count')
  if (!['grid', 'two-overlay', 'three-top', 'three-left'].includes(variant)) throw new RangeError('Invalid video layout variant')
  if (variant === 'two-overlay') return { gridTemplateColumns: 'minmax(0, 1fr)', gridTemplateRows: 'minmax(0, 1fr)' }
  if (variant.startsWith('three-')) return { gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gridTemplateRows: 'repeat(2, minmax(0, 1fr))' }
  const columns = count === 2 ? 2 : Math.ceil(Math.sqrt(count))
  return { gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))`, gridTemplateRows: `repeat(${Math.ceil(count / columns)}, minmax(0, 1fr))` }
}

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
