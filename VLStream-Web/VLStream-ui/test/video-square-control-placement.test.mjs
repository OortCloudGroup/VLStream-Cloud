import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import * as Vue from 'vue'
import { createSSRApp, h } from 'vue'
import { parse, compileTemplate } from 'vue/compiler-sfc'
import { renderToString } from 'vue/server-renderer'

const descriptor = parse(readFileSync(new URL('../src/components/VideoLayoutDialog.vue', import.meta.url), 'utf8')).descriptor
const generated = compileTemplate({source:descriptor.template.content, filename:'VideoLayoutDialog.vue', id:'control-placement'})
assert.deepEqual(generated.errors, [])
const names = [...new Set([...generated.code.matchAll(/_ctx\.([\w$]+)/g)].map(match => match[1]))]
const bindings = [...generated.code.matchAll(/import\s*\{([^}]+)\}\s*from\s*['"]vue['"]/g)]
    .flatMap(match => match[1].split(',').map(value => value.trim().split(/\s+as\s+/)))
    .map(([name,alias]) => `${name}:${alias || name}`).join(',')
const renderCode = generated.code.replace(/^import\s+.*?from\s+['"]vue['"];?\s*$/gm, '').replace('export function render', 'return function render')
const render = new Function('__vue', `const {${bindings}}=__vue;${renderCode}`)(Vue)

async function layout(mode, count = 1) {
    const noop = () => {}
    const context = Object.fromEntries(names.map(name => [name, noop]))
    Object.assign(context, {
        $tp:value => value, embedded:true, maximized:false, presentationMode:mode,
        dialog:{id:1, position:{x:0,y:0}, layoutCount:count, cameras:[{id:'fixture',name:'Fixture',deviceData:{}}]},
        activeWindowIndex:0, activeCamera:{id:'fixture'}, active:true, showPtz:false,
        draggedWindowIndex:null, dragOverWindowIndex:null, movingWindowIndex:null,
        floatingWindows:{}, thumbnailDevices:[], thumbnailFrames:{}, windowMedia:{},
        mediaState:{muted:true,canMute:false,canScreenshot:false}, playerFullscreen:false, capturing:false,
        workspaceSize:{width:900,height:600}, carouselWidth:300, carouselOffset:0,
        getLayoutClass:() => 'custom', videoGridStyle:() => ({}), windowPlacement:() => ({}),
    })
    const app = createSSRApp({render:() => render(context, [])})
    app.component('VideoSquarePlaybackControls', {
        props:['overlay','compact'],
        setup:props => () => h('i', {'data-toolbar':props.overlay ? 'overlay' : 'normal'}),
    })
    for (const name of ['VideoSquareThumbnailStrip','VideoSquareFloatingPtz','VideoSquareDevicePlayer','VlsDevicePlayer']) {
        app.component(name, {render:() => null})
    }
    return renderToString(app)
}

test('actual default template has one independent toolbar after the picture and no header strip', async () => {
    const html = await layout('default')
    assert.equal((html.match(/data-toolbar=/g) || []).length, 1)
    assert.equal(html.includes('window-titlebar'), false)
    assert.ok(html.indexOf('data-toolbar=') > html.indexOf('no-signal-placeholder'))
    assert.match(html, /default-playback-controls/)
})

test('actual bottom and split templates keep their header controls without the default footer', async () => {
    for (const [mode,count] of [['bottom',1],['grid',4]]) {
        const html = await layout(mode,count)
        assert.equal((html.match(/data-toolbar=/g) || []).length, 1)
        assert.ok(html.indexOf('window-titlebar') < html.indexOf('data-toolbar='))
        assert.ok(html.indexOf('data-toolbar=') < html.indexOf('no-signal-placeholder'))
        assert.equal(html.includes('default-playback-controls'), false)
        assert.match(html, mode === 'grid' ? /data-toolbar="overlay"/ : /data-toolbar="normal"/)
    }
})
