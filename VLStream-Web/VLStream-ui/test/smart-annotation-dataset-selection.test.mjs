import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'
import { canAccessPage, filterMenuRoutes } from '../src/utils/menuAccess.mjs'

const component = await readFile(new URL('../src/views/AlgorithmTraining/SmartAnnotation.vue', import.meta.url), 'utf8')
const readiness = component.slice(component.indexOf('const ready ='), component.indexOf('const boxCount ='))
const actions = component.slice(component.indexOf('const datasetDisabled ='), component.indexOf('const openReview ='))
const eligibility = component.slice(component.indexOf('const confirmable ='), component.indexOf('const canReview ='))
const navigation = component.slice(component.indexOf('const refresh ='), component.indexOf('const leave ='))

const generated = { id: '2122504598019956737', annotationName: '已生成', annotationType: 'object_detection', datasetGenerated: true }
const ungenerated = { id: '2122504598019956738', annotationName: '待生成', annotationType: 'object_detection', datasetGenerated: false }

function harness(overrides = {}) {
  const ref = value => ({ value })
  const computed = fn => ({ get value() { return fn() } })
  const requests = { projects: [], options: [], create: [], errors: [], compute: 0, list: [], destinations: [] }
  const form = { datasetId: '', computeNodeId: '10', taskName: '', mode: 'active', providerId: '', modelName: '', prompt: '', llmConfidence: 0.50 }
  const provider = { id: '2122504598019956740', name: 'OortCloud', modelName: 'vision-default' }
  const llmProviders = ref([])
  const state = {
    form, computed, activeLearning: computed(() => form.mode === 'active'),
    datasets: ref([]), datasetsLoading: ref(false), datasetNeedsGeneration: ref(false), creating: ref(false),
    options: ref(null), sourceKey: ref(''), detail: ref(null),
    computeLoading: ref(false), selectedCompute: ref({ schedulable: true }),
    props: { llmOnly: false }, disposed: false,
    loading: ref(false), tasks: ref([]), taskPage: ref(1), taskTotal: ref(0), unfilteredTaskPage: ref(false),
    listSmartTasks: async params => { requests.list.push(params); return { records: [], total: 0 } },
    llmProviders, llmModels: ref([]),
    llmProvidersLoading: ref(false), llmModelsLoading: ref(false),
    llmProviderError: ref(''), llmModelError: ref(''),
    llmProviderName: computed(() => llmProviders.value.find(item => item.id === form.providerId)?.name),
    listSmartLlmProviders: async () => [provider],
    listSmartLlmModels: async () => ['vision-default', 'vision-alternative'],
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
    route: { query: {} }, router: { replace: async target => { requests.destinations.push(target) } },
    loadCompute: async () => { requests.compute++ }, getSmartTask: async () => ({}),
    resetPredictionQuery: () => {}, showPredictions: () => false,
    run: operation => operation(),
    createSmartTask: async payload => { requests.create.push(payload); return { id: '501' } },
    uiText: value => value, ElMessage: { success: () => {} },
    ...overrides
  }
  const handlers = vm.runInNewContext(`(() => {
    ${readiness}
    ${actions}
    ${navigation}
    return { ready, datasetDisabled, searchDatasets, loadOptions, changeDataset, changeMode, openCreate, create, refresh, enter }
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

test('LLM task submits the selected model directly without GPU fields', async () => {
  const ui = harness({
    route: { query: { dataset: ungenerated.id } },
    computeLoading: { value: true }, selectedCompute: { value: null }
  })
  ui.state.form.mode = 'llm'
  await ui.openCreate()
  assert.equal(ui.requests.compute, 0)
  assert.equal(ui.requests.projects[0].includeGeneratedStatus, false)
  assert.deepEqual(ui.requests.options, [{ id: ungenerated.id, forActiveLearning: false }])
  assert.equal(ui.state.form.modelName, 'vision-default')
  ui.state.form.modelName = 'vision-alternative'
  ui.state.form.prompt = '  Check labels carefully.  '
  assert.equal(ui.ready.value, true)
  await ui.create()
  assert.deepEqual({ ...ui.requests.create[0] }, {
    datasetId: ungenerated.id, taskName: '待生成 - 大模型标注', mode: 'llm',
    providerId: '2122504598019956740', modelName: 'vision-alternative', prompt: 'Check labels carefully.', confidence: 0.5
  })
  assert.equal(ui.requests.compute, 0)
})

test('LLM catalog failure retains the configured model when switching datasets', async () => {
  const ui = harness({
    route: { query: { dataset: ungenerated.id } },
    listSmartLlmModels: async () => { throw new Error('catalog unavailable') }
  })
  ui.state.form.mode = 'llm'
  await ui.openCreate()
  assert.equal(ui.state.form.modelName, 'vision-default')
  assert.ok(ui.state.llmModelError.value)
  assert.equal(ui.ready.value, true)
  ui.state.form.datasetId = generated.id
  await ui.changeDataset()
  assert.equal(ui.ready.value, true)
})

test('LLM page confirmation requires a strict finite score and excludes manual-review candidates', () => {
  const row = (id, confidence, manualReview = false) => ({
    candidate: { id, reviewState: 'PENDING', boxesJson: JSON.stringify([{ labelId: '1', confidence }]) },
    predictionQuality: { manualReview }
  })
  const predictions = [row('threshold', 0.85), row('above', 0.9), row('low', 0.8), row('missing', null), row('manual', 0.99, true), row('string', '0.99')]
  const confirmable = vm.runInNewContext(`(() => { ${eligibility}; return confirmable })()`, {
    computed: fn => ({ get value() { return fn() } }),
    predictions: { value: predictions }, llmTask: { value: true }, detail: { value: { task: { confidence: 0.85 } } }
  })
  assert.deepEqual(confirmable.value.map(item => item.candidate.id), ['above'])
})

test('each annotation entry requests its own task mode while preserving the dataset filter', async () => {
  const ordinary = harness({ route: { query: { dataset: generated.id } } })
  const model = harness({ props: { llmOnly: true }, route: { query: { dataset: ungenerated.id } } })
  await ordinary.refresh()
  await model.refresh()
  assert.deepEqual({ ...ordinary.requests.list[0] }, { datasetId: generated.id, page: 1, mode: 'compute' })
  assert.deepEqual({ ...model.requests.list[0] }, { datasetId: ungenerated.id, page: 1, mode: 'llm' })
})

test('mixed responses cannot display another annotation mode in either task list', async () => {
  const records = [
    { id: 'active-task', mode: 'active' },
    { id: 'specified-model-task', mode: 'model' },
    { id: 'large-model-task', mode: 'llm' }
  ]
  const listSmartTasks = async () => ({ records, total: records.length })
  const ordinary = harness({ listSmartTasks })
  const model = harness({ props: { llmOnly: true }, listSmartTasks })
  await ordinary.refresh()
  await model.refresh()
  assert.deepEqual(ordinary.state.tasks.value.map(row => row.id), ['active-task', 'specified-model-task'])
  assert.deepEqual(model.state.tasks.value.map(row => row.id), ['large-model-task'])
  assert.equal(ordinary.state.unfilteredTaskPage.value, true)
  assert.equal(model.state.unfilteredTaskPage.value, true)
})

test('an incompatible server page containing no matching tasks still allows later pages to be reached', async () => {
  const ui = harness({
    listSmartTasks: async ({ page }) => ({
      records: page === 1 ? [{ id: 'llm-task', mode: 'llm' }] : [{ id: 'compute-task', mode: 'active' }],
      total: 21
    })
  })
  await ui.refresh()
  assert.deepEqual(ui.state.tasks.value, [])
  assert.equal(ui.state.taskTotal.value, 21)
  ui.state.taskPage.value = 2
  await ui.refresh()
  assert.deepEqual(ui.state.tasks.value.map(row => row.id), ['compute-task'])
  assert.equal(ui.state.taskTotal.value, 21)
  assert.equal(ui.state.unfilteredTaskPage.value, true)
})

test('a task link entered from its former URL opens the matching annotation page', async () => {
  const ui = harness({
    getSmartTask: async id => ({ task: { id, mode: 'llm' } }),
    route: { query: { dataset: ungenerated.id } }
  })
  await ui.enter('501')
  const [destination] = ui.requests.destinations
  assert.equal(destination.path, '/llm-smart-annotation')
  assert.deepEqual({ ...destination.query }, { dataset: ungenerated.id, task: '501' })
  assert.equal(ui.state.detail.value, null)
})

test('LLM annotation navigation shares the annotation grant without granting provider management', () => {
  const path = '/llm-smart-annotation'
  const grant = ['vls:menu:smart-annotation']
  assert.equal(canAccessPage(path, []), false)
  assert.equal(canAccessPage(path, ['vls:menu:llm-provider-management']), false)
  assert.equal(canAccessPage(path, grant), true)
  assert.equal(canAccessPage('/llm-provider-management', grant), false)
  assert.deepEqual(filterMenuRoutes([{ path: '/training', children: [{ path }] }], grant)[0].children, [{ path }])
})
