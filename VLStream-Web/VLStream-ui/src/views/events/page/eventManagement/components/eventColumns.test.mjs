import assert from 'node:assert/strict'
import { test } from 'node:test'
import { defaultEventColumns, normalizeEventColumns, moveEventColumn } from './eventColumns.mjs'

test('saved order and hidden fields survive while obsolete and duplicate fields are removed', () => {
    const columns = normalizeEventColumns([
        { key: 'video', visible: false }, { key: 'device_name' },
        { key: 'video', visible: true }, { key: 'removed' }
    ])
    assert.deepEqual(columns.slice(0, 2).map(({ key, visible }) => [key, visible]), [['video', false], ['device_name', true]])
    assert.equal(columns.length, defaultEventColumns.length)
    assert.equal(new Set(columns.map(column => column.key)).size, columns.length)
    assert.equal(columns.at(-1).key, 'finish_at')
})

test('invalid saved settings and all-hidden settings always leave a usable table', () => {
    for (const saved of [null, {}, 'bad', [null], defaultEventColumns.map(column => ({ key: column.key, visible: false }))]) {
        const columns = normalizeEventColumns(saved)
        assert.equal(columns.length, defaultEventColumns.length)
        assert.ok(columns.some(column => column.visible))
    }
})

test('moving in either direction preserves visibility and does not mutate the original/defaults', () => {
    const original = normalizeEventColumns([{ key: 'video', visible: false }])
    const moved = moveEventColumn(original, 'device_name', 'video')
    assert.equal(moved[0].key, 'device_name')
    assert.equal(original[0].key, 'video')
    assert.equal(moved.find(column => column.key === 'video').visible, false)
    assert.equal(moveEventColumn(moved, 'device_name', 'finish_at').at(-1).key, 'device_name')
    assert.deepEqual(moveEventColumn(original, 'unknown', 'video'), original)
    assert.deepEqual(normalizeEventColumns().map(column => column.key), defaultEventColumns.map(column => column.key))
})
