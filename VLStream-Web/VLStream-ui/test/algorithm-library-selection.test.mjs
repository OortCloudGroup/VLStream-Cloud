import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import { algorithmRepositoryOptions } from '../src/utils/algorithmRepositoryOptions.mjs'
test('repository selector preserves long IDs and nested category membership', () => {
 const id = '2122225251566968834'
 const result = algorithmRepositoryOptions([{id,name:'库',parentId:'0',status:1},{id:'2122225251566968835',parentId:id,name:'下级',status:1}])
 assert.equal(result[0].id,id)
 assert.equal(result[0].children[0].parentId,id)
 assert.equal(result[0].disabled,false)
})
test('disabled catalogs stay visible but cannot be assigned', () => {
 assert.equal(algorithmRepositoryOptions([{id:'1',name:'停用',status:0}])[0].disabled,true)
})
test('empty, orphaned and cyclic catalogs do not lose nodes or recurse forever', () => {
 assert.deepEqual(algorithmRepositoryOptions(null),[])
 assert.equal(algorithmRepositoryOptions([{id:'1',parentId:'missing'}]).length,1)
 assert.equal(algorithmRepositoryOptions([{id:'1',parentId:'2'},{id:'2',parentId:'1'}]).length,2)
})
test('catalog selection has accessible selected styles', () => {
 const source=readFileSync(new URL('../src/views/AlgorithmWarehouse/components/AlgorithmCatalogView.vue',import.meta.url),'utf8')
 assert.match(source, /active: activeCategoryId === group.id/)
 assert.match(source, /active: activeCategoryId === child.id/)
 assert.match(source, /aria-pressed/)
})
test('algorithm editor loads authorized catalogs and submits repositoryId', () => {
 const source=readFileSync(new URL('../src/views/AlgorithmWarehouse/AlgorithmManagement.vue',import.meta.url),'utf8')
 assert.match(source, /getAlgorithmCatalogCategories\(\)/)
 assert.match(source, /el-tree-select v-model="algorithmEditForm.repositoryId"/)
 assert.match(source, /repositoryId: algorithmEditForm.value.repositoryId/)
 assert.match(source, /repositoryId: normalizeRepositoryId\(algorithm.repositoryId\)/)
})

test('disabled ancestor prevents assignment to an enabled child', () => {
 const result=algorithmRepositoryOptions([{id:'1',status:0},{id:'2',parentId:'1',status:1}])
 assert.equal(result[0].children[0].disabled,true)
})

test('tree and flat navigation share selected category and catalog filtering API', () => {
 const source=readFileSync(new URL('../src/views/AlgorithmWarehouse/components/AlgorithmCatalogView.vue',import.meta.url),'utf8')
 assert.match(source, /@node-click="selectCategory"/)
 assert.match(source, /activeCategoryId.value = data.id === ROOT_ID \? '0' : String\(data.id\)\s*return submitAlgorithmSearch\(\)/)
 assert.match(source, /activeCategoryId.value = String\(id\); return submitAlgorithmSearch\(\)/)
 assert.match(source, /getAlgorithmCatalogPage\(\{\s*categoryId: activeCategoryId.value === '0' \? undefined : activeCategoryId.value/)
})
import { runInNewContext } from 'node:vm'

for (const loaded of [false, true]) {
 test(`algorithm save ${loaded ? 'saves loaded review' : 'preserves unloaded review without a write'}`, async () => {
  const source=readFileSync(new URL('../src/views/AlgorithmWarehouse/AlgorithmManagement.vue',import.meta.url),'utf8')
  const start=source.indexOf('const handleAlgorithmEditConfirm = async () => {')
  const end=source.indexOf('const handleAlgorithmEditCancel =',start)
  const calls=[]
  const context={
   imageUploading:{value:false},algorithmEditFormRef:{value:{validate:async()=>{}}},submitting:{value:false},
   algorithmEditForm:{value:{repositoryId:'2',name:'fire',imageUrl:''}},editingAlgorithm:{value:{id:'9007199254740993'}},
   updateAlgorithm:async(id,data)=>{calls.push(['algorithm',id,data.repositoryId]);return {code:200}},
   catalogRef:{value:{refreshAll:async()=>calls.push(['refresh'])}},llmReviewConfigLoaded:{value:loaded},
   llmReviewForm:{value:{enabled:false}},saveAlgorithmLlmReviewConfig:async(id)=>{calls.push(['review',id]);return {code:200}},
   ElMessage:{success:()=>{},error:message=>{throw new Error(message)}},uiText:x=>x,
   showAlgorithmEditDialog:{value:true},loadAlgorithmsByRepository:async()=>{},console
  }
  await runInNewContext(source.slice(start,end)+'\nhandleAlgorithmEditConfirm()',context)
  assert.equal(calls.filter(call=>call[0]==='algorithm').length,1)
  assert.equal(calls.filter(call=>call[0]==='review').length,loaded?1:0)
  assert.equal(calls[0][1],'9007199254740993')
  assert.equal(calls[0][2],'2')
  assert.equal(context.showAlgorithmEditDialog.value,false)
 })
}

for (const isSystem of ['YES', 'NO']) {
 test(`create ${isSystem} forwards cover and review and omits hidden draft paths`, async () => {
  const source = readFileSync(new URL('../src/views/AlgorithmWarehouse/AlgorithmManagement.vue', import.meta.url), 'utf8')
  const handler = source.slice(source.indexOf('const handleAlgorithmAddConfirm ='), source.indexOf('const handleAlgorithmEditConfirm ='))
  const pendingCover = { name: 'cover.png' }
  const review = { enabled: true, providerId: '42' }
  const calls = []
  const context = {
   imageUploading: {value:false}, submitting: {value:false}, algorithmEditFormRef: {value:{validate:async () => {}}},
   algorithmEditForm: {value:{name:'fire', repositoryId:'100', isSystem, ptModelFilePath:'/models/base.pt', onnxModelFilePath:'/models/base.onnx'}},
   pendingCover: {value:pendingCover}, llmReviewForm: {value:review}, showAlgorithmEditDialog: {value:true},
   createAlgorithm: async (...args) => { calls.push(args); return {code:200} },
   loadAlgorithmsByRepository: async () => {}, catalogRef:{value:null}, uiText:key => key,
   ElMessage: {success:() => {}, error:message => assert.fail(message)}
  }
  await runInNewContext(handler + '\nhandleAlgorithmAddConfirm()', context)
  assert.equal(calls.length, 1)
  const [data, file, config] = calls[0]
  assert.equal(data.isSystem, isSystem)
  assert.equal(data.ptModelFilePath, isSystem === 'NO' ? '/models/base.pt' : '')
  assert.equal(data.onnxModelFilePath, isSystem === 'NO' ? '/models/base.onnx' : '')
  assert.equal(file, pendingCover)
  assert.equal(config, review)
  assert.equal(context.showAlgorithmEditDialog.value, false)
 })
}
