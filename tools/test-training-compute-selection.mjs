// Run real training-page functions against isolated API responses. No network or GPU jobs.
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import vm from 'node:vm'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const requireUi = createRequire(path.join(root, 'VLStream-Web/VLStream-ui/package.json'))
const { parse } = requireUi('@vue/compiler-sfc')
const { parse: parseJs } = requireUi('@babel/parser')
const source = readFileSync(path.join(root, 'VLStream-Web/VLStream-ui/src/views/AlgorithmTraining/AlgorithmTraining.vue'), 'utf8')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
const script = descriptor.scriptSetup.content
const declarations = new Map()
for (const statement of parseJs(script, { sourceType: 'module' }).program.body) {
  if (statement.type === 'VariableDeclaration') {
    for (const declaration of statement.declarations) declarations.set(declaration.id.name, `${statement.kind} ${script.slice(declaration.start, declaration.end)}`)
  }
}
const ref = value => ({ value })
const computed = fn => ({ get value() { return fn() } })
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
const gpu = { id: '2109924335639650306', name: 'Fixture GPU', gpuIndex: 0, billingType: 'FREE', schedulingMode: 'SHARED_GPU_QUEUE', requiresClaim: false, claimState: 'SHARED_QUEUE', schedulable: true }
const otherGpu = { ...gpu, id: '2109944661014523906' }
function fixture(nodes = [gpu]) {
  const calls = { starts: [], auth: 0, messages: [], routes: [], polls: 0 }
  const context = {
    ref, computed, uiText: text => text, console: { error() {}, log() {}, warn() {} },
    currentTrainingItem: ref({ originalData: { id: '2110657440730079233', algorithmId: '1' } }),
    selectedDatasets: ref([{ value: '999999999999999999', annotatedCount: 8 }]), selectedDatasetIds: ref(['999999999999999999']),
    showTrainingConfig: ref(true), trainingBackendReady: ref(true), isTraining: ref(false),
    trainingMode: ref('auto'), epochTotal: ref(1), batchSize: ref(16), trainImgSize: ref(640), autoPublish: ref('yes'),
    lastModelPath: ref(''), lastLogPath: ref(''),
    algorithmOptions: ref([{ value: '1', type: 'detect' }]), loadAlgorithmOptions: async () => {},
    listComputeSelection: async () => ({ data: nodes }),
    getAlgorithmAnnotationById: async () => ({ code: 200, data: { annotatedCount: 8 } }),
    buildTrainingOptions: ({ epochs, batchSize, imgSize }) => ({ epochs, batchSize, imgSize, extraParams: '{"mode":"auto","autoPublish":true}' }),
    captureComputeIdentity: () => ({ assertCurrent: () => { if (context.identityChanged) throw new Error('Identity changed') } }),
    requireComputePlatformAuthorization: async () => { calls.auth++; return { platformToken: 'fixture-proof' } },
    isComputeAuthorizationCurrent: () => true, saveComputeDraft() {}, bindComputeDraftToActor() {},
    ElMessageBox: { confirm: async () => {} }, ElMessage: { warning: value => calls.messages.push(value), error: value => calls.messages.push(value) },
    goCompute: route => calls.routes.push(route), stopAllPolling() {}, appendLogLines() {},
    startCloudTraining: async (...args) => calls.starts.push(args), refreshPublication: async () => {},
    startPollingForTask: () => calls.polls++, loadTrainingData: async () => {},
    datasetOptions: ref([]), loadDatasetOptions: async () => {}, dataStrategy: ref('default'), resolution: ref('auto'), epochMode: ref('auto'), customValidation: ref(true), publicationState: ref(''),
    module: { exports: {} }
  }
  const names = ['normalizeComputeNodeId', 'computeNodeId', 'cloudModelSource', 'computeNodes', 'computeNodesLoading', 'computeNodesError', 'trainingSubmitting',
    'computeRequestGeneration', 'trainingConfigGeneration', 'trainingSubmitGeneration', 'selectedCompute', 'selectedSharedCompute', 'computeSelectionHint', 'loadComputeNodes',
    'computeClaimLabels', 'computeLabel', 'selectCompute', 'isDatasetReady', 'resolveBaseModelPath', 'buildTrainingPayload', 'handleStartTraining', 'restoreTrainingConfig', 'resetToDefaultConfig']
  for (const name of names) assert.ok(declarations.has(name), `Missing ${name}`)
  vm.runInNewContext(names.map(name => declarations.get(name)).join('\n') + `\nmodule.exports = { ${names.filter(name => !name.endsWith('Generation')).join(',')}, invalidate: () => { trainingConfigGeneration++; computeRequestGeneration++ } }`, context)
  const api = context.module.exports
  api.computeNodeId.value = gpu.id
  return { api, context, calls }
}
const tests = []
const test = (name, fn) => tests.push([name, fn])

