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
    DeviceClassificationLayout: {}, SquareProtocolDevicePlayer: {}, CustomAvailabilityBadge: {}, MapGaoDe: {},
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
  instance.locationMap.value = { Destruction() { destroyed++ }, inGaDeMap() { initialized++ } }
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

test('empty geocode never clears an existing address; zero coordinates remain valid', () => {
  const { instance: c } = harness()
  c.openAdd(); c.form.address = '保留已有地址'; c.openLocationPicker()
  c.confirmLocation({ lng: 0, lat: 0, address: '', detailedStreet: '' })
  assert.equal(c.form.address, '保留已有地址')
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

test('editing keeps the existing update path and cannot open or confirm the new picker', async () => {
  const { instance: c, calls } = harness()
  await c.openEdit({ id: 'fixture-existing' })
  const before = { ...c.form }
  c.openLocationPicker(); c.confirmLocation({ lng: 1, lat: 2, address: '拒绝' })
  assert.equal(c.locationVisible.value, false)
  assert.deepEqual({ ...c.form }, before)
  await c.saveDevice()
  assert.equal(calls.find(call => call.name === 'updateCustomDevice').value.address, '原设备地址')
  assert.equal(calls.some(call => call.name === 'addCustomDevice'), false)
})

test('actual shared map rejects failed reverse geocoding without clearing the add form', async () => {
  const { instance: c, calls } = harness()
  c.openAdd(); Object.assign(c.form, { longitude: 10, latitude: 20, address: '已有地址' }); c.openLocationPicker()
  const before = { ...c.form }, geocodes = []
  let click, exposed, warnings = 0
  const SDK = {
    Map: class { addControl() {} add() {} clearMap() {} setCenter() {} setZoom() {} destroy() {}
      on(name, callback) { if (name === 'click') click = callback } },
    Scale: class {}, HawkEye: class {}, Marker: class {}, AutoComplete: class { on() {} },
    Geocoder: class { getAddress(position, callback) { geocodes.push(callback) } },
  }
  const deps = { ref, nextTick, onMounted() {}, onBeforeUnmount() {}, loadAMap: async () => SDK,
    getConfigKey: async () => ({ msg: '' }), AMapUI: { loadUI() {} },
    getCurrentInstance: () => ({ proxy: { $modal: { msgWarning() { warnings++ } } } }),
  }
  const mapSource = readFileSync(new URL('../src/components/MapGaoDe/index.vue', import.meta.url), 'utf8')
  const mapCompiled = compileScript(parse(mapSource).descriptor, { id: 'custom-map-geocode-test' }).content
  const mapExecutable = mapCompiled.replace(/^import[\s\S]*?from\s+['"][^'"]+['"];?\s*$/gm, '').replace('export default', 'return')
  const component = new Function(...Object.keys(deps), mapExecutable)(...Object.values(deps))
  const picker = component.setup({ position: [10, 20], toponym: '已有地址', autoInitialize: false }, {
    expose(value) { exposed = value }, emit(name, value) { c.confirmLocation(value) },
  })
  picker.mapContainer.value = { isConnected: true }; picker.addressInput.value = { input: {} }
  await exposed.inGaDeMap()
  click({ lnglat: { lng: 30, lat: 40 } })
  geocodes.at(-1)('error', { info: 'fixture geocode failure' })
  picker.onConfirm()
  assert.equal(warnings, 1)
  assert.deepEqual({ ...c.form }, before)
  assert.equal(calls.length, 0)
  exposed.Destruction(); c.closeLocationPicker()
})
