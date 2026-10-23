import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import { createSharedCameraRtcSession, sharedCameraRtcUrl, sharedIceConfiguration, sharedCameraRtcProtocols, SHARED_CAMERA_RTC_AUTH_HEADERS } from '../src/utils/sharedCameraRtc.mjs'
import { replaceHeader, clearIdentityHeaders } from '../src/utils/requestIdentity.mjs'
const source=readFileSync(new URL('../src/utils/cameraRtcSession.js',import.meta.url),'utf8')
const {CameraRtcSession}=await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`)
const ticket='t'.repeat(43)
const lease={deviceRef:'a'.repeat(32),sessionId:'b'.repeat(64),ticket,webSocketPath:`/vlstream/device/shared/camera-rtc?ticket=${ticket}`,ticketExpiresInSeconds:30}
const authHeaders={accesstoken:'platform-token-current','X-VLStream-Token':'local-token-current','X-WVP-Auth-Source':'vlstream',tenantid:'tenant-current',requesttype:'app',appid:'fixture-app',secretkey:'fixture-secret'}
const decodeAuth=protocols=>JSON.parse(Buffer.from(protocols[1].slice('rtc-auth.'.length),'base64url').toString('utf8'))
test('relay URL uses only the returned same-origin ticket route and WVP proxy prefix',()=>{
  assert.equal(sharedCameraRtcUrl(lease,'/wvp-api','http://localhost:3000'),`ws://localhost:3000/wvp-api${lease.webSocketPath}`)
  assert.equal(sharedCameraRtcUrl(lease,'/bus/apaas-wvp-server','https://example.com:2443'),`wss://example.com:2443/bus/apaas-wvp-server${lease.webSocketPath}`)
  for(const value of [
    {...lease,webSocketPath:`//upstream.example.com/wswebclient/device?ticket=${ticket}`},
    {...lease,webSocketPath:`/wswebclient/private-device?ticket=${ticket}`},
    {...lease,webSocketPath:`${lease.webSocketPath}&ticket=${ticket}`},
    {...lease,webSocketPath:`${lease.webSocketPath}&user=admin`},
    {...lease,ticket:'different'}, {...lease,ticketExpiresInSeconds:0},
  ]) assert.throws(()=>sharedCameraRtcUrl(value,'/wvp-api','https://example.com'),error=>!error.message.includes(ticket))
  assert.throws(()=>sharedCameraRtcUrl(lease,'https://upstream.example.com','https://example.com'))
})
test('shared ICE excludes TURN credentials, local hosts and private IPs without environment fallbacks',()=>{
  const config=sharedIceConfiguration({iceServers:[
    {urls:['stun:stun.example.com:3478','turn:turn.example.com:3478'],username:'not-forwarded',credential:'not-forwarded'},
    {urls:['stun:192.168.1.1','stun:127.0.0.1','stun:internal','stun:host.local.','stun:100.64.0.1','stun:0177.0.0.1','stun:user@public.example.com','stun:public.example.com?password=not-forwarded']},
  ]})
  assert.deepEqual(config.iceServers,[{urls:['stun:stun.example.com:3478']}])
  assert.equal(JSON.stringify(config).includes('credential'),false)
  assert.deepEqual(sharedIceConfiguration('bad json').iceServers,[])
  assert.deepEqual(sharedIceConfiguration({iceServers:[null,7,{}, {urls:null}]}).iceServers,[])
})
function harness({getAuthHeaders=()=>({...authHeaders}),socketFailure=false}={}) {
  const sockets=[],peers=[],statuses=[],timers=new Map();let timerId=0
  class Socket {
    static OPEN=1
    constructor(url,protocols){this.url=url;this.protocols=protocols;this.readyState=0;this.sent=[];sockets.push(this);if(socketFailure)throw new Error('fixture socket failure')}
    open(){this.readyState=1;this.onopen?.()}
    send(value){this.sent.push(JSON.parse(value))}
    close(){this.readyState=3;this.onclose?.({code:1000})}
  }
  class Peer {
    constructor(configuration){this.configuration=configuration;this.signalingState='stable';peers.push(this)}
    createDataChannel(){return {readyState:'open',close(){this.readyState='closed'}}}
    async setRemoteDescription(value){this.remoteDescription=value}
    async createAnswer(){return {type:'answer',sdp:'controlled-answer'}}
    async setLocalDescription(value){this.localDescription=value}
    async addIceCandidate(){}
    close(){this.signalingState='closed'}
  }
  const runtime={WebSocket:Socket,RTCPeerConnection:Peer,crypto:{randomUUID:()=>`00000000-0000-4000-8000-000000000001`},
    setTimeout:fn=>{timers.set(++timerId,fn);return timerId},clearTimeout:id=>timers.delete(id),setInterval:fn=>{timers.set(++timerId,fn);return timerId},clearInterval:id=>timers.delete(id)}
  const video={srcObject:null,play:()=>Promise.resolve()}
  const session=createSharedCameraRtcSession({CameraRtcSession,lease,wvpBase:'/wvp-api',origin:'https://example.com',videoElement:video,getAuthHeaders,runtime,onStatus:value=>statuses.push(value)})
  return {session,sockets,peers,statuses,video,timers}
}
test('actual media session connects exact relay path, strips owner credentials and allows only protocol events',async()=>{
  const h=harness();h.session.start();h.sockets[0].open()
  assert.equal(h.sockets[0].url,`wss://example.com/wvp-api${lease.webSocketPath}`)
  assert.equal(h.sockets[0].url.includes('/wswebclient/'),false)
  assert.equal(h.sockets[0].protocols[0],'vlstream-shared-rtc.v1')
  assert.deepEqual(decodeAuth(h.sockets[0].protocols),authHeaders)
  for(const name of ['accesstoken','X-VLStream-Token','secretkey'])assert.equal(h.sockets[0].url.includes(authHeaders[name]),false)
  const sessionId=h.session.sessionId
  await h.session.handleSocketMessage({data:JSON.stringify({eventName:'_create',data:{sessionId,state:'online',iceServers:{iceServers:[{urls:'stun:stun.example.com',credential:'not-forwarded'},{urls:'turn:turn.example.com',username:'not-forwarded',credential:'not-forwarded'}]}}})})
  const call=h.sockets[0].sent.find(message=>message.eventName==='__call')
  assert.ok(call)
  assert.equal(call.data.user,undefined);assert.equal(call.data.pwd,undefined)
  assert.equal(call.data.to,lease.deviceRef)
  assert.equal(call.data.iceservers.includes('credential'),false)
  assert.equal(h.session.send('__ptz',{direction:'up'}),false)
  assert.equal(h.sockets[0].sent.some(message=>message.eventName==='__ptz'),false)
  h.session.stop();assert.equal(h.timers.size,0)
})
test('signaling close immediately stops real media tracks and never reuses a consumed ticket',async()=>{
  const h=harness();h.session.start();h.sockets[0].open()
  await h.session.handleSocketMessage({data:JSON.stringify({eventName:'_offer',data:{sessionId:h.session.sessionId,type:'offer',sdp:'controlled-offer'}})})
  const track={enabled:true,stopped:false,stop(){this.stopped=true}}
  const stream={getTracks:()=>[track],getAudioTracks:()=>[track]}
  h.peers[0].ontrack({streams:[stream]});await Promise.resolve()
  assert.equal(h.video.srcObject,stream)
  h.sockets[0].close()
  assert.equal(track.stopped,true);assert.equal(h.video.srcObject,null)
  assert.equal(h.peers[0].signalingState,'closed')
  assert.equal(h.statuses.at(-1).state,'failed')
  h.session.retryNow();h.session.start()
  assert.equal(h.sockets.length,1)
  assert.equal(h.timers.size,0)
})
test('a queued message from a closed relay cannot create a new media peer after stop',async()=>{
  const h=harness();h.session.start();h.sockets[0].open()
  const queuedMessage=h.sockets[0].onmessage
  const sessionId=h.session.sessionId
  h.session.stop()
  await queuedMessage({data:JSON.stringify({eventName:'_offer',data:{sessionId,type:'offer',sdp:'late-controlled-offer'}})})
  assert.equal(h.peers.length,0)
  assert.equal(h.video.srcObject,null)
  assert.equal(h.timers.size,0)
})

