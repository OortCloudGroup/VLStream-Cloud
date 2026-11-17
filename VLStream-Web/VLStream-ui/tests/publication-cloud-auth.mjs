import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import {harness} from './publication-component-harness.mjs'
const source=(await fs.readFile('src/components/FeaturedPublishActions.vue','utf8')).replace(/\r\n/g,'\n')
for(const platform of [false,true]){
 const h=await harness({platform,initial:{publishState:'UNCERTAIN'},reconcile:async()=>{throw Object.assign(new Error('cloud auth failed'),{code:4401})}})
 await h.reconcile(true)
 assert.equal(h.cloudAuthRequired.value,true);assert.equal(h.calls.cleared,platform?0:1)
 assert.equal(h.calls.publish,0);assert.equal(h.calls.login,0);assert.equal(h.latestTimer(),undefined)
}
assert(!source.includes('logoutModelHubSession'))
assert(source.includes('startModelHubLogin(null, { returnToCurrent: true })'))
assert(source.includes('clearPendingModelHubPublish()'))
assert(source.includes('重新登录 OortCloud'))
console.log('Cloud 4401 stops polling, only clears independent auth, never auto-logs-in or resubmits')
const sessionSource = (await fs.readFile('src/utils/sessionExpiry.js','utf8')).replace(/\r\n/g,'\n')
const detectBody = sessionSource.match(/export function isLoginFailureResponse\(response\) \{([\s\S]*?)\n\}/)[1]
const localSessionFailure = new Function('response', detectBody)
assert.equal(localSessionFailure({status:200,data:{code:4401,msg:'开发广场账户验证未通过，请重新登录与当前开发广场同一环境的 OortCloud 账号'}}),false)
assert.equal(localSessionFailure({status:200,data:{code:401,msg:'认证失败'}}),true)
console.log('Independent 4401 does not trigger global VLS session expiry')
