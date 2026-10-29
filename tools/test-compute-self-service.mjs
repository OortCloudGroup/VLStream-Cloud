// Exercise the real management component with synthetic local identities and APIs.
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import vm from 'node:vm'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const ui = createRequire(path.join(root, 'VLStream-Web/VLStream-ui/package.json'))
const { parse } = ui('@vue/compiler-sfc')
const { parse: parseJs } = ui('@babel/parser')
const { ref, reactive, computed } = ui('vue')
const source = readFileSync(path.join(root, 'VLStream-Web/VLStream-ui/src/views/AIComputingScheduling/AutoDlNodes.vue'), 'utf8')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
const script = descriptor.scriptSetup.content
const declarations = new Map()
for (const statement of parseJs(script, { sourceType: 'module' }).program.body) {
  if (statement.type === 'VariableDeclaration') for (const declaration of statement.declarations) {
    declarations.set(declaration.id.name, `${statement.kind} ${script.slice(declaration.start, declaration.end)}`)
  }
}
const permission = (access, key) => access?.permissions?.includes(key) || access?.permissions?.includes('*:*:*')
function fixture() {
  const calls = { auth: 0, saved: [], probed: [], messages: [] }
  let inventory = []
  const context = {
    ref, reactive, computed, pageActive: true, uiText: value => value, hasComputePermission: permission,
    captureComputeIdentity: () => ({ assertCurrent() {} }),
    requireComputePlatformAuthorization: async () => { calls.auth++; throw Error('Self-service requested market proof') },
    listComputeNodes: async () => ({ data: inventory }), getUserInfo: async () => ({ data: { permissions: [], isAdmin: false } }),
    listComputeSelection: async () => ({ data: [] }),
    providerNode: ref(''), publicNode: ref(''), providerVisible: ref(false), publicIdentityVisible: ref(false), clearProviderSecrets() {},
    saveOwnComputeNode: async (id, data) => {
      calls.saved.push({ id, data }); const row = { ...data, id: id || '999999999999999999', managementOwnership: 'MINE', selfServiceNode: true }
      inventory = [row]; return { data: row }
    },
    saveComputeNode: async () => { throw Error('Ordinary self-service called manager API') },
    probeOwnComputeNode: async id => { calls.probed.push(id); return { data: { ...inventory[0], probeState: 'ERROR' } } },
    probeComputeNode: async () => { throw Error('Ordinary self-service called manager probe') },
    ElMessage: Object.fromEntries(['warning', 'error'].map(key => [key, text => calls.messages.push(text)])),
    module: { exports: {} }
  }
  const names = ['access', 'permissionError', 'capabilities', 'capabilitiesCheckedAt', 'hasPermission', 'ownNode', 'canRemoveNode', 'canEditNode', 'canBindDeployment',
    'nodes', 'loading', 'managementError', 'nodeLoadEpoch', 'nodeIdentityRetry', 'managementFailure', 'visible', 'saving', 'probing', 'editingId', 'editingAuthMode',
    'sshCommand', 'formRef', 'showProbe', 'probeResult', 'defaults', 'form', 'rules', 'clearPassword', 'load', 'edit', 'parseSsh', 'probe', 'save']
  for (const name of names) assert.ok(declarations.has(name), name)
  vm.runInNewContext(names.map(name => declarations.get(name)).join('\n') + `\nmodule.exports = { ${names.join(',')} }`, context)
  const api = context.module.exports
  api.formRef.value = { validate: async () => true }
  return { api, calls, context }
}
const tests = []
const test = (name, run) => tests.push([name, run])

