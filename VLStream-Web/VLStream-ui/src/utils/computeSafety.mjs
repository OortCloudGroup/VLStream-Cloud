// The displayed rows and the confirmation must belong to the same two-token identity.
export async function loadSafetyRuns(list, identity) {
  const response = await list()
  identity.assertCurrent()
  if (response?.code !== 200 || !Array.isArray(response.data) || response.data.some(row =>
    !row || typeof row.runId !== 'string' || !row.runId.trim() || typeof row.nodeId !== 'string' || !row.nodeId.trim()
      || typeof row.state !== 'string' || !row.state.trim())) {
    throw new Error('算力状态未确认，请刷新重试')
  }
  return response.data.map(({ runId, nodeId, state }) => ({ runId, nodeId, state }))
}

export async function stopSafetyRun(row, identity, confirm, stop) {
  identity.assertCurrent()
  await confirm(row)
  identity.assertCurrent()
  const response = await stop(row.runId)
  identity.assertCurrent()
  if (response?.code !== 200 || response.data !== true) throw new Error('停止请求状态未确认，请刷新重试')
}
