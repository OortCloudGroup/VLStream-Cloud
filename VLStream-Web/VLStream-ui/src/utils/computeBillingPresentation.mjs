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

export function paidRunQuoteText(quote) {
  const amount = integer(quote.reserveQuota).toString()
  if (!Number.isSafeInteger(quote.maximumRunSeconds) || quote.maximumRunSeconds < 1 || quote.maximumRunSeconds > 86400) throw new Error('运行时限尚未确认')
  if (typeof quote.hourlyPrice !== 'string' || !/^\d+(\.\d+)?$/.test(quote.hourlyPrice)) throw new Error('价格尚未确认')
  return `计算价格 ${quote.hourlyPrice} ${quote.currency}/小时；本次最多运行 ${quote.maximumRunSeconds} 秒，预冻结 ${amount} ${quote.quotaUnit}（含停止安全预留）。按实际确认的运行用量结算，未消费部分在停止与对账后释放。训练完成后实例仍会运行并计费，请主动停止；到达本次时限也会请求安全停止。允许 VLS 查询平台余额并按本次已确认的用量扣费；统一平台可能保存此计费授权，可在平台撤销。`
}
