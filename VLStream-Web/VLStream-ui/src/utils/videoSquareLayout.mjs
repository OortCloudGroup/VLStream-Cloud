export function videoGridStyle(count, variant = 'grid') {
  if (!Number.isInteger(count) || count < 1 || count > 25) throw new RangeError('Invalid video window count')
  if ((variant === 'two-overlay' && count !== 2) || (['three-top', 'three-left'].includes(variant) && count !== 3)) throw new RangeError('Layout variant does not match window count')
  if (!['grid', 'two-overlay', 'three-top', 'three-left'].includes(variant)) throw new RangeError('Invalid video layout variant')
  if (variant === 'two-overlay') return { gridTemplateColumns: 'minmax(0, 1fr)', gridTemplateRows: 'minmax(0, 1fr)' }
  if (variant.startsWith('three-')) return { gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gridTemplateRows: 'repeat(2, minmax(0, 1fr))' }
  const columns = count === 2 ? 2 : Math.ceil(Math.sqrt(count))
  return { gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))`, gridTemplateRows: `repeat(${Math.ceil(count / columns)}, minmax(0, 1fr))` }
}

export function resizeVideoLayout(dialog, count, variant = 'grid') {
  videoGridStyle(count, variant)
  const cameras = dialog.cameras.slice(0, count)
  while (cameras.length < count) cameras.push(null)
  dialog.cameras = cameras
  dialog.layoutCount = count
  dialog.layoutVariant = variant
}

export function catalogVideoCamera(device) {
  return { id: device.id, name: device.deviceName, deviceData: { ...device } }
}

export function assignVideoWindow(dialogs, target, device, authorizedDevices) {
  if (!target || !authorizedDevices.some(item => item.id === device.id)) return false
  const dialog = dialogs.find(item => item.id === target.dialogId)
  if (!dialog || !Number.isInteger(target.windowIndex) || target.windowIndex < 0 || target.windowIndex >= dialog.layoutCount) return false
  dialog.cameras[target.windowIndex] = catalogVideoCamera(device)
  return true
}
