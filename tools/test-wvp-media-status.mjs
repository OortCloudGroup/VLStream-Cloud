import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { readFileSync, mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { pathToFileURL, fileURLToPath } from 'node:url'
import path from 'node:path'
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../VLStream-Web/VLStream-ui')
const require = createRequire(path.join(root, 'package.json'))
const { createRenderer, h } = require('vue')
const { createServer } = await import(pathToFileURL(path.join(root, 'node_modules/vite/dist/node/index.js')))
const messages = []
globalThis.__wvpTestMessages = messages
const storage = () => ({ getItem: () => null, removeItem() {} })
globalThis.sessionStorage = storage(); globalThis.localStorage = storage()
globalThis.window = { navigator:{userAgent:'node-regression'}, location: { origin: 'http://localhost', href: 'http://localhost/', search: '' },localStorage,sessionStorage }
const mocks = { name: 'wvp-status-mocks', enforce: 'pre', resolveId(id) {
  if (['element-plus', 'file-saver'].includes(id)) return '\0mock-' + id
}, load(id) {
  if (id === '\0mock-element-plus') return 'export const ElMessage={error:m=>globalThis.__wvpTestMessages.push(m)}'
  if (id === '\0mock-file-saver') return 'export const saveAs=()=>{}'
  if (id.replaceAll('\\','/').endsWith('/src/i18n/index.js')) return 'export const currentLocale={value:"zh-CN"}; export const translateUiMessage=x=>x'
} }
const server = await createServer({root, cacheDir:mkdtempSync(path.join(tmpdir(),'wvp-status-regression-')), configFile:false, plugins:[mocks], ssr:{noExternal:['element-plus','file-saver']}, resolve:{alias:{'@':path.join(root,'src')}}, server:{middlewareMode:true,hmr:false},logLevel:'error'})
let checks=0
const check = (name, fn) => { fn(); checks++; console.log('PASS',name) }
const deferred = () => { let resolve,reject; const promise=new Promise((a,b)=>{resolve=a;reject=b});return {promise,resolve,reject} }
try {
  const { normalizeWvpResponse, readWvpMediaAvailability } = await server.ssrLoadModule('/src/utils/wvpResponse.mjs')
  check('nested success and string code',()=>assert.equal(readWvpMediaAvailability({code:'200',data:{code:200,data:{available:true}}}),true))
  check('explicit unavailable',()=>assert.equal(readWvpMediaAvailability({code:200,data:{available:false}}),false))
  check('malformed availability remains an error',()=>assert.throws(()=>readWvpMediaAvailability({code:200,data:{available:'false'}})))
  check('inner tenant rejection survives outer success',()=>assert.throws(()=>normalizeWvpResponse({code:200,data:{code:403,msg:'tenant denied'}}),/tenant denied/))
  check('success=false remains an error',()=>assert.throws(()=>normalizeWvpResponse({code:200,success:false,msg:'denied'}),/denied/))
  check('table total normalized without changing source',()=>{const source={code:200,total:'2',rows:[{}]}; assert.equal(normalizeWvpResponse(source).total,2);assert.equal(source.total,'2')})
  const {useWvpMediaStatus}=await server.ssrLoadModule('/src/utils/useWvpMediaStatus.js')
  let state, next
  const renderer=createRenderer({createElement:()=>({}),insert(){},remove(){},setElementText(){},createText:()=>({}),setText(){},createComment:()=>({}),parentNode(){},nextSibling(){},patchProp(){}})
  const app=renderer.createApp({setup(){state=useWvpMediaStatus(()=>next.promise);return ()=>h('div')}})
  app.mount({})
  check('initial media state unknown',()=>{assert.equal(state.mediaAvailable.value,null);assert.equal(state.mediaLabel.value,'未知')})
  next=deferred(); let pending=state.loadMediaStatus()
  check('pending media state checking',()=>{assert.equal(state.mediaAvailable.value,null);assert.equal(state.mediaLabel.value,'检测中')})
  next.resolve({code:200,data:{code:200,data:{available:true}}}); await pending
  check('probe success available',()=>{assert.equal(state.mediaAvailable.value,true);assert.equal(state.mediaTagType.value,'success')})
  next=deferred();pending=state.loadMediaStatus();next.reject(new Error('probe timeout'));await pending
  check('failed probe is unknown, not unavailable',()=>{assert.equal(state.mediaAvailable.value,null);assert.equal(state.mediaLabel.value,'检测失败');assert.equal(state.mediaError.value,'probe timeout')})
  next=deferred();pending=state.loadMediaStatus();next.resolve({code:200,data:{available:false}});await pending
  check('confirmed unavailable is red',()=>{assert.equal(state.mediaAvailable.value,false);assert.equal(state.mediaTagType.value,'danger')})
  const older=deferred();next=older;const old=state.loadMediaStatus();next=deferred();pending=state.loadMediaStatus();next.resolve({code:200,data:{available:true}});await pending;older.resolve({code:200,data:{available:false}});await old
  check('late older probe cannot overwrite latest',()=>assert.equal(state.mediaAvailable.value,true))
  // Execute the actual page loader with a real Axios response interceptor and a synthetic tenant failure.
  const wvp=(await server.ssrLoadModule('/src/utils/wvpRequest.js')).default
  wvp.defaults.adapter=async config=>({config,data:{code:200,data:{code:403,msg:'tenant denied'}},status:200,statusText:'OK',headers:{}})
  await assert.rejects(wvp.get('/custom/device/list'), error => { assert.equal(error.message,'tenant denied'); return true })
  messages.length=0
  const source=readFileSync(path.join(root,'src/views/VideoAggregation/CustomProtocolDevice.vue'),'utf8')
  const body=source.match(/async function loadDevices\(\) \{([\s\S]*?)\n\}\nfunction search/)[1]
  const ctx={listGeneration:0,disposed:false,loading:{value:false},query:{pageNum:1},devices:{value:[{id:9}]},total:{value:1},statusClock:{value:0},selectedIds:{value:['9']},classificationDeviceKeys:{value:['9']},stampCustomStatus:x=>x,listCustomDevices:params=>wvp.get('/custom/device/list',{params})}
  const load=new Function('ctx',`with(ctx){return async function(){${body}}}`)(ctx)
  await load()
  check('actual list 403 reports error once and clears stale selection',()=>{assert.deepEqual(messages,['tenant denied']);assert.equal(ctx.loading.value,false);assert.deepEqual(ctx.devices.value,[]);assert.deepEqual(ctx.selectedIds.value,[]);assert.equal(state.mediaAvailable.value,true)})
  check('persistent list alert removed',()=>assert.equal(source.includes('v-if="serviceError"'),false))
  wvp.defaults.adapter=async config=>({config,data:{code:'200',data:{code:200,data:{available:true}}},status:200,statusText:'OK',headers:{}})
  const response=await wvp.get('/custom/device/media/status')
  check('real interceptor unwraps nested media response',()=>assert.equal(response.data.available,true))
  next=deferred();pending=state.loadMediaStatus();app.unmount();next.resolve({code:200,data:{available:false}});await pending
  check('unmounted page ignores late response',()=>assert.equal(state.mediaAvailable.value,null))
  console.log(`${checks} WVP media/list regression checks passed`)
} finally { await server.close() }