test('auth projection accepts exactly the nine case-insensitive headers and UTF8 JSON',()=>{
  assert.equal(SHARED_CAMERA_RTC_AUTH_HEADERS.length,9)
  const provided={ACCESSTOKEN:'current-token','x-vlstream-token':'local-token','x-wvp-auth-source':'vlstream',TENANTID:'当前租户',REQUESTTYPE:'app',APPID:'app',SECRETKEY:'secret',authorization:'Bearer native-token','BLADE-AUTH':'Bearer native-token'}
  const protocols=sharedCameraRtcProtocols(provided)
  assert.equal(protocols.length,2)
  assert.equal(protocols[0],'vlstream-shared-rtc.v1')
  assert.match(protocols[1],/^rtc-auth\.[A-Za-z0-9_-]+$/)
  assert.deepEqual(decodeAuth(protocols),Object.fromEntries(SHARED_CAMERA_RTC_AUTH_HEADERS.map(name=>[name,provided[Object.keys(provided).find(key=>key.toLowerCase()===name.toLowerCase())]])))
})

test('missing, duplicate, unknown, non-string, blank or injected auth headers are rejected without secrets',()=>{
  for(const headers of [undefined,null,[],{}, {'X-WVP-Auth-Source':'vlstream'}, {accesstoken:''}, {accesstoken:'   '},
    {accesstoken:7},{accesstoken:null},{accesstoken:['fixture-secret']},{accesstoken:'fixture-secret',AccessToken:'duplicate'},
    {accesstoken:'fixture-secret',cookie:'unapproved'},{accesstoken:'fixture-secret\r\nforged: header'},
    {accesstoken:'fixture-secret',tenantid:'tenant\nforged'},{accesstoken:'fixture-secret\u0085'},{accesstoken:'fixture-secret',requesttype:false},
    {accesstoken:'fixture-secret',[Symbol('unknown')]:'symbol'},new Map([['accesstoken','fixture-secret']])]) {
    assert.throws(()=>sharedCameraRtcProtocols(headers),error=>!error.message.includes('fixture-secret'))
  }
})

