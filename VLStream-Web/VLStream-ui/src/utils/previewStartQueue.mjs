// Limits how many camera previews are opened at once. Thumbnail players all mount together, and an IP camera
// only accepts a few RTSP sessions, so starts are capped and spaced instead of hitting the camera in one burst.
export function createPreviewStartQueue({
  limit = 3,
  spacingMs = 300,
  now = () => Date.now(),
  sleep = ms => new Promise(resolve => setTimeout(resolve, ms)),
} = {}) {
  const waiting = []
  let active = 0
  let nextStartAt = 0

  function pump() {
    while (active < limit && waiting.length) run(waiting.shift())
  }

  async function run(job) {
    active++
    try {
      if (job.cancelled()) { job.resolve(undefined); return }
      const startAt = Math.max(now(), nextStartAt)
      nextStartAt = startAt + spacingMs
      if (startAt > now()) await sleep(startAt - now())
      // The player may have been replaced or unmounted while this start was queued.
      if (job.cancelled()) { job.resolve(undefined); return }
      job.resolve(await job.task())
    } catch (error) {
      job.reject(error)
    } finally {
      active--
      pump()
    }
  }

  // Resolves with the task result, or undefined when `cancelled()` became true before the task started.
  function schedule(task, cancelled = () => false) {
    return new Promise((resolve, reject) => {
      waiting.push({ task, cancelled, resolve, reject })
      pump()
    })
  }

  return { schedule, get pending() { return waiting.length }, get active() { return active } }
}
