// Refresh only missing shared-pool checks. Never boot a paid instance or fabricate readiness.
export async function prepareSharedCompute(resources, refresh) {
  const candidates = (Array.isArray(resources) ? resources : []).filter(node =>
    node.billingType === 'FREE' && node.schedulingMode === 'SHARED_GPU_QUEUE'
    && node.canRefreshEnvironment && node.healthState !== 'READY' && !node.operationPending)
  if (!candidates.length) return false
  // A failed host must not prevent another healthy host from being selected.
  await Promise.allSettled(candidates.map(node => refresh(node.id)))
  return true
}
