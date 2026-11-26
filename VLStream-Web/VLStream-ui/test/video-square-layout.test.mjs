import test from 'node:test'
import assert from 'node:assert/strict'
import { videoGridStyle, assignVideoWindow, resizeVideoLayout, fillLayoutWindows, resolveLayoutSpec, MAX_VIDEO_WINDOWS } from '../src/utils/videoSquareLayout.mjs'

test('all supported window counts fit in finite nonzero grid tracks', () => {
  for (let count = 1; count <= MAX_VIDEO_WINDOWS; count++) {
    const style = videoGridStyle(count)
    const columns = Number(style.gridTemplateColumns.match(/repeat\((\d+)/)[1])
    const rows = Number(style.gridTemplateRows.match(/repeat\((\d+)/)[1])
    assert.ok(columns * rows >= count && columns > 0 && rows > 0)
    assert.ok(columns * (rows - 1) < count)
  }
  for (const count of [0, 82, 2.5, NaN, '4']) assert.throws(() => videoGridStyle(count), RangeError)
})

test('custom grids place equal windows in row order regardless of the preset with the same count', () => {
  const dialog = { id: 1, cameras: [] }
  resizeVideoLayout(dialog, 6, 'custom-2x3')
  const spec = resolveLayoutSpec(dialog.layoutCount, dialog.layoutVariant)

  assert.equal(dialog.cameras.length, 6)
  assert.equal(spec.rows, 2)
  assert.equal(spec.cols, 3)
  assert.deepEqual(spec.cells.map(({row, col}) => [row, col]), [[0, 0], [0, 1], [0, 2], [1, 0], [1, 1], [1, 2]])
  assert.ok(spec.cells.every(({rowSpan, colSpan}) => rowSpan === 1 && colSpan === 1))
  assert.equal(resolveLayoutSpec(6, 'custom-3x2').cols, 2)
  assert.equal(resolveLayoutSpec(6).rows, 3)
})

test('custom grids allow both dimensions from 1 to 9 and retain their rectangular cell positions', () => {
  assert.equal(resolveLayoutSpec(9, 'custom-1x9').cells.length, 9)
  assert.equal(resolveLayoutSpec(27, 'custom-3x9').rows, 3)
  assert.equal(resolveLayoutSpec(27, 'custom-3x9').cols, 9)
  const full = resolveLayoutSpec(81, 'custom-9x9')
  assert.equal(full.cells.length, 81)
  assert.deepEqual(full.cells[80], { row: 8, col: 8, colSpan: 1, rowSpan: 1 })
  for (const [count, variant] of [[90, 'custom-10x9'], [10, 'custom-1x10'], [1, 'custom-0x1'], [80, 'custom-9x9']]) {
    assert.throws(() => videoGridStyle(count, variant), RangeError)
  }
})

test('assign changes only the selected window and retains the real catalog source', () => {
  const device = { id: 'ONVIF:8', deviceName: 'real device', catalogSource: 'VIDEO_SQUARE', sources: [{ protocol: 'ONVIF', rowId: 8 }] }
  const original = { id: 'existing' }
  const dialogs = [{ id: 10, layoutCount: 2, cameras: [original, null] }]
  assert.equal(assignVideoWindow(dialogs, { dialogId: 10, windowIndex: 1 }, device, [device]), true)
  assert.equal(dialogs[0].cameras[0], original)
  assert.equal(dialogs[0].cameras[1].deviceData.catalogSource, 'VIDEO_SQUARE')
  assert.equal(dialogs[0].cameras[1].deviceData.sources, device.sources)
  assert.notEqual(dialogs[0].cameras[1].deviceData, device)
})

test('stale dialog, invalid window and removed authorization cannot assign a device', () => {
  const device = { id: 'ONVIF:8' }
  const dialogs = [{ id: 10, layoutCount: 1, cameras: [null] }]
  for (const target of [null, { dialogId: 9, windowIndex: 0 }, { dialogId: 10, windowIndex: -1 }, { dialogId: 10, windowIndex: 1 }, { dialogId: 10, windowIndex: .5 }]) {
    assert.equal(assignVideoWindow(dialogs, target, device, [device]), false)
  }
  assert.equal(assignVideoWindow(dialogs, { dialogId: 10, windowIndex: 0 }, device, []), false)
  assert.deepEqual(dialogs[0].cameras, [null])
})

test('resizing preserves existing camera objects, fills only new slots and validates variants', () => {
  const camera = { id: 'selected', deviceData: { sources: [{ protocol: 'ONVIF', rowId: 7 }] } }
  const dialog = { id: 9, layoutCount: 1, cameras: [camera] }
  resizeVideoLayout(dialog, 3, 'three-left')
  assert.equal(dialog.id, 9)
  assert.equal(dialog.cameras[0], camera)
  assert.deepEqual(dialog.cameras.slice(1), [null, null])
  assert.equal(dialog.layoutVariant, 'three-left')
  resizeVideoLayout(dialog, 2, 'two-overlay')
  assert.equal(dialog.cameras.length, 2)
  assert.equal(dialog.cameras[0], camera)
  assert.throws(() => resizeVideoLayout(dialog, 2, 'three-top'), RangeError)
  assert.equal(dialog.layoutCount, 2)
  assert.throws(() => videoGridStyle(3, 'unsupported'), RangeError)
})

test('bottom default keeps playing cameras and fills empty slots with online devices first', () => {
  const playing = { id: 'playing' }
  const dialog = { id: 1, layoutCount: 1, cameras: [playing], layoutVariant: 'grid' }
  const devices = [
    { id: 'offline', deviceName: 'offline cam', status: 'OFFLINE' },
    { id: 'online-1', deviceName: 'online 1', status: 'ONLINE' },
    { id: 'online-2', deviceName: 'online 2', status: 'ONLINE' },
  ]
  assert.equal(fillLayoutWindows(dialog, devices, 4), 3)
  assert.equal(dialog.layoutCount, 4)
  assert.equal(dialog.cameras[0], playing)
  assert.deepEqual(dialog.cameras.slice(1).map(camera => camera.id), ['online-1', 'online-2', 'offline'])
  assert.notEqual(dialog.cameras[1].deviceData, devices[1])
})

test('an empty bottom layout shrinks to the default count and never reuses a device', () => {
  const dialog = { id: 2, layoutCount: 9, cameras: Array.from({ length: 9 }, () => null), layoutVariant: 'grid' }
  const devices = [{ id: 'a', deviceName: 'A', status: 'OFFLINE' }, { id: 'b', deviceName: 'B', status: 'ONLINE' }]
  assert.equal(fillLayoutWindows(dialog, devices, 4), 2)
  assert.equal(dialog.layoutCount, 4)
  assert.deepEqual(dialog.cameras.map(camera => camera?.id || null), ['b', 'a', null, null])
  const filled = { id: 3, layoutCount: 6, cameras: [null, { id: 'b' }, null, null, null, null], layoutVariant: 'grid' }
  assert.equal(fillLayoutWindows(filled, devices, 4), 1)
  assert.equal(filled.layoutCount, 6)
  assert.equal(filled.cameras[0].id, 'a')
  assert.equal(filled.cameras[1].id, 'b')
})
