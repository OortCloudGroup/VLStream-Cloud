import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse, compileScript } from 'vue/compiler-sfc'
import { transformSync } from 'esbuild'
import { canAccessPage } from '../src/utils/menuAccess.mjs'

const source = readFileSync(new URL('../src/views/AlgorithmWarehouse/AlgorithmTrainingPlatform.vue', import.meta.url), 'utf8')
const script = compileScript(parse(source).descriptor, { id: 'training-permissions' }).content
const js = transformSync(script, { loader: 'js', format: 'esm' }).code
  .replace(/^import\s[\s\S]*?from\s+["'][^"']+["'];\s*$/gm, '').replace(/export\s*\{[^}]+\};?\s*$/m, '')
const create = new Function('__deps', `
  const {uiText, ref, computed, watch, onBeforeUnmount, ElMessage, ElMessageBox,
    Plus, Search, MoreFilled, View, VideoPause, Upload, Delete, Loading, Check, Clock, Box, Tools,
    getTrainingPage, stopTrainingApi, delTrainingTask, getModelPage, deleteModelApi,
    canAccessPage, menuPermissions} = __deps;
  ${js}
  return stdin_default;
`)
const flush = () => new Promise(resolve => setImmediate(resolve))

async function harness(permissions, training, options = {}) {
  const menuPermissions = { value: permissions }, calls = { training: 0, models: 0 }, hooks = {}
  const deps = {
    uiText: value => value, ref: value => ({ value }), computed: fn => ({ get value() { return fn() } }),
    watch: (_, fn, options) => { hooks.change = fn; if (options.immediate) fn() }, onBeforeUnmount: fn => { hooks.unmount = fn },
    ElMessage: { error() {}, info() {}, success() {} }, ElMessageBox: { confirm: options.confirm || (async () => {}) },
    Plus: {}, Search: {}, MoreFilled: {}, View: {}, VideoPause: {}, Upload: {}, Delete: {}, Loading: {}, Check: {}, Clock: {}, Box: {}, Tools: {},
    getTrainingPage: () => { calls.training++; return training || Promise.resolve({ data: { records: [{ id: 't', taskName: 'task' }] } }) },
    getModelPage: async () => { calls.models++; return { data: { records: [{ id: 'm', modelName: 'model' }] } } },
    stopTrainingApi: options.stop || (async () => {}), delTrainingTask: async () => {}, deleteModelApi: async () => {}, canAccessPage, menuPermissions
  }
  const vm = create(deps).setup({}, { expose() {} })
  await flush()
  return { vm, calls, hooks, menuPermissions }
}

test('data-only and unauthorised users cannot deep-link the training/model aggregation', () => {
  assert.equal(canAccessPage('/algorithm-training-platform', ['vls:menu:data-management']), false)
  assert.equal(canAccessPage('/algorithm-training-platform', []), false)
  assert.equal(canAccessPage('/algorithm-training-platform', ['vls:menu:algorithm-training']), true)
})

test('training-only and model-only users load only their authorised API and tab', async () => {
  const training = await harness(['vls:menu:algorithm-training'])
  assert.deepEqual(training.calls, { training: 1, models: 0 })
  assert.equal(training.vm.activeTab.value, 'training')
  assert.equal(training.vm.canModels.value, false)
  const model = await harness(['vls:menu:algorithm-model'])
  assert.deepEqual(model.calls, { training: 0, models: 1 })
  assert.equal(model.vm.activeTab.value, 'models')
  assert.equal(model.vm.canTraining.value, false)
})

test('revocation clears data and ignores a late training response', async () => {
  let resolve
  const pending = new Promise(done => { resolve = done })
  const h = await harness(['vls:menu:algorithm-training'], pending)
  h.menuPermissions.value = []
  h.hooks.change()
  resolve({ data: { records: [{ id: 'secret', taskName: 'stale' }] } })
  await flush()
  assert.deepEqual(h.vm.trainingTasks.value, [])
  assert.equal(h.vm.activeTab.value, '')
  assert.equal(h.calls.models, 0)
})

test('a refreshed identity with the same permission clears the previous identity data', async () => {
  const h = await harness(['vls:menu:algorithm-training'])
  assert.equal(h.vm.trainingTasks.value.length, 1)
  h.menuPermissions.value = ['vls:menu:algorithm-training']
  h.hooks.change()
  assert.deepEqual(h.vm.trainingTasks.value, [])
  await flush()
  assert.equal(h.calls.training, 2)
})

test('an open stop confirmation cannot act after identity permissions refresh', async () => {
  let confirm, stopped = 0
  const pending = new Promise(done => { confirm = done })
  const h = await harness(['vls:menu:algorithm-training'], null, { confirm: () => pending, stop: async () => { stopped++ } })
  h.vm.stopTraining({ id: 'old-user-task' })
  h.menuPermissions.value = ['vls:menu:algorithm-training']
  h.hooks.change()
  confirm()
  await flush()
  assert.equal(stopped, 0)
})