test('auth payload enforces decoded UTF8 byte and encoded payload limits at the boundary',()=>{
  const overhead=Buffer.byteLength(JSON.stringify({accesstoken:''}))
  const protocols=sharedCameraRtcProtocols({accesstoken:'a'.repeat(6144-overhead)})
  assert.equal(Buffer.from(protocols[1].slice('rtc-auth.'.length),'base64url').length,6144)
  assert.equal(protocols[1].slice('rtc-auth.'.length).length,8192)
  assert.throws(()=>sharedCameraRtcProtocols({accesstoken:'a'.repeat(6145-overhead)}))
  assert.throws(()=>sharedCameraRtcProtocols({accesstoken:'界'.repeat(Math.ceil((6145-overhead)/3))}))
})

test('missing auth callback never creates a socket or anonymously retries the ticket',()=>{
  const h=harness({getAuthHeaders:null})
  h.session.start()
  assert.equal(h.sockets.length,0)
  assert.equal(h.statuses.at(-1).state,'failed')
  h.session.retryNow();h.session.start()
  assert.equal(h.sockets.length,0)
  assert.equal(h.timers.size,0)
})

test('invalid or failed current identity lookup fails closed and never exposes callback errors',()=>{
  for(const getAuthHeaders of [()=>({}),()=>({accesstoken:'fixture-secret',unknown:'bad'}),()=>{throw new Error('fixture-secret')}]) {
    const h=harness({getAuthHeaders});h.session.start()
    assert.equal(h.sockets.length,0)
    assert.equal(h.statuses.at(-1).state,'failed')
    assert.equal(JSON.stringify(h.statuses).includes('fixture-secret'),false)
    assert.equal(h.timers.size,0)
  }
})

test('identity is fetched at socket opening after an identity change, never captured at issuance or construction',()=>{
  let current={...authHeaders},reads=0
  const h=harness({getAuthHeaders:()=>{reads++;return current}})
  assert.equal(reads,0)
  current={...authHeaders,accesstoken:'platform-token-new-person','X-VLStream-Token':'local-token-new-person',tenantid:'new-person'}
  h.session.start()
  assert.equal(reads,1)
  assert.deepEqual(decodeAuth(h.sockets[0].protocols),current)
  assert.equal(h.sockets[0].url.includes('new-person'),false)
  h.session.stop();h.session.retryNow();h.session.start()
  assert.equal(reads,1)
  assert.equal(h.sockets.length,1)
  assert.equal(h.timers.size,0)
})

