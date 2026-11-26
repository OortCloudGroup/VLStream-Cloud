import test from 'node:test'
import assert from 'node:assert/strict'
import { buildingGeometry, codeTail, readSceneUpdates, repairPrompt, restoredMessages, sceneDeviceIds, sceneFollowUps, sceneSamples, sceneStarters, topologyLayout }
    from '../src/utils/videoSquareScenes.mjs'

test('renders UTF-8 preview records as they arrive and only accepts a saved terminal result', async () => {
    let writer
    const body = new ReadableStream({ start(controller) { writer = controller } })
    const bytes = new TextEncoder().encode('{"type":"preview","scene":{"title":"设备"}}\n')
    let seen
    const preview = new Promise(resolve => { seen = resolve })
    const result = readSceneUpdates(body, seen)
    for (const byte of bytes) writer.enqueue(Uint8Array.of(byte))
    assert.equal((await preview).title, '设备')
    writer.enqueue(new TextEncoder().encode('{"type":"saved","value":{"revision":2,"scene":{"title":"保存"}}}\n'))
    writer.close()
    assert.equal((await result).revision, 2)
    await assert.rejects(readSceneUpdates(new Response('{"type":"sav').body, () => {}), { code: 'SCENE_STREAM_INTERRUPTED' })
    const uncertain = new Response('{"type":"error","code":500,"message":"请重新载入","reload":true}\n')
    await assert.rejects(readSceneUpdates(uncertain.body, () => {}), { code: 500, reload: true })
    for (const payload of ['{"type":"started"}\n', '{"type":"error","code":429,"message":"模型受限"}\n']) {
        const stream = new Response(payload).body
        await assert.rejects(readSceneUpdates(stream, () => {}), /中断|模型受限/)
    }
})

test('prompt and previous scene prioritize only devices still in the filtered authorized directory', () => {
    const devices = Array.from({ length: 110 }, (_, i) => ({ id: 'd' + i, deviceName: '摄像头[' + i + ']' }))
    const previous = { buildings: [{ floors: [{ rooms: [{ cameras: [{ deviceId: 'revoked' }, { deviceId: 'd95' }] }] }] }] }
    const ids = sceneDeviceIds(devices, '把摄像头[100]放入大厅', previous)
    assert.deepEqual(ids.slice(0, 2), ['d100', 'd95'])
    assert.equal(ids.length, 8, 'a page binds a handful of cameras unless all are requested')
    assert.ok(!ids.includes('revoked'))
    assert.equal(sceneDeviceIds(devices, '展示全部摄像头', previous).length, 80)
    const mixed = [{ id: 'off', status: 'OFFLINE' }, { id: 'unknown' }, { id: 'on', status: 'ONLINE' }]
    assert.deepEqual(sceneDeviceIds(mixed, '链路图', null).slice(0, 3), ['on', 'unknown', 'off'])
    const generated = { cameras: { c1: 'd90', c2: 'revoked', c3: 'd95' } }
    const afterEdit = sceneDeviceIds(devices, '突出摄像头[100]', generated)
    assert.deepEqual(afterEdit.slice(0, 3), ['d100', 'd90', 'd95'])
    assert.ok(!afterEdit.includes('revoked'))
})
test('topology preserves lanes and supports forward, backward and vertical connections', () => {
    const sample = sceneSamples.topology
    const graph = topologyLayout({ ...sample, edges: [...sample.edges, { from: 'west', to: 'east' }, { from: 'screen', to: 'west' }] })
    assert.equal(graph.nodes[0].x, graph.nodes[1].x)
    assert.ok(graph.nodes[0].y < graph.nodes[1].y && graph.nodes[1].x < graph.nodes[2].x)
    assert.equal(graph.edges.length, 6)
    assert.ok(graph.edges.every(path => !/NaN|undefined/.test(path)))
})
test('changing a BIM floor changes the rendered rooms and preserves camera locations and binding', () => {
    const building = structuredClone(sceneSamples.bim.buildings[0])
    building.floors[1].rooms[0].cameras[0].deviceId = 'ONVIF:7'
    const overview = buildingGeometry(building, null)
    const level = buildingGeometry(building, 1)
    assert.equal(overview.layers.length, 3)
    assert.equal(level.layers.length, 1)
    const tableAndChairs = level.layers[0].rooms[0].fixtures
    assert.equal(tableAndChairs.filter(fixture => fixture.tone === 'upholstery').length, 6)
    assert.ok(tableAndChairs.every(fixture => [fixture.top, fixture.front, fixture.side].every(points => points.split(' ').length === 4 && !points.includes('NaN'))))
    const pin = level.layers[0].rooms[0].pins[0]
    assert.equal(pin.location, '办公楼 / 2F / 会议室')
    assert.equal(pin.deviceId, 'ONVIF:7')
    assert.notEqual(level.viewBox, overview.viewBox)
})

test('the conversation is rebuilt from saved history, including undone turns', () => {
    const restored = restoredMessages({ revision: 3, prompt: '三栋楼', scene: { title: '园区', log: [
        { kind: 'generate', prompt: '三栋楼', title: '园区', edit: 'full', model: 'glm' },
        { kind: 'generate', prompt: '加二层剖面', title: '园区剖面', edit: 'patch', edits: 2 },
        { kind: 'undo', title: '园区' },
    ] } })
    assert.deepEqual(restored.map(message => message.role), ['user', 'assistant', 'user', 'assistant', 'system'])
    assert.equal(restored[3].meta.edits, 2)
    assert.equal(restored[4].text, '已回退到上一版')
    assert.deepEqual(restoredMessages({ revision: 1, prompt: '旧描述', scene: { title: '旧版' } }).map(message => message.state), ['restored', 'saved'])
    assert.deepEqual(restoredMessages({ revision: 0, prompt: '', scene: null }), [])
})

test('repair requests carry the error and its source line within the prompt limit', () => {
    const prompt = repairPrompt([{ message: 'TypeError: x is undefined', line: 42, code: 'a()\nx.y()\nb()' }, { message: 'Second' }],
        (text, params) => params ? text.replace('{line}', params.line) : text)
    assert.match(prompt, /^修复当前页面的运行错误/)
    assert.match(prompt, /1\. TypeError: x is undefined（第 42 行）：x\.y\(\)/)
    assert.match(prompt, /2\. Second$/)
    assert.ok(repairPrompt([{ message: 'e'.repeat(3000) }]).length <= 2000)
})

test('the live code ticker shows the newest trimmed lines', () => {
    assert.equal(codeTail('<a>\n\n   <b>\n<c>', 2), '<b>\n<c>')
    assert.equal(codeTail('x'.repeat(100)).length, 72)
    assert.equal(codeTail(null), '')
    for (const mode of ['topology', 'bim']) assert.ok(sceneStarters[mode].length >= 3 && sceneFollowUps[mode].length >= 3)
})
