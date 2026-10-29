export const isFreeSharedCompute = node => node?.scope === 'PLATFORM' && node?.billingType === 'FREE' && node?.schedulingMode === 'SHARED_GPU_QUEUE'

export function trainingComputeGroups(nodes) {
  return [
    { key: 'free', title: 'OortCloud算力Free', nodes: nodes.filter(isFreeSharedCompute) },
    { key: 'market', title: '算力市场', nodes: nodes.filter(node => !isFreeSharedCompute(node) && node.scope === 'PLATFORM') },
    { key: 'private', title: '我的算力', nodes: nodes.filter(node => !isFreeSharedCompute(node) && node.scope !== 'PLATFORM') }
  ].filter(group => group.key !== 'private' || group.nodes.length)
}

export function queuePresentation(snapshot, { now = Date.now(), loading = false } = {}) {
  if (loading) return { phase: '正在读取队列', queue: '排队数读取中', eta: '预计等待：待估算', known: false }
  const count = snapshot?.waitingCount
  const checked = snapshot?.checkedAt
  const valid = snapshot?.state === 'KNOWN' && Number.isInteger(count) && count >= 0 && count <= 50000 &&
    Number.isSafeInteger(checked) && checked <= now + 5000 && now - checked <= 30000 &&
    [null, 'RUNNING', 'ARCHIVING'].includes(snapshot.activePhase)
  if (!valid) return { phase: '任务状态未知', queue: '排队数未知', eta: '预计等待：待估算', known: false }
  const active = snapshot.activePhase !== null
  const name = typeof snapshot.activeTaskName === 'string' ? snapshot.activeTaskName.trim() : ''
  const phase = snapshot.activePhase === 'ARCHIVING' ? '模型归档中' : active ? '正在训练' : '暂无共享执行任务'
  const prefix = active ? `${name || '共享任务'} · ` : ''
  const seconds = snapshot.estimatedWaitSeconds
  const supported = snapshot.estimationBasis === 'MATCHED_OBSERVED_LEASE_P75' && Number.isInteger(snapshot.estimationSampleCount) && snapshot.estimationSampleCount >= 3
  const estimate = supported && Number.isSafeInteger(seconds) && seconds > 0 ?
    (seconds < 3600 ? `约 ${Math.ceil(seconds / 60)} 分钟` : `约 ${Math.ceil(seconds / 360) / 10} 小时`) : '待估算'
  return {
    phase: prefix + phase,
    queue: count ? `${count} 个任务排队中` : '暂无排队任务',
    eta: `预计等待：${!active && !count ? '当前无共享任务' : estimate}`,
    estimateNote: estimate !== '待估算' ? `参考至少 ${snapshot.estimationSampleCount} 个同配置成功任务` : '',
    known: true
  }
}

export function computeAvailabilityLabel(node) {
  if (node?.stopped || node?.powerState === 'STOPPED') return '已关机'
  if (node?.operationPending || ['STARTING', 'STOPPING'].includes(node?.powerState)) return '状态切换中'
  if (['OTHER', 'ISOLATED'].includes(node?.claimState)) return '暂不可选择'
  if (node?.schedulable) return isFreeSharedCompute(node) ? '可提交排队' : '可训练'
  if (node?.healthState === 'CHECKING') return '环境检查中'
  if (node?.healthState === 'FAILED' || node?.healthState === 'ERROR') return '环境异常'
  return '尚未就绪'
}

export function computeFixedPriceLabel(node) {
  if (node?.billingType === 'FREE') return ''
  // Presentation consumes the explicit official price contract, never pointsPerHour,
  // supplier quotes, model names or the fact that a node is configuration-managed.
  const amount = node?.userHourlyPrice
  if (node?.pricePolicy !== 'VLS_FIXED' || node?.priceUnit !== 'PER_INSTANCE_HOUR' || node?.priceCurrency !== 'CNY' ||
    typeof amount !== 'string' || !/^(?:0|[1-9]\d*)(?:\.\d+)?$/.test(amount) || !/[1-9]/.test(amount)) return '收费 · 价格待确认'
  return `固定价 ${amount} 元 / 整实例小时`
}
