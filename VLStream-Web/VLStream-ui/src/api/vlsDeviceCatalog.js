import { getMqttDevicePage } from '@/api/vlstreamMqttDevice'

// Keep WVP row IDs separate from the device's business ID.
export function normalizeVlsDevice(device) {
  return {
    ...device,
    catalogSource: 'VLSTREAM',
    deviceName: device.deviceName || device.deviceId || '未命名设备',
    deviceType: '摄像头',
    status: [true, 1, '1', 'true'].includes(device.online) ? 1 : 0,
    ipAddress: device.ipAddr || device.ip || device.ipAddress || '',
    tag: 'VLStream',
  }
}

export async function getVlsDeviceCatalog({ limit, keyword = '' } = {}) {
  const devices = []
  let pageNum = 1
  let total = Infinity
  while (devices.length < total && (!limit || devices.length < limit)) {
    const result = await getMqttDevicePage({ pageNum, pageSize: limit || 100, keyword })
    const rows = result?.rows || []
    total = Number(result?.total ?? rows.length)
    if (!rows.length) break
    devices.push(...rows.map(normalizeVlsDevice))
    pageNum += 1
  }
  return limit ? devices.slice(0, limit) : devices
}

export function createVlsDeviceTree(devices) {
  return devices.length ? [{
    id: 'vlstream', label: 'VLStream 设备', type: 'group', expanded: true,
    children: devices.map(device => ({ ...device, label: `${device.deviceName} (${device.deviceId})`, type: 'device' })),
  }] : []
}
