import test from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import * as Vue from 'vue'
import {parse, compileScript} from 'vue/compiler-sfc'
import * as layout from '../src/utils/videoSquareLayout.mjs'
import {applyVideoPresentation} from '../src/utils/videoSquarePresentation.mjs'
const descriptor=parse(readFileSync(new URL('../src/views/VideoAggregation/VideoSquareRefactored.vue',import.meta.url),'utf8')).descriptor
const compiled=compileScript(descriptor,{id:'square-entry'})
const code=compiled.content.replace(/^import .*$/gm,'').replaceAll('import.meta.env','({})').replaceAll('import.meta.url',JSON.stringify(import.meta.url)).replace('export default','return')
function square(view='video',presentation='default',count=1,closed=false) {
    const scope=Vue.effectScope(),calls=[]
    const deps=Object.fromEntries(Object.keys(compiled.imports).map(name=>[name,()=>{}]))
    Object.assign(deps,Vue,layout,{onMounted:()=>{},onUnmounted:()=>{},applyVideoPresentation,
        readVideoSquarePreferences:()=>({view,presentation,layout:{count,variant:'grid'},closed}),saveVideoSquarePreferences:()=>{},
        useCustomAvailabilityClock:()=>Vue.ref(Date.now()),createVideoAddressResolver:()=>()=>Promise.resolve(''),
        WEBRTC_SERVER_BASE_URL:'',VIDEO_PROTOCOLS:[],ElMessage:{warning(){},info(){},success(){},error(){}},
        getVideoDeviceCatalog:()=>{calls.push('catalog');return Promise.resolve({devices:[]})},
        normalizeVideoDevice:device=>device,videoDeviceForProtocols:device=>device,matchesVideoDevice:()=>true})
    delete deps['module.exports']
    const component=new Function(...Object.keys(deps),code)(...Object.values(deps))
    return {state:scope.run(()=>component.setup({},{expose(){}})),calls,stop:()=>scope.stop()}
}
test('first entry mounts the saved layout with no selected or requested video and collapsed catalogue',()=>{
    for(const view of ['video','map','list']) for(const mode of ['default','grid','bottom']) {
        const h=square(view,mode,8),s=h.state,d=s.layoutDialogs.value[0]
        assert.equal(s.viewMode.value,view);assert.equal(s.presentationMode.value,mode)
        assert.equal(d.layoutCount,mode==='default'?1:8)
        assert.ok(d.cameras.every(camera=>camera===null))
        assert.equal(s.devicePanelCollapsed.value,true);assert.deepEqual(h.calls,[]);h.stop()
    }
})
test('layouts and repeated bottom selection preserve the view and never auto-bind directory devices',()=>{
    for(const view of ['video','map','list']) {
        const h=square(view),s=h.state
        s.deviceList.value=[{id:'not-selected',deviceName:'Do not autoplay'}]
        for(const [count,variant] of [[1,'grid'],[8,'grid'],[2,'two-overlay'],[3,'three-left'],[25,'grid']]) {
            s.handleLayoutSelect(count,variant)
            assert.equal(s.viewMode.value,view);assert.equal(s.layoutDialogs.value[0].layoutCount,count)
            assert.ok(s.layoutDialogs.value[0].cameras.every(camera=>camera===null))
        }
        s.selectPresentation('bottom');s.selectPresentation('bottom')
        assert.equal(s.viewMode.value,view);assert.equal(s.layoutDialogs.value[0].layoutCount,4)
        s.selectPresentation('default');assert.equal(s.viewMode.value,view)
        assert.equal(s.layoutDialogs.value[0].layoutCount,1);assert.deepEqual(h.calls,[]);h.stop()
    }
})
test('red phone removes the last slot without recreating a window',()=>{
    const h=square('list','grid',3),s=h.state,d=s.layoutDialogs.value[0]
    d.cameras=[{id:'a'},{id:'b'},{id:'c'}]
    s.removeDeviceFromWindow(d.id,1)
    assert.equal(d.layoutCount,2);assert.deepEqual(d.cameras.map(item=>item.id),['a','c'])
    s.removeDeviceFromWindow(d.id,1);s.removeDeviceFromWindow(d.id,0)
    const replacement=s.layoutDialogs.value[0]
    assert.equal(replacement.id,d.id);assert.equal(replacement.layoutCount,0);assert.equal(s.layoutClosed.value,true);assert.deepEqual(replacement.cameras,[])
    assert.equal(s.viewMode.value,'list');assert.equal(s.presentationMode.value,'grid')
    assert.deepEqual(h.calls,[]);h.stop()
})
test('double click enlarges the clicked source instead of playing the first directory row',()=>{
    const h=square('map','grid',3),s=h.state,d=s.layoutDialogs.value[0]
    d.cameras=[{id:'first'},null,{id:'clicked'}]
    s.handleVideoWindowDoubleClick(d.id,2)
    assert.equal(d.layoutCount,1);assert.equal(d.cameras[0].id,'clicked');assert.equal(s.viewMode.value,'map');h.stop()
})



test('all views restore a deliberately closed workspace with zero windows until an explicit action',()=>{
    for(const view of ['video','map','list']) for(const mode of ['default','grid','bottom']) {
        const h=square(view,mode,8,true),s=h.state,d=s.layoutDialogs.value[0]
        assert.equal(d.layoutCount,0);assert.deepEqual(d.cameras,[])
        s.ensureLayoutDialog();assert.equal(d.layoutCount,0)
        s.deviceList.value=[{id:'a',deviceName:'A'}]
        assert.equal(d.layoutCount,0)
        s.restoreLayoutWindows(d.id)
        assert.equal(d.layoutCount,mode==='default'?1:8)
        assert.ok(d.cameras.every(camera=>!camera));assert.equal(s.layoutClosed.value,false)
        assert.equal(s.viewMode.value,view);h.stop()
    }
})
test('stale close and stale picker events cannot close or replace the next window',()=>{
    const h=square('video','grid',3),s=h.state,d=s.layoutDialogs.value[0]
    const oldId=d.windowIds[0]
    d.cameras=[{id:'a'},{id:'b'},{id:'c'}];d.activeWindowIndex=2
    s.addDeviceToWindow(d.id,0)
    s.removeDeviceFromWindow(d.id,0,oldId)
    assert.equal(d.activeWindowIndex,1);assert.equal(d.cameras[1].id,'c')
    s.removeDeviceFromWindow(d.id,0,oldId)
    assert.equal(d.layoutCount,2);assert.equal(d.cameras[0].id,'b')
    s.deviceList.value=[{id:'a'}];s.assignSelectedDevice({id:'a'})
    assert.equal(d.cameras[0].id,'b');h.stop()
})
test('explicit source selection after closing creates one new window and never plays a neighbour',()=>{
    for(const mode of ['default','grid','bottom']) {
        const h=square('list',mode,1),s=h.state,d=s.layoutDialogs.value[0],old=d.windowIds[0]
        s.removeDeviceFromWindow(d.id,0,old);assert.equal(d.layoutCount,0)
        s.handleDeviceSelect({id:'new',deviceName:'New'})
        assert.equal(d.layoutCount,1);assert.equal(d.cameras[0].id,'new')
        assert.notEqual(d.windowIds[0],old);assert.equal(s.viewMode.value,'list');h.stop()
    }
})
