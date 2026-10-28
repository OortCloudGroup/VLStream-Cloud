// A coordinate-only cache: no device/event/user IDs, tokens, or media are retained or sent to the provider.
export function createEventLocationResolver(resolveAddress, { now = Date.now, timeout = 20000, concurrency = 2 } = {}) {
  const cache = new Map(), pending = new Map(), queue = []
  let active = 0, blockedUntil = 0
  const remember = (key, result) => {
    cache.delete(key)
    cache.set(key, { result, expires: now() + (result.address ? 86400000 : 60000) })
    while (cache.size > 2048) cache.delete(cache.keys().next().value)
  }
  const pump = () => {
    while (active < concurrency && queue.length) {
      const job = queue.shift()
      if (!job.viewers.some(isCurrent => isCurrent())) {
        pending.delete(job.key); job.finish({ address: '', status: 'CANCELLED' }); continue
      }
      if (blockedUntil > now()) {
        const result = { address: '', status: 'UNAVAILABLE' }
        pending.delete(job.key); remember(job.key, result); job.finish(result); continue
      }
      active++
      let timer
      Promise.race([
        Promise.resolve().then(() => resolveAddress(job.coordinate)),
        new Promise((_, reject) => { timer = setTimeout(() => reject(new Error('地址解析超时')), timeout) })
      ]).then(address => {
        if (typeof address !== 'string' || !address.trim()) throw new Error('地址解析无结果')
        return { address: address.trim(), status: 'RESOLVED' }
      }).catch(() => {
        blockedUntil = now() + 60000
        return { address: '', status: 'UNAVAILABLE' }
      }).then(result => {
        remember(job.key, result); job.finish(result)
      }).finally(() => {
        clearTimeout(timer); pending.delete(job.key); active--; pump()
      })
    }
  }
  return (coordinate, isCurrent = () => true) => {
    const { lng, lat, coordinateSystem } = coordinate || {}
    if (typeof lng !== 'number' || typeof lat !== 'number' || !Number.isFinite(lng) || !Number.isFinite(lat)
      || Math.abs(lng) > 180 || Math.abs(lat) > 90 || !['WGS84', 'GCJ02', 'BD09'].includes(coordinateSystem)) {
      return Promise.resolve({ address: '', status: 'INVALID' })
    }
    const key = `${coordinateSystem}:${lng.toFixed(8)},${lat.toFixed(8)}`
    const cached = cache.get(key)
    if (cached?.expires > now()) {
      cache.delete(key); cache.set(key, cached)
      return Promise.resolve(cached.result)
    }
    const existing = pending.get(key)
    if (existing) { existing.viewers.push(isCurrent); return existing.promise }
    if (queue.length >= 100) return Promise.resolve({ address: '', status: 'RATE_LIMITED' })
    let finish
    const promise = new Promise(resolve => { finish = resolve })
    const job = { key, coordinate: { lng, lat, coordinateSystem }, viewers: [isCurrent], finish, promise }
    pending.set(key, job); queue.push(job); pump()
    return promise
  }
}
