import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { computed, effectScope, nextTick, reactive, ref, watch } from 'vue'
import { parse, compileScript } from 'vue/compiler-sfc'
import { createDeviceSharingApi, createShareResource, safeOwnerGrantsPage, safeSharedDevice, canReadSharedDevice, canRequestSharedDevice, shareAccessLabels, shareGrantLabels, shareRequestLabels } from '../src/utils/videoSquareSharing.mjs'
import { createSharedVideoLease } from '../src/utils/sharedVideoLease.mjs'
const deviceRef = 'a'.repeat(32)
const device = { id:`VLSTREAM:${deviceRef}`,deviceName:'controlled device',visibility:'PRIVATE',viewerAccess:'NONE' }
const deferred = () => { let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return {promise,resolve,reject} }
const flush = async () => { for(let i=0;i<5;i++){await Promise.resolve();await nextTick()} }

test('shared projection rejects numeric identity and strips restricted fields even if supplied', () => {
  assert.throws(()=>safeSharedDevice({...device,id:'VLSTREAM:1'}))
  const projected=safeSharedDevice({...device,deviceId:'private-id',ipAddress:'10.0.0.1',sourceUrl:'rtsp://secret',status:'ONLINE'})
  assert.equal(projected.status,null)
  assert.equal(projected.deviceId,undefined)
  assert.equal(projected.ipAddress,undefined)
  assert.equal(canReadSharedDevice(projected),false)
  assert.equal(canRequestSharedDevice(projected),true)
  assert.equal(canRequestSharedDevice({...projected,viewerAccess:'PENDING'}),false)
})

test('API paths use opaque references, never caller tenant/body extras or original preview endpoints', async () => {
  const calls=[];const api=createDeviceSharingApi(async value=>{calls.push(value);return {data:{}}})
  await api.directory({pageNum:2,pageSize:20,keyword:'camera',tenantId:'other'})
  await api.apply(deviceRef,'reason')
  await api.approve('700',null)
  await api.start(deviceRef,'900')
  await api.renew(deviceRef,'lease_1')
  await api.close(deviceRef,'lease_1')
  assert.deepEqual(calls[0].params,{pageNum:2,pageSize:20,keyword:'camera'})
  assert.deepEqual(calls[1].data,{reason:'reason'})
  assert.deepEqual(calls[2].data,{})
  assert.deepEqual(calls[3].data,{streamId:'900'})
  assert.ok(calls.slice(3).every(value=>value.url.startsWith(`/vlstream/device/shared/${deviceRef}/preview-sessions`)))
  assert.throws(()=>api.start('1','900'))
  assert.ok(calls.every(value=>!JSON.stringify(value).includes('tenantId')))
  await api.grants('1',{pageNum:2,pageSize:20,status:'ACTIVE',tenantId:'other'})
  assert.equal(calls.at(-1).url,'/vlstream/device/1/access-grants')
  assert.deepEqual(calls.at(-1).params,{pageNum:2,pageSize:20,status:'ACTIVE'})
})

test('owner history keeps string grant IDs and actual expiry, stripping unrelated fields and invalid actions',()=>{
  const row={grantId:'2113223186584698881',requestId:'700',status:'ACTIVE',canRevoke:true,expiresAt:'2026-10-23 08:30:00',sourceUrl:'rtsp://secret'}
  const projected=safeOwnerGrantsPage({rows:[row,{...row,grantId:'800',status:'EXPIRED'},{...row,grantId:'801',status:'REVOKED'}],total:3,asOf:'2026-10-22 08:30:00'})
  assert.equal(projected.rows[0].grantId,row.grantId)
  assert.equal(projected.rows[0].expiresAt,row.expiresAt)
  assert.equal(projected.rows[0].sourceUrl,undefined)
  assert.deepEqual(projected.rows.map(value=>value.canRevoke),[true,false,false])
  assert.throws(()=>safeOwnerGrantsPage({rows:[{...row,grantId:Number(row.grantId)}],total:1}))
  assert.throws(()=>safeOwnerGrantsPage({rows:[{...row,status:'APPROVED'}],total:1}))
})

