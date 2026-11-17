import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
let calls = [], response = { code: 200, data: { featured: true } }, changed = false, failure = null
const context = {
  request: async config => { calls.push(config); if (failure) throw failure; return response },
  captureComputeIdentity: () => ({ assertCurrent: () => { if (changed) throw new Error('identity changed') } }),
  computePathPart: value => { if (!value || /[\\/?#]/.test(String(value))) throw new Error('invalid id'); return encodeURIComponent(value) }
}
globalThis.__publicationTest = context
let source = await fs.readFile(new URL('../src/api/modelHubPublication.js', import.meta.url), 'utf8')
source = source.replace(/^import .*$/gm, '')
source = 'const {request,captureComputeIdentity,computePathPart}=globalThis.__publicationTest;\n' + source
const api = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))
assert.deepEqual(await api.getPublication('model', '123'), { featured: true })
assert.equal(calls[0].url, '/vlsModelHub/publications/model/123')
assert.equal(calls[0].sensitiveData, true)
await api.setPublicationFeatured('dataset', '123', false)
assert.deepEqual(calls[1].data, { featured: false })
assert.equal(calls[1].method, 'put')
await api.publishToModelHub('model', '123', {}, { accessToken: 'test-only', tenantId: 't1' })
assert.equal(calls[2].headers['X-Model-Hub-Tenant'], 't1')
await assert.rejects(api.getPublication('other', '123'))
await assert.rejects(api.getPublication('model', '../123'))
changed = true
await assert.rejects(api.getPublication('model', '123'))
assert.equal(calls.length, 3)
changed = false; response = { code: 403, msg: 'not allowed' }
await assert.rejects(api.getPublication('model', '123'), /not allowed/)
response = { code: 4401, msg: 'OortCloud login rejected' }
await assert.rejects(api.publishToModelHub('model', '123', {}, {accessToken:'test-only'}), error => error.code === 4401)
failure = { data: { code: 4401, msg: 'realm mismatch' } }
await assert.rejects(api.publishToModelHub('dataset', '123', {}, {accessToken:'test-only'}), error => error.code === 4401 && error.message === 'realm mismatch')
failure = null
response={code:200,data:{publishState:'PROCESSING',receiptState:'PROCESSING'}}
const before=calls.length
await api.reconcilePublication('model','123',{accessToken:'test-only'})
assert.equal(calls[before].url,'/vlsModelHub/publications/model/123/reconcile')
assert.equal(calls[before].method,'post')
assert.deepEqual(calls[before].data,{})
const resumeIndex=calls.length
await api.resumePublication('dataset','123',{accessToken:'test-only'})
assert.equal(calls[resumeIndex].url,'/vlsModelHub/publications/dataset/123/resume')
assert.equal(calls[resumeIndex].method,'post')
assert.deepEqual(calls[resumeIndex].data,{})
delete globalThis.__publicationTest
console.log('Publication API identity, structured business errors, and cloud 4401 tests passed')
