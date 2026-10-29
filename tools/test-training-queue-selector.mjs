// Execute the real selector setup against deferred read APIs. No browser, GPU or network.
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import vm from 'node:vm'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import * as presentation from '../VLStream-Web/VLStream-ui/src/utils/trainingComputeQueue.mjs'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const requireUi = createRequire(path.join(root, 'VLStream-Web/VLStream-ui/package.json'))
const { parse } = requireUi('@vue/compiler-sfc')
const { parse: parseJs } = requireUi('@babel/parser')
const source = readFileSync(path.join(root, 'VLStream-Web/VLStream-ui/src/views/AlgorithmTraining/components/TrainingComputeSelector.vue'), 'utf8')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
const setup = descriptor.scriptSetup.content
const statements = parseJs(setup, { sourceType: 'module' }).program.body.filter(node => node.type !== 'ImportDeclaration').map(node => setup.slice(node.start, node.end)).join('\n')
const device = { id: '-99', scope: 'PLATFORM', billingType: 'FREE', schedulingMode: 'SHARED_GPU_QUEUE', powerState: 'RUNNING' }
const deferred = () => { let resolve, reject; const promise = new Promise((yes,no) => { resolve=yes; reject=no }); return { promise, resolve, reject } }
function fixture() {
  const props = { nodes: [], modelValue: '-99' }, watchers = [], events = [], requests = [], timers = new Map()
  let unmount, identity = 1, timerId = 0
  const context = {
    ...presentation, Map, Date, Promise,
    ref: value => ({ value }), computed: fn => ({ get value() { return fn() } }),
    defineProps: () => props, defineEmits: () => (...args) => events.push(args),
    watch: (get, fn, options) => { watchers.push(fn); if (options?.immediate) fn() },
    onUnmounted: fn => { unmount = fn },
    setInterval: fn => { timers.set(++timerId, fn); return timerId }, clearInterval: id => timers.delete(id),
    captureComputeIdentity: () => { const current = identity; return { assertCurrent: () => { if (current !== identity) throw new Error('changed') } } },
    getComputeQueueSnapshot: id => { const request = { id, ...deferred() }; requests.push(request); return request.promise },
    module: { exports: {} }
  }
  vm.runInNewContext(statements + '\nmodule.exports={refresh,handleVisible,snapshots,pending,presentation}', context)
  return { api: context.module.exports, props, watchers, requests, events, timers,
    setNodes: nodes => { props.nodes=nodes; watchers[0]() }, switchIdentity: () => identity++, unmount: () => unmount() }
}
const known = { state: 'KNOWN', waitingCount: 3, checkedAt: Date.now(), activePhase: 'RUNNING', activeTaskName: null }
const settle = () => new Promise(resolve => setImmediate(resolve))
let passed = 0
async function test(name, fn) { await fn(); passed++; console.log(`PASS ${name}`) }
await test('read refresh preserves selection and emits no selection or training action', async () => {
  const f=fixture(); f.setNodes([device]); const run=f.api.refresh()
  assert.equal(f.requests.length,1); f.requests[0].resolve({data:known}); await run; await settle()
  assert.equal(f.props.modelValue,'-99'); assert.equal(f.events.length,0); assert.equal(f.api.snapshots.value['-99'].waitingCount,3)
  f.unmount()
})
await test('list refresh coalesces reads and drops removed-device responses', async () => {
  const f=fixture(); f.setNodes([device]); f.setNodes([device]); assert.equal(f.requests.length,1)
  f.setNodes([]); f.requests[0].resolve({data:known}); await settle()
  assert.equal(f.api.snapshots.value['-99'],undefined); assert.equal(f.props.modelValue,'-99'); f.unmount()
})
await test('identity change rejects a late private task name', async () => {
  const f=fixture(); f.setNodes([device]); f.switchIdentity(); f.requests[0].resolve({data:{...known,activeTaskName:'old private name'}}); await settle()
  assert.equal(f.api.snapshots.value['-99'],null); f.unmount()
})
await test('failure removes stale counts and explicit retry recovers original ID', async () => {
  const f=fixture(); f.setNodes([device]); f.requests[0].resolve({data:known}); await settle()
  const first=f.api.refresh(); f.requests[1].reject(new Error('offline')); await first
  assert.equal(f.api.snapshots.value['-99'],null); assert.equal(f.props.modelValue,'-99')
  const retry=f.api.refresh(); f.requests[2].resolve({data:known}); await retry
  assert.equal(f.api.snapshots.value['-99'].waitingCount,3); assert.equal(f.events.length,0); f.unmount()
})
await test('stopped maintenance and paid resources never cause a queue SSH read', async () => {
  const f=fixture(); f.setNodes([{...device,stopped:true},{...device,id:'-173',operationPending:true},{...device,id:'-100',billingType:'POINTS'}]); await settle()
  assert.equal(f.requests.length,0); f.unmount()
})
await test('closing popup stops polling and unmount discards outstanding response', async () => {
  const f=fixture(); f.setNodes([device]); f.api.handleVisible(true); assert.equal(f.timers.size,2)
  f.api.handleVisible(false); assert.equal(f.timers.size,1)
  f.unmount(); assert.equal(f.timers.size,0); f.requests[0].resolve({data:known}); await settle()
  assert.equal(f.api.snapshots.value['-99'],undefined)
})
console.log(`${passed} selector async tests passed; no network or training operations`)
