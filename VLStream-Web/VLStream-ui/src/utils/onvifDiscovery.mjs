import { prepareOnvifAuthentication } from './onvifAuthentication.js'

function endpoint(value) {
  try {
    const url = new URL(/^https?:\/\//i.test(value) ? value : `http://${value}`)
    return url.host.toLowerCase()
  } catch { return '' }
}

export function discoveryRows(discovered, existing) {
  const saved = new Map(existing.map(row => [endpoint(row.ip), row]))
  const seen = new Set()
  return discovered.flatMap(item => {
    const key = endpoint(item.hostName || item.ip)
    if (!key || seen.has(key)) return []
    seen.add(key)
    const device = saved.get(key)
    return [{ ...item, key, ip: key, existingId: device?.id, name: device?.name || item.ip,
      username: device?.userName || '', password: device?.password || '', auth: '1',
      state: 'pending', error: '', busy: false, result: null, fingerprint: '', revision: 0 }]
  })
}

function fingerprint(row) {
  return JSON.stringify([row.ip, row.username, row.password, row.auth])
}

export function invalidateAuthentication(row) {
  row.revision += 1
  row.result = null
  row.fingerprint = ''
  row.state = 'pending'
  row.error = ''
}

export async function authenticateRow(row, authenticate, isCurrent = () => true) {
  if (row.busy) return false
  invalidateAuthentication(row)
  const revision = row.revision
  if (!row.username.trim() || !row.password.trim()) {
    row.state = 'failed'
    row.error = '请输入用户名和密码'
    return false
  }
  const account = { ip: row.ip, username: row.username, password: row.password, auth: row.auth }
  const signature = fingerprint(row)
  row.busy = true
  try {
    const response = await authenticate(account)
    if (!isCurrent() || row.revision !== revision || signature !== fingerprint(row)) return false
    if (response.code !== 200) throw new Error(response.msg || '认证失败')
    const verified = prepareOnvifAuthentication(response.data, account)
    const channel = verified.options[0].value
    row.result = { ...verified.result, name: row.name, auth: row.auth, channel,
      url: verified.result.streamUris[channel] }
    row.fingerprint = signature
    row.state = 'authenticated'
    return true
  } catch (error) {
    if (isCurrent() && row.revision === revision && signature === fingerprint(row)) {
      row.state = 'failed'
      row.error = error.message || '认证失败，请检查设备账号、权限和网络'
    }
    return false
  } finally { row.busy = false }
}

export function canRegister(row) {
  return !row.existingId && !row.busy && ['authenticated', 'saveFailed'].includes(row.state) &&
    !!row.result && row.fingerprint === fingerprint(row)
}

export async function registerRows(rows, save, isCurrent = () => true) {
  const summary = { added: 0, failed: 0, skipped: 0 }
  for (const row of rows) {
    if (!isCurrent()) break
    if (!canRegister(row)) { summary.skipped += 1; continue }
    if (!row.name?.trim()) { row.error = '请输入设备名称'; summary.failed += 1; continue }
    row.busy = true
    row.error = ''
    const result = row.result
    try {
      const response = await save({ name: row.name.trim(), ip: row.ip, userName: row.username,
        password: row.password, auth: row.auth, firm: result.firm, model: result.model,
        firmwareVersion: result.firmwareVersion, channel: result.channel,
        url: result.streamUris[result.channel], streamUris: result.streamUris, playType: result.playType,
        streamId: result.streamId, easyNTSUrl: result.easyNTSUrl })
      if (response.code !== 200) throw new Error(response.msg || '添加失败')
      row.state = 'added'
      summary.added += 1
    } catch (error) {
      row.state = 'saveFailed'
      row.error = error.message || '添加结果未确认，请重新发现核对后重试'
      summary.failed += 1
    } finally { row.busy = false }
  }
  return summary
}
