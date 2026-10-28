import test from 'node:test'
import assert from 'node:assert/strict'
import { eventLocationView, eventLocationCoordinate } from './eventLocation.mjs'

test('prefers original event address over heartbeat projection in cell and tooltip', () => {
  const view = eventLocationView({ point: { address: '  历史地址  ' }, display_location: { address: '心跳地址' } })
  assert.equal(view.text, '历史地址')
  assert.equal(view.tooltip, '历史地址')
})
test('unresolved valid zero coordinates support lookup without appearing in tooltip', () => {
  const event = { display_location: { longitude: 0, latitude: 0, coordinate_system: 1, status: 'UNAVAILABLE' } }
  assert.deepEqual(eventLocationCoordinate(event), { lng: 0, lat: 0, coordinateSystem: 'WGS84' })
  const view = eventLocationView(event)
  assert.equal(view.text, '—')
  assert.equal(view.tooltip, '—')
})
test('heartbeat metadata is retained in data and omitted from both visible texts', () => {
  const event = { display_location: { address: '模拟地址', source: 'DEVICE_HEARTBEAT',
    longitude: 114.1177, latitude: 22.5543, coordinate_system: 1, heartbeat_at: 1793000000000 } }
  const original = structuredClone(event)
  const view = eventLocationView(event)
  assert.equal(view.text, '模拟地址')
  assert.equal(view.tooltip, '模拟地址')
  assert.deepEqual(event, original)
})
test('missing, stale, unavailable and invalid positions show only dash in cell and tooltip', () => {
  const events = [{}, { point: { lng: 0, lat: 0, coord_system_type: 0 } },
    { display_location: { status: 'STALE' } }, { display_location: { status: 'UNAVAILABLE' } }]
  for (const lng of [null, '', true, Infinity, 181, 'NaN', '经度114.1']) events.push({ point: { lng, lat: 22 } })
  for (const event of events) {
    assert.equal(eventLocationView(event).text, '—')
    assert.equal(eventLocationView(event).tooltip, '—')
  }
})
test('long address remains complete in tooltip without adding rate-limit status', () => {
  const address = '模拟长地址'.repeat(100)
  const view = eventLocationView({ display_location: { address, status: 'RATE_LIMITED' } })
  assert.equal(view.text, address)
  assert.equal(view.tooltip, address)
})

test('stale device address is displayed alone while provenance remains unchanged', () => {
  const address = '广东省深圳市罗湖区东门街道童乐路37号同乐公寓'
  const event = { display_location: { longitude: 114.1177, latitude: 22.5543, address,
    coordinate_system: 1, source: 'DEVICE_HEARTBEAT', stale: true, heartbeat_at: 1793097110000 } }
  const original = structuredClone(event)
  const view = eventLocationView(event)
  assert.equal(view.text, address)
  assert.equal(view.tooltip, address)
  assert.deepEqual(event, original)
})

test('lookup and provider failures never expose coordinates, source, time or status in tooltip', () => {
  for (const status of ['LOOKUP', 'NO_RESULT', 'UNAVAILABLE', 'RATE_LIMITED']) {
    const view = eventLocationView({ display_location: { longitude: 114.1177, latitude: 22.5543,
      coordinate_system: 1, source: 'DEVICE_HEARTBEAT', stale: true, heartbeat_at: 1793097110000, status } })
    assert.equal(view.text, '—')
    assert.equal(view.tooltip, '—')
  }
})
