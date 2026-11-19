import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'

const component = await readFile(new URL('../src/views/AlgorithmTraining/SmartAnnotation.vue', import.meta.url), 'utf8')
const readiness = component.slice(component.indexOf('const ready ='), component.indexOf('const boxCount ='))
const actions = component.slice(component.indexOf('const datasetDisabled ='), component.indexOf('const openReview ='))

const generated = { id: '2122504598019956737', annotationName: '已生成', annotationType: 'object_detection', datasetGenerated: true }
const ungenerated = { id: '2122504598019956738', annotationName: '待生成', annotationType: 'object_detection', datasetGenerated: false }

function harness(overrides = {}) {
  const ref = value => ({ value })
  const computed = fn => ({ get value() { return fn() } })
  const requests = { projects: [], options: [], create: [], errors: [] }
  const form = { datasetId: '', computeNodeId: '10', taskName: '', mode: 'active' }
  const state = {
    form, computed, activeLearning: computed(() => form.mode === 'active'),
    datasets: ref([]), datasetsLoading: ref(false), datasetNeedsGeneration: ref(false), creating: ref(false),
    options: ref(null), sourceKey: ref(''), detail: ref(null),
    computeLoading: ref(false), selectedCompute: ref({ schedulable: true }),
    typeNames: { object_detection: '物体检测' },
    listDataProjects: async query => {
      requests.projects.push(query)
      return { records: query.includeGeneratedStatus ? [generated, ungenerated]
        : [generated, ungenerated].map(({ datasetGenerated, ...row }) => row) }
    },
    smartOptions: async (id, forActiveLearning) => {
      requests.options.push({ id, forActiveLearning })
      if (forActiveLearning && id === ungenerated.id) {
        return { datasetGenerated: false, datasetName: ungenerated.annotationName }
      }
      return {
        datasetGenerated: id === generated.id,
        datasetName: id === generated.id ? generated.annotationName : ungenerated.annotationName,
        unlabeled: 200, minUnlabeledImages: 101, minBoxesPerLabel: 10, labelCounts: [{ count: 20 }]
      }
    },
    handleError: error => requests.errors.push(error),
    route: { query: {} }, router: { replace: async () => {} },
    loadCompute: async () => {}, getSmartTask: async () => ({}),
    run: operation => operation(),
    createSmartTask: async payload => { requests.create.push(payload); return { id: '501' } },
    uiText: value => value, ElMessage: { success: () => {} },
    ...overrides
  }
  const handlers = vm.runInNewContext(`(() => {
    ${readiness}
    ${actions}
    return { ready, datasetDisabled, searchDatasets, loadOptions, changeMode, openCreate, create }
  })()`, state)
  return { state, requests, ...handlers }
}

test('active learning requests generation status and retains all options with disabled rows marked', async () => {
  const ui = harness()
  await ui.openCreate()
  assert.equal(ui.requests.projects[0].includeGeneratedStatus, true)
  assert.equal(ui.requests.projects[0].generatedOnly, undefined)
  assert.deepEqual(ui.state.datasets.value.map(row => row.id), [generated.id, ungenerated.id])
  assert.equal(ui.datasetDisabled(ui.state.datasets.value[0]), false)
  assert.equal(ui.datasetDisabled(ui.state.datasets.value[1]), true)
  assert.match(component, /<el-option v-for="dataset in datasets"[\s\S]*?:disabled="datasetDisabled\(dataset\)"/)
  assert.match(component, /训练包未生成或已失效/)
})

test('an ungenerated deep link in the list clears silently without loading model options', async () => {
  const ui = harness({ route: { query: { dataset: ungenerated.id } } })
  await ui.openCreate()
  assert.deepEqual(ui.requests.options, [])
  assert.deepEqual(ui.requests.errors, [])
  assert.equal(ui.state.form.datasetId, '')
  assert.equal(ui.state.datasetNeedsGeneration.value, true)
  await ui.create()
  assert.equal(ui.requests.create.length, 0)
})

test('an ungenerated deep link outside the list is rejected by the active options response without a toast', async () => {
  const ui = harness({
    route: { query: { dataset: ungenerated.id } },
    listDataProjects: async () => ({ records: [generated] })
  })
  await ui.openCreate()
  assert.deepEqual(ui.requests.options, [{ id: ungenerated.id, forActiveLearning: true }])
  assert.deepEqual(ui.requests.errors, [])
  assert.equal(ui.state.form.datasetId, '')
  assert.equal(ui.state.datasetNeedsGeneration.value, true)
})

