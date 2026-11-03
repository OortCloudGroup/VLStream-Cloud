const PREFIX = 'vls.event-work-order-intent.v1:'

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical)
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])]))
  }
  return value
}

export function eventWorkOrderActor(cachedUser, storeUser, tenant) {
  const user = cachedUser?.user || cachedUser
  const userId = user?.userId ?? user?.user_id ?? cachedUser?.userId ?? storeUser?.userId ?? storeUser?.user_id
  const tenantId = tenant || user?.tenantId || cachedUser?.tenantId || storeUser?.tenantId
  if (!userId || !tenantId || (typeof userId === 'number' && !Number.isSafeInteger(userId))) {
    throw new Error('当前用户或租户无法确认')
  }
  return { userId: String(userId), tenantId: String(tenantId) }
}

// Persist only a random operation identifier and hash, never event payloads or credentials.
export async function eventWorkOrderIntent(storage, crypto, actor, eventId, payload, create) {
  if (!actor?.userId || !actor?.tenantId || !eventId || !crypto?.subtle || !crypto?.getRandomValues) {
    throw new Error('无法安全保存建单意图')
  }
  const key = PREFIX + encodeURIComponent(JSON.stringify([actor.tenantId, actor.userId, String(eventId)]))
  const sanitized = JSON.parse(JSON.stringify(payload))
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(JSON.stringify(canonical(sanitized))))
  const fingerprint = Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, '0')).join('')
  let record
  const saved = storage.getItem(key)
  if (saved) {
    try { record = JSON.parse(saved) } catch { throw new Error('建单状态无法确认，请联系管理员核查') }
    if (!/^[0-9a-f-]{36}$/.test(record?.requestId || '') || !/^[0-9a-f]{64}$/.test(record?.fingerprint || '')) {
      throw new Error('建单状态无法确认，请联系管理员核查')
    }
    if (record.fingerprint !== fingerprint) throw new Error('上次建单结果未确认，请先核实后再修改内容')
  } else if (create) {
    const bytes = crypto.getRandomValues(new Uint8Array(16))
    bytes[6] = (bytes[6] & 0x0f) | 0x40
    bytes[8] = (bytes[8] & 0x3f) | 0x80
    const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
    record = { requestId: `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`, fingerprint }
    storage.setItem(key, JSON.stringify(record))
    if (storage.getItem(key) !== JSON.stringify(record)) throw new Error('无法安全保存建单意图')
  }
  return record ? { ...record, key } : null
}

export function clearEventWorkOrderIntent(storage, intent) {
  if (!intent) return
  const saved = storage.getItem(intent.key)
  if (saved && JSON.parse(saved)?.requestId === intent.requestId) storage.removeItem(intent.key)
}

export function rememberConfirmedWorkOrder(storage, intent, procInsId) {
  const saved = storage.getItem(intent.key)
  const record = saved && JSON.parse(saved)
  if (!record || record.requestId !== intent.requestId || !procInsId) throw new Error('建单状态无法确认')
  record.confirmedProcessId = String(procInsId)
  storage.setItem(intent.key, JSON.stringify(record))
}

export function markPreviousOrderStop(storage, intent, procInsId, state) {
  const saved = storage.getItem(intent.key)
  const record = saved && JSON.parse(saved)
  if (!record || record.requestId !== intent.requestId || !procInsId || !['PENDING', 'CONFIRMED'].includes(state)
    || (record.previousOrderId && record.previousOrderId !== String(procInsId))) {
    throw new Error('原工单状态未确认，请刷新后核实')
  }
  record.previousOrderId = String(procInsId)
  record.previousStopState = state
  storage.setItem(intent.key, JSON.stringify(record))
  return { ...record, key: intent.key }
}

export function findPendingWorkOrderIntent(storage, actor, eventId) {
  if (!actor?.userId || !actor?.tenantId || !eventId) throw new Error('建单身份无法确认')
  const key = PREFIX + encodeURIComponent(JSON.stringify([actor.tenantId, actor.userId, String(eventId)]))
  const saved = storage.getItem(key)
  if (!saved) return null
  const record = JSON.parse(saved)
  if (!/^[0-9a-f-]{36}$/.test(record?.requestId || '') || !/^[0-9a-f]{64}$/.test(record?.fingerprint || '')) {
    throw new Error('建单状态无法确认')
  }
  return { ...record, key }
}
