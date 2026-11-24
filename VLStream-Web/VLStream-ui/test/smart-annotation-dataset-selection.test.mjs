import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'
import { canAccessPage, filterMenuRoutes } from '../src/utils/menuAccess.mjs'

const component = await readFile(new URL('../src/views/AlgorithmTraining/SmartAnnotation.vue', import.meta.url), 'utf8')
const qualityDefaults = component.slice(component.indexOf('const llmQualityDefaults ='), component.indexOf('const form ='))
const readiness = component.slice(component.indexOf('const llmQualitySupported ='), component.indexOf('const boxCount ='))
const actions = component.slice(component.indexOf('const llmDatasetIssues ='), component.indexOf('const openReview ='))
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
    datasets: ref([]), datasetsLoading: ref(false), datasetNeedsGeneration: ref(false), datasetIssue: ref(''), creating: ref(false),
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
        unlabeled: 200, minUnlabeledImages: 101, minBoxesPerLabel: 10, labelCounts: [{ count: 20 }], llmQualitySupported: true
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
    ${qualityDefaults}
    ${readiness}
    ${actions}
    ${navigation}
    return { ready, llmQualitySupported, datasetReason, datasetDisabled, searchDatasets, loadOptions, changeDataset, changeMode, openCreate, create, refresh, enter }
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

test('LLM task submits the selected model with default quality controls and no GPU fields', async () => {
  const ui = harness({
    route: { query: { dataset: ungenerated.id } },
    computeLoading: { value: true }, selectedCompute: { value: null }
  })
  ui.state.form.mode = 'llm'
  await ui.openCreate()
  assert.equal(ui.requests.compute, 0)
  assert.equal(ui.requests.projects[0].includeGeneratedStatus, false)
  assert.equal(ui.requests.projects[0].includeLlmAnnotationStatus, true)
  assert.deepEqual(ui.requests.options, [{ id: ungenerated.id, forActiveLearning: false }])
  assert.equal(ui.state.form.modelName, 'vision-default')
  ui.state.form.modelName = 'vision-alternative'
  ui.state.form.prompt = '  Check labels carefully.  '
  assert.equal(ui.ready.value, true)
  await ui.create()
  assert.deepEqual({ ...ui.requests.create[0] }, {
    datasetId: ungenerated.id, taskName: '待生成 - 大模型标注', mode: 'llm',
    providerId: '2122504598019956740', modelName: 'vision-alternative', prompt: 'Check labels carefully.',
    confidence: 0.5, llmPilotSize: 10, llmReviewPercent: 5
  })
  assert.equal(ui.requests.compute, 0)
})

test('quality-enabled submission validates settings and older services allow direct annotation', async () => {
  const ui = harness({ route: { query: { dataset: ungenerated.id } } })
  ui.state.form.mode = 'llm'
  await ui.openCreate()
  ui.state.form.llmPilotSize = 0
  assert.equal(ui.ready.value, false)
  ui.state.form.llmPilotSize = 10
  ui.state.form.llmReviewPercent = 100.5
  assert.equal(ui.ready.value, false)

  ui.state.options.value.llmQualitySupported = undefined
  assert.equal(ui.llmQualitySupported.value, false)
  assert.equal(ui.state.form.llmPilotEnabled, true)
  assert.equal(ui.ready.value, true)
  await ui.create()
  assert.equal(ui.requests.create.length, 1)
  assert.equal(Object.hasOwn(ui.requests.create[0], 'llmPilotSize'), false)
  assert.equal(Object.hasOwn(ui.requests.create[0], 'llmReviewPercent'), false)
  assert.doesNotMatch(component, /当前服务版本不支持小批试标或高分抽检/)
})

test('LLM dataset options retain unsupported and ineligible rows with reasons', async () => {
  const rows = [
    { ...generated, llmAnnotationStatus: 'READY', totalCount: 0 },
    { ...ungenerated, llmAnnotationStatus: 'NO_UNLABELED_IMAGES', totalCount: 99, annotatedCount: 14 },
    { ...generated, id: 'label-less', llmAnnotationStatus: 'MISSING_LABELS' },
    { ...generated, id: 'old-type', annotationType: 'unknown' },
    { ...generated, id: 'old-full', totalCount: 4, annotatedCount: 4 }
  ]
  const ui = harness({ listDataProjects: async () => ({ records: rows }) })
  ui.state.form.mode = 'llm'
  await ui.openCreate()
  assert.equal(ui.state.datasets.value.length, rows.length)
  assert.deepEqual(ui.state.datasets.value.map(ui.datasetReason), ['', '无可标注图片', '请先添加标注标签', '不支持的标注类型', '无可标注图片'])
  assert.deepEqual(ui.state.datasets.value.map(ui.datasetDisabled), [false, true, true, true, true])
  assert.deepEqual(ui.requests.options, [])
})

test('a dataset that is no longer eligible is cleared and disabled with an inline reason', async () => {
  const ui = harness({ smartOptions: async () => ({ unlabeled: 0, labelCounts: [{ count: 20 }] }) })
  ui.state.form.mode = 'llm'
  await ui.openCreate()
  ui.state.form.datasetId = generated.id
  await ui.loadOptions()
  assert.equal(ui.state.form.datasetId, '')
  assert.equal(ui.state.datasetIssue.value, '无可标注图片')
  assert.equal(ui.datasetDisabled(ui.state.datasets.value[0]), true)
  assert.equal(ui.ready.value, false)
  assert.deepEqual(ui.requests.errors, [])
})