test('late or closed reads cannot restore stale rows, and unavailable service fails closed', async () => {
  const states=[],old=deferred();const resource=createShareResource(state=>states.push(state))
  const pending=resource.load(()=>old.promise)
  await resource.load(async()=>({rows:['new']}))
  old.resolve({rows:['old']});await pending
  assert.deepEqual(states.at(-1).rows,['new'])
  const late=deferred(),closing=resource.load(()=>late.promise)
  resource.clear();late.resolve({rows:['closed']});await closing
  assert.deepEqual(states.at(-1).rows,[])
  await resource.load(async()=>{throw {response:{status:404}}})
  assert.deepEqual(states.at(-1).rows,[])
  assert.match(states.at(-1).error,/尚未部署/)
})

function leaseHarness(options={}) {
  const {allowCameraRtc=false,...apiOptions}=options
  const states=[],closes=[],timers=new Map();let next=0
  const api={start:async()=>({data:{sessionId:'lease_1',playMode:'ZLM',expiresInSeconds:2}}),renew:async()=>({data:{sessionId:'lease_1',expiresInSeconds:2}}),close:async(ref,id)=>closes.push([ref,id]),...apiOptions}
  const lease=createSharedVideoLease({api,allowCameraRtc,onChange:state=>states.push(state),setTimer:(fn,ms)=>{const id=++next;timers.set(id,{fn,ms});return id},clearTimer:id=>timers.delete(id)})
  return {lease,states,closes,timers,fire:ms=>[...timers.values()].find(timer=>timer.ms===ms).fn()}
}
test('closing before start completes closes the late server lease without starting playback', async()=>{
  const pending=deferred(),h=leaseHarness({start:()=>pending.promise})
  const starting=h.lease.start(deviceRef,1);h.lease.stop()
  pending.resolve({data:{sessionId:'late_lease',playMode:'ZLM',expiresInSeconds:2}});await starting;await flush()
  assert.deepEqual(h.closes,[[deviceRef,'late_lease']])
  assert.equal(h.states.at(-1).status,'idle')
  assert.equal(h.states.some(state=>state.status==='active'),false)
})
test('local expiry stops media while renew hangs, and a late renew cannot revive it', async()=>{
  const pending=deferred(),h=leaseHarness({renew:()=>pending.promise})
  await h.lease.start(deviceRef,1)
  const renewal=h.fire(1000)
  h.fire(2000);await flush()
  assert.equal(h.states.at(-1).status,'error')
  assert.equal(h.states.at(-1).lease,null)
  pending.resolve({data:{sessionId:'lease_1',expiresInSeconds:2}});await renewal
  assert.equal(h.states.at(-1).status,'error')
  assert.deepEqual(h.closes,[[deviceRef,'lease_1']])
})
test('revoked authorization fails renewal and closes the lease; CameraRTC conflict never falls back', async()=>{
  const h=leaseHarness({renew:async()=>{throw {response:{data:{msg:'授权已撤销'}}}}})
  await h.lease.start(deviceRef,1);await h.fire(1000);await flush()
  assert.equal(h.states.at(-1).error,'授权已撤销')
  assert.deepEqual(h.closes,[[deviceRef,'lease_1']])
  const unsupported=leaseHarness({start:async()=>{throw {response:{status:409,data:{msg:'CameraRTC共享尚不可用'}}}}})
  await unsupported.lease.start(deviceRef,1)
  assert.match(unsupported.states.at(-1).error,/CameraRTC/)
  assert.equal(unsupported.states.some(state=>state.status==='active'),false)
})
test('CameraRTC remains unavailable by default and closes an unused issued ticket',async()=>{
  const id='b'.repeat(64),h=leaseHarness({start:async()=>({data:{sessionId:id,playMode:'cameraRTC',expiresInSeconds:120}})})
  await h.lease.start(deviceRef,1);await flush()
  assert.equal(h.states.at(-1).status,'error')
  assert.match(h.states.at(-1).error,/尚未完成网关安全配置/)
  assert.deepEqual(h.closes,[[deviceRef,id]])
})
test('enabled CameraRTC renews and closes through the same scoped lease API',async()=>{
  const id='b'.repeat(64),h=leaseHarness({allowCameraRtc:true,start:async()=>({data:{sessionId:id,playMode:'cameraRTC',expiresInSeconds:2}}),renew:async()=>({data:{sessionId:id,expiresInSeconds:2}})})
  await h.lease.start(deviceRef,1);await h.fire(1000)
  assert.equal(h.states.at(-1).status,'active')
  h.lease.stop();await flush()
  assert.deepEqual(h.closes,[[deviceRef,id]])
})

