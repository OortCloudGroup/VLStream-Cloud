import test from 'node:test'
import assert from 'node:assert/strict'
import { categorySubtreeIds, createClassificationReader } from '../src/utils/videoSquareClassification.mjs'

test('a parent category includes descendants but no siblings', () => {
  const tree = [{ id: 1, children: [{ id: 2, children: [{ id: 3 }] }] }, { id: 4 }]
  assert.deepEqual([...categorySubtreeIds(tree, '1')], ['1', '2', '3'])
  assert.deepEqual([...categorySubtreeIds(tree, 9)], [])
})

test('equal row IDs across protocols cannot share classification accidentally', async () => {
  const devices = [
    { id: 'ONVIF:1', sources: [{ protocol: 'ONVIF', rowId: 1 }] },
    { id: 'RTSP:1', sources: [{ protocol: 'RTSP', rowId: 1 }] },
  ]
  const calls = []
  const reader = createClassificationReader(async (protocol, rowId) => {
    calls.push([protocol, rowId])
    return { regionId: protocol === 'ONVIF' ? 5 : 6 }
  })
  assert.deepEqual(await reader.matches(devices, 'REGION', new Set(['5'])), ['ONVIF:1'])
  assert.deepEqual(await reader.matches(devices, 'REGION', new Set(['6'])), ['RTSP:1'])
  assert.deepEqual(calls, [['ONVIF', '1'], ['RTSP', '1']])
})

test('deduplicates source reads, bounds concurrency, supports multi-source devices', async () => {
  let concurrent = 0, max = 0, calls = 0
  const reader = createClassificationReader(async () => {
    calls++; max = Math.max(max, ++concurrent)
    await new Promise(resolve => setTimeout(resolve, 2))
    concurrent--
    return { tagIds: [8] }
  })
  const devices = Array.from({ length: 12 }, (_, id) => ({ id: `device${id}`, sources: [{ protocol: 'ONVIF', rowId: id }] }))
  devices[0].sources.push({ protocol: 'ONVIF', rowId: 1 })
  assert.equal((await reader.matches(devices, 'TAG', new Set(['8']))).length, 12)
  assert.equal(calls, 12)
  assert.ok(max <= 4)
})

test('a stale result is discarded; a failed read rejects and may be retried', async () => {
  let current = true, fail = true
  const devices = [{ id: 'ONVIF:1', sources: [{ protocol: 'ONVIF', rowId: 1 }] }]
  const staleReader = createClassificationReader(async () => { current = false; return { groupId: 1 } })
  assert.deepEqual(await staleReader.matches(devices, 'GROUP', new Set(['1']), () => current), [])
  const reader = createClassificationReader(async () => { if (fail) throw Error('denied'); return { groupId: 1 } })
  await assert.rejects(reader.matches(devices, 'GROUP', new Set(['1'])), /denied/)
  fail = false
  assert.deepEqual(await reader.matches(devices, 'GROUP', new Set(['1'])), ['ONVIF:1'])
})
