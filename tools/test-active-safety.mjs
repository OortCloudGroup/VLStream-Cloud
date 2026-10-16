// Synthetic browser regression: real secure.vue logic/template and Element Plus;
// every imported business service is replaced, and no real backend is contacted.
// Set VLS_PLAYWRIGHT_MODULE to a local Playwright package path when it is not installed.
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { readFileSync, mkdirSync } from 'node:fs'
import { createServer } from 'node:http'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const ui = path.join(root, 'VLStream-Web/VLStream-ui')
const requireUi = createRequire(path.join(ui, 'package.json'))
const { parse, compileTemplate } = requireUi('@vue/compiler-sfc')
const { transformSync } = requireUi('esbuild')
const source = readFileSync(path.join(ui, 'src/views/events/page/eventManagement/secure.vue'), 'utf8')
const { descriptor } = parse(source)
const setup = descriptor.scriptSetup.content.replace(/^import .*$/gm, '')
const names = [...setup.matchAll(/^(?:const|let) (\w+)/gm)].map(match => match[1])
const script = transformSync(setup, { loader: 'ts', target: 'es2020' }).code
const template = compileTemplate({ source: descriptor.template.content, filename: 'secure.vue', id: 'synthetic',
  compilerOptions: { expressionPlugins: ['typescript'] } })
assert.deepEqual(template.errors, [])
const renderSource = template.code.replace(/import \{([\s\S]*?)\} from "vue"/, (_, imports) =>
  `const {${imports.replace(/\bas\b/g, ':')}} = Vue`).replace('export function render', 'function render')
const render = transformSync(renderSource, { loader: 'ts', target: 'es2020' }).code
const serialize = value => JSON.stringify(value).replace(/</g, '\\u003c')
const assets = {
  '/vue.js': 'vue/dist/vue.global.js',
  '/element.js': 'element-plus/dist/index.full.js',
  '/element.css': 'element-plus/dist/index.css',
  '/dayjs.js': 'dayjs/dayjs.min.js'
}
const html = `<!doctype html><html lang="zh-CN"><meta charset="utf-8"><link rel="stylesheet" href="/element.css">
<style>body{margin:20px;font-family:sans-serif}.eventImg{width:48px;height:48px}.device-table-panel{width:100%}.tabs-btn{margin:12px 0}.paginationBox{margin-top:18px}</style>
<div id="app"></div><script src="/vue.js"></script><script src="/element.js"></script><script src="/dayjs.js"></script><script>
const picture = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48"><rect width="48" height="48" fill="steelblue"/></svg>');
const row=(id)=>({id,uuid:'same-camera-owner',created_at:'2026-10-15 12:00:00',status:2,point:{address:'合成测试位置'},item:'synthetic_event',pics:[picture],device_name:'Synthetic Camera',device_id:'synthetic-device',describe:'仅测试数据'});
const fixture=window.fixture={rows:[row('page1-a'),row('page1-b'),row('page1-c'),row('page2-a')],deletions:[],outcomes:{},messages:[],confirm:'accept',calls:[],pending:[]};
const translate=(text,params)=>text.replace(/\\{(\\w+)\\}/g,(match,key)=>params&&key in params?String(params[key]):match);
const eventList=async(data)=>{fixture.calls.push({...data});if(fixture.defer){return new Promise(resolve=>fixture.pending.push({data:{...data},resolve}));}return {code:200,data:{list:fixture.rows.filter(r=>r.id.startsWith(data.page===2?'page2':'page1')),count:20}}};
const delEvent=async({id})=>{fixture.deletions.push(id);const result=fixture.outcomes[id];if(result==='network')throw new Error('synthetic network failure');if(result==='business')return {code:5003,msg:'synthetic business failure'};fixture.rows=fixture.rows.filter(r=>r.id!==id);return {code:200}};
const bindings={ref:Vue.ref,reactive:Vue.reactive,onMounted:Vue.onMounted,uiText:translate,translatePhrase:translate,
ElMessage:Object.fromEntries(['success','warning','error'].map(type=>[type,text=>{fixture.messages.push({type,text});ElementPlus.ElMessage[type](text)}])),
ElMessageBox:{confirm:()=>fixture.confirm==='cancel'?Promise.reject('cancel'):fixture.confirm==='pending'?new Promise(resolve=>fixture.confirmResolve=resolve):Promise.resolve()},
getClassificationDeviceIds:async()=>({data:[]}),delEvent,eventList,event_item_list:async()=>({code:200,data:{list:[]}}),
clacPXToVW:v=>v,useUserStore:()=>({token:'synthetic-token',userInfo:{}}),covertCurrentLocationURL:v=>v,dayjs};
const runSetup=new Function(...Object.keys(bindings),${serialize(script + '\nreturn {covertCurrentLocationURL,clacPXToVW,' + names.join(',') + '}')});
const renderPage=new Function('Vue',${serialize(render + '\nreturn render')})(Vue);
const app=Vue.createApp({render:renderPage,setup(){const state=runSetup(...Object.values(bindings));fixture.state=state;return state;}});
app.use(ElementPlus);app.config.globalProperties.$tp=translate;
app.component('DeviceClassificationLayout',{template:'<div><slot/></div>'});
app.component('button-group',{props:['buttonList'],template:'<button data-test="delete" @click="buttonList[0].clickFn">删除所选事件</button>'});
for(const name of ['search-height-box','export-excel-pdf','ConfirmEventDialog','event-phone-f-back','event-details-dialog','event-phone-confirm','SchedulingDialog','OortVideoPlayer','oort-svg-icon'])app.component(name,{template:'<span/>'});
app.mount('#app');</script></html>`

