import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { parse, compileScript } from 'vue/compiler-sfc'
import { applyVideoPresentation, captureThumbnailFrame } from '../src/utils/videoSquarePresentation.mjs'

test('default and bottom move the same focused camera without recreating its object',()=>{
    const first={id:'ONVIF:1'},selected={id:'VLSTREAM:2'}
    const dialog={layoutCount:2,cameras:[first,selected],activeWindowIndex:1}
    applyVideoPresentation(dialog,'default')
    assert.equal(dialog.cameras[0],selected)
    assert.equal(dialog.layoutCount,1)
    const cameras=dialog.cameras
    applyVideoPresentation(dialog,'bottom')
    assert.equal(dialog.cameras,cameras)
    assert.equal(dialog.cameras[0],selected)
    assert.equal(dialog.presentationMode,'bottom')
    applyVideoPresentation(dialog,'grid')
    assert.equal(dialog.cameras,cameras)
    assert.throws(()=>applyVideoPresentation(dialog,'unknown'))
    assert.equal(dialog.presentationMode,'grid')
})

test('thumbnails contain only a decodable actual frame and preserve portrait aspect within the size limit',()=>{
    const draws=[],canvas={getContext:()=>({drawImage:(...args)=>draws.push(args)}),toDataURL:()=> 'data:image/jpeg;base64,controlled'}
    assert.equal(captureThumbnailFrame({readyState:1,videoWidth:1920,videoHeight:1080},()=>canvas),null)
    const video={readyState:2,videoWidth:720,videoHeight:1280}
    assert.match(captureThumbnailFrame(video,()=>canvas),/^data:image\/jpeg/)
    assert.equal(canvas.width,158);assert.equal(canvas.height,280)
    assert.equal(draws[0][0],video)
    assert.equal(captureThumbnailFrame(video,()=>({getContext:()=>({drawImage(){throw new Error('cross origin')}})})),null)
})

const source=readFileSync(new URL('../src/components/VideoLayoutControls.vue',import.meta.url),'utf8')
const compiled=compileScript(parse(source).descriptor,{id:'presentation-controls',inlineTemplate:true}).content
const stripped=compiled.replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm,'').replace('export default','return')
const vue=await import('vue')
const imported=[...compiled.matchAll(/import\s*\{([^}]+)\}\s*from\s*['"]vue['"]/g)].flatMap(m=>m[1].split(',').map(x=>x.trim().split(/\s+as\s+/))).map(([from,to])=>[to||from,vue[from]])
const deps=Object.fromEntries(imported)
const assets=[...compiled.matchAll(/^import\s+(\w+)\s+from\s+['"][^'"]+['"];?\s*$/gm)].map(match=>match[1])
const controls=new Function('deps',`const {${Object.keys(deps).join(',')}}=deps;const FullScreen={render(){return null}};${assets.map(name=>`const ${name}='test-asset';`).join('')}${stripped}`)(deps)
test('actual layout controls show both named design options and emit the selected mode',async()=>{
    const sent=[],clicks={}
    const app=createSSRApp(controls,{showPresentation:true,presentationMode:'bottom',layoutMode:'1x1',onPresentationSelect:value=>sent.push(value)})
    app.config.globalProperties.$tp=x=>x
    app.component('el-button',{props:['type'],setup(props,{attrs,slots}){if(attrs['aria-label'])clicks[attrs['aria-label']]=attrs.onClick;return()=>h('button',{'data-variant':props.type},slots.default?.())}})
    app.component('el-icon',{template:'<i><slot/></i>'})
    const html=await renderToString(app)
    assert.match(html,/aria-label="默认展示"/)
    assert.match(html,/aria-label="底部排列"/)
    assert.match(html,/aria-pressed="true"/)
    assert.equal((html.match(/data-variant="primary"/g)||[]).length,1)
    clicks['底部排列']()
    assert.deepEqual(sent,['bottom'])
})
