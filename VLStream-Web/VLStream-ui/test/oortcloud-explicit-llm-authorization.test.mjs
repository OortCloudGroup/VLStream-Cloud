import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/utils/llmAuthorization.js', import.meta.url), 'utf8')
const factory = new Function('deps', `
  const { authorizeOortCloudLlm, getOortCloudLlmAuthorization, getOortCloudTokenKey,
    getOortCloudTokenList, getModelHubUserInfo, getOortCloudSession, authManager, window, CustomEvent } = deps;
  ${source.replace(/^import .*$/gm, '').replace('export async function', 'async function')}
  return syncLlmAuthorization;
`)
const deferred = () => {
  let resolve
  const promise = new Promise(r => { resolve = r })
  return { promise, resolve }
}
function harness(options = {}) {
  const calls = { list: 0, key: 0, post: 0, events: 0 }
  let revision = 1
  const session = { accessToken: options.loggedOut ? '' : 'fixture-session', tenantId: 'fixture-tenant', usesPlatformSession: options.multi || false }
  const sync = factory({
    authManager: { getIdentitySnapshot: () => ({ token: 'fixture-local', revision }), isIdentityCurrent: i => i.revision === revision },
    getOortCloudSession: () => ({ ...session }),
    getOortCloudLlmAuthorization: async () => ({ code: 200, data: options.state || { authorized: false, revoked: false } }),
    getModelHubUserInfo: async () => ({ code: 200, data: { userId: 'fixture-user', userName: 'Fixture' } }),
    getOortCloudTokenList: async () => { calls.list++; return options.list ? options.list() : [{ id: 1, status: 0 }, { id: 2, status: 1 }] },
    getOortCloudTokenKey: async id => { calls.key++; assert.equal(id, 2); return options.key ? options.key() : 'fixture-key' },
    authorizeOortCloudLlm: async (data, identity, guard) => {
      assert.equal(identity.revision, revision); assert.equal(guard(), true)
      calls.post++; calls.data = data
      return { code: 200, data: { authorized: true, revoked: false } }
    },
    window: { dispatchEvent: () => { calls.events++ } }, CustomEvent: class {}
  })
  return { sync, calls, session, switchIdentity: () => { revision++ } }
}
test('default authorization works in single and multi modes without a confirmation', async () => {
  for (const multi of [false, true]) {
    const h = harness({ multi })
    assert.equal((await h.sync(multi ? 'multi' : 'single')).authorized, true)
    assert.deepEqual([h.calls.list, h.calls.key, h.calls.post], [1, 1, 1])
    assert.equal(h.calls.data.automatic, 'true')
    assert.equal(h.calls.data.platformUserId, 'fixture-user')
  }
})
test('existing authorization and cancellation survive repeated account loads and new sessions', async () => {
  for (const state of [{ authorized: true }, { authorized: false, revoked: true }]) {
    const h = harness({ state })
    await h.sync('single'); h.session.accessToken = 'new-session'; await h.sync('single')
    assert.deepEqual([h.calls.list, h.calls.key, h.calls.post], [0, 0, 0])
  }
})
test('explicit management action can restore a cancelled authorization', async () => {
  const h = harness({ state: { authorized: false, revoked: true } })
  assert.equal((await h.sync('single', { automatic: false })).authorized, true)
  assert.equal(h.calls.data.automatic, 'false')
})
test('logged-out sessions cannot read or submit credentials', async () => {
  const h = harness({ loggedOut: true })
  assert.equal(await h.sync('single'), null)
  await assert.rejects(h.sync('single', { automatic: false }))
  assert.equal(h.calls.post, 0); assert.equal(h.calls.key, 0)
})
for (const phase of ['list', 'key']) {
  for (const change of ['cloud', 'tenant', 'local', 'unmount']) {
    test(`${change} change during ${phase} stops authorization`, async () => {
      const pending = deferred(), h = harness({ [phase]: () => pending.promise })
      let active = true
      const action = h.sync('single', { isCurrent: () => active })
      await new Promise(r => setImmediate(r))
      if (change === 'cloud') h.session.accessToken = 'new-session'
      if (change === 'tenant') h.session.tenantId = 'new-tenant'
      if (change === 'local') h.switchIdentity()
      if (change === 'unmount') active = false
      pending.resolve(phase === 'list' ? [{ id: 2, status: 1 }] : 'fixture-key')
      assert.equal(await action, null)
      assert.equal(h.calls.post, 0)
    })
  }
}
test('missing enabled token or empty key cannot produce successful authorization', async () => {
  for (const options of [{ list: () => [] }, { key: () => '' }]) {
    const h = harness(options)
    await assert.rejects(h.sync('single'))
    assert.equal(h.calls.post, 0)
  }
})
test('account popover removes the second authorization action', () => {
  const popover = readFileSync(new URL('../src/layout/OortCloudPopover.vue', import.meta.url), 'utf8')
  assert.doesNotMatch(popover, /handleAuthorizeLlm|授权当前账号用于视觉复核|ElMessageBox/)
})