test('failed socket creation never retries the same one-use capability',()=>{
  let reads=0
  const h=harness({getAuthHeaders:()=>{reads++;return {...authHeaders}},socketFailure:true})
  h.session.start();h.session.retryNow();h.session.start()
  assert.equal(reads,1)
  assert.equal(h.sockets.length,1)
  assert.equal(h.statuses.at(-1).state,'failed')
  assert.equal(h.timers.size,0)
})

test('WVP auth getter reuses current REST header generation and projects only handshake headers',()=>{
  const wvpSource=readFileSync(new URL('../src/utils/wvpRequest.js',import.meta.url),'utf8')
    .replace(/^import .*$/gm,'').replace(/import\.meta\.env/g,'env')
    .replace(/^export default wvpRequest$/m,'').replace(/^export /gm,'')
  let identity={platform:'platform-one',local:'local-one',tenant:'tenant-one'},gatewayCalls=0
  let requestInterceptor
  const scope={env:{DEV:true},window:{location:{search:''}},SHARED_CAMERA_RTC_AUTH_HEADERS,replaceHeader,clearIdentityHeaders,
    axios:{create:()=>({interceptors:{request:{use:fn=>{requestInterceptor=fn}},response:{use(){}}}})},
    getPlatformAccessToken:()=>identity.platform,getLocalSessionToken:()=>identity.local,
    getStoredToken:()=>{throw new Error('unexpected fallback')},isMultiTenantMode:()=>true,
    sessionValue:keys=>keys.includes('tenantId')?identity.tenant:'',
    isTrustedApiRequest:config=>config.baseURL==='/wvp-api'&&config.url.startsWith('/vlstream/device/'),
    applyPlatformGatewayHeaders:config=>{gatewayCalls++;Object.assign(config.headers,{requesttype:'app',appid:'current-app',secretkey:'current-secret','Accept-Language':'zh','Content-Language':'zh','X-Unapproved':'omit'})},
    normalizeWvpResponse:value=>value,invalidateApiResponse:()=>{},ElMessage:{error(){}},saveAs(){},
  }
  runInNewContext(`${wvpSource}\nthis.getCurrentHeaders=getSharedCameraRtcAuthHeaders`,scope)
  const first=JSON.parse(JSON.stringify(scope.getCurrentHeaders()))
  assert.deepEqual(first,{accesstoken:'platform-one','X-VLStream-Token':'local-one',tenantid:'tenant-one',requesttype:'app',appid:'current-app',secretkey:'current-secret','X-WVP-Auth-Source':'vlstream'})
  assert.equal(gatewayCalls,1)
  identity={platform:'platform-two',local:'local-two',tenant:'tenant-two'}
  const second=JSON.parse(JSON.stringify(scope.getCurrentHeaders()))
  const currentRest=requestInterceptor({baseURL:'/wvp-api',url:'/vlstream/device/streams',headers:{}}).headers
  assert.deepEqual(second,Object.fromEntries(Object.entries(currentRest).filter(([key])=>SHARED_CAMERA_RTC_AUTH_HEADERS.some(name=>name.toLowerCase()===key.toLowerCase()))))
  assert.equal(second.accesstoken,'platform-two')
  assert.equal(second['X-VLStream-Token'],'local-two')
  assert.equal(second.tenantid,'tenant-two')
  assert.equal(second['X-Unapproved'],undefined)
  identity={platform:'platform-two',local:'',tenant:'tenant-two'}
  assert.throws(()=>scope.getCurrentHeaders(),error=>error.code==='PLATFORM_AUTH_REQUIRED')
})

