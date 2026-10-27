import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { batchSelectionError, deploymentStatus, deploymentTag, retryableDeviceIds, modelDeploymentImpact } from '../src/utils/modelDispatchBatch.mjs'

const source = readFileSync(new URL('../src/views/DecisionAI/components/ModelDeploymentDialog.vue', import.meta.url), 'utf8')
  .split('<script setup>')[1].split('</script>')[0].replace(/^import .*$/gm, '')
const deferred = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b }); return { promise, resolve, reject } }
function fixture(overrides = {}) {
  let revision = 0, serial = 0, unmount
  const props = { modelValue: true, devices: [{ deviceId:'CAM-A' }, { deviceId:'CAM-B' }], existingOperationId:'', model:null }
  const watchers = [], events = [], timers = new Map(), posts = [], retries = []
  const batch = { operationId:'fixture-op', items:[{deviceId:'CAM-A',state:'PUBLISHED',retryable:false},{deviceId:'CAM-B',state:'FAILED',retryable:true}], failed:1,pending:1 }
  const api = {
    submitModelBatch: async request => { posts.push(structuredClone(request)); return {code:200,data:batch} },
    getModelBatch: async () => ({code:200,data:batch}),
    retryModelBatch: async (id, request) => { retries.push({id,...structuredClone(request)}); return {code:200,data:batch} },
    ...overrides
  }
  const dependencies = {
    ref: value => ({value}), watch: (get, callback) => watchers.push({get,callback}), onBeforeUnmount: fn => {unmount=fn},
    defineProps: () => props, defineEmits: () => (...args) => events.push(args), uuid: () => `fixture-uuid-${++serial}`,
    uiText: text => text, getAlgorithmPage: async () => ({code:200,data:{records:[{id:'7',name:'算法'}]}}),
    captureComputeIdentity: () => { const original=revision; return {assertCurrent() {if(original!==revision) throw new Error('changed')} } },
    setTimeout: fn => {timers.set(++serial,fn);return serial}, clearTimeout: id=>timers.delete(id),
    setInterval: fn => {timers.set(++serial,fn);return serial}, clearInterval:id=>timers.delete(id),
    batchSelectionError, deploymentStatus, deploymentTag, retryableDeviceIds, modelDeploymentImpact, ...api
  }
  const make = new Function(...Object.keys(dependencies), source + '\nreturn { submit, refresh, retryFailed, recoverSubmission, close, batch, busy, algorithmId, confirmed, operationId, submitRequest, targets, error };')
  const vm = make(...Object.values(dependencies))
  const visibleWatch = watchers.find(item => typeof item.get === 'function')
  return { vm, props, posts, retries, events, timers, batch, async open() {await visibleWatch.callback(true);vm.algorithmId.value='7';vm.confirmed.value=true},
    async closeProp() {props.modelValue=false;await visibleWatch.callback(false)}, switchIdentity(){revision++}, unmount(){unmount()} }
}

test('rejects empty, duplicate, missing ID and >20 device selections', () => {
  assert.ok(batchSelectionError([])); assert.ok(batchSelectionError([{id:7}]));
  assert.ok(batchSelectionError([{deviceId:'A'},{deviceId:'A'}]));
  assert.ok(batchSelectionError(Array.from({length:21},(_,i)=>({deviceId:String(i)}))));
  assert.equal(batchSelectionError([{deviceId:'CAM-A'}]),'')
})
test('published/downloaded/unknown are never labelled deployment success or retryable', () => {
  assert.match(deploymentStatus({state:'PUBLISHED'}),/等待设备/); assert.match(deploymentStatus({state:'DOWNLOADED'}),/等待部署/)
  assert.equal(deploymentTag({state:'UNKNOWN'}),'warning')
  assert.deepEqual(retryableDeviceIds({items:[{deviceId:'A',retryable:false},{deviceId:'B',retryable:true}]}),['B'])
})
test('two submit clicks while HTTP is pending issue exactly one immutable batch', async () => {
  const pending=deferred(); const requests=[]
  const f=fixture({submitModelBatch: request=>{requests.push(structuredClone(request));return pending.promise}}); await f.open()
  const first=f.vm.submit(); const id=f.vm.operationId.value
  await f.vm.submit(); assert.equal(requests.length,1); assert.equal(f.vm.busy.value,true)
  pending.resolve({code:200,data:f.batch}); await first
  assert.equal(requests[0].operationId,id); assert.deepEqual(requests[0].deviceIds,['CAM-A','CAM-B']); assert.equal(f.vm.batch.value.pending,1)
})
test('cancel before submit has no write, and close/unmount ignore late responses', async () => {
  const f=fixture(); await f.open(); f.vm.close(); assert.equal(f.posts.length,0)
  const pending=deferred(); const g=fixture({submitModelBatch:()=>pending.promise}); await g.open()
  const first=g.vm.submit(); await g.closeProp(); pending.resolve({code:200,data:g.batch}); await first
  assert.equal(g.vm.batch.value,null); assert.equal(g.events.filter(e=>e[0]==='submitted').length,0)
  g.unmount(); assert.equal(g.timers.size,0)
})
test('identity switch clears target/selection/result and rejects late submit success', async () => {
  const pending=deferred(); const f=fixture({submitModelBatch:()=>pending.promise}); await f.open()
  const first=f.vm.submit(); f.switchIdentity(); pending.resolve({code:200,data:f.batch}); await first
  assert.deepEqual(f.vm.targets.value,[]); assert.equal(f.vm.batch.value,null); assert.equal(f.vm.operationId.value,'')
  assert.ok(f.events.some(event=>event[0]==='update:modelValue' && event[1]===false))
})
test('lost submit response recovery reuses exact original key and payload', async () => {
  let count=0; const requests=[]; const f=fixture({submitModelBatch:async request=>{requests.push(structuredClone(request));if(++count===1)throw new Error('HTTP lost');return {code:200,data:f.batch}}})
  await f.open(); await f.vm.submit(); const id=f.vm.operationId.value; assert.ok(id); assert.equal(f.vm.batch.value,null)
  await f.vm.recoverSubmission(); assert.deepEqual(requests[0],requests[1]); assert.equal(f.vm.operationId.value,id)
})
test('retry uses only failed IDs and retains exact retry key after lost response', async () => {
  let count=0; const requests=[]; const f=fixture({retryModelBatch:async (id, request)=>{requests.push(structuredClone(request));if(++count===1)throw new Error('HTTP lost');return {code:200,data:f.batch}}})
  await f.open(); await f.vm.submit(); await f.vm.retryFailed(); await f.vm.retryFailed()
  assert.deepEqual(requests[0],requests[1]); assert.deepEqual(requests[0].deviceIds,['CAM-B'])
})
test('existing operation restores by read only without choosing model again', async () => {
  const f=fixture();f.props.existingOperationId='fixture-existing';await f.open();await Promise.resolve();await Promise.resolve()
  assert.equal(f.posts.length,0);assert.equal(f.vm.operationId.value,'fixture-existing');assert.equal(f.vm.batch.value.pending,1)
})
