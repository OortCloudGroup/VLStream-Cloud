import assert from 'node:assert/strict'
import { harness } from './publication-component-harness.mjs'
import {publicationPollDelay,publicationLabel,confirmedPublication,uploadAllowed} from '../src/utils/publicationProgress.mjs'
const first=await harness();first.visible.value=true
let finish
const pending=await harness({publish:()=>new Promise(resolve=>{finish=resolve})})
const send=pending.publish();pending.visible.value=false
await pending.publish();assert.equal(pending.calls.publish,1)
finish({publishState:'PROCESSING',publishable:true});await send
assert.equal(pending.listLabel.value,'发布中');assert.equal(pending.calls.success.length,0)
assert(pending.latestTimer());await pending.latestTimer().fn()
assert.equal(pending.calls.reconcile,1);assert.equal(pending.calls.publish,1)
assert.equal(pending.listLabel.value,'已发布');assert.equal(pending.calls.success.length,1)
for(const receipt of ['PROCESSING','NOT_FOUND','FAILED','PUBLISHED']){
 const x=await harness({initial:{publishState:'UNCERTAIN',publishable:true},reconcile:async()=>({publishState:'UNCERTAIN',receiptState:receipt,publishable:true})})
 await x.publish();assert.equal(x.calls.publish,0)
 await x.reconcile(true);assert.equal(x.calls.reconcile,1);assert.equal(x.calls.success.length,0)
 x.destroy();assert.equal(x.latestTimer(),undefined)
}
const restored=await harness({initial:{publishState:'PROCESSING'}});assert(restored.latestTimer());assert.equal(restored.calls.publish,0)
let resolveOld
const stale=await harness({publish:()=>new Promise(resolve=>{resolveOld=resolve})});const old=stale.publish();stale.changeIdentity();resolveOld({publishState:'PUBLISHED'});await old
assert.equal(stale.state.value,null);assert.equal(stale.calls.success.length,0);assert.equal(stale.latestTimer(),undefined)
let resolveUnmount
const unmounted=await harness({publish:()=>new Promise(resolve=>{resolveUnmount=resolve})});const run=unmounted.publish();unmounted.destroy();resolveUnmount({publishState:'PUBLISHED'});await run
assert.equal(unmounted.calls.success.length,0);assert.equal(unmounted.latestTimer(),undefined)
assert.equal(publicationPollDelay(0),5000);assert.equal(publicationPollDelay(9),30000);assert.equal(publicationPollDelay(0,true),120000)
assert.equal(confirmedPublication({publishState:'PUBLISHED',receiptState:'FAILED'}),false)
assert.equal(uploadAllowed({publishState:'UNCERTAIN',publishable:true}),false)
assert.equal(publicationLabel({publishState:'UNKNOWN'}),'核实状态')
console.log('Async close/restore, reconcile-only polling, outcome states, backoff, identity and unmount guards passed')

const notFound={publishState:'UNCERTAIN',receiptState:'NOT_FOUND',publishable:true,resumeAllowed:true}
const cancelled=await harness({initial:notFound});await cancelled.resume();assert.equal(cancelled.calls.confirm,1);assert.equal(cancelled.calls.resume,0)
const continued=await harness({initial:notFound,confirmResume:true});assert.equal(continued.calls.resume,0);await continued.resume();assert.equal(continued.calls.resume,1);assert.equal(continued.calls.publish,0);assert.equal(continued.listLabel.value,'发布中')
const processing=await harness({initial:{publishState:'PROCESSING',receiptState:'PROCESSING'},confirmResume:true});await processing.resume();assert.equal(processing.calls.resume,0);assert.equal(processing.calls.confirm,0)
console.log('NOT_FOUND only resumes after explicit confirmation; cancel and processing never upload')

const activeNoReceipt=await harness({initial:{publishState:'PUBLISHING',receiptState:'NOT_FOUND'},confirmResume:true});await activeNoReceipt.resume();assert.equal(activeNoReceipt.calls.confirm,0);assert.equal(activeNoReceipt.calls.resume,0)
console.log('Active submission cannot resume even before upstream receipt appears')

for(const allowed of [false,undefined]) {
 const denied=await harness({initial:{publishState:'UNCERTAIN',receiptState:'NOT_FOUND',resumeAllowed:allowed},confirmResume:true})
 await denied.resume();assert.equal(denied.calls.confirm,0);assert.equal(denied.calls.resume,0)
}
console.log('NOT_FOUND requires explicit backend resumeAllowed=true for original identity and snapshot')

const remoteFailed=await harness({initial:{publishState:'REMOTE_FAILED',publishable:true},reconcile:async()=>({publishState:'REMOTE_FAILED',receiptState:'FAILED'})})
assert.equal(remoteFailed.listLabel.value,'发布失败');assert.equal(remoteFailed.latestTimer(),undefined)
await remoteFailed.publish();assert.equal(remoteFailed.calls.publish,0)
await remoteFailed.reconcile(true);assert.equal(remoteFailed.calls.reconcile,1);assert.equal(remoteFailed.latestTimer(),undefined);assert.equal(remoteFailed.calls.success.length,0)
console.log('REMOTE_FAILED persists failure, allows manual read-only reconcile and never upload or poll')
