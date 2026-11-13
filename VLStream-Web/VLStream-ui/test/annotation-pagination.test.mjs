import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'

const source = await readFile(new URL('../src/views/AlgorithmTraining/AlgorithmStandard.vue', import.meta.url), 'utf8')
const start = source.indexOf('// Only the requested page')
const end = source.indexOf('// annotationinstance\nconst reloadSavedImageAnnotations', start)
const loader = source.slice(start, end)
const image = id => ({ id, imageName: 'same-name.jpg', previewUrl: `/image/${id}`, instances: [] })
const page = (current, records, total = 330) => ({ current, records, total })

function harness(load) {
  const ref = value => ({ value })
  const state = {
    annotationLoadSequence: 0,
    annotationDataLoading: ref(false),
    annotationLoadError: ref(''),
    annotationImageQuery: ref({ page: 1, size: 10, annotationStatus: 'all', labelId: null }),
    annotationImageTotal: ref(0),
    annotationLabels: ref([]),
    selectedAnnotationLabel: ref(null),
    uploadedImages: ref([]),
    currentImageIndex: ref(-1),
    imageAnnotations: ref([]),
    selectedAnnotation: ref(null),
    getCurrentAnnotationId: () => '2118653742822842369',
    getAnnotationLabels: async () => ({ code: 200, data: [{ id: '12', name: 'person' }] }),
    getAnnotationImagePage: load,
    uiText: value => value
  }
  const actions = vm.runInNewContext(`(() => { ${loader}; return { loadAnnotationPage, handleAnnotationFilterChange, handleAnnotationPageChange } })()`, state)
  return { state, ...actions }
}

test('loads ten images and only their instances, then replaces the page', async () => {
  const calls = []
  const context = harness(async (id, query) => {
    calls.push({ id, ...query })
    return page(query.page, Array.from({ length: 10 }, (_, i) => image(String(query.page * 10 + i))))
  })
  assert.equal(await context.loadAnnotationPage(), true)
  assert.equal(calls[0].id, '2118653742822842369')
  assert.equal(calls[0].size, 10)
  context.state.annotationImageQuery.value.page = 2
  await context.loadAnnotationPage()
  assert.equal(context.state.uploadedImages.value.length, 10)
  assert.equal(context.state.uploadedImages.value[0].id, '20')
  assert.equal(context.state.annotationImageTotal.value, 330)
})

test('keeps images with identical names distinct by their complete IDs', async () => {
  const context = harness(async () => page(1, [image('2118653742822842369'), image('2118653742822842370')], 2))
  await context.loadAnnotationPage()
  assert.equal(context.state.uploadedImages.value.length, 2)
  assert.equal(context.state.uploadedImages.value[1].id, '2118653742822842370')
})

test('deleting the last page reloads the last valid page', async () => {
  const calls = []
  const context = harness(async (id, query) => {
    calls.push(query.page)
    return page(query.page, query.page === 11 ? [] : [image('a')], 11)
  })
  context.state.annotationImageQuery.value.page = 11
  await context.loadAnnotationPage()
  assert.deepEqual(calls, [11, 2])
  assert.equal(context.state.annotationImageQuery.value.page, 2)
  assert.equal(context.state.annotationDataLoading.value, false)
})

test('late responses cannot replace a newer page', async () => {
  let resolveFirst
  const context = harness(async (id, query) => query.page === 1
    ? new Promise(resolve => { resolveFirst = resolve })
    : page(2, [image('new')]))
  const first = context.loadAnnotationPage()
  context.state.annotationImageQuery.value.page = 2
  await context.loadAnnotationPage()
  resolveFirst(page(1, [image('old')]))
  assert.equal(await first, false)
  assert.equal(context.state.uploadedImages.value[0].id, 'new')
  assert.equal(context.state.annotationImageQuery.value.page, 2)
})

test('a load failure exposes retry state and releases the loading flag', async () => {
  const context = harness(async () => { throw new Error('unavailable') })
  assert.equal(await context.loadAnnotationPage(), false)
  assert.ok(context.state.annotationLoadError.value)
  assert.equal(context.state.annotationDataLoading.value, false)
})

test('legacy malformed annotations are skipped while valid instances remain editable', async () => {
  const sample = image('2118653742822842369')
  sample.instances = [
    { id: '1', labelId: '12', annotationType: 'rect', annotationData: '{invalid' },
    { id: '2', labelId: '12', annotationType: 'rect', annotationData: '{"x":10}' }
  ]
  const context = harness(async () => page(1, [sample], 1))
  assert.equal(await context.loadAnnotationPage(), true)
  assert.equal(context.state.uploadedImages.value[0].annotations.length, 1)
  assert.equal(context.state.uploadedImages.value[0].annotations[0].id, '2')
})

test('object-storage errors are exposed as a retryable page failure', async () => {
  const sample = { ...image('1'), previewError: 'unavailable' }
  const context = harness(async () => page(1, [sample], 1))
  assert.equal(await context.loadAnnotationPage(), false)
  assert.ok(context.state.annotationLoadError.value)
})

test('filter changes reset the page and send label and status to the server', async () => {
  const calls = []
  const context = harness(async (id, query) => { calls.push(query); return page(query.page, [], 0) })
  context.state.annotationImageQuery.value.page = 4
  context.handleAnnotationFilterChange({ labelId: '12', annotationStatus: 'annotated' })
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(calls[0].page, 1)
  assert.equal(calls[0].labelId, '12')
  assert.equal(calls[0].annotationStatus, 'annotated')
})

test('generation repair accepts the server-located page for the requested image', async () => {
  const context = harness(async (id, query) => {
    assert.equal(query.imageId, '2118653742822842369')
    return page(4, [image(query.imageId)])
  })
  await context.loadAnnotationPage({ imageId: '2118653742822842369' })
  assert.equal(context.state.annotationImageQuery.value.page, 4)
})

test('grid uses the shared page-size choices without loading all instances', async () => {
  const grid = await readFile(new URL('../src/views/AlgorithmTraining/AnnotationGridView.vue', import.meta.url), 'utf8')
  assert.match(grid, /:page-sizes="\[10, 20, 50, 100\]"/)
  assert.doesNotMatch(grid, /getAllAnnotationInstances|handleGridScroll|renderCount/)
})