test('configured resources cannot be edited and legacy entries expose only authorized removal', async () => {
  const { api } = fixture(); await api.load(); api.access.value = { permissions: ['*:*:*'] }
  assert.equal(api.canEditNode({ configurationManaged: true }), false)
  assert.equal(api.canRemoveNode({ configurationManaged: true }), false)
  assert.equal(api.canEditNode({ configurationReplaced: true }), false)
  assert.equal(api.canRemoveNode({ configurationReplaced: true }), true)
  assert.equal(api.canRemoveNode({ configurationMigrationPending: true }), false)
  assert.equal(api.canEditNode({ configurationMigrationPending: true }), false)
  assert.equal(api.canEditNode({ provider: 'self', managementOwnership: 'MINE', selfServiceNode: true }), false)
  assert.doesNotMatch(descriptor.template.content, /value="self"/)
  assert.match(descriptor.template.content, /v-else-if="!row.configurationReplaced && !row.configurationManaged"/)
})
test('normal login with no provider role loads and opens SSH form without market proof', async () => {
  const { api, calls } = fixture(); await api.load(); await api.edit()
  assert.equal(api.visible.value, true); assert.equal(calls.auth, 0); assert.equal(api.form.provider, 'autodl')
  assert.equal(api.form.username, ''); assert.equal(api.form.pythonPath, ''); assert.equal(api.form.workDir, '')
  assert.equal(api.canBindDeployment.value, false)
})
test('tampered self provider is rejected before any save API', async () => {
  const { api, calls } = fixture(); await api.load(); await api.edit(); api.form.provider = 'self'
  await api.save(); assert.equal(calls.saved.length, 0); assert.match(calls.messages[0], /YAML/)
})
test('save uses own API, clears password, refreshes and automatically probes saved node', async () => {
  const { api, calls } = fixture(); await api.load(); await api.edit()
  Object.assign(api.form, { name: 'Fixture', host: 'gpu.example.com', username: 'trainer', password: 'synthetic-only', pythonPath: '/home/trainer/vlstream/envs/vls-standard/bin/python', workDir: '/home/trainer/vlstream' })
  await api.save()
  assert.equal(calls.saved.length, 1); assert.equal(calls.saved[0].data.username, 'trainer'); assert.equal(api.form.password, '')
  assert.deepEqual(calls.probed, ['999999999999999999']); assert.equal(api.showProbe.value, true)
  assert.equal(api.probeResult.value.probeState, 'ERROR'); assert.equal(api.nodes.value.length, 1); assert.equal(calls.auth, 0)
})
test('another users or public node never exposes ordinary edit/probe actions', async () => {
  const { api, calls } = fixture(); await api.load()
  const other = { id: 'other', managementOwnership: 'ADMIN', selfServiceNode: true }
  assert.equal(api.canEditNode(other), false); await api.edit(other); await api.probe(other)
  assert.equal(api.visible.value, false); assert.equal(calls.probed.length, 0)
  assert.equal(api.canEditNode({ managementOwnership: 'MINE', selfServiceNode: false }), false)
})
test('duplicate save is blocked before asynchronous form validation completes', async () => {
  const { api, calls } = fixture(); await api.load(); await api.edit()
  Object.assign(api.form, { name: 'Fixture', host: 'gpu.example.com', username: 'trainer', password: 'synthetic-only', pythonPath: '/fixture/python', workDir: '/fixture/work' })
  let release
  api.formRef.value = { validate: () => new Promise(resolve => { release = resolve }) }
  const first = api.save(); await api.save(); assert.equal(api.saving.value, true)
  release(true); await first; assert.equal(calls.saved.length, 1); assert.equal(api.saving.value, false)
})
test('admin provider and deployment permissions remain separate', async () => {
  const { api } = fixture(); await api.load(); api.access.value.permissions = ['vls:compute:configure']
  assert.equal(api.canBindDeployment.value, false)
  api.access.value.permissions.push('vls:compute:manage-all'); assert.equal(api.canBindDeployment.value, true)
})
test('logged out state cannot open or save connection', async () => {
  const { api, calls } = fixture(); await api.edit(); await api.save()
  assert.equal(api.visible.value, false); assert.equal(calls.saved.length, 0)
})
test('initial tenant restoration retries with current identity and releases the loading mask', async () => {
  const { api, context } = fixture(); let revision = 0, requests = 0
  context.captureComputeIdentity = () => { const captured = revision; return { assertCurrent() { if (captured !== revision) throw Error('identity changed') } } }
  context.getUserInfo = async () => { if (++requests === 1) revision++; return { data: { permissions: [], isAdmin: false } } }
  await api.load(); assert.equal(requests, 2); assert.equal(api.loading.value, false); assert.ok(api.access.value)
})
test('repeated identity changes stop retrying and expose refresh instead of an endless spinner', async () => {
  const { api, context } = fixture(); let revision = 0, requests = 0
  context.captureComputeIdentity = () => { const captured = revision; return { assertCurrent() { if (captured !== revision) throw Error('identity changed') } } }
  context.getUserInfo = async () => { requests++; revision++; return { data: { permissions: [] } } }
  await api.load(); assert.equal(requests, 2); assert.equal(api.loading.value, false); assert.equal(api.access.value, null)
  assert.match(api.managementError.value, /登录账户或租户已变化/)
})
test('SSH command parser preserves custom port, account and host', async () => {
  const { api } = fixture(); api.parseSsh('ssh -p 2255 trainer@gpu.example.com')
  assert.equal(api.form.port, 2255); assert.equal(api.form.username, 'trainer'); assert.equal(api.form.host, 'gpu.example.com')
})
test('entry and guide are present and share the versioned user document', () => {
  assert.match(descriptor.template.content, /v-if="access"[^>]+@click="edit\(\)"/)
  assert.doesNotMatch(descriptor.template.content, /授权平台资源归属/)
  assert.doesNotMatch(descriptor.template.content, /找不到旧接入记录|legacy-help/)
  assert.match(descriptor.template.content, /class="guide-help-icon"[^>]*><QuestionFilled/)
  const guide = readFileSync(path.join(root, 'VLStream-Web/VLStream-ui/src/views/AIComputingScheduling/ComputeOnboardingGuide.vue'), 'utf8')
  assert.match(guide, /docs\/NEW_GPU_ONBOARDING\.md\?raw/)
})
for (const [name, run] of tests) { await run(); console.log(`PASS ${name}`) }
console.log(`${tests.length} self-service regressions passed`)
