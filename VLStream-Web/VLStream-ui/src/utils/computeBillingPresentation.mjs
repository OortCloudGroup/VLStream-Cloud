const integer = value => {
  if (typeof value !== 'string' || !/^(0|[1-9][0-9]*)$/.test(value) || BigInt(value) > 9223372036854775807n) throw new Error('余额数据尚未确认')
  return BigInt(value)
}

// quota = currency * numerator / denominator. Never pass wallet values through Number.
export function billingCurrency(quota, numerator, denominator) {
  const amount = integer(quota), n = integer(numerator), d = integer(denominator)
  if (!n || !d) throw new Error('余额换算尚未确认')
  const scale = 1000000n, scaled = amount * d * scale / n
  const fraction = (scaled % scale).toString().padStart(6, '0').replace(/0+$/, '')
  return `${amount * d * scale % n ? '≈ ' : ''}${scaled / scale}${fraction ? `.${fraction}` : ''}`
}

const labels = {
  QUOTED: '待确认开机', FUNDING: '正在预冻结', STARTING: '厂商开机处理中', RUNNING: '实例运行中', STOPPING: '实例关机处理中',
  CANCELLING: '正在取消开机', RELEASING: '正在释放未消费余额', COMPLETED: '已停止并完成结算', CANCELLED: '已取消并完成释放'
}
export const paidRunState = state => labels[state] || '状态待确认'
export const paidRunActive = run => !['QUOTED', 'COMPLETED', 'CANCELLED'].includes(run.state)

export function paidRunErrorPresentation(run) {
  if (!run?.errorCode && !run?.startFailureCode) return null
  const pending = '状态确认暂未完成，系统会查询原单；请勿重复提交启停操作。'
  // The original order's start cause is separate from current funding/recovery errors.
  // Older responses may carry only errorCode; never infer capacity from provider text.
  const startCode = Object.prototype.hasOwnProperty.call(run, 'startFailureCode') ? run.startFailureCode : run.errorCode
  const knownStartCodes = ['NO_STOCK', 'START_REJECTED']
  const conflictingCodes = knownStartCodes.includes(run.errorCode) && run.errorCode !== startCode
  const failureState = ['STARTING', 'CANCELLING', 'STOPPING', 'RELEASING', 'CANCELLED'].includes(run.state)
  const rejected = !conflictingCodes && failureState
    ? startCode === 'NO_STOCK' ? '当前资源被占用，本次开机未成功。'
      : startCode === 'START_REJECTED' ? '本次开机被供应商拒绝。' : null
    : null
  // Keep unresolved funding/release diagnostics first, including a contradictory terminal state.
  if (rejected && run.errorCode && run.errorCode !== startCode) return { message: pending + rejected, type: 'warning' }
  const message = rejected
    ? rejected + (run.state === 'CANCELLED' ? '原单已取消，未消费冻结余额已完成释放。' : '原单取消及未消费冻结余额释放仍待确认，请勿重复开机。')
    : pending
  return { message, type: 'warning' }
}

export function paidRunQuoteText(quote) {
  const amount = integer(quote.reserveQuota).toString()
  if (!Number.isSafeInteger(quote.maximumRunSeconds) || quote.maximumRunSeconds < 1 || quote.maximumRunSeconds > 86400) throw new Error('运行时限尚未确认')
  if (typeof quote.hourlyPrice !== 'string' || !/^\d+(\.\d+)?$/.test(quote.hourlyPrice)) throw new Error('价格尚未确认')
  return `计算价格 ${quote.hourlyPrice} ${quote.currency}/小时；本次最多运行 ${quote.maximumRunSeconds} 秒，预冻结 ${amount} ${quote.quotaUnit}（含停止安全预留）。按实际确认的运行用量结算，未消费部分在停止与对账后释放。训练完成后实例仍会运行并计费，请主动停止；到达本次时限也会请求安全停止。允许 VLS 查询平台余额并按本次已确认的用量扣费；统一平台可能保存此计费授权，可在平台撤销。`
}