const server = createServer((req, res) => {
  if (req.url === '/') { res.setHeader('Content-Type', 'text/html; charset=utf-8'); res.end(html); return }
  if (assets[req.url]) {
    res.setHeader('Content-Type', req.url.endsWith('.css') ? 'text/css' : 'text/javascript')
    res.end(readFileSync(requireUi.resolve(assets[req.url])))
    return
  }
  res.writeHead(404).end()
})
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
const address = `http://127.0.0.1:${server.address().port}`
const playwright = process.env.VLS_PLAYWRIGHT_MODULE
  ? await import(pathToFileURL(process.env.VLS_PLAYWRIGHT_MODULE)) : requireUi('playwright')
let browser
let checks = 0
const check = () => { checks++ }
try {
  browser = await playwright.chromium.launch({ headless: true,
    ...(process.env.VLS_CHROMIUM_EXECUTABLE ? { executablePath: process.env.VLS_CHROMIUM_EXECUTABLE } : {}) })
  const page = await browser.newPage({ viewport: { width: 1700, height: 950 } })
  const errors = []
  const outsideRequests = []
  page.on('pageerror', error => { errors.push(error.message); console.error('Synthetic page error:', error.message) })
  page.on('console', message => { if (message.type() === 'error') console.error(message.text()) })
  await page.route('**/*', async route => {
    if (route.request().url().startsWith(address) || route.request().url().startsWith('data:')) await route.continue()
    else { outsideRequests.push(route.request().url()); await route.abort() }
  })
  const fresh = async () => {
    await page.goto(address)
    await page.waitForFunction(() => window.fixture?.state?.tableData.value.length === 3)
    await page.locator('.el-table__body-wrapper tbody tr').first().waitFor()
  }
  const selected = () => page.evaluate(() => fixture.state.selectedRows.value.map(row => row.id))
  const all = async () => {
    await page.locator('.el-table__header-wrapper .el-checkbox').click()
    await page.waitForFunction(() => fixture.state.selectedRows.value.length > 0)
  }
  const remove = () => page.getByRole('button', { name: '删除所选事件' }).click()
  const waitDeletion = () => page.waitForFunction(() => !fixture.state.deleting.value && fixture.deletions.length > 0)

  await fresh()
  await remove()
  assert.deepEqual(await page.evaluate(() => fixture.messages.at(-1)), { type: 'warning', text: '请选择要删除的事件' }); check()
  await all()
  assert.deepEqual(await selected(), ['page1-a', 'page1-b', 'page1-c']); check()
  await page.locator('.el-pagination .el-pager').getByText('2', { exact: true }).click()
  await page.waitForFunction(() => fixture.state.tableData.value[0]?.id === 'page2-a')
  assert.deepEqual(await selected(), []); check()
  assert.equal(await page.locator('.el-table__body-wrapper .el-checkbox.is-checked').count(), 0); check()
  await page.locator('.el-table__body-wrapper tbody tr').first().click()
  assert.deepEqual(await selected(), ['page2-a']); check()
  await page.locator('.el-pagination .el-pager').getByText('1', { exact: true }).click()
  await page.waitForFunction(() => fixture.state.tableData.value[0]?.id === 'page1-a')
  assert.deepEqual(await selected(), []); check()
  await page.evaluate(() => fixture.state.getList())
  assert.deepEqual(await selected(), []); check()
  assert.equal(await page.locator('.el-table__body-wrapper img').first().evaluate(img => img.complete && img.naturalWidth > 0), true); check()

  await page.evaluate(() => { ElementPlus.ElMessage.closeAll(); fixture.outcomes['page1-b'] = 'business'; fixture.outcomes['page1-c'] = 'network' })
  await all(); await remove(); await waitDeletion()
  assert.deepEqual(await page.evaluate(() => fixture.deletions), ['page1-a', 'page1-b', 'page1-c']); check()
  assert.deepEqual(await page.evaluate(() => fixture.messages.at(-1)), { type: 'warning', text: '已删除 1 条事件，2 条删除失败，请重试' }); check()
  assert.equal(await page.locator('.el-table__body-wrapper tbody tr').count(), 2); check()
  const output = path.join(root, 'output/playwright')
  mkdirSync(output, { recursive: true })
  await page.waitForFunction(() => [...document.querySelectorAll('.el-message')].some(message =>
    message.textContent.includes('已删除 1 条事件，2 条删除失败，请重试') && getComputedStyle(message).opacity === '1'))
  await page.screenshot({ path: path.join(output, 'active-safety-partial-delete.png'), fullPage: true })
  await page.evaluate(() => { fixture.outcomes = {}; fixture.messages = [] })
  await all(); await remove(); await waitDeletion()
  assert.deepEqual(await page.evaluate(() => fixture.messages.at(-1)), { type: 'success', text: '已删除 2 条事件' }); check()
  assert.equal(await page.evaluate(() => fixture.deletions.includes('page2-a')), false); check()

  await fresh(); await all()
  await page.evaluate(() => { fixture.confirm = 'cancel' })
  await remove()
  assert.deepEqual(await page.evaluate(() => fixture.deletions), []); check()
  await page.evaluate(() => { fixture.confirm = 'pending' })
  await remove(); await remove()
  await page.evaluate(() => fixture.confirmResolve())
  await waitDeletion()
  assert.equal(await page.evaluate(() => fixture.deletions.length), 3); check()

  await fresh(); await all()
  await page.evaluate(() => { fixture.outcomes = Object.fromEntries(fixture.rows.map(row => [row.id, 'business'])) })
  await remove(); await waitDeletion()
  assert.deepEqual(await page.evaluate(() => fixture.messages.at(-1)), { type: 'error', text: '3 条事件删除失败，请重试' }); check()

  await fresh()
  await page.evaluate(() => {
    fixture.defer = true
    fixture.state.params.page = 1; fixture.state.getList()
    fixture.state.params.page = 2; fixture.state.getList()
    fixture.pending[1].resolve({ code: 200, data: { list: [fixture.rows[3]], count: 20 } })
  })
  await page.waitForFunction(() => fixture.state.tableData.value[0]?.id === 'page2-a')
  await page.evaluate(async () => {
    fixture.pending[0].resolve({ code: 200, data: { list: fixture.rows.slice(0, 3), count: 20 } })
    await Vue.nextTick()
  })
  assert.equal(await page.evaluate(() => fixture.state.tableData.value[0].id), 'page2-a'); check()
  assert.deepEqual(errors, []); check()
  assert.deepEqual(outsideRequests, []); check()
  console.log(`Active-safety browser regressions: ${checks} checks passed; no external requests.`)
} finally {
  await browser?.close()
  await new Promise(resolve => server.close(resolve))
}
