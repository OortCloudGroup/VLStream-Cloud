import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizeVideoSquarePreferences, readVideoSquarePreferences, saveVideoSquarePreferences } from '../src/utils/videoSquareWorkspacePreferences.mjs'

test('each view and presentation restores independently and saves only display preferences', () => {
    const storage = new Map()
    const adapter = {getItem:key => storage.get(key), setItem:(key,value) => storage.set(key,value)}
    for (const view of ['video','map','list']) for (const presentation of ['default','bottom','grid']) {
        saveVideoSquarePreferences({view, presentation, layout:{count:4,variant:'grid'}, device:'private-source', position:{x:100}}, adapter)
        assert.deepEqual(readVideoSquarePreferences(adapter), {view,presentation,layout:{count:4,variant:'grid'},closed:false})
        assert.ok(![...storage.values()][0].includes('private-source'))
        assert.ok(![...storage.values()][0].includes('position'))
    }
})

test('invalid layout combinations, malformed storage and blocked storage remain usable', () => {
    assert.deepEqual(normalizeVideoSquarePreferences({view:'unknown',presentation:'unknown',layout:{count:26,variant:'grid'}}), {view:'video',presentation:'default',layout:{count:1,variant:'grid'},closed:false})
    assert.equal(normalizeVideoSquarePreferences({layout:{count:4,variant:'two-overlay'}}).layout.count, 1)
    assert.equal(readVideoSquarePreferences({getItem:()=>'{broken'}).view, 'video')
    const blocked = {getItem:()=>{throw Error('blocked')},setItem:()=>{throw Error('blocked')}}
    assert.equal(readVideoSquarePreferences(blocked).presentation, 'default')
    assert.doesNotThrow(()=>saveVideoSquarePreferences({view:'map'}, blocked))
})


test('explicitly closed is persisted as display state without reducing the saved layout to an invalid count',()=>{
    let saved
    const store={setItem:(key,value)=>{saved=value},getItem:()=>saved}
    saveVideoSquarePreferences({view:'map',presentation:'bottom',layout:{count:8,variant:'grid'},closed:true,device:'not saved'},store)
    assert.deepEqual(readVideoSquarePreferences(store),{view:'map',presentation:'bottom',layout:{count:8,variant:'grid'},closed:true})
    assert.ok(!saved.includes('not saved'))
})
