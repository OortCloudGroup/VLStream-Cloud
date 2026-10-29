import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { pairKey, attemptResult } from '../src/utils/savedModelDispatch.mjs'

const source = readFileSync(new URL('../src/views/VideoAggregation/components/DeviceModelDrawer.vue', import.meta.url), 'utf8')
  .split('<script setup>')[1].split('</script>')[0].replace(/^import .*$/gm, '')
const deferred = () => { let resolve; const promise = new Promise(done => { resolve = done }); return { promise, resolve } }
function fixture(overrides = {}, single = false) {
  let revision = 0, serial = 0, unmount
  const props = { modelValue: true, device: single ? { deviceId: 'CAM-A', online: true } : null,
    devices: single ? null : [{ deviceId: 'CAM-A', deviceName: '设备甲', online: true }, { deviceId: 'CAM-B', deviceName: '设备乙', online: true }] }
  const watchers = [], calls = [], events = [], timers = new Map()
  const models = [{ id: '1', modelName: '检测甲', deployable: true }, { id: '2', modelName: '检测乙', deployable: true }]
  const api = {
    getDeviceModelCandidates: async () => ({ code: 200, data: { records: models, total: 2 } }),
    submitSavedModelAttempt: async (modelId, deviceId, operationId) => {
      calls.push({ modelId, deviceId, operationId }); return { code: 200, data: { requestId: modelId + deviceId, state: 'PUBLISHED' } }
    },
    getSavedModelAttempt: async () => ({ code: 200, data: { requestId: 'request', state: 'SUCCESS' } }), ...overrides
  }
  const dependencies = {
    ref: value => ({ value }), computed: fn => ({ get value() { return fn() } }),
    onMounted: fn => fn(), onBeforeUnmount: fn => { unmount = fn }, watch: (get, callback) => watchers.push({ get, callback }),
    defineProps: () => props, defineEmits: () => (...args) => events.push(args),
    ElMessage: { success() {} }, uiText: text => text, window: { addEventListener() {}, removeEventListener() {} },
    crypto: { randomUUID: () => `fixture-uuid-${++serial}` },
    captureComputeIdentity: () => { const original = revision; return { assertCurrent() { if (original !== revision) throw new Error('changed') } } },
    setTimeout: fn => { timers.set(++serial, fn); return serial }, clearTimeout: id => timers.delete(id),
    setInterval: fn => { timers.set(++serial, fn); return serial }, clearInterval: id => timers.delete(id),
    pairKey, attemptResult, ...api
  }
  const vm = new Function(...Object.keys(dependencies), source + '\nreturn {selected,results,submitting,targetDevices,pendingModels,retryablePairs,uncertainPairs,toggleModel,submitSelection,retryFailed,recoverUncertain,closeDrawer,refreshResults};')(...Object.values(dependencies))
  const change = async visible => { props.modelValue = visible; await watchers[0].callback([visible, (props.devices || [props.device]).map(item => item.deviceId).join('|')]) }
  return { vm, props, calls, events, timers, models, change, async open() { await change(true); models.forEach(vm.toggleModel) }, switchIdentity() { revision++ }, unmount() { unmount() } }
}

test('single device picker still submits several saved models; multiple device picker submits all four pairs', async () => {
  const f = fixture(); await f.open(); await f.vm.submitSelection()
  assert.deepEqual(f.calls.map(item => [item.modelId, item.deviceId]), [['1','CAM-A'],['1','CAM-B'],['2','CAM-A'],['2','CAM-B']])
  assert.equal(Object.keys(f.vm.results.value).length, 4)
  assert.ok(Object.values(f.vm.results.value).every(item => item.state === 'pending'))
  const single = fixture({}, true); await single.open(); await single.vm.submitSelection()
  assert.deepEqual(single.calls.map(item => item.modelId), ['1', '2'])
})

test('double click while a request is pending sends the immutable selection once and blocks close', async () => {
  const wait = deferred(), sent = []
  const f = fixture({ submitSavedModelAttempt: (...args) => { sent.push(args); return wait.promise } }); await f.open()
  const first = f.vm.submitSelection(); await f.vm.submitSelection(); f.vm.closeDrawer()
  assert.equal(sent.length, 1); assert.equal(f.events.some(item => item[0] === 'update:modelValue'), false)
  f.vm.toggleModel(f.models[0]); assert.equal(f.vm.selected.value.length, 2)
  wait.resolve({ code: 200, data: { requestId: 'pending', state: 'PUBLISHED' } }); await first
  assert.equal(sent.length, 4)
})

