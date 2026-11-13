import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { parse, compileTemplate } from 'vue/compiler-sfc'
import * as Vue from 'vue'
import { renderToString } from '@vue/server-renderer'

const source = await readFile(new URL('../src/utils/computeMarketPresentation.js', import.meta.url), 'utf8')
const { marketDisplayResources } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))
const specificationSource = await readFile(new URL('../src/utils/computeMarketSpecifications.js', import.meta.url), 'utf8')
const { getMarketSpecifications, formatMarketCpu } = await import('data:text/javascript;base64,' + Buffer.from(specificationSource).toString('base64'))
const historical = Object.freeze({ id: '792022144331380001', provider: 'autodl', configurationManaged: false, configurationCode: null, serverGroup: 'autodl-pro-7920a22db144', billingType: 'POINTS', name: 'RTX 6000D' })
const official = Object.freeze({ id: '-6000', provider: 'autodl', configurationManaged: true, configurationCode: 'official-autodl', billingType: 'POINTS', name: 'RTX 6000D' })
const free = id => Object.freeze({ id, provider: 'self', billingType: 'FREE', name: 'RTX 6000D' })

test('platform specifications update existing and newly managed machine cards', () => {
  const specification = { cpuCount: 48, cpuUnit: 'CORE', cpuModel: 'Platform CPU', memory: '256 GB', cuda: '12.8', recordedAt: '2026-11-13' }
  for (const id of ['-173', '-900']) {
    const result = getMarketSpecifications({ id, gpuModel: 'New GPU', schedulingMode: 'SHARED_GPU_QUEUE', specifications: specification })
    assert.equal(result.cpuCount, 48)
    assert.equal(result.gpuLabel, 'New GPU')
    assert.equal(result.cudaMax, '12.8')
    assert.equal(result.observedDate, '2026-11-13')
    assert.equal(result.shared, true)
  }
})

test('cleared platform specifications keep legacy display facts cleared', () => {
  assert.equal(getMarketSpecifications({ ...official, gpuModel: 'RTX 6000D', specifications: {} }), null)
})

test('platform CPU text is displayed when numeric CPU fields are absent', () => {
  assert.equal(formatMarketCpu({ cpu: '32 vCPU' }), '32 vCPU')
  assert.equal(formatMarketCpu({ cpu: '48 vCPU', cpuCount: 32, cpuUnit: 'CORE', cpuModel: 'Old CPU' }), '48 vCPU')
  assert.equal(formatMarketCpu({}), '')
})

test('platform specification aliases preserve canonical values and older date formats', () => {
  assert.equal(getMarketSpecifications({ specifications: { cuda_max: '12.8', observed_date: '2026-11-13' } }).cudaMax, '12.8')
  assert.equal(getMarketSpecifications({ specifications: { recorded_at: '2026-11-13' } }).observedDate, '2026-11-13')
  const result = getMarketSpecifications({ specifications: { cudaMax: '13.0', cuda: '12.8', observedDate: '2026-11-13', recordedAt: '2026-10-01' } })
  assert.equal(result.cudaMax, '13.0')
  assert.equal(result.observedDate, '2026-11-13')
  const cleared = getMarketSpecifications({ specifications: { cudaMax: '', cuda: '12.8', observedDate: '', recordedAt: '2026-10-01' } })
  assert.equal(cleared.cudaMax, '')
  assert.equal(cleared.observedDate, '')
  const unset = getMarketSpecifications({ specifications: { cudaMax: null, cuda: '12.8', observedDate: null, recordedAt: '2026-10-01' } })
  assert.equal(unset.cudaMax, null)
  assert.equal(unset.observedDate, null)
})

test('historical card remains when the configured official card is absent', () => {
  const rows = [free('-173'), historical]
  assert.equal(marketDisplayResources(rows), rows)
  assert.deepEqual(marketDisplayResources(), [])
})

test('exact historical card is hidden only when its configured replacement is present', () => {
  assert.deepEqual(marketDisplayResources([historical, official]), [official])
})

test('the verified API projection with null server groups hides only the historical card', () => {
  const oldProjection = { ...historical, serverGroup: null }
  const newProjection = { ...official, serverGroup: null, pricePolicy: 'VLS_FIXED', userHourlyPrice: '13.0', priceCurrency: 'CNY', priceUnit: 'PER_INSTANCE_HOUR' }
  assert.deepEqual(marketDisplayResources([free('-173'), oldProjection, free('-99'), newProjection]), [free('-173'), free('-99'), newProjection])
})

test('another resource with the same name and a different ID remains visible', () => {
  const other = { ...historical, id: '792022144331380002' }
  assert.deepEqual(marketDisplayResources([historical, other, official]), [other, official])
})

test('historical identity, paid billing and non-configured status must all match', () => {
  for (const changed of [
    { id: 'different' }, { id: undefined }, { provider: 'aliyun' },
    { provider: undefined }, { serverGroup: 'another-instance' }, { serverGroup: '' },
    { serverGroup: undefined }, { configurationManaged: true }, { configurationManaged: undefined },
    { configurationManaged: 'false' }, { billingType: 'FREE' }, { billingType: undefined }
  ]) {
    const other = { ...historical, ...changed }
    assert.deepEqual(marketDisplayResources([other, official]), [other, official])
  }
})