test('LLM catalog failure leaves the visual model unselected', async () => {
  const ui = harness({
    route: { query: { dataset: ungenerated.id } },
    listSmartLlmModels: async () => { throw new Error('catalog unavailable') }
  })
  ui.state.form.mode = 'llm'
  await ui.openCreate()
  assert.equal(ui.state.form.modelName, '')
  assert.ok(ui.state.llmModelError.value)
  assert.equal(ui.ready.value, false)
  ui.state.form.datasetId = generated.id
  await ui.changeDataset()
  assert.equal(ui.ready.value, false)
})

test('LLM catalog excludes an unlisted provider default and selects the sole available vision model', async () => {
  const ui = harness({
    route: { query: { dataset: generated.id } },
    listSmartLlmProviders: async () => [{ id: '1', name: 'Provider', modelName: 'text-default' }],
    listSmartLlmModels: async () => ['vision-alternative']
  })
  ui.state.form.mode = 'llm'
  await ui.openCreate()
  assert.deepEqual(Array.from(ui.state.llmModels.value), ['vision-alternative'])
  assert.equal(ui.state.form.modelName, 'vision-alternative')
  assert.equal(ui.ready.value, true)
})

test('LLM page confirmation requires a strict finite score and excludes pilot and spot-check candidates', () => {
  const row = (id, confidence, manualReview = false, llmReviewKind) => ({
    candidate: { id, reviewState: 'PENDING', llmReviewKind, boxesJson: JSON.stringify([{ labelId: '1', confidence }]) },
    predictionQuality: { manualReview }
  })
  const predictions = [row('threshold', 0.85), row('above', 0.9), row('low', 0.8), row('missing', null),
    row('manual', 0.99, true), row('string', '0.99'), row('pilot', 0.99, false, 'PILOT'), row('sample', 0.99, false, 'SPOT_CHECK')]
  const confirmable = vm.runInNewContext(`(() => { ${eligibility}; return confirmable })()`, {
    computed: fn => ({ get value() { return fn() } }),
    predictions: { value: predictions }, llmTask: { value: true }, detail: { value: { task: { confidence: 0.85 } } }
  })
  assert.deepEqual(confirmable.value.map(item => item.candidate.id), ['above'])
})

test('a round containing only manual LLM reviews cannot be confirmed in bulk', () => {
  const eligibility = component.slice(component.indexOf('const canConfirmRound ='), component.indexOf('const showPredictions ='))
  const detail = { value: { counts: { PENDING: 2 }, pendingHard: 2 } }
  const llmTask = { value: true }
  const canConfirm = vm.runInNewContext(`(() => { ${eligibility}; return canConfirmRound })()`, {
    computed: fn => ({ get value() { return fn() } }), detail, llmTask
  })
  assert.equal(canConfirm.value, false)
  detail.value.counts.PENDING++
  assert.equal(canConfirm.value, true)
  detail.value.counts.PENDING = 2
  llmTask.value = false
  assert.equal(canConfirm.value, true)
})

test('spot-check filter requests pending spot-check candidates and clears on state change', async () => {
  const filters = component.slice(component.indexOf('const changePredictionFilter ='), component.indexOf('const resetPredictionQuery ='))
  const query = { page: 3, state: 'ACCEPTED', reviewKind: '' }
  const requests = []
  const { filterSpotChecks, changePredictionFilter } = vm.runInNewContext(`(() => {
    ${filters}
    return { filterSpotChecks, changePredictionFilter }
  })()`, { query, loadPredictions: async () => requests.push({ ...query }) })
  await filterSpotChecks(true)
  assert.deepEqual(requests[0], { page: 1, state: 'PENDING', reviewKind: 'SPOT_CHECK' })
  query.state = 'SKIPPED'
  await changePredictionFilter()
  assert.deepEqual(requests[1], { page: 1, state: 'SKIPPED', reviewKind: '' })
})

test('pilot preview never submits official labels and uses separate rerun and continue operations', async () => {
  const save = component.slice(component.indexOf('const saveReview ='), component.indexOf('const batchConfirm ='))
  const pilot = component.slice(component.indexOf('const continuePilot ='), component.indexOf('const retryLlm ='))
  const detail = { value: { task: { id: '501', taskState: 'PILOT_REVIEW', llmPrompt: 'initial' }, canContinueLlm: false } }
  const pilotPrompt = { value: '' }
  const rerunningPilot = { value: false }
  const requests = []
  const { saveReview, openPilotRerun, rerunPilot, continuePilot } = vm.runInNewContext(`(() => {
    ${save}
    ${pilot}
    return { saveReview, openPilotRerun, rerunPilot, continuePilot }
  })()`, {
    run: operation => operation(), detail, pilotReview: { value: true }, pilotPrompt, rerunningPilot,
    reviewItem: { value: { candidate: { id: '502' } } },
    reviewSmartPrediction: async () => requests.push('formal-review'),
    rerunSmartLlmPilot: async (id, { prompt }) => requests.push(['rerun', id, prompt]),
    continueSmartLlm: async id => requests.push(['continue', id]),
    clearPredictions: () => {}, refresh: async () => {}
  })
  await saveReview({ boxes: [] })
  await continuePilot()
  assert.deepEqual(requests, [])
  openPilotRerun()
  assert.equal(pilotPrompt.value, 'initial')
  assert.equal(rerunningPilot.value, true)
  pilotPrompt.value = '  revised  '
  await rerunPilot()
  assert.equal(rerunningPilot.value, false)
  detail.value.canContinueLlm = true
  await continuePilot()
  assert.deepEqual(requests, [['rerun', '501', 'revised'], ['continue', '501']])
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
