import test from 'node:test'
import assert from 'node:assert/strict'
import { createOnvifDevicePresetApi, createOnvifPresetController } from '../src/utils/onvifDevicePresets.mjs'
import { normalizeWvpResponse } from '../src/utils/wvpResponse.mjs'

const access = id => ({id,canRead:true,create:true,goTo:true,remove:true})
const defer = () => {let resolve,reject; const promise=new Promise((yes,no)=>{resolve=yes;reject=no}); return {promise,resolve,reject}}
const rows = [{name:'Door',token:'one',password:'must-not-return'}]

test('device preset API uses scoped IDs and sends only the required name or token', async () => {
    const calls=[]
    const api=createOnvifDevicePresetApi(value=>{calls.push(value);return Promise.resolve()})
    await api.list(11); await api.create(11,'Door'); await api.goTo(11,'one'); await api.remove(11,'one')
    assert.deepEqual(calls.map(row=>[row.url,row.method,row.data]),[
        ['/onvif/service/device/11/presets','get',undefined],
        ['/onvif/service/device/11/presets','post',{name:'Door'}],
        ['/onvif/service/device/11/presets/goto','post',{token:'one'}],
        ['/onvif/service/device/11/presets','delete',{token:'one'}],
    ])
    for (const id of [null,0,-1,'../11','1?password=x'])assert.throws(()=>api.list(id))
    assert.equal(calls.length,4)
})

test('read-only and per-action permissions gate dispatch; response is reduced to preset labels', async () => {
    const writes=[]
    const controller=createOnvifPresetController({api:{list:async()=>({data:rows}),goTo:async(...args)=>writes.push(args)}})
    await controller.open({...access(11),goTo:false})
    assert.deepEqual(controller.snapshot().rows,[{name:'Door',token:'one'}])
    assert.equal(await controller.action('goTo','one'),false)
    assert.equal(await controller.action('remove','unknown'),false)
    assert.equal(await controller.action('create',''),false)
    controller.close()
    assert.equal(await controller.action('goTo','one'),false)
    assert.equal(writes.length,0)
})

test('late reads from old source or closed panel never replace the selected source', async () => {
    const a=defer(), b=defer()
    const controller=createOnvifPresetController({api:{list:id=>id==='11'?a.promise:b.promise}})
    const old=controller.open(access(11)), current=controller.open(access(12))
    b.resolve({data:[{name:'Current',token:'two'}]}); await current
    a.resolve({data:rows}); await old
    assert.equal(controller.snapshot().rows[0].token,'two')
    controller.close(); assert.deepEqual(controller.snapshot().rows,[])
})

test('switching source while waiting for own hold stop cancels a preset command', async () => {
    const stop=defer(), writes=[]
    const controller=createOnvifPresetController({api:{list:async()=>({data:rows}),goTo:async(...args)=>writes.push(args)},beforeAction:()=>stop.promise})
    await controller.open(access(11))
    const action=controller.action('goTo','one')
    await controller.open(access(12)); stop.resolve()
    assert.equal(await action,false); assert.equal(writes.length,0)
})

test('pending write blocks double clicks and is never retried after uncertain transport failure', async () => {
    const pending=defer(), calls=[], events=[]
    const controller=createOnvifPresetController({api:{list:async()=>{calls.push('list');return {data:rows}},goTo:()=>{calls.push('write');return pending.promise}},beforeAction:()=>events.push('stop')})
    await controller.open(access(11))
    const action=controller.action('goTo','one')
    await Promise.resolve()
    assert.deepEqual(events,['stop'])
    assert.equal(await controller.action('goTo','one'),false)
    pending.reject(Error('private raw transport detail'))
    assert.equal(await action,false)
    assert.deepEqual(calls,['list','write','list'])
    assert.match(controller.snapshot().error,/未确认/)
    assert.equal(controller.snapshot().busy,false)
})

test('missing deployment and withdrawn permission disable actions without credential fallback', async () => {
    for (const [status,message] of [[404,/尚未部署/],[403,/权限/]]) {
        const calls=[]
        const controller=createOnvifPresetController({api:{list:async()=>{calls.push('list');throw {response:{status}}},goTo:()=>calls.push('write')}})
        await controller.open(access(11))
        assert.match(controller.snapshot().error,message)
        assert.equal(await controller.action('goTo','one'),false)
        assert.deepEqual(calls,['list'])
    }
})

test('WVP AjaxResult inside HTTP 200 preserves business denial and never dispatches a preset write', async () => {
    for (const code of [401,403]) {
        const writes=[]
        const controller=createOnvifPresetController({api:{
            list:async()=>{
                try { return normalizeWvpResponse({code,msg:'fixture denial'}) }
                catch(error) { error.response={status:200,data:error.data}; throw error }
            },
            goTo:(...args)=>writes.push(args),
        }})
        await controller.open(access(11))
        assert.match(controller.snapshot().error,/权限/)
        assert.equal(controller.snapshot().ready,false)
        assert.equal(await controller.action('goTo','one'),false)
        assert.deepEqual(writes,[])
    }
})
