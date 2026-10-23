import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse, compileScript } from 'vue/compiler-sfc'

// Exercise the actual SFC setup with controlled SDK callbacks, without credentials,
// HTTP requests, device writes or a second implementation of the map component.
const source = readFileSync(new URL('../src/components/MapGaoDe/index.vue', import.meta.url), 'utf8')
const compiled = compileScript(parse(source).descriptor, { id: 'map-picker-test' }).content
const executable = compiled.replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm, '')
  .replace('export default', 'return')
const createComponent = new Function('__deps', `
  const {ref,getCurrentInstance,nextTick,onMounted,onBeforeUnmount,loadAMap,getConfigKey,AMapUI,AMap}=__deps;
  ${executable}
`)
const defer = () => {
  let resolve, reject
  const promise = new Promise((a, b) => { resolve = a; reject = b })
  return { promise, resolve, reject }
}

function harness(options = {}) {
  const maps = [], geocodes = [], uiCallbacks = [], mounts = [], unmounts = [], emitted = []
  class Map {
    constructor(container, settings) {
      assert.equal(container.isConnected, true)
      this.settings = settings; this.handlers = {}; this.destroyed = false; this.controls = 0
      maps.push(this)
    }
    addControl() { assert.equal(this.destroyed, false); this.controls++ }
    add() { assert.equal(this.destroyed, false) }
    clearMap() { assert.equal(this.destroyed, false) }
    setCenter(value) { this.center = value }
    setZoom() {}
    on(name, callback) { this.handlers[name] = callback }
    destroy() { assert.equal(this.destroyed, false); this.destroyed = true }
  }
  const SDK = {
    Map, Scale: class {}, HawkEye: class {}, Marker: class { on() {} },
    Geocoder: class { getAddress(position, callback) { geocodes.push({ position, callback }) } },
    AutoComplete: class { on() {} },
    Geolocation: class { getCurrentPosition() { assert.fail('valid coordinates must not request browser location') } },
  }
  let loader = options.loader || (() => Promise.resolve(SDK))
  const deps = {
    ref: value => ({ value }), nextTick: () => Promise.resolve(),
    getCurrentInstance: () => ({ proxy: { $modal: { msgWarning() {}, msgError() {} } } }),
    onMounted: callback => mounts.push(callback), onBeforeUnmount: callback => unmounts.push(callback),
    getConfigKey: () => Promise.resolve(options.center ?? { msg: '' }),
    loadAMap: () => loader(SDK), AMap: SDK,
    AMapUI: { loadUI: (_, callback) => uiCallbacks.push(callback) },
  }
  let exposed
  const instance = createComponent(deps).setup({ position: [0, 0], toponym: '已有地址', autoInitialize: false }, {
    expose: value => { exposed = value }, emit: (...args) => emitted.push(args),
  })
  instance.mapContainer.value = { isConnected: true }
  instance.addressInput.value = { input: {} }
  return { instance, exposed, maps, geocodes, uiCallbacks, mounts, unmounts, emitted,
    setLoader: value => { loader = value } }
}

const addressResult = text => ({ info: 'OK', regeocode: { formattedAddress: text, addressComponent: {
  province: '省', city: '市', district: '区', township: '街道', street: '路', streetNumber: '1',
} } })

test('opened-driven mode does not initialize on mount; duplicate open initializes once', async () => {
  const h = harness()
  await h.mounts[0]()
  assert.equal(h.maps.length, 0)
  await Promise.all([h.exposed.inGaDeMap(), h.exposed.inGaDeMap()])
  await h.exposed.inGaDeMap()
  assert.equal(h.maps.length, 1)
  assert.deepEqual(h.maps[0].settings.center, [0, 0])
  h.exposed.Destruction()
})

test('cancel before nextTick or SDK completion prevents late map creation', async () => {
  const h = harness()
  const early = h.exposed.inGaDeMap()
  h.exposed.Destruction()
  await early
  assert.equal(h.maps.length, 0)
  const pending = defer()
  h.setLoader(() => pending.promise)
  const opening = h.exposed.inGaDeMap()
  await Promise.resolve(); await Promise.resolve()
  h.exposed.Destruction()
  h.setLoader(SDK => Promise.resolve(SDK))
  await h.exposed.inGaDeMap()
  pending.resolve({})
  await opening
  assert.equal(h.maps.length, 1)
  assert.equal(h.instance.mapLoading.value, false)
  h.exposed.Destruction()
})

test('repeated open/cancel destroys once and ignores old UI/geocode callbacks', async () => {
  const h = harness()
  await h.exposed.inGaDeMap()
  h.exposed.Destruction(); h.exposed.Destruction()
  h.geocodes[0].callback('complete', addressResult('迟到旧地址'))
  h.uiCallbacks[0]({ Zoom: class {}, LayerSwitcher: class {} })
  assert.equal(h.instance.site.value, '')
  await h.exposed.inGaDeMap()
  assert.equal(h.maps.length, 2)
  assert.equal(h.maps[0].destroyed, true)
  assert.equal(h.maps[1].destroyed, false)
  h.unmounts[0]()
  assert.equal(h.maps[1].destroyed, true)
  assert.equal(h.emitted.length, 0)
})

test('SDK failure is visible and retry can initialize successfully', async () => {
  const h = harness({ loader: () => Promise.reject(Error('SDK unavailable')) })
  await h.exposed.inGaDeMap()
  assert.equal(h.maps.length, 0)
  assert.match(h.instance.mapError.value, /地图加载失败/)
  assert.equal(h.instance.mapLoading.value, false)
  h.setLoader(SDK => Promise.resolve(SDK))
  await h.exposed.inGaDeMap()
  assert.equal(h.maps.length, 1)
  assert.equal(h.instance.mapError.value, '')
  h.exposed.Destruction()
})

test('optional missing or malformed map_center cannot leave a blank map', async () => {
  for (const center of [{}, { msg: null }, { msg: '' }, { msg: 'bad' }, { msg: '91,181' }]) {
    const h = harness({ center })
    await h.exposed.inGaDeMap()
    assert.equal(h.maps.length, 1)
    assert.equal(h.instance.mapError.value, '')
    h.exposed.Destruction()
  }
})

test('newer selection wins; cancel never emits a device update', async () => {
  const h = harness()
  await h.exposed.inGaDeMap()
  h.maps[0].handlers.click({ lnglat: { lng: 1, lat: 2 } })
  h.maps[0].handlers.click({ lnglat: { lng: 3, lat: 4 } })
  h.geocodes[2].callback('complete', addressResult('最新选点'))
  h.geocodes[1].callback('complete', addressResult('旧选点'))
  h.geocodes[0].callback('complete', addressResult('初始解析'))
  assert.equal(h.instance.site.value, '最新选点')
  assert.equal(h.instance.MapLng.value, 3)
  assert.equal(h.instance.MapLat.value, 4)
  h.exposed.Destruction()
  assert.equal(h.emitted.length, 0)
})

test('confirmation before initial geocode preserves the saved address and zero coordinates', async () => {
  const h = harness()
  await h.exposed.inGaDeMap()
  h.instance.onConfirm()
  assert.equal(h.emitted.length, 1)
  assert.equal(h.emitted[0][1].address, '已有地址')
  assert.equal(h.emitted[0][1].detailedStreet, '')
  assert.equal(h.emitted[0][1].lng, 0)
  assert.equal(h.emitted[0][1].lat, 0)
  h.exposed.Destruction()
  h.instance.onConfirm()
  assert.equal(h.emitted.length, 1)
})
