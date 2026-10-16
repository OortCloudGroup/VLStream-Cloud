// Run actual UI/API code with synthetic fixtures; no browser identity or backend writes.
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import path from 'node:path'
import vm from 'node:vm'
import { fileURLToPath } from 'node:url'
const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const require = createRequire(path.join(uiRoot, 'package.json'))
const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')
const { parse: parseJs } = require('@babel/parser')
const { ref, reactive, computed } = require('vue')
const read = name => readFileSync(path.join(uiRoot, 'src', name), 'utf8')
function execute(source, context, names) {
  const statements = parseJs(source, { sourceType: 'module' }).program.body
  const code = statements.filter(node => node.type !== 'ImportDeclaration').map(node => {
    if (node.type === 'ExportNamedDeclaration') return source.slice(node.declaration.start, node.declaration.end)
    return source.slice(node.start, node.end)
  }).join('\n')
  context.module = { exports: {} }
  vm.runInNewContext(`${code}\nmodule.exports = { ${names.join(',')} };`, context)
  return context.module.exports
}
function component(file, context, names) {
  const { descriptor, errors } = parse(read(file))
  assert.deepEqual(errors, [])
  compileScript(descriptor, { id: file })
  const template = compileTemplate({ source: descriptor.template.content, filename: file, id: file })
  assert.deepEqual(template.errors, [])
  return execute(descriptor.scriptSetup.content, {
    ref, reactive, computed, onMounted() {}, uiText: text => text,
    buildPlatformLoginUrl: () => '/existing-identity-login', ...context
  }, names)
}
const compat = execute(read('api/system/ruoyiCompat.js'), {}, ['mapBladeUserToRuoyi', 'toStringIds'])
const policy = execute(read('utils/localPassword.js'), {}, ['localPasswordRules'])
let passed = 0
async function test(name, run) { await run(); passed++; console.log(`PASS ${name}`) }
await test('create never injects a shared default credential', () => {
  assert.equal(compat.mapBladeUserToRuoyi({ account: 'fixture' }).password, undefined)
  assert.equal(compat.mapBladeUserToRuoyi({ account: 'fixture', password: 'synthetic' }).password, 'synthetic')
  assert.equal(compat.mapBladeUserToRuoyi({ id: 'fixture-id', password: 'injected' }).password, undefined)
})
await test('reset and own-change requests carry explicit credentials in JSON only', async () => {
  const calls = []
  const api = execute(read('api/system/user.js'), { request: config => { calls.push(config); return Promise.resolve({ code: 200 }) },
    toStringIds: compat.toStringIds }, ['resetUserPassword', 'changeOwnPassword'])
  await api.resetUserPassword('fixture-id', 'synthetic')
  assert.equal(calls[0].data.userId, 'fixture-id')
  assert.equal(calls[0].data.password, 'synthetic')
  assert.equal(calls[0].params, undefined)
  await api.changeOwnPassword('old-synthetic', 'new-synthetic')
  assert.equal(calls[1].data.oldPassword, 'old-synthetic')
  assert.equal(calls[1].data.newPassword, 'new-synthetic')
  assert.equal(calls[1].data.userId, undefined)
  assert.equal(calls[1].params, undefined)
})
await test('password rule preserves server length and confirms identical input', () => {
  const rules = policy.localPasswordRules(() => 'synthetic', x => x)
  assert.equal(rules.password[1].min, 5)
  assert.equal(rules.password[1].max, 20)
  let error
  rules.confirmPassword[1].validator({}, 'different', value => { error = value })
  assert.equal(error.message, '两次输入密码不一致')
  rules.confirmPassword[1].validator({}, 'synthetic', value => { error = value })
  assert.equal(error, undefined)
})
function management() {
  return component('views/System/UserManagement.vue', { localPasswordRules: policy.localPasswordRules },
    ['tenantMode', 'handleCreate', 'handleResetPassword', 'dialogVisible', 'passwordVisible', 'form', 'rules', 'clearCreationPassword'])
}
await test('unknown/multi tenant modes cannot create local credentials', () => {
  for (const mode of ['', 'multi']) {
    const page = management(); page.tenantMode.value = mode; page.handleCreate()
    assert.equal(page.dialogVisible.value, false)
  }
})
await test('local create starts empty and clears secrets when closed', () => {
  const page = management(); page.tenantMode.value = 'single'; page.handleCreate()
  assert.equal(page.dialogVisible.value, true)
  assert.equal(page.form.value.password, '')
  assert.equal(page.rules.value.password[1].min, 5)
  page.form.value.password = 'synthetic'; page.form.value.confirmPassword = 'synthetic'
  page.clearCreationPassword()
  assert.equal(page.form.value.password, '')
  assert.equal(page.form.value.confirmPassword, '')
  page.form.value.id = 'fixture-id'
  assert.equal(page.rules.value.password, undefined)
})
await test('reset refuses SSO targets, multi mode and protected superadmin', () => {
  for (const [mode, row] of [['multi', { id: 'fixture-id' }], ['single', { id: 'fixture-id', platformUserId: 'platform-fixture' }],
    ['single', { id: '1' }]]) {
    const page = management(); page.tenantMode.value = mode; page.handleResetPassword(row)
    assert.equal(page.passwordVisible.value, false)
  }
  const page = management(); page.tenantMode.value = 'single'; page.handleResetPassword({ id: 'fixture-id', account: 'fixture' })
  assert.equal(page.passwordVisible.value, true)
})
function dialog(own = false, response = { code: 200 }) {
  const calls = [], events = [], messages = []
  const page = component('components/LocalPasswordDialog.vue', {
    defineProps: () => ({ own, userId: 'fixture-id' }), defineEmits: () => (...args) => events.push(args),
    localPasswordRules: policy.localPasswordRules,
    resetUserPassword: async (...args) => { calls.push(args); return response },
    changeOwnPassword: async (...args) => { calls.push(args); return response },
    ElMessage: { success: text => messages.push(text), error: text => messages.push(text) }
  }, ['form', 'formRef', 'saving', 'submit', 'clearForm'])
  page.formRef.value = { validate: async () => true, clearValidate() {} }
  Object.assign(page.form, { oldPassword: 'old-synthetic', password: 'new-synthetic', confirmPassword: 'new-synthetic' })
  return { page, calls, events, messages }
}
await test('reset submits entered password and clears it after confirmed success', async () => {
  const fixture = dialog(); await fixture.page.submit()
  assert.deepEqual(fixture.calls[0], ['fixture-id', 'new-synthetic'])
  assert.equal(fixture.page.form.password, '')
  assert.equal(fixture.events.at(-1)[1], false)
})
await test('own password change submits old/new pair without selecting a target', async () => {
  const fixture = dialog(true); await fixture.page.submit()
  assert.deepEqual(fixture.calls[0], ['old-synthetic', 'new-synthetic'])
})
await test('validation failure does not send any password request', async () => {
  const fixture = dialog(); fixture.page.formRef.value.validate = async () => { throw Error('invalid fixture') }
  await fixture.page.submit(); assert.equal(fixture.calls.length, 0); assert.equal(fixture.page.saving.value, false)
})
await test('server rejection leaves the dialog open without success reporting', async () => {
  const fixture = dialog(false, { code: 500, msg: 'denied' }); await fixture.page.submit()
  assert.equal(fixture.events.length, 0)
  assert.equal(fixture.messages[0], 'denied')
})
await test('duplicate submit is blocked while async validation is pending', async () => {
  const fixture = dialog(); let release
  fixture.page.formRef.value.validate = () => new Promise(resolve => { release = resolve })
  const first = fixture.page.submit(); await fixture.page.submit(); release(true); await first
  assert.equal(fixture.calls.length, 1)
})
console.log(`${passed} local-password regressions passed; no external API or identity used.`)
