const safeCell = value => {
  let text = String(value ?? '')
  if (/^[\s]*[=+\-@]/.test(text) || /^[\t\r]/.test(text)) text = `'${text}`
  return `"${text.replaceAll('"', '""')}"`
}

// Deliberately export only the directory's visible safe fields. Source media
// addresses, credentials, signaling URLs and signed preview leases are excluded.
export function videoCatalogCsv(devices, translate = text => text) {
  const headings = ['序号', '设备名称', '设备ID', '协议', '状态', '地址'].map(translate)
  const rows = devices.map((device, index) => [index + 1, device.deviceName,
    device.deviceId || device.ipAddress || device.id, (device.protocols || []).join(' / '),
    translate(({ ONLINE: '在线', OFFLINE: '离线', UNKNOWN: '未知状态' })[device.status] || '未知状态'),
    device.address || device.addressMap || ''])
  return '\ufeff' + [headings, ...rows].map(row => row.map(safeCell).join(',')).join('\r\n')
}
