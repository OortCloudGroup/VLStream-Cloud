import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createComputeSelectionLoader } from '../src/utils/computeSelectionLoader.mjs'

const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
const tick = () => new Promise(resolve => setImmediate(resolve))
const node = id => ({ id, billingType: 'FREE', schedulingMode: 'SHARED_GPU_QUEUE', canRefreshEnvironment: true, healthState: 'UNKNOWN' })
function fixture(read) {
  let actor = 0
  const checks = [], updates = []
  const load = createComputeSelectionLoader({
    captureIdentity: () => {
      const captured = actor
      return { assertCurrent() { if (actor !== captured) throw new Error('identity changed') } }
    },
    read,
    refresh: id => {
      const pending = deferred()
      checks.push({ id, actor, ...pending })
      return pending.promise
    }
  })
  return { load, checks, updates, changeIdentity() { actor++ } }
}
test('first real snapshot returns immediately; each host refreshes independently, including failure', async () => {
  let reads = 0
  const f = fixture(async () => ({ data: [node('slow'), { ...node('healthy'), healthState: reads++ ? 'READY' : 'UNKNOWN' }] }))
  const first = await f.load(value => f.updates.push(value))
  assert.equal(first.data[1].healthState, 'UNKNOWN')
  await tick()
  assert.equal(f.checks.length, 2)
  f.checks[1].resolve()
  await tick()
  assert.equal(f.updates.length, 1)
  assert.equal(f.updates[0].data[1].healthState, 'READY')
  f.checks[0].reject(new Error('offline'))
  await tick()
  assert.equal(f.updates.length, 2)
  assert.equal(reads, 3)
})
test('concurrent callers share GET and per-node checks and both receive updates', async () => {
  const first = deferred()
  let reads = 0
  const f = fixture(() => ++reads === 1 ? first.promise : Promise.resolve({ data: [] }))
  const a = [], b = []
  const pending = [f.load(value => a.push(value)), f.load(value => b.push(value))]
  await tick()
  assert.equal(reads, 1)
  first.resolve({ data: [node('one')] })
  await Promise.all(pending)
  await tick()
  assert.equal(f.checks.length, 1)
  f.checks[0].resolve()
  await tick()
  assert.equal(reads, 2)
  assert.equal(a.length, 1)
  assert.equal(b.length, 1)
})
test('check finishing during a GET requires a later fresh GET without waiting for another host', async () => {
  let reads = 0
  const during = deferred()
  const f = fixture(() => {
    reads++
    return reads === 2 ? during.promise : Promise.resolve({ data: reads === 1 ? [node('a'), node('b')] : [] })
  })
  await f.load(value => f.updates.push(value))
  await tick()
  f.checks[0].resolve()
  await tick()
  f.checks[1].resolve()
  await tick()
  assert.equal(reads, 2)
  during.resolve({ data: [node('b')] })
  await tick()
  assert.equal(reads, 3)
  assert.deepEqual(f.updates.at(-1).data, [])
})
test('identity changes isolate same-node checks and suppress all old callbacks and follow-up reads', async () => {
  let reads = 0
  const f = fixture(async () => { reads++; return { data: [node('same')] } })
  const old = [], current = []
  await f.load(value => old.push(value))
  await tick()
  f.changeIdentity()
  await f.load(value => current.push(value))
  await tick()
  assert.deepEqual(f.checks.map(check => check.actor), [0, 1])
  f.checks[0].resolve()
  await tick()
  assert.equal(reads, 2)
  assert.equal(old.length, 0)
  f.checks[1].resolve()
  await tick()
  assert.equal(current.length, 1)
})
test('old in-flight GET is never reused by a new identity; read failures allow retry', async () => {
  const old = deferred()
  let reads = 0
  const f = fixture(() => ++reads === 1 ? old.promise : Promise.resolve({ data: [] }))
  const pending = assert.rejects(f.load(), /identity changed/)
  await tick()
  f.changeIdentity()
  await f.load()
  old.resolve({ data: [node('old')] })
  await pending
  assert.equal(f.checks.length, 0)
  assert.equal(reads, 2)
  let attempts = 0
  const retry = fixture(async () => {
    if (!attempts++) throw new Error('offline')
    return { data: [] }
  })
  await assert.rejects(retry.load(), /offline/)
  assert.deepEqual(await retry.load(), { data: [] })
})
test('no callback still performs automatic checks and read failures never reject detached work', async () => {
  let reads = 0
  const f = fixture(async () => {
    if (reads++) throw new Error('snapshot unavailable')
    return { data: [node('one'), { ...node('paid'), billingType: 'POINTS' }] }
  })
  await f.load()
  await tick()
  assert.equal(f.checks.length, 1)
  f.checks[0].resolve()
  await tick()
  assert.equal(reads, 2)
})
