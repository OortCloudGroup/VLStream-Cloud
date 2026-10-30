import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = await readFile(new URL('../src/utils/computeMarketPresentation.js', import.meta.url), 'utf8')
const { marketDisplayResources } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))
const specificationSource = await readFile(new URL('../src/utils/computeMarketSpecifications.js', import.meta.url), 'utf8')
const { getMarketSpecifications } = await import('data:text/javascript;base64,' + Buffer.from(specificationSource).toString('base64'))
const historical = Object.freeze({ id: '792022144331380001', provider: 'autodl', configurationManaged: false, configurationCode: null, serverGroup: 'autodl-pro-7920a22db144', billingType: 'POINTS', name: 'RTX 6000D' })
const official = Object.freeze({ id: '-6000', provider: 'autodl', configurationManaged: true, configurationCode: 'official-autodl', billingType: 'POINTS', name: 'RTX 6000D' })
const free = id => Object.freeze({ id, provider: 'self', billingType: 'FREE', name: 'RTX 6000D' })

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
