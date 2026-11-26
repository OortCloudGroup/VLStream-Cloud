// Limits how many camera previews are opened at once. Thumbnail players all mount together, and an IP camera
// only accepts a few RTSP sessions, so starts are capped and spaced instead of hitting the camera in one burst.
export function createPreviewStartQueue({
  limit = 3,
  spacingMs = 300,
  now = () => performance.now(),
  sleep = ms => new Promise(resolve => setTimeout(resolve, ms)),
} = {}) {
  const waiting = []
  let active = 0
  let nextStartAt = 0
  let waitingForStart = false

  function pump() {
    while (!waitingForStart && active < limit && waiting.length) {
      const delay = nextStartAt - now()
      if (delay > 0) {
        waitForStart(delay)
        return
      }
      run(waiting.shift())
    }
  }

  async function waitForStart(delay) {
    waitingForStart = true
    try {
      await sleep(delay)
    } catch (error) {
      waiting.shift().reject(error)
    } finally {
      waitingForStart = false
      pump()
    }
  }

  async function run(job) {
    active++
    try {
      if (job.cancelled()) { job.resolve(undefined); return }
      nextStartAt = now() + spacingMs
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