function actualWvpIdentity({multi=false,local='current-local-token',platform='current-platform-token',search='',storedWvp=''}={}) {
  const wvpSource=readFileSync(new URL('../src/utils/wvpRequest.js',import.meta.url),'utf8')
    .replace(/^import .*$/gm,'').replace(/import\.meta\.env/g,'env')
    .replace(/^export default wvpRequest$/m,'').replace(/^export /gm,'')
  let requestInterceptor
  const scope={env:{DEV:true},window:{location:{search}},URLSearchParams,SHARED_CAMERA_RTC_AUTH_HEADERS,replaceHeader,clearIdentityHeaders,
    axios:{create:()=>({interceptors:{request:{use:fn=>{requestInterceptor=fn}},response:{use(){}}}})},
    getPlatformAccessToken:()=>platform,getLocalSessionToken:()=>local,getStoredToken:()=>platform||local,isMultiTenantMode:()=>multi,
    sessionValue:keys=>keys.includes('wvpAccessToken')?storedWvp:keys.includes('tenantId')?'current-tenant':'',
    isTrustedApiRequest:config=>config.baseURL==='/wvp-api'&&config.url.startsWith('/vlstream/device/'),
    applyPlatformGatewayHeaders:config=>Object.assign(config.headers,{requesttype:'app',appid:'current-app',secretkey:'current-secret'}),
    normalizeWvpResponse:value=>value,invalidateApiResponse:()=>{},ElMessage:{error(){}},saveAs(){},
  }
  runInNewContext(`${wvpSource}\nthis.getCurrentHeaders=getSharedCameraRtcAuthHeaders`,scope)
  // VM objects have a different Object.prototype; bridge only their data into the actual socket adapter.
  return {
    getAuthHeaders:()=>JSON.parse(JSON.stringify(scope.getCurrentHeaders())),
    restHeaders:()=>JSON.parse(JSON.stringify(requestInterceptor({baseURL:'/wvp-api',url:'/vlstream/device/streams',headers:{}}).headers)),
  }
}

for(const staleSource of ['url','storage'])test(`single-mode current VLS logout rejects stale WVP ${staleSource} token before opening a socket`,()=>{
  const actual=actualWvpIdentity({local:'',platform:'',search:staleSource==='url'?'?wvpAccessToken=previous-wvp-token':'',storedWvp:staleSource==='storage'?'previous-wvp-token':''})
  // General REST compatibility is intentionally unchanged by the stricter shared socket getter.
  assert.equal(actual.restHeaders().accesstoken,'previous-wvp-token')
  const h=harness({getAuthHeaders:actual.getAuthHeaders});h.session.start()
  assert.equal(h.sockets.length,0)
  assert.equal(h.statuses.at(-1).state,'failed')
  assert.equal(JSON.stringify(h.statuses).includes('previous-wvp-token'),false)
  assert.throws(actual.getAuthHeaders,error=>error.code==='PLATFORM_AUTH_REQUIRED')
  h.session.retryNow();h.session.start()
  assert.equal(h.sockets.length,0)
  assert.equal(h.timers.size,0)
})

test('actual shared getter rejects whitespace, bare Bearer and non-string current VLS identities',()=>{
  for(const local of ['   ','Bearer','Bearer    ','  Bearer  ',null,7,{},['token']]) {
    const actual=actualWvpIdentity({local,platform:'',search:'?wvpAccessToken=previous-wvp-token'})
    const h=harness({getAuthHeaders:actual.getAuthHeaders});h.session.start()
    assert.equal(h.sockets.length,0)
    assert.equal(h.statuses.at(-1).state,'failed')
    assert.throws(actual.getAuthHeaders,error=>error.code==='PLATFORM_AUTH_REQUIRED')
  }
})

test('actual shared getter preserves current single-mode VLS and existing multi-mode gateway identity',()=>{
  for(const multi of [false,true]) {
    const actual=actualWvpIdentity({multi,local:'Bearer current-local-token',platform:'current-platform-token',search:'?wvpAccessToken=previous-wvp-token',storedWvp:'stored-wvp-token'})
    const h=harness({getAuthHeaders:actual.getAuthHeaders});h.session.start()
    assert.equal(h.sockets.length,1)
    const headers=decodeAuth(h.sockets[0].protocols)
    assert.deepEqual(headers,{accesstoken:multi?'current-platform-token':'previous-wvp-token','X-VLStream-Token':'current-local-token',tenantid:'current-tenant',requesttype:'app',appid:'current-app',secretkey:'current-secret','X-WVP-Auth-Source':'vlstream'})
    assert.equal(h.sockets[0].url.includes('token'),false)
    h.session.stop()
    assert.equal(h.timers.size,0)
  }
})
