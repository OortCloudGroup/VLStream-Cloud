// Serialize movement and stop requests so a late start cannot run after its stop.
export function createPtzHoldController({ startRequest, stopRequest, onError = () => {}, setTimer = setTimeout, clearTimer = clearTimeout }) {
  let active = null
  let tail = Promise.resolve()
  const enqueue = job => {
    const result = tail.then(job)
    tail = result.catch(onError)
    return tail
  }
  const move = context => enqueue(async () => {
    if (!context.pressed) return
    context.sent = true
    try {
      await startRequest({ id: context.id, direction: context.direction, controSpeed: context.speed })
    } catch (error) {
      context.pressed = false
      if (active === context) active = null
      await stopRequest({ id: context.id }).catch(() => {})
      throw error
    }
    if (context.pressed) context.timer = setTimer(() => move(context), 700)
  })
  const stop = () => {
    const context = active
    if (!context) return tail
    active = null
    context.pressed = false
    clearTimer(context.timer)
    return enqueue(() => context.sent ? stopRequest({ id: context.id }) : undefined)
  }
  const start = ({ id, direction, speed }) => {
    stop()
    if (id === null || id === undefined) return tail
    const context = { id, direction, speed, pressed: true, sent: false, timer: null }
    active = context
    return move(context)
  }
  return { start, stop, settled: () => tail }
}
