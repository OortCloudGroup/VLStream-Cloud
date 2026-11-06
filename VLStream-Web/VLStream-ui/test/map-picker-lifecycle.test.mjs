import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse, compileScript } from 'vue/compiler-sfc'
import { ref, nextTick } from 'vue'

const source = readFileSync(new URL('../src/components/MapLocation/index.vue', import.meta.url), 'utf8')
const compiled = compileScript(parse(source).descriptor, { id: 'map-picker-test' }).content
const executable = compiled.replace(/^import[^\n]*\n/gm, '').replaceAll('import.meta.env.VITE_OSM_TILE_URL', 'undefined').replace('export default', 'return')
function harness(position = [0, 0]) {
  const maps = [], requests = [], emitted = [], mounts = [], unmounts = []
  const deps = {
    ref, nextTick, uiText: value => value, hasGeocoding: true,
    onMounted: fn => mounts.push(fn), onBeforeUnmount: fn => unmounts.push(fn),
    ElMessage: { warning() {}, info() {} }, ResizeObserver: class { observe() {} disconnect() {} },
    reverseGeocode: () => new Promise((resolve, reject) => requests.push({ resolve, reject })),
    searchAddress: async () => [{ lon: '10', lat: '20', display_name: '搜索地址' }],
    L: {
      map() { const map = { handlers: {}, setView() { return this }, on(name, fn) { this.handlers[name] = fn; return this }, invalidateSize() {}, remove() { this.removed = true } }; maps.push(map); return map },
      tileLayer() { return { on() { return this }, addTo() { return this } } },
      control: { scale() { return { addTo() {} } } }, divIcon: value => value,
      marker() { return { addTo() { return this }, on() {}, setLatLng() {} } },
    },
  }
  let exposed
  const component = new Function(...Object.keys(deps), executable)(...Object.values(deps))
  const instance = component.setup({ position, toponym: '已有地址', autoInitialize: false }, {
    expose: value => { exposed = value }, emit: (...args) => emitted.push(args),
  })
  instance.container.value = { isConnected: true }
  return { instance, exposed, maps, requests, emitted, mounts, unmounts }
}
test('opened-driven mode initializes once and preserves zero coordinates', async () => {
  const h = harness(); h.mounts[0]()
  assert.equal(h.maps.length, 0)
  await Promise.all([h.exposed.initialize(), h.exposed.initialize()])
  assert.equal(h.maps.length, 1)
  h.instance.confirm()
  assert.deepEqual(h.emitted[0][1], { show: false, lng: 0, lat: 0, address: '已有地址', detailedStreet: '' })
})
test('cancel before nextTick prevents late map creation', async () => {
  const h = harness(), opening = h.exposed.initialize()
  h.exposed.destroy(); await opening
  assert.equal(h.maps.length, 0)
})
test('unknown location is not fabricated; failed geocode allows manual address and saving', async () => {
  const h = harness(null)
  await h.exposed.initialize(); h.instance.confirm()
  assert.equal(h.emitted.length, 0)
  h.maps[0].handlers.click({ latlng: { lng: 10, lat: 20 } })
  h.requests[0].reject(Error('unavailable')); await nextTick(); await nextTick()
  h.instance.address.value = '手填地址'; h.instance.confirm()
  assert.equal(h.emitted[0][1].address, '手填地址')
  assert.equal(h.emitted[0][1].lng, 10)
})
test('late queries cannot overwrite a newer selection or reopened map', async () => {
  const h = harness(); await h.exposed.initialize()
  h.instance.selectPosition([1, 2]); h.instance.selectPosition([3, 4])
  h.requests[1].resolve('新地址'); await nextTick()
  h.requests[0].resolve('旧地址'); await nextTick()
  assert.equal(h.instance.address.value, '新地址')
  h.instance.selectPosition([5, 6]); h.exposed.destroy(); h.exposed.destroy()
  await h.exposed.initialize()
  h.requests[2].resolve('过期地址'); await nextTick()
  assert.equal(h.instance.address.value, '已有地址')
  assert.equal(h.maps[0].removed, true)
  h.unmounts[0](); h.instance.confirm()
  assert.equal(h.emitted.length, 0)
})
test('search results change location only after choosing a result', async () => {
  const h = harness(null); await h.exposed.initialize()
  h.instance.query.value = '测试地点'; await h.instance.locate()
  assert.equal(h.instance.selected.value, null)
  h.instance.chooseResult(h.instance.results.value[0]); h.instance.confirm()
  assert.equal(h.emitted[0][1].address, '搜索地址')
  assert.equal(h.emitted[0][1].lat, 20)
})