test('mode changes clear ineligible selection and make the same row available again in specified model', async () => {
  const ui = harness({ route: { query: { dataset: ungenerated.id } } })
  ui.state.form.mode = 'model'
  await ui.openCreate()
  assert.equal(ui.requests.projects[0].includeGeneratedStatus, false)
  assert.equal(ui.state.form.datasetId, ungenerated.id)
  assert.equal(ui.state.options.value.datasetGenerated, false)
  ui.state.sourceKey.value = 'model:9'
  assert.equal(ui.ready.value, true)

  ui.state.form.mode = 'active'
  await ui.changeMode()
  assert.equal(ui.requests.projects.at(-1).includeGeneratedStatus, true)
  assert.equal(ui.state.form.datasetId, '')
  assert.equal(ui.state.datasetNeedsGeneration.value, true)
  assert.equal(ui.state.sourceKey.value, '')
  assert.equal(ui.datasetDisabled(ui.state.datasets.value.find(row => row.id === ungenerated.id)), true)
  assert.deepEqual(ui.requests.options, [{ id: ungenerated.id, forActiveLearning: false }])

  ui.state.form.mode = 'model'
  await ui.changeMode()
  const row = ui.state.datasets.value.find(dataset => dataset.id === ungenerated.id)
  assert.equal(ui.datasetDisabled(row), false)
  ui.state.form.datasetId = row.id
  await ui.loadOptions()
  assert.equal(ui.state.form.datasetId, ungenerated.id)
  assert.equal(ui.state.datasetNeedsGeneration.value, false)
  assert.deepEqual(ui.requests.errors, [])
})

test('unknown generation status remains visible and disabled', async () => {
  const { datasetGenerated, ...unknown } = generated
  const ui = harness({ listDataProjects: async () => ({ records: [unknown] }) })
  await ui.openCreate()
  assert.equal(ui.state.datasets.value[0].id, generated.id)
  assert.equal(ui.datasetDisabled(ui.state.datasets.value[0]), true)
  ui.state.form.datasetId = generated.id
  await ui.loadOptions()
  assert.equal(ui.state.form.datasetId, '')
  assert.equal(ui.state.datasetNeedsGeneration.value, true)
  assert.deepEqual(ui.requests.options, [])
  assert.deepEqual(ui.requests.errors, [])
})

test('generated deep link can create a task even when the project is outside the first page', async () => {
  const ui = harness({
    route: { query: { dataset: generated.id } },
    listDataProjects: async () => ({ records: [] })
  })
  await ui.openCreate()
  assert.equal(ui.state.form.taskName, '已生成 - 智能标注')
  assert.equal(ui.ready.value, true)
  await ui.create()
  assert.equal(ui.requests.create.length, 1)
  assert.equal(ui.requests.create[0].datasetId, generated.id)
  assert.equal(ui.requests.create[0].sourceType, 'system')
})

test('late project search results cannot overwrite active generation statuses after switching modes', async () => {
  let resolveOld
  const ui = harness({
    listDataProjects: query => query.includeGeneratedStatus
      ? Promise.resolve({ records: [generated, ungenerated] })
      : new Promise(resolve => { resolveOld = resolve })
  })
  ui.state.form.mode = 'model'
  const oldSearch = ui.openCreate()
  ui.state.form.mode = 'active'
  await ui.changeMode()
  resolveOld({ records: [ungenerated] })
  await oldSearch
  assert.equal(ui.state.datasets.value[0].id, generated.id)
  assert.equal(ui.datasetDisabled(ui.state.datasets.value[0]), false)
  assert.equal(ui.datasetDisabled(ui.state.datasets.value[1]), true)
  assert.equal(ui.state.datasetsLoading.value, false)
})

test('fresh options can disable a stale generated row without hiding it', async () => {
  const ui = harness({ smartOptions: async () => ({ datasetGenerated: false }) })
  await ui.openCreate()
  ui.state.form.datasetId = generated.id
  await ui.loadOptions()
  assert.equal(ui.state.form.datasetId, '')
  assert.equal(ui.state.datasetNeedsGeneration.value, true)
  assert.equal(ui.state.datasets.value[0].id, generated.id)
  assert.equal(ui.datasetDisabled(ui.state.datasets.value[0]), true)
  assert.deepEqual(ui.requests.errors, [])
})

test('results for a previous selection cannot clear a newer generated selection', async () => {
  let resolveOld
  const ui = harness({
    listDataProjects: async () => ({ records: [generated, { ...ungenerated, datasetGenerated: true }] }),
    smartOptions: id => id === ungenerated.id
      ? new Promise(resolve => { resolveOld = resolve })
      : Promise.resolve({ datasetGenerated: true, datasetName: generated.annotationName })
  })
  await ui.openCreate()
  ui.state.form.datasetId = ungenerated.id
  const oldOptions = ui.loadOptions()
  ui.state.form.datasetId = generated.id
  await ui.loadOptions()
  resolveOld({ datasetGenerated: false })
  await oldOptions
  assert.equal(ui.state.form.datasetId, generated.id)
  assert.equal(ui.state.datasetNeedsGeneration.value, false)
  assert.equal(ui.state.options.value.datasetGenerated, true)
})
