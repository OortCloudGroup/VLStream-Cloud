export const shareAccessLabels = { OWNER: '所属租户', PUBLIC: '公开可查看', GRANTED: '已授权查看', PENDING: '待审批', NONE: '未授权' }
export const shareRequestLabels = { PENDING: '待审批', APPROVED: '已批准', REJECTED: '已拒绝', REVOKED: '已撤销', EXPIRED: '已到期' }
export const canReadSharedDevice = device => ['OWNER', 'PUBLIC', 'GRANTED'].includes(device?.viewerAccess)
export const canRequestSharedDevice = device => device?.visibility === 'PRIVATE' && device.viewerAccess === 'NONE'
export function sharedDeviceRef(device) {
  const value = device?.deviceRef || device?.sources?.find(source => source.protocol === 'VLSTREAM')?.rowId || String(device?.id || '').replace(/^VLSTREAM:/, '')
  return /^[a-f0-9]{32}$/i.test(String(value)) ? String(value) : null
}
export function safeSharedDevice(device) {
  const ref = sharedDeviceRef(device)
  if (!ref || !['PUBLIC', 'PRIVATE'].includes(device?.visibility) || !Object.hasOwn(shareAccessLabels, device?.viewerAccess)) throw new Error('共享设备响应无效')
  return { id: `VLSTREAM:${ref}`, deviceRef: ref, deviceName: device.deviceName || '', visibility: device.visibility,
    viewerAccess: device.viewerAccess, myAccessRequestStatus: device.myAccessRequestStatus || null,
    status: canReadSharedDevice(device) ? device.status : null }
}

export const shareGrantLabels = { ACTIVE: '有效', REVOKED: '已撤销', EXPIRED: '已到期' }
export function safeOwnerGrantsPage(data) {
  if (!Array.isArray(data?.rows) || !Number.isSafeInteger(data.total) || data.total < 0) throw new Error('授权记录响应无效')
  const rows = data.rows.map(row => {
    if (typeof row?.grantId !== 'string' || !/^\d+$/.test(row.grantId) || !Object.hasOwn(shareGrantLabels, row.status)) throw new Error('授权记录响应无效')
    const safe = { grantId: row.grantId, status: row.status, canRevoke: row.status === 'ACTIVE' && row.canRevoke === true }
    for (const field of ['requestId', 'requesterTenantId', 'requesterUserId', 'reason', 'requestCreatedAt', 'approvedAt', 'approvedByUserId', 'expiresAt', 'expiredAt', 'revokedAt', 'revocationAuditId', 'revokedByUserId', 'expirationAuditId']) {
      if (row[field] == null || ['string', 'number'].includes(typeof row[field])) safe[field] = row[field]
    }
    return safe
  })
  return { rows, total: data.total, asOf: data.asOf }
}

// A superseded request cannot restore stale rows or a success state after close.
export function createShareResource(onChange) {
  let revision = 0
  const publish = state => onChange({ rows: [], error: '', loading: false, ...state })
  return {
    async load(request) {
      const current = ++revision
      publish({ loading: true })
      try {
        const result = await request()
        if (current !== revision) return false
        publish(result)
        return true
      } catch (error) {
        if (current === revision) publish({ error: error?.response?.status === 404 ? '共享服务尚未部署，请稍后重试' : error?.response?.data?.msg || error?.message || '读取共享数据失败' })
        return false
      }
    },
    clear() { revision++; publish({}) },
  }
}

const pathId = value => {
  const text = String(value ?? '')
  if (!/^[a-z0-9_-]{1,128}$/i.test(text)) throw new Error('设备或记录引用无效')
  return encodeURIComponent(text)
}
const refPath = value => {
  if (!/^[a-f0-9]{32}$/i.test(String(value))) throw new Error('共享设备引用无效')
  return `/vlstream/device/shared/${value}`
}
export function createDeviceSharingApi(send) {
  const request = (url, method = 'get', data, params) => send({ url, method, ...(data === undefined ? {} : { data }), ...(params ? { params } : {}), timeout: 15000, silentError: true })
  return {
    directory: ({ pageNum = 1, pageSize = 20, keyword = '' } = {}) => request('/video/device/shared-list', 'get', undefined, { pageNum, pageSize, keyword }),
    access: ref => request(`${refPath(ref)}/access`),
    streams: ref => request(`${refPath(ref)}/streams`),
    apply: (ref, reason) => request(`${refPath(ref)}/access-requests`, 'post', { reason }),
    mine: () => request('/vlstream/device/access-requests/mine'),
    owner: rowId => request(`/vlstream/device/${pathId(rowId)}/detail`),
    requests: rowId => request(`/vlstream/device/${pathId(rowId)}/access-requests`),
    grants: (rowId, { pageNum = 1, pageSize = 20, status = 'ALL' } = {}) => request(`/vlstream/device/${pathId(rowId)}/access-grants`, 'get', undefined, { pageNum, pageSize, status }),
    visibility: (rowId, visibility) => request(`/vlstream/device/${pathId(rowId)}/visibility`, 'put', { visibility }),
    approve: (id, expiresInDays) => request(`/vlstream/device/access-requests/${pathId(id)}/approve`, 'post', expiresInDays == null ? {} : { expiresInDays }),
    reject: (id, reason) => request(`/vlstream/device/access-requests/${pathId(id)}/reject`, 'post', { reason }),
    revoke: id => request(`/vlstream/device/access-grants/${pathId(id)}/revoke`, 'post'),
    start: (ref, streamId) => request(`${refPath(ref)}/preview-sessions`, 'post', { streamId }),
    renew: (ref, id) => request(`${refPath(ref)}/preview-sessions/${pathId(id)}`, 'put'),
    close: (ref, id) => request(`${refPath(ref)}/preview-sessions/${pathId(id)}`, 'delete'),
  }
}