const source=readFileSync(new URL('../src/views/VideoAggregation/components/VideoSquareSharingPanel.vue',import.meta.url),'utf8')
const compiled=compileScript(parse(source).descriptor,{id:'share-ui-test'}).content
const executable=compiled.replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm,'').replace('export default','return')
const createComponent=new Function('__deps',`const {computed,onBeforeUnmount,ref,watch,ElMessage,ElMessageBox,tp,deviceSharingApi,checkPermi,canReadSharedDevice,canRequestSharedDevice,createShareResource,safeOwnerGrantsPage,safeSharedDevice,shareAccessLabels,shareGrantLabels,shareRequestLabels,VideoSquareSharedPlayer}=__deps;${executable}`)
function panelHarness(overrides={},allowed=true) {
  const calls=[],unmounts=[];let pending=true,grant=null
  const api={directory:async()=>({data:{rows:[{...device,viewerAccess:pending?'NONE':'PENDING'}],total:1}}),mine:async()=>({data:[]}),owner:async()=>({data:{device:{visibility:'PRIVATE'}}}),requests:async()=>({data:pending?[{requestId:'700',status:'PENDING',reason:'controlled reason'}]:[]}),
    grants:async()=>({data:{rows:grant?[grant]:[],total:grant?1:0,asOf:'2026-10-22 08:30:00'}}),
    apply:async(ref,reason)=>{calls.push(['apply',ref,reason]);pending=false;return {data:{status:'PENDING'}}},
    approve:async(id,days)=>{calls.push(['approve',id,days]);pending=false;grant={grantId:'800',requestId:id,status:'ACTIVE',canRevoke:true,expiresAt:'2026-10-23 08:30:00'};return {data:{status:'APPROVED',grantId:'800',requestId:id,expiresInDays:7}}},
    revoke:async id=>{calls.push(['revoke',id]);grant={...grant,status:'REVOKED',canRevoke:false};return {data:{grantId:id,status:'REVOKED'}}},
    reject:async(id,reason)=>{calls.push(['reject',id,reason]);pending=false;return {data:{status:'REJECTED'}}},...overrides}
  const props=reactive({modelValue:false,ownedDevices:[{deviceName:'owner',sources:[{protocol:'VLSTREAM',rowId:'1'}]}],api})
  const scope=effectScope();const instance=scope.run(()=>createComponent({computed,ref,watch,onBeforeUnmount:fn=>unmounts.push(fn),ElMessage:{success(){}},ElMessageBox:{confirm:async()=>{}},tp:text=>text,deviceSharingApi:api,checkPermi:()=>allowed,canReadSharedDevice,canRequestSharedDevice,createShareResource,safeOwnerGrantsPage,safeSharedDevice,shareAccessLabels,shareGrantLabels,shareRequestLabels,VideoSquareSharedPlayer:{}}).setup(props,{expose(){},emit(){}}))
  return {props,instance,calls,close:()=>{unmounts.forEach(fn=>fn());scope.stop()}}
}
test('actual sharing SFC submits once, waits for server state, and resets reason on reopen', async()=>{
  const result=deferred(),h=panelHarness({apply:(ref,reason)=>{h.calls.push(['apply',ref,reason]);return result.promise}})
  h.props.modelValue=true;await flush()
  h.instance.openRequest(h.instance.directory.value.rows[0]);h.instance.reason.value='need view'
  const first=h.instance.submitRequest();h.instance.submitRequest()
  assert.equal(h.calls.length,1)
  assert.equal(h.instance.directory.value.rows[0].viewerAccess,'NONE')
  result.resolve({data:{status:'PENDING'}});await first
  assert.equal(h.instance.requestDevice.value,null)
  h.instance.openRequest(device);assert.equal(h.instance.reason.value,'')
  h.close()
})
test('failed application retains the form and reason without inventing granted access', async()=>{
  const h=panelHarness({apply:async()=>{throw {response:{data:{msg:'请求被拒绝'}}}}})
  h.instance.openRequest(device);h.instance.reason.value='keep this'
  await h.instance.submitRequest()
  assert.equal(h.instance.reason.value,'keep this')
  assert.equal(h.instance.requestDevice.value.viewerAccess,'NONE')
  assert.equal(h.instance.actionError.value,'请求被拒绝')
  assert.equal(h.instance.busy.value,false)
  h.close()
})
test('approval and rejection use actual request identities and wait for decision response', async()=>{
  const h=panelHarness();h.instance.ownerId.value='1';await flush()
  h.instance.openDecision(h.instance.queue.value.rows[0],'approve')
  await h.instance.submitDecision()
  assert.deepEqual(h.calls,[['approve','700',null]])
  assert.equal(h.instance.grants.value.rows[0].grantId,'800')
  assert.equal(h.instance.grants.value.rows[0].expiresAt,'2026-10-23 08:30:00')
  assert.deepEqual(h.instance.queue.value.rows,[])
  h.instance.openDecision({requestId:'701',status:'PENDING'},'reject');h.instance.decisionReason.value='not needed'
  await h.instance.submitDecision()
  assert.deepEqual(h.calls[1],['reject','701','not needed'])
  h.close()
})

