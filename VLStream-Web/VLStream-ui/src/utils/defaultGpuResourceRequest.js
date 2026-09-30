// Keep default-server polling confined to its visible tab, including late responses.
export function createDefaultGpuResourceRequest({ active, fetchSnapshot, onSnapshot, onError }) {
  let current = null
  const cancel = () => {
    const pending = current
    current = null
    pending?.abort()
  }
  const refresh = async () => {
    if (!active() || current) return
    const pending = new AbortController()
    current = pending
    try {
      const response = await fetchSnapshot({ signal: pending.signal, silentError: true })
      if (current === pending && !pending.signal.aborted && active()) onSnapshot(response)
    } catch (error) {
      if (current === pending && !pending.signal.aborted && active()) onError(error)
    } finally {
      if (current === pending) current = null
    }
  }
  return { refresh, cancel }
}
