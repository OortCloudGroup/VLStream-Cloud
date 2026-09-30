// Check the authoritative device state, independently of the media connection.
export function monitorDevicePreview(readDevice, onBlocked, interval = 5000) {
  let stopped = false
  let timer
  async function check() {
    try {
      const device = await readDevice()
      if (stopped) return
      if (device?.online !== true) {
        stopped = true
        onBlocked()
        return
      }
    } catch {
      if (stopped) return
      stopped = true
      onBlocked()
      return
    }
    timer = setTimeout(check, interval)
  }
  timer = setTimeout(check, interval)
  return () => {
    stopped = true
    clearTimeout(timer)
  }
}