test('only explicit failed combinations retry with a new operation; accepted and uncertain remain untouched', async () => {
  let retry = false; const sent = []
  const f = fixture({ submitSavedModelAttempt: async (model, device, operationId) => {
    sent.push({ model, device, operationId }); return { code: 200, data: { requestId: model+device,
      state: !retry && model === '1' && device === 'CAM-B' ? 'FAILED' : model === '2' && device === 'CAM-B' ? 'UNKNOWN' : 'PUBLISHED', retryable: true } }
  } }); await f.open(); await f.vm.submitSelection()
  assert.equal(f.vm.retryablePairs.value.length, 1); assert.equal(f.vm.uncertainPairs.value.length, 1)
  retry = true; await f.vm.retryFailed()
  assert.equal(sent.length, 5); assert.equal(sent[4].model, '1'); assert.equal(sent[4].device, 'CAM-B')
  assert.notEqual(sent[1].operationId, sent[4].operationId)
  await f.vm.recoverUncertain(); assert.equal(sent.length, 6)
  assert.equal(sent[3].operationId, sent[5].operationId)
})

test('transport failures preserve operation IDs and never enable the failure retry button', async () => {
  const sent = []; const f = fixture({ submitSavedModelAttempt: async (...args) => { sent.push(args); throw new Error('connection lost') } }, true)
  await f.open(); await f.vm.submitSelection(); assert.equal(f.vm.retryablePairs.value.length, 0)
  await f.vm.recoverUncertain(); assert.equal(sent.length, 4); assert.equal(sent[0][2], sent[2][2])
})

test('cancel before submit writes nothing; reopening original targets restores outcomes and polls read-only', async () => {
  const f = fixture(); await f.open(); f.vm.closeDrawer(); assert.equal(f.calls.length, 0)
  await f.vm.submitSelection(); await f.change(false); await f.change(true)
  await f.vm.refreshResults(); assert.equal(f.calls.length, 4)
  assert.ok(Object.values(f.vm.results.value).every(item => item.state === 'success'))
  f.unmount(); assert.equal(f.timers.size, 0)
})

test('identity change stops remaining combinations and removes old targets and results', async () => {
  const wait = deferred(); const f = fixture({ submitSavedModelAttempt: () => wait.promise }); await f.open()
  const first = f.vm.submitSelection(); f.switchIdentity(); wait.resolve({code:200,data:{requestId:'late',state:'PUBLISHED'}}); await first
  assert.equal(Object.keys(f.vm.results.value).length, 0); assert.equal(f.vm.targetDevices.value.length, 0)
  assert.ok(f.events.some(item => item[0] === 'update:modelValue' && item[1] === false))
  await f.open(); assert.equal(f.vm.targetDevices.value.length, 2)
  assert.equal(f.vm.selected.value.length, 2)
})

test('changing targets or closing externally discards late responses and stops subsequent submissions', async () => {
  const wait = deferred(), sent = []; const f = fixture({ submitSavedModelAttempt: (...args) => { sent.push(args); return wait.promise } }); await f.open()
  const first = f.vm.submitSelection(); f.props.devices = [{deviceId:'CAM-C',online:true}]; await f.change(true)
  wait.resolve({code:200,data:{requestId:'late',state:'PUBLISHED'}}); await first
  assert.equal(sent.length, 1); assert.equal(Object.keys(f.vm.results.value).length, 0)
  assert.equal(f.vm.targetDevices.value[0].deviceId, 'CAM-C')
})

test('status labels never call submitted/downloaded/unknown deployment success; missing response is uncertain', () => {
  for (const state of ['PUBLISHED','DOWNLOADED','UNKNOWN']) assert.notEqual(attemptResult({requestId:'a',state}).state,'success')
  assert.equal(attemptResult({requestId:'a',state:'UNKNOWN',retryable:true}).retryable,false)
  assert.throws(() => attemptResult({requestId:'a'}))
})

test('offline or denied status refresh explains uncertainty without enabling a new attempt', async () => {
  const f = fixture({getSavedModelAttempt: async () => {throw new Error('设备已离线或权限已撤销')}}, true)
  await f.open(); await f.vm.submitSelection(); await f.vm.refreshResults()
  assert.equal(f.vm.retryablePairs.value.length,0)
  assert.ok(Object.values(f.vm.results.value).every(item => item.state === 'uncertain' && /离线/.test(item.message)))
})
