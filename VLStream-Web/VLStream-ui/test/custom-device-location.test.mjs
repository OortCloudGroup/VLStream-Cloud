import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { nextTick, reactive, ref } from 'vue'
import { parse, compileScript } from 'vue/compiler-sfc'

// Execute the production SFC with isolated API and map fixtures; never write devices.
const source = readFileSync(new URL('../src/views/VideoAggregation/CustomProtocolDevice.vue', import.meta.url), 'utf8')
const compiled = compileScript(parse(source).descriptor, { id: 'custom-location-test' }).content
const executable = compiled.replace(/^import[\s\S]*?from\s+['"][^'"]+['"];?\s*$/gm, '').replace('export default', 'return')

function harness() {
  const calls = [], unmounts = []
  let destroyed = 0, initialized = 0
  const deps = {
    nextTick, reactive, ref, onMounted() {}, onBeforeUnmount: callback => unmounts.push(callback),
    uiText: text => text, ElMessage: { success() {}, warning() {} }, ElMessageBox: {},
    window: { clearInterval() {}, removeEventListener() {} }, document: { removeEventListener() {} },
    DeviceClassificationLayout: {}, SquareProtocolDevicePlayer: {}, CustomAvailabilityBadge: {}, MapLocation: {},
    stampCustomStatus: row => row, useCustomAvailabilityClock: () => ref(0),
    useWvpMediaStatus: () => ({ mediaAvailable: ref(true), loadMediaStatus() {} }),
  }
  for (const name of ['addCustomDevice', 'updateCustomDevice', 'deleteCustomDevices', 'exportCustomDevices',
    'getCustomDevice', 'getCustomMediaStatus', 'getCustomRecordPlan', 'getCustomRecordStatus',
    'listCustomDevices', 'saveCustomRecordPlan', 'startCustomRecord', 'stopCustomRecord']) {
    deps[name] = async value => { calls.push({ name, value }); return { data: name === 'getCustomDevice' ? { id: value, longitude: 10, latitude: 20, address: '原设备地址' } : {}, rows: [], total: 0 } }
  }
  const component = new Function(...Object.keys(deps), executable)(...Object.values(deps))
  const instance = component.setup({}, { expose() {} })
  instance.formRef.value = { clearValidate() {}, async validate() {} }
  instance.locationMap.value = { Destruction() { destroyed++ }, initialize() { initialized++ } }
  return { instance, calls, unmounts, initialized: () => initialized, destroyed: () => destroyed }
}

test('picker initializes only after opening and confirmation fills unsaved add form without coordinate conversion', () => {
  const h = harness(), c = h.instance
  c.openAdd(); c.openLocationPicker()
  assert.equal(h.initialized(), 0)
  assert.equal(c.locationPosition.value, null)
  c.initializeLocationPicker()
  assert.equal(h.initialized(), 1)
  c.confirmLocation({ lng: 116.3912757, lat: 39.906217, address: '测试省市区', detailedStreet: '测试街道1号' })
  assert.equal(c.form.longitude, 116.3912757)
  assert.equal(c.form.latitude, 39.906217)
  assert.equal(c.form.address, '测试省市区测试街道1号')
  assert.equal(c.locationVisible.value, false)
  assert.equal(h.calls.length, 0)
})

test('map cancel leaves every form field untouched and reopening uses only confirmed coordinates', () => {
  const h = harness(), c = h.instance
  c.openAdd(); Object.assign(c.form, { longitude: 0, latitude: 0, address: '原地址', remark: '保留备注' })
  const before = { ...c.form }
  c.openLocationPicker()
  assert.deepEqual(c.locationPosition.value, [0, 0])
  c.closeLocationPicker()
  assert.deepEqual({ ...c.form }, before)
  c.confirmLocation({ lng: 1, lat: 2, address: '迟到地址' })
  assert.deepEqual({ ...c.form }, before)
  c.openLocationPicker()
  assert.deepEqual(c.locationPosition.value, [0, 0])
})

test('parent close, reset and unmount reject stale confirmation; a new form has no old selection', () => {
  const h = harness(), c = h.instance
  c.openAdd(); c.openLocationPicker(); c.formVisible.value = false
  c.confirmLocation({ lng: 1, lat: 2, address: '旧弹窗' })
  assert.equal(c.form.longitude, undefined)
  c.closeLocationPicker(); c.openAdd()
  assert.equal(c.locationVisible.value, false)
  assert.equal(c.locationPosition.value, null)
  assert.equal(c.form.address, '')
  c.openLocationPicker(); h.unmounts[0]()
  assert.equal(c.locationVisible.value, false)
})

test('confirming a new point with an empty address clears stale text; zero coordinates remain valid', () => {
  const { instance: c } = harness()
  c.openAdd(); c.form.address = '保留已有地址'; c.openLocationPicker()
  c.confirmLocation({ lng: 0, lat: 0, address: '', detailedStreet: '' })
  assert.equal(c.form.address, '')
  assert.equal(c.form.longitude, 0)
  assert.equal(c.form.latitude, 0)
})

test('invalid coordinates cannot fill the form', () => {
  for (const [lng, lat] of [[null, 1], ['', 1], [181, 1], [1, 91], [NaN, 1], [false, 1]]) {
    const { instance: c } = harness()
    c.openAdd(); c.openLocationPicker(); c.confirmLocation({ lng, lat, address: '非法' })
    assert.equal(c.form.longitude, undefined)
    assert.equal(c.form.address, '')
  }
})

test('save uses the existing add API with the selected location and all other fields', async () => {
  const { instance: c, calls } = harness()
  c.openAdd()
  Object.assign(c.form, { deviceName: '隔离夹具', deviceCode: 'fixture-only', streamUrl: 'rtsp://example.invalid/fixture', remark: '不写业务库' })
  c.openLocationPicker(); c.confirmLocation({ lng: 116.3912757, lat: 39.906217, address: '夹具地址', detailedStreet: '1号' })
  const expected = { ...c.form }
  await c.saveDevice()
  const writes = calls.filter(call => ['addCustomDevice', 'updateCustomDevice'].includes(call.name))
  assert.deepEqual(writes, [{ name: 'addCustomDevice', value: expected }])
})

test('editing can select a location but only save writes through the existing update path', async () => {
  const { instance: c, calls } = harness()
  await c.openEdit({ id: 'fixture-existing' })
  c.openLocationPicker()
  assert.deepEqual(c.locationPosition.value, [10, 20])
  c.confirmLocation({ lng: 1, lat: 2, address: '新地址' })
  assert.equal(c.locationVisible.value, false)
  assert.equal(c.form.longitude, 1)
  assert.equal(c.form.latitude, 2)
  assert.equal(calls.some(call => call.name === 'updateCustomDevice'), false)
  await c.saveDevice()
  assert.equal(calls.find(call => call.name === 'updateCustomDevice').value.address, '新地址')
  assert.equal(calls.some(call => call.name === 'addCustomDevice'), false)
})
