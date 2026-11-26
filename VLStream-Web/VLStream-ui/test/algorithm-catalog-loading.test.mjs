import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/views/AlgorithmWarehouse/components/AlgorithmCatalogView.vue', import.meta.url), 'utf8')
const loader = source.slice(source.indexOf('let catalogRequest'), source.indexOf('function resetEditor'))
const page = (...records) => ({ data: { records, total: records.length } })

function harness(readPage, getSettings = async () => ({ data: 'flat' }), getCategories = async () => ({ data: [] })) {
  const ref = value => ({ value })
  const timers = new Map()
  let timerId = 0
  const state = {
    getAlgorithmCatalogPage: readPage,
    getAlgorithmCatalogCategories: getCategories,
    getAlgorithmCatalogSettings: getSettings,
    setTimeout: action => { timers.set(++timerId, action); return timerId },
    clearTimeout: id => timers.delete(id),
    rows: ref([]), viewMode: ref('flat'), catalogLoading: ref(false), catalogLoadFailed: ref(false),
    algorithmsLoading: ref(false), algorithmLoadFailed: ref(false), algorithms: ref([]),
    section: ref('library'), activeCategoryId: ref('0'), keyword: ref(''), algorithmType: ref(''),
    algorithmCurrent: ref(1), algorithmTotal: ref(0), algorithmPageSize: 24
  }
  const compile = new Function(...Object.keys(state), `${loader}\nreturn { refreshAll, loadAlgorithms, scheduleAlgorithmSearch }`)
  const actions = compile(...Object.values(state))
  return { ...actions, state, timers }
}

test('the first algorithm results do not wait for categories or layout settings', async () => {
  let finishSettings
  const h = harness(async () => page('first algorithm'),
    () => new Promise(resolve => { finishSettings = resolve }), async () => { throw new Error('categories unavailable') })
  const refresh = h.refreshAll()
  await Promise.resolve()
  assert.deepEqual(h.state.algorithms.value, ['first algorithm'])
  assert.equal(h.state.algorithmsLoading.value, false)
  assert.equal(h.state.catalogLoading.value, true)
  finishSettings({ data: 'tree' })
  await refresh
  assert.equal(h.state.catalogLoadFailed.value, true)
  assert.equal(h.state.viewMode.value, 'tree')
})

test('typing coalesces requests and aborts stale pages before the delayed search', async () => {
  const calls = []
  let finishOldPage
  const h = harness((params, options) => {
    calls.push({ params, signal: options.signal })
    if (calls.length === 1) return new Promise(resolve => { finishOldPage = resolve })
    return Promise.resolve(page('matching algorithm'))
  })
  const oldPage = h.loadAlgorithms()
  for (const keyword of ['安', '安全', '安全帽']) {
    h.state.keyword.value = keyword
    h.scheduleAlgorithmSearch()
  }
  assert.equal(h.timers.size, 1)
  assert.equal(calls.length, 1)
  assert.equal(calls[0].signal.aborted, true)
  finishOldPage(page('obsolete algorithm'))
  await oldPage
  assert.deepEqual(h.state.algorithms.value, [])
  await [...h.timers.values()][0]()
  assert.equal(calls.length, 2)
  assert.equal(calls[1].params.keyword, '安全帽')
  assert.deepEqual(h.state.algorithms.value, ['matching algorithm'])
})

test('an unavailable algorithm page exposes a retry and recovers without refreshing categories', async () => {
  let attempts = 0
  const h = harness(async () => {
    if (++attempts === 1) throw new Error('catalog unavailable')
    return page('recovered')
  })
  await h.loadAlgorithms()
  assert.equal(h.state.algorithmLoadFailed.value, true)
  assert.equal(h.state.algorithmsLoading.value, false)
  await h.loadAlgorithms()
  assert.equal(h.state.algorithmLoadFailed.value, false)
  assert.deepEqual(h.state.algorithms.value, ['recovered'])
})
