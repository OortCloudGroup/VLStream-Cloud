// Keep the WVP AjaxResult contract while removing duplicate gateway envelopes.
export function normalizeWvpResponse(value) {
  let result = value
  for (let depth = 0; depth < 10; depth += 1) {
    if (!result || typeof result !== 'object') return result
    const hasCode = Object.prototype.hasOwnProperty.call(result, 'code')
    if (result.success === false || (hasCode && Number(result.code) !== 200)) {
      const error = new Error(result.msg || result.message || `WVP 接口返回异常（${result.code}）`)
      error.data = result
      throw error
    }
    const inner = result.data
    if (inner && typeof inner === 'object' && Object.prototype.hasOwnProperty.call(inner, 'code')) {
      result = inner
      continue
    }
    return Object.prototype.hasOwnProperty.call(result, 'total')
      ? { ...result, total: Number(result.total) } : result
  }
  throw new Error('WVP 响应嵌套层数异常')
}

export function readWvpMediaAvailability(response) {
  const available = normalizeWvpResponse(response)?.data?.available
  if (typeof available !== 'boolean') throw new Error('WVP 媒体状态响应无效')
  return available
}
