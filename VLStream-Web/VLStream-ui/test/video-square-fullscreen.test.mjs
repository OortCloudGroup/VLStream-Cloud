import test from 'node:test'
import assert from 'node:assert/strict'
import { isVideoFullscreen, toggleVideoFullscreen, videoFullscreenContext } from '../src/utils/videoSquareFullscreen.mjs'

function fixture() {
    const calls = []
    const window = {navigator:{userActivation:{isActive:true}}}
    window.top = window
    const document = {defaultView:window,fullscreenElement:null,fullscreenEnabled:true,hasFocus:()=>true,visibilityState:'visible',permissionsPolicy:{allowsFeature:()=>true}}
    const area = {tagName:'DIV',isConnected:true,ownerDocument:document,contains:element=>element===area}
    area.requestFullscreen = () => { calls.push('request'); document.fullscreenElement=area; return Promise.resolve() }
    document.exitFullscreen = () => { calls.push('exit'); document.fullscreenElement=null; return Promise.resolve() }
    return {area,document,calls}
}

test('native request is invoked synchronously in the user click stack and exit follows actual browser state',async()=>{
    const {area,calls}=fixture()
    const entered=toggleVideoFullscreen(area)
    assert.deepEqual(calls,['request'])
    await entered
    assert.equal(isVideoFullscreen(area),true)
    await toggleVideoFullscreen(area)
    assert.equal(isVideoFullscreen(area),false)
    assert.deepEqual(calls,['request','exit'])
})

test('an ancestor fullscreen exits instead of making another nested request',async()=>{
    const {area,document,calls}=fixture()
    document.fullscreenElement={contains:element=>element===area}
    await toggleVideoFullscreen(area)
    assert.deepEqual(calls,['exit'])
})

test('native rejection stays rejected without CSS fallback or fake fullscreen state',async()=>{
    const {area,document}=fixture()
    const denied=new TypeError('Permissions check failed')
    area.requestFullscreen=()=>Promise.reject(denied)
    await assert.rejects(toggleVideoFullscreen(area),error=>error===denied)
    assert.equal(document.fullscreenElement,null)
    assert.equal(isVideoFullscreen(area),false)
    const context=videoFullscreenContext(area)
    assert.equal(context.userActivation,true)
    assert.equal(context.policyAllowsFullscreen,true)
    assert.equal(context.topLevel,true)
    assert.equal(Object.hasOwn(context,'url'),false)
})

test('detached targets and missing fullscreen support fail without a browser request',async()=>{
    const {area,calls}=fixture()
    area.isConnected=false
    await assert.rejects(toggleVideoFullscreen(area),/not connected/)
    area.isConnected=true
    delete area.requestFullscreen
    await assert.rejects(toggleVideoFullscreen(area),/unavailable/)
    assert.deepEqual(calls,[])
    assert.equal(videoFullscreenContext(null).targetConnected,false)
})