test('all configured replacement markers are required and names never activate filtering', () => {
  for (const changed of [
    { configurationManaged: false }, { configurationManaged: 'true' },
    { configurationManaged: undefined }, { configurationCode: 'another-official' },
    { configurationCode: undefined }, { provider: 'aliyun' }, { provider: undefined },
    { id: '-6001' }, { id: undefined }
  ]) {
    const other = { ...official, ...changed }
    assert.deepEqual(marketDisplayResources([historical, other]), [historical, other])
  }
})

test('all free resources remain even if a free row shares the historical identity', () => {
  const freeHistorical = { ...historical, billingType: 'FREE' }
  const rows = [free('-173'), free('-99'), freeHistorical, official]
  assert.deepEqual(marketDisplayResources(rows), rows)
})

test('filter preserves input rows, object identities and remaining order', () => {
  const first = free('-173'), last = free('-99')
  const rows = Object.freeze([first, historical, official, last])
  const result = marketDisplayResources(rows)
  assert.deepEqual(rows, [first, historical, official, last])
  assert.deepEqual(result, [first, official, last])
  assert.equal(result[0], first)
  assert.equal(result[1], official)
  assert.equal(result[2], last)
})

test('the exact configured official identity reuses the existing RTX 6000D specification', () => {
  const specification = getMarketSpecifications({ ...historical, gpuModel: 'RTX 6000D 84 GB' })
  assert.ok(specification)
  for (const id of ['-6000', -6000]) {
    assert.equal(getMarketSpecifications({ ...official, id, gpuModel: 'RTX 6000D 84 GB', name: 'Renamed official card' }), specification)
  }
})

test('the verified official API GPU label resolves the catalog without repeating its memory size', () => {
  const resource = { id: '-6000', name: 'OortCloud RTX 6000D', gpuModel: 'RTX 6000D', configurationManaged: true, configurationCode: 'official-autodl', provider: 'autodl', billingType: 'POINTS' }
  const specification = getMarketSpecifications({ ...historical, gpuModel: 'RTX 6000D 84 GB' })
  assert.equal(getMarketSpecifications(resource), specification)
  assert.equal(specification.gpuLabel, 'RTX 6000D / 84 GB')
  assert.equal(getMarketSpecifications({ ...historical, gpuModel: 'RTX 6000D' }), null)
  assert.equal(getMarketSpecifications({ name: 'OortCloud RTX 6000D', gpuModel: 'RTX 6000D', billingType: 'POINTS' }), null)
})

test('a partial official identity or incompatible GPU or billing never receives the specification', () => {
  const resource = { ...official, gpuModel: 'RTX 6000D 84 GB' }
  for (const changed of [
    { configurationManaged: false }, { configurationManaged: 'true' },
    { configurationManaged: undefined }, { configurationCode: 'another-official' },
    { configurationCode: undefined }, { provider: 'aliyun' }, { provider: undefined },
    { id: '-6001' }, { id: undefined }, { gpuModel: 'Tesla P100-PCIE-16GB' },
    { gpuModel: 'RTX 6000D 48 GB' }, { gpuModel: 'RTX 6000D Pro' },
    { billingType: 'FREE' }
  ]) {
    assert.equal(getMarketSpecifications({ ...resource, ...changed }), null)
  }
})

test('the actual market keeps its paid startup entry and explains missing target configuration', async () => {
  const marketSource = await readFile(new URL('../src/views/AIComputingScheduling/ComputeMarket.vue', import.meta.url), 'utf8')
  const compiled = compileTemplate({ source: parse(marketSource).descriptor.template.content, id: 'always-available-market' })
  assert.deepEqual(compiled.errors, [])
  const render = new Function('Vue', compiled.code
    .replace(/^import \{(.*?)\} from "vue"$/gm, (_, names) => `const {${names.replace(/ as /g, ': ')}} = Vue`)
    .replace('export function render', 'function render') + '\nreturn render')(Vue)
  for (const configured of [false, true]) {
    const item = { ...official, claimState: 'FREE', powerState: 'UNKNOWN', paidAdmissionEnabled: configured,
      paidUnavailableReason: configured ? null : '官方算力供应商调用身份未配置' }
    const app = Vue.createSSRApp({ setup: () => ({
      loading: false, route: { query: {} }, resources: [item], pagedResources: [item], page: 1, pageSize: 20,
      marketText: text => text, marketError: '', operating: '', marketResourceName: row => row.name,
      getMarketSpecifications: () => null, marketPricePresentation: () => ({ amount: '13.00', unit: '/整实例小时' }),
      load() {}, claim() {}, release() {}, refresh() {}, health() {}, power() {}, billingPanel: { activeRun: () => null }, router: { push() {} }
    }), render })
    app.component('ComputeBillingPanel', { render: () => Vue.h('section') })
    for (const name of ['el-alert', 'el-empty', 'ArrowRight', 'el-icon', 'el-pagination']) app.component(name, { render: () => Vue.h('span') })
    app.component('el-button', { props: ['disabled'], render() { return Vue.h('button', { disabled: this.disabled }, this.$slots.default?.()) } })
    app.directive('loading', { getSSRProps: () => ({}) })
    const html = await renderToString(app)
    assert.match(html, /付费开机/)
    assert.match(html, /确认报价后按用量结算/)
    assert.doesNotMatch(html, /暂未开放|尚未启用|READ_ONLY|PAID|OFF/)
    if (configured) {
      assert.doesNotMatch(html, /<button[^>]*disabled[^>]*>付费开机/)
      assert.doesNotMatch(html, /官方算力供应商调用身份未配置/)
    } else {
      assert.match(html, /<button[^>]*disabled[^>]*>付费开机/)
      assert.match(html, /官方算力供应商调用身份未配置/)
    }
  }
})
