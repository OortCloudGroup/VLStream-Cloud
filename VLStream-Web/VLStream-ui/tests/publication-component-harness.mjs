import fs from 'node:fs/promises'
import { pendingPublication, confirmedPublication, uploadAllowed, publicationPollDelay, publicationLabel, resumeAllowed } from '../src/utils/publicationProgress.mjs'
export async function harness(options = {}) {
 const source = (await fs.readFile('src/components/FeaturedPublishActions.vue','utf8')).replace(/\r\n/g,'\n')
 const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'')
 const calls={publish:0,reconcile:0,resume:0,confirm:0,star:0,cleared:0,success:[],timers:[],login:0};let identityRevision=0,destroy,propsChanged
 const defaultState={publishState:'DRAFT',publishable:true,featured:false}
 const props={kind:'model',id:'123',name:'test'}
 const ref=value=>({value}),computed=fn=>({get value(){return fn()}})
 const mocks={
 computed,ref,Star:{},StarFilled:{},defineProps:()=>props,
 watch:(sources,fn,opts)=>{propsChanged=fn;if(opts?.immediate)fn()},onBeforeUnmount:fn=>{destroy=fn},
 ElMessage:{success:message=>calls.success.push(message),error(){}},ElMessageBox:{confirm:async()=>{calls.confirm++;if(!options.confirmResume)throw Error('cancel')}},uiText:x=>x,
 getPublication:async()=>options.initial||defaultState,
 setPublicationFeatured:async(kind,id,value)=>{calls.star++;return {...defaultState,featured:value}},
 publishToModelHub:async()=>{calls.publish++;return options.publish?options.publish():{...defaultState,publishState:'PROCESSING'}},
 resumePublication:async()=>{calls.resume++;return {publishState:'PROCESSING',publishable:true}},
 reconcilePublication:async()=>{calls.reconcile++;return options.reconcile?options.reconcile():{...defaultState,publishState:'PUBLISHED',receiptState:'PUBLISHED'}},
 getOortCloudSession:()=>({accessToken:options.noToken?'':'test-only',usesPlatformSession:!!options.platform}),isMultiTenantMode:()=>!!options.platform,
 redirectToPlatformLogin:()=>{calls.login++},startModelHubLogin:()=>{calls.login++},clearPendingModelHubPublish(){},
 clearModelHubAuth:()=>{calls.cleared++;identityRevision++},
 captureComputeIdentity:()=>{const captured=identityRevision;return{assertCurrent(){if(captured!==identityRevision)throw Error('identity changed')}}},
 pendingPublication,confirmedPublication,uploadAllowed,publicationPollDelay,publicationLabel,resumeAllowed,
 document:{hidden:false,addEventListener(){},removeEventListener(){}},
 setTimeout:(fn,delay)=>{const timer={fn,delay,cancelled:false};calls.timers.push(timer);return timer},clearTimeout:timer=>{if(timer)timer.cancelled=true}
 }
 const result=new Function(...Object.keys(mocks),script+'\nreturn {state,visible,submitting,checking,cloudAuthRequired,listLabel,error,publish,reconcile,resume,setFeatured,load,open};')(...Object.values(mocks))
 await Promise.resolve();await Promise.resolve()
 return {...result,calls,props,changeProps:()=>propsChanged(),destroy:()=>destroy(),changeIdentity:()=>{identityRevision++},latestTimer:()=>calls.timers.filter(t=>!t.cancelled).at(-1)}
}
