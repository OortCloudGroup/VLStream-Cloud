import assert from 'node:assert/strict'
import { isFreeSharedCompute, queuePresentation, trainingComputeGroups, computeAvailabilityLabel, computeFixedPriceLabel } from '../VLStream-Web/VLStream-ui/src/utils/trainingComputeQueue.mjs'

const free = { id: '-99', scope: 'PLATFORM', billingType: 'FREE', schedulingMode: 'SHARED_GPU_QUEUE' }
const paid = { id: '-1', scope: 'PLATFORM', billingType: 'POINTS' }
const own = { id: '2', scope: 'PRIVATE', billingType: 'FREE' }
const now = 1800000000000
const snapshot = { state: 'KNOWN', waitingCount: 15, checkedAt: now, activePhase: 'RUNNING', activeTaskName: '未带安全帽', estimatedWaitSeconds: null }
let checks = 0
function test(name, fn) { fn(); checks++; console.log(`PASS ${name}`) }
test('free company pool, paid market and own nodes keep their real IDs', () => {
  const groups = trainingComputeGroups([paid, own, free])
  assert.deepEqual(groups.map(group => group.title), ['OortCloud算力Free', '算力市场', '我的算力'])
  assert.equal(groups[0].nodes[0], free); assert.equal(groups[1].nodes[0], paid); assert.equal(groups[2].nodes[0], own)
  assert.equal(isFreeSharedCompute(own), false)
})
test('empty market retains its title without inventing a resource', () => {
  assert.deepEqual(trainingComputeGroups([free]).map(group => [group.title, group.nodes.length]), [['OortCloud算力Free', 1], ['算力市场', 0]])
})
test('official configuration does not make a paid GPU free or invent a quote', () => {
  const official = { ...paid, configurationManaged: true, userHourlyPrice: '13', priceCurrency: 'CNY', priceUnit: 'PER_INSTANCE_HOUR', pricePolicy: 'VLS_FIXED' }
  assert.equal(trainingComputeGroups([free, official])[1].nodes[0], official)
  assert.equal(computeFixedPriceLabel(official), '固定价 13 元 / 整实例小时')
  for (const missing of ['userHourlyPrice', 'priceCurrency', 'priceUnit', 'pricePolicy']) assert.equal(computeFixedPriceLabel({ ...official, [missing]: null }), '收费 · 价格待确认')
  assert.equal(computeFixedPriceLabel({ ...paid, pointsPerHour: 13 }), '收费 · 价格待确认')
  assert.equal(computeFixedPriceLabel({ ...official, userHourlyPrice: '0' }), '收费 · 价格待确认')
})
test('busy task, registered waiting count and unknown ETA', () => {
  const view = queuePresentation(snapshot, { now })
  assert.equal(view.phase, '未带安全帽 · 正在训练'); assert.equal(view.queue, '15 个任务排队中'); assert.equal(view.eta, '预计等待：待估算')
})
test('redacted task uses generic occupancy', () => {
  assert.equal(queuePresentation({ ...snapshot, activeTaskName: null }, { now }).phase, '共享任务 · 正在训练')
})
test('archive remains occupied and is not another waiting task', () => {
  const view = queuePresentation({ ...snapshot, activePhase: 'ARCHIVING', waitingCount: 0 }, { now })
  assert.equal(view.phase, '未带安全帽 · 模型归档中'); assert.equal(view.queue, '暂无排队任务'); assert.equal(view.eta, '预计等待：待估算')
})
test('empty registered queue does not claim GPU availability', () => {
  const view = queuePresentation({ ...snapshot, activePhase: null, waitingCount: 0 }, { now })
  assert.equal(view.phase, '暂无共享执行任务'); assert.equal(view.eta, '预计等待：当前无共享任务')
})
test('waiting without an active lease is not idle', () => {
  assert.equal(queuePresentation({ ...snapshot, activePhase: null }, { now }).eta, '预计等待：待估算')
})
test('stale future missing malformed and failed snapshots never show idle or zero', () => {
  for (const bad of [null, {}, { ...snapshot, state: 'UNKNOWN' }, { ...snapshot, checkedAt: now - 30001 }, { ...snapshot, checkedAt: now + 5001 },
    { ...snapshot, waitingCount: '15' }, { ...snapshot, waitingCount: -1 }, { ...snapshot, waitingCount: 50001 }, { ...snapshot, activePhase: 'DONE' }]) {
    const view = queuePresentation(bad, { now }); assert.equal(view.known, false); assert.equal(view.queue, '排队数未知'); assert.equal(view.eta, '预计等待：待估算')
  }
})
test('loading suppresses old queue evidence', () => {
  assert.equal(queuePresentation(snapshot, { now, loading: true }).queue, '排队数读取中')
})
test('only a positive numeric ETA can become an estimate', () => {
  const supported = { ...snapshot, estimationBasis: 'MATCHED_OBSERVED_LEASE_P75', estimationSampleCount: 3 }
  assert.equal(queuePresentation({ ...supported, estimatedWaitSeconds: 3600 }, { now }).eta, '预计等待：约 1 小时')
  assert.equal(queuePresentation({ ...supported, estimatedWaitSeconds: 61 }, { now }).eta, '预计等待：约 2 分钟')
  assert.equal(queuePresentation({ ...supported, estimatedWaitSeconds: 3600 }, { now }).estimateNote, '参考至少 3 个同配置成功任务')
  assert.equal(queuePresentation({ ...supported, estimationSampleCount: 2, estimatedWaitSeconds: 3600 }, { now }).eta, '预计等待：待估算')
  assert.equal(queuePresentation({ ...snapshot, estimatedWaitSeconds: 3600 }, { now }).eta, '预计等待：待估算')
  assert.equal(queuePresentation({ ...snapshot, estimatedWaitSeconds: '3600' }, { now }).eta, '预计等待：待估算')
})
test('offline unknown error maintenance and ready statuses remain distinct', () => {
  assert.equal(computeAvailabilityLabel({ ...free, powerState: 'STOPPED', schedulable: true }), '已关机')
  assert.equal(computeAvailabilityLabel({ ...free, healthState: 'UNKNOWN' }), '尚未就绪')
  assert.equal(computeAvailabilityLabel({ ...free, healthState: 'ERROR' }), '环境异常')
  assert.equal(computeAvailabilityLabel({ ...free, operationPending: true }), '状态切换中')
  assert.equal(computeAvailabilityLabel({ ...free, schedulable: true }), '可提交排队')
})
console.log(`${checks} queue presentation tests passed; no network or training operations`)