test('configured company GPU IDs survive refresh and submit without a database node', async () => {
  const device = { ...gpu, id: '-173' }
  const f = fixture([device]); f.api.computeNodeId.value = '-173'
  await f.api.loadComputeNodes(); await f.api.handleStartTraining()
  assert.equal(f.api.computeNodeId.value, '-173')
  assert.equal(f.calls.starts[0][1].nodeId, '-173'); assert.equal(f.calls.auth, 0)
  assert.equal(f.api.normalizeComputeNodeId(-99), '-99')
  assert.equal(f.api.normalizeComputeNodeId('../173'), '')
})
test('saved configured GPU IDs restore as strings from historical training configuration', async () => {
  const device = { ...gpu, id: '-99' }
  const f = fixture([device]); await f.api.restoreTrainingConfig({ configParams: JSON.stringify({ computeNodeId: '-99', modelSource: 'preset' }) })
  assert.equal(f.api.computeNodeId.value, '-99'); assert.equal(f.api.selectedCompute.value.id, '-99')
  assert.equal(f.calls.starts.length, 0)
})
test('selector contains no virtual default resource or legacy submission', () => {
  assert.doesNotMatch(source, /平台默认 GPU 服务器|startTrainingWithParams|当前选择默认 GPU|Intel \(R\) Xeon/)
  assert.match(source, /:loading="computeNodesLoading"/)
  assert.match(source, /!selectedCompute\?\.schedulable/)
})
test('no selected device never submits', async () => {
  const f = fixture(); f.api.computeNodeId.value = ''; await f.api.handleStartTraining()
  assert.equal(f.calls.starts.length, 0); assert.match(f.calls.messages[0], /请选择实际 GPU/)
})
test('device selector delegates grouped presentation without changing submission names', () => {
  assert.match(descriptor.template.content, /<TrainingComputeSelector/)
  assert.match(descriptor.template.content, /:nodes="computeNodes"/)
  const f = fixture()
  const node = { ...gpu, name: '免费 GPU 173（P100）', gpuModel: 'Tesla P100-PCIE-16GB' }
  assert.equal(f.api.computeLabel(node), 'GPU 173（P100） · GPU 0')
  assert.equal(node.name, '免费 GPU 173（P100）')
})
test('device label keeps actual model and GPU index when absent from name', () => {
  const f = fixture()
  assert.equal(f.api.computeLabel({ ...gpu, name: 'Compute 1', gpuModel: 'RTX 4090', gpuIndex: 2 }), 'Compute 1 · RTX 4090 · GPU 2')
})
test('device labels retain unavailable and exclusive claim hints', () => {
  const f = fixture()
  assert.match(f.api.computeLabel({ ...gpu, schedulable: false }), /暂不可训练$/)
  assert.match(f.api.computeLabel({ ...gpu, stopped: true }), /请管理员检查共享环境$/)
  assert.match(f.api.computeLabel({ ...gpu, requiresClaim: true, claimState: 'OTHER' }), /他人占用/)
  assert.match(f.api.computeLabel({ ...gpu, billingType: 'POINTS' }), /付费实例运行中$/)
  assert.match(f.api.computeLabel({ ...gpu, billingType: 'POINTS', schedulable: false }), /请先完成付费开机与环境检查$/)
})
test('clean label and refresh still submit the exact selected device ID', async () => {
  const device = { ...gpu, name: '免费 GPU 173（P100）' }
  const f = fixture([device, otherGpu]); await f.api.loadComputeNodes()
  assert.equal(f.api.computeLabel(f.api.selectedCompute.value), 'GPU 173（P100） · GPU 0')
  await f.api.loadComputeNodes(); await f.api.handleStartTraining()
  assert.equal(f.calls.starts[0][1].nodeId, gpu.id); assert.equal(device.name, '免费 GPU 173（P100）')
})
test('shared GPU submits exact string IDs and uses no exclusive authorization', async () => {
  const f = fixture(); await f.api.handleStartTraining()
  assert.equal(f.calls.starts.length, 1); assert.equal(f.calls.auth, 0)
  assert.equal(f.calls.starts[0][0], '2110657440730079233')
  assert.equal(f.calls.starts[0][1].nodeId, gpu.id)
  assert.equal(f.calls.starts[0][1].datasetId, '999999999999999999')
  assert.equal(f.calls.starts[0][1].modelSource, 'preset'); assert.equal(f.api.trainingSubmitting.value, false)
})
test('private GPU preserves no-claim behavior', async () => {
  const f = fixture([{ ...gpu, billingType: 'EXTERNAL', schedulingMode: 'INSTANCE_EXCLUSIVE' }]); await f.api.handleStartTraining()
  assert.equal(f.calls.starts.length, 1); assert.equal(f.calls.auth, 0)
})
test('ordinary exclusive GPU keeps platform proof', async () => {
  const f = fixture([{ ...gpu, requiresClaim: true, claimState: 'MINE', schedulingMode: 'INSTANCE_EXCLUSIVE' }]); await f.api.handleStartTraining()
  assert.equal(f.calls.auth, 1); assert.equal(f.calls.starts[0][2], 'fixture-proof')
})
test('paid GPU remains blocked even after authorization', async () => {
  const f = fixture([{ ...gpu, billingType: 'POINTS' }]); await f.api.handleStartTraining()
  assert.equal(f.calls.auth, 1); assert.equal(f.calls.starts.length, 0); assert.equal(f.api.trainingSubmitting.value, false)
})
test('no inventory never selects another device', async () => {
  const f = fixture([]); await f.api.handleStartTraining()
  assert.equal(f.api.computeNodeId.value, gpu.id); assert.equal(f.calls.starts.length, 0)
})
test('removed selected GPU cannot fall back to remaining GPU', async () => {
  const f = fixture([otherGpu]); await f.api.handleStartTraining()
  assert.equal(f.api.computeNodeId.value, gpu.id); assert.equal(f.calls.starts.length, 0); assert.match(f.api.computeSelectionHint.value, /原选 GPU/)
})
test('unschedulable GPU is rejected', async () => {
  const f = fixture([{ ...gpu, schedulable: false }]); await f.api.handleStartTraining(); assert.equal(f.calls.starts.length, 0)
})
test('loading failure is distinct from an empty inventory', async () => {
  const f = fixture(); f.context.listComputeSelection = async () => { throw new Error('offline') }
  await f.api.handleStartTraining(); assert.equal(f.api.computeNodesError.value, true)
  assert.match(f.api.computeSelectionHint.value, /加载失败/); assert.equal(f.calls.starts.length, 0)
})
test('reload failure clears stale resources and retry recovers same selection', async () => {
  const f = fixture(); await f.api.loadComputeNodes(); f.context.listComputeSelection = async () => { throw new Error('offline') }
  await f.api.loadComputeNodes(); assert.equal(f.api.selectedCompute.value, undefined)
  f.context.listComputeSelection = async () => ({ data: [gpu] }); await f.api.loadComputeNodes()
  assert.equal(f.api.selectedCompute.value.id, gpu.id); assert.equal(f.api.computeNodesError.value, false)
})
test('newest refresh wins over late success', async () => {
  const f = fixture(); const first = deferred(); f.context.listComputeSelection = () => first.promise
  const pending = f.api.loadComputeNodes(); f.context.listComputeSelection = async () => ({ data: [otherGpu] }); await f.api.loadComputeNodes()
  first.resolve({ data: [gpu] }); await pending; assert.equal(f.api.computeNodes.value[0].id, otherGpu.id)
})
test('newest refresh wins over late error', async () => {
  const f = fixture(); const first = deferred(); f.context.listComputeSelection = () => first.promise
  const pending = f.api.loadComputeNodes(); f.context.listComputeSelection = async () => ({ data: [gpu] }); await f.api.loadComputeNodes()
  first.reject(new Error('late')); await pending; assert.equal(f.api.computeNodesError.value, false)
})
test('repeated clicks during first refresh submit once', async () => {
  const f = fixture(); const response = deferred(); f.context.listComputeSelection = () => response.promise
  const first = f.api.handleStartTraining(); await f.api.handleStartTraining(); assert.equal(f.api.trainingSubmitting.value, true)
  response.resolve({ data: [gpu] }); await first; assert.equal(f.calls.starts.length, 1)
})
test('selection changes while loading abort submission', async () => {
  const f = fixture(); const response = deferred(); f.context.listComputeSelection = () => response.promise
  const first = f.api.handleStartTraining(); f.api.computeNodeId.value = otherGpu.id
  response.resolve({ data: [gpu, otherGpu] }); await first; assert.equal(f.calls.starts.length, 0)
})
test('configuration changes while loading abort submission', async () => {
  const f = fixture(); const response = deferred(); f.context.listComputeSelection = () => response.promise
  const first = f.api.handleStartTraining(); f.context.epochTotal.value = 2
  response.resolve({ data: [gpu] }); await first; assert.equal(f.calls.starts.length, 0)
})
test('node becomes unavailable during dataset validation', async () => {
  const f = fixture(); let count = 0
  f.context.listComputeSelection = async () => ({ data: ++count === 1 ? [gpu] : [otherGpu] })
  await f.api.handleStartTraining(); assert.equal(f.calls.starts.length, 0)
})
test('identity changes while loading cannot submit', async () => {
  const f = fixture(); const response = deferred(); f.context.listComputeSelection = () => response.promise
  const first = f.api.handleStartTraining(); f.context.identityChanged = true
  response.resolve({ data: [gpu] }); await first; assert.equal(f.calls.starts.length, 0)
})
test('task switch while loading cannot submit or poll old task', async () => {
  const f = fixture(); const response = deferred(); f.context.listComputeSelection = () => response.promise
  const first = f.api.handleStartTraining(); f.context.currentTrainingItem.value = { id: 'other-task' }; f.api.invalidate()
  response.resolve({ data: [gpu] }); await first; assert.equal(f.calls.starts.length, 0); assert.equal(f.calls.polls, 0)
})
test('confirmation cancellation releases submit guard for retry', async () => {
  const f = fixture(); f.context.currentTrainingItem.value.originalData.modelOutputPath = 'existing.pt'
  f.context.ElMessageBox.confirm = async () => { throw 'cancel' }
  await f.api.handleStartTraining(); assert.equal(f.api.trainingSubmitting.value, false)
  f.context.ElMessageBox.confirm = async () => {}; await f.api.handleStartTraining(); assert.equal(f.calls.starts.length, 1)
})
test('failed submit releases guard', async () => {
  const f = fixture(); f.context.startCloudTraining = async () => { throw new Error('rejected') }
  await f.api.handleStartTraining(); assert.equal(f.api.trainingSubmitting.value, false); assert.equal(f.context.isTraining.value, false)
})
test('history restores only its saved node, preserving missing node ID', async () => {
  const f = fixture([otherGpu]); await f.api.restoreTrainingConfig({ configParams: JSON.stringify({ computeNodeId: gpu.id, modelSource: 'algorithm' }) })
  assert.equal(f.api.computeNodeId.value, gpu.id); assert.equal(f.api.selectedCompute.value, undefined); assert.equal(f.api.cloudModelSource.value, 'algorithm')
})
test('legacy history without node requires explicit selection', async () => {
  const f = fixture(); await f.api.restoreTrainingConfig({ configParams: '{}' })
  assert.equal(f.api.computeNodeId.value, ''); assert.equal(f.api.selectedCompute.value, undefined)
})
test('malformed legacy configuration requires explicit selection', async () => {
  const f = fixture(); await f.api.restoreTrainingConfig({ configParams: '{bad' }); assert.equal(f.api.computeNodeId.value, '')
})
test('unsafe numeric history ID is not rounded into a different GPU', async () => {
  const f = fixture(); await f.api.restoreTrainingConfig({ configParams: { computeNodeId: Number(gpu.id) } }); assert.equal(f.api.computeNodeId.value, '')
})
test('string history ID remains exact and safe small numeric IDs remain compatible', () => {
  const f = fixture(); assert.equal(f.api.normalizeComputeNodeId(gpu.id), gpu.id); assert.equal(f.api.normalizeComputeNodeId(173), '173')
})
test('switching away during exclusive navigation prompt cancels old redirect', async () => {
  const f = fixture([{ ...gpu, requiresClaim: true, claimState: 'FREE' }]); await f.api.loadComputeNodes()
  const confirm = deferred(); f.context.ElMessageBox.confirm = () => confirm.promise
  const pending = f.api.selectCompute(gpu.id); f.api.computeNodeId.value = otherGpu.id; confirm.resolve(); await pending
  assert.equal(f.calls.routes.length, 0)
})
test('unmounted view discards pending inventory', async () => {
  const f = fixture(); const response = deferred(); f.context.listComputeSelection = () => response.promise
  const pending = f.api.loadComputeNodes(); f.api.invalidate(); response.resolve({ data: [gpu] }); await pending
  assert.equal(f.api.computeNodes.value.length, 0)
})
for (const [name, run] of tests) { await run(); console.log(`PASS ${name}`) }
console.log(`${tests.length} training GPU selection checks passed; no external requests or jobs.`)
