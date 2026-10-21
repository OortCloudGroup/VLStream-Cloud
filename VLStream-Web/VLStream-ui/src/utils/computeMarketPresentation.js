// Presentation only: keep stored inventory names and ownership unchanged.
export function marketResourceName(resource, field = 'name') {
  const name = resource?.[field] || ''
  return ['autodl', 'aliyun'].includes(resource?.provider)
    ? name.replace(/^OortCloud(?:[\s·\-_:：]+|(?=RTX|GPU))/i, '').trim()
    : name
}

// pointsPerHour is the configured sale quota, not the supplier's raw cost.
// Cost ×2 belongs to the trusted backend rate snapshot; never apply it twice here
// or label quota as CNY. Missing/zero paid rates do not mean a free offer.
export function marketHourlyPrice(resource) {
  if (resource?.billingType !== 'POINTS') return null
  const price = resource.pointsPerHour
  if (!['number', 'string'].includes(typeof price) || String(price).trim() === '') return null
  const amount = Number(price)
  return Number.isFinite(amount) && amount > 0 ? String(price).trim() : null
}

// Dated display reference, observed read-only in the authenticated AutoDL Pro
// instance console. This is not a live quote, provider API unit conversion,
// billing rate, or authorization to rent/start. Disk daily fees are excluded.
export const verifiedMarketPriceReference = Object.freeze({
  nodeId: '792022144331380001',
  provider: 'autodl',
  serverGroup: 'autodl-pro-7920a22db144',
  gpuAmount: 1,
  currency: 'CNY',
  costUnit: 'PER_INSTANCE_HOUR',
  costCentsPerHour: 646,
  saleMultiplier: 2,
  observedDate: '2026-10-19',
  source: 'https://www.autodl.com/console/instance/pro/list'
})

export function marketPricePresentation(resource) {
  const configured = marketHourlyPrice(resource)
  if (configured !== null) return { amount: configured, unit: '额度/小时', reference: false }
  const evidence = verifiedMarketPriceReference
  // Never apply another instance's observation, replace a configured zero/invalid
  // rate, or reprice free stock. Require all existing identity/quota fields.
  if (resource?.billingType !== 'POINTS' || resource.pointsPerHour != null
    || String(resource.id) !== evidence.nodeId || resource.provider !== evidence.provider
    || resource.serverGroup !== evidence.serverGroup || resource.providerGpuAmount !== evidence.gpuAmount) return null
  return {
    amount: '￥' + (evidence.costCentsPerHour * evidence.saleMultiplier / 100).toFixed(2),
    unit: '/小时', reference: true, observedDate: evidence.observedDate,
    sourceNote: 'AutoDL Pro · ' + evidence.observedDate + ' · ￥6.46/小时 × 2'
  }
}
