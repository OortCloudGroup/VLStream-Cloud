import test from 'node:test'
import assert from 'node:assert/strict'
import { videoCatalogCsv } from '../src/utils/videoSquareExport.mjs'

test('CSV only exports safe visible fields and protects formulas/quotes/newlines', () => {
  const csv = videoCatalogCsv([{ id: 'ONVIF:1', deviceName: '=HYPERLINK("secret")\nline', status: 'ONLINE',
    protocols: ['ONVIF'], address: 'address,"quoted"', streamUrl: 'private-stream-token', password: 'private-password',
    sources: [{ signalingUrl: 'private-signaling' }] }])
  assert.ok(csv.startsWith('\ufeff'))
  assert.ok(csv.includes('"\'=HYPERLINK(""secret"")\nline"'))
  assert.ok(csv.includes('"address,""quoted"""'))
  for (const secret of ['private-stream-token', 'private-password', 'private-signaling']) assert.ok(!csv.includes(secret))
})

test('unknown state stays unknown and export never creates online status', () => {
  const csv = videoCatalogCsv([{ id: 'CUSTOM:2', deviceName: '@formula', status: 'UNCONFIRMED' }])
  assert.ok(csv.includes('未知状态'))
  assert.ok(!csv.includes('"在线"'))
  assert.ok(csv.includes('"\'@formula"'))
})