test('owner history reloads on reopen and revocation waits for actual server history',async()=>{
  const h=panelHarness();h.props.initialOwnerId='1';h.props.modelValue=true;await flush()
  h.instance.openDecision(h.instance.queue.value.rows[0],'approve');await h.instance.submitDecision()
  h.props.modelValue=false;await flush()
  assert.deepEqual(h.instance.grants.value.rows,[])
  h.props.modelValue=true;await flush()
  assert.equal(h.instance.grants.value.rows[0].grantId,'800')
  const pending=deferred();let revoked=false
  h.props.api.revoke=id=>{h.calls.push(['revoke',id]);return pending.promise}
  h.props.api.grants=async()=>({data:{rows:[{grantId:'800',status:revoked?'REVOKED':'ACTIVE',canRevoke:!revoked}],total:1}})
  const revoking=h.instance.revoke(h.instance.grants.value.rows[0]);await flush()
  assert.equal(h.instance.grants.value.rows[0].status,'ACTIVE')
  revoked=true;pending.resolve({data:{grantId:'800',status:'REVOKED'}});await revoking
  assert.equal(h.instance.grants.value.rows[0].status,'REVOKED')
  assert.equal(h.instance.grants.value.rows[0].canRevoke,false)
  await h.instance.revoke(h.instance.grants.value.rows[0])
  assert.equal(h.calls.filter(call=>call[0]==='revoke').length,1)
  h.close()
})

test('owner history filters and pagination stay scoped, while delayed or denied reads clear records',async()=>{
  const reads=[],pending=deferred()
  const h=panelHarness({grants:async(id,query)=>{reads.push([id,query]);return {data:{rows:[],total:0}}}})
  h.instance.ownerId.value='1';await flush()
  h.instance.grantPage.value=3;await h.instance.loadGrants()
  assert.deepEqual(reads.at(-1),['1',{pageNum:3,pageSize:20,status:'ALL'}])
  h.instance.grantStatus.value='EXPIRED';h.instance.searchGrants();await flush()
  assert.deepEqual(reads.at(-1),['1',{pageNum:1,pageSize:20,status:'EXPIRED'}])
  h.props.api.grants=()=>pending.promise
  const loading=h.instance.loadGrants();h.instance.clear()
  pending.resolve({data:{rows:[{grantId:'800',status:'ACTIVE',canRevoke:true}],total:1}});await loading
  assert.deepEqual(h.instance.grants.value.rows,[])
  h.props.api.grants=async()=>{throw {response:{status:403,data:{msg:'无审批权限'}}}}
  await h.instance.loadGrants()
  assert.deepEqual(h.instance.grants.value.rows,[])
  assert.equal(h.instance.grants.value.error,'无审批权限')
  h.close()
  let calls=0
  const denied=panelHarness({grants:async()=>{calls++}},false)
  denied.instance.ownerId.value='1';await flush()
  await denied.instance.loadGrants();await denied.instance.revoke({grantId:'800',status:'ACTIVE',canRevoke:true})
  assert.equal(calls,0);assert.deepEqual(denied.calls,[])
  denied.close()
})

test('a conflicting or uncertain revocation refreshes history without retrying or claiming success',async()=>{
  let expired=false,writes=0
  const h=panelHarness({
    grants:async()=>({data:{rows:[{grantId:'800',status:expired?'EXPIRED':'ACTIVE',canRevoke:!expired}],total:1}}),
    revoke:async()=>{writes++;expired=true;throw {response:{status:409,data:{msg:'授权已过期'}}}},
  })
  h.instance.ownerId.value='1';await flush()
  await h.instance.revoke(h.instance.grants.value.rows[0])
  assert.equal(writes,1)
  assert.equal(h.instance.grants.value.rows[0].status,'EXPIRED')
  assert.equal(h.instance.actionError.value,'授权已过期')
  assert.equal(h.instance.busy.value,false)
  h.close()
})
