import assert from 'node:assert/strict'
import { hasDeviceManifest, hubDeploymentPayload, checkedHubDeploymentResults, createHubAttemptStore } from '../src/utils/modelHubDeployment.mjs'
const model = { uid: 'model1', published_commit: 'a'.repeat(40), device_manifest_path: 'vls-device-manifest.json' }
assert.equal(hasDeviceManifest(model), true)
assert.equal(hasDeviceManifest({ ...model, published_commit: 'main' }), false)
assert.equal(hasDeviceManifest({ ...model, device_manifest_path: 'other.json' }), false)
const devices = [{ deviceId: 'd1', online: true }, { deviceId: 'd1', online: true }]
assert.deepEqual(hubDeploymentPayload(model, 'a1', devices, 'operation1').deviceIds, ['d1'])
assert.throws(() => hubDeploymentPayload(model, '', devices, 'operation1'))
assert.throws(() => hubDeploymentPayload(model, 'a1', [{ deviceId: 'd1', online: false }], 'operation1'))
assert.throws(() => hubDeploymentPayload(model, 'a1', [], 'operation1'))
assert.throws(() => hubDeploymentPayload(model, 'a1', Array.from({ length: 21 }, (_, i) => ({ deviceId: String(i), online: true })), 'operation1'))
assert.throws(() => checkedHubDeploymentResults([], ['d1']))
assert.throws(() => checkedHubDeploymentResults([{ deviceId: 'other', requestId: 'r1', status: 'SUCCESS' }], ['d1']))
const pending = [{ deviceId: 'd1', requestId: 'r1', status: 'PUBLISHED' }]
assert.equal(checkedHubDeploymentResults(pending, ['d1'])[0].status, 'PUBLISHED')
console.log('Model Hub deployment contract: 11 assertions passed')

const cache = createHubAttemptStore()
let sameUser = true
const firstIdentity = { assertCurrent() { if (!sameUser) throw new Error('changed') } }
cache.set('resource', { operationId: 'original' }, firstIdentity)
assert.equal(cache.get('resource', firstIdentity).operationId, 'original')
sameUser = false
assert.equal(cache.get('resource', { assertCurrent() {} }), undefined)
console.log('Hub attempts survive remounts and clear on identity changes')
