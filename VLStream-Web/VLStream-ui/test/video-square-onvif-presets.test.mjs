import test from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import {computed,ref,watch,effectScope,reactive,nextTick} from 'vue'
import {parse,compileScript} from 'vue/compiler-sfc'
import {createOnvifDevicePresetApi,createOnvifPresetController} from '../src/utils/onvifDevicePresets.mjs'

const source=readFileSync(new URL('../src/views/VideoAggregation/components/VideoSquareOnvifPresets.vue',import.meta.url),'utf8')
const executable=compileScript(parse(source).descriptor,{id:'preset-test'}).content.replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm,'').replace('export default','return')
const component=new Function('__deps',`const {computed,ref,watch,onBeforeUnmount,ElMessage,ElMessageBox,request,checkPermi,createOnvifDevicePresetApi,createOnvifPresetController,translatePhrase}=__deps;${executable}`)
const defer=()=>{let resolve,reject;const promise=new Promise((yes,no)=>{resolve=yes;reject=no});return {promise,resolve,reject}}
function harness() {
    const confirmation=defer(), calls=[], messages=[], unmount=[]
    const props=reactive({source:{rowId:11,protocol:'ONVIF'},visible:true,viewOnly:false,capable:true,beforeAction:()=>{}})
    const scope=effectScope()
    const instance=scope.run(()=>component({computed,ref,watch,onBeforeUnmount:fn=>unmount.push(fn),
        ElMessage:{success:value=>messages.push(value)},ElMessageBox:{confirm:()=>confirmation.promise},
        request:async request=>{calls.push(request);return {data:[{token:'one',name:'Door'}]}},
        checkPermi:()=>true,createOnvifDevicePresetApi,createOnvifPresetController,translatePhrase:value=>value,
    }).setup(props,{expose(){}}))
    return {props,instance,calls,messages,confirmation,close:()=>{unmount.forEach(fn=>fn());scope.stop()}}
}
test('delete confirmation cancelled by source change even when returning to the original source',async()=>{
    const h=harness();await nextTick();await nextTick()
    h.instance.selected.value='one'
    const deleting=h.instance.removeSelected()
    h.props.source={rowId:12,protocol:'ONVIF'};await nextTick()
    h.props.source={rowId:11,protocol:'ONVIF'};await nextTick();await nextTick()
    h.instance.selected.value='one'
    h.confirmation.resolve();await deleting
    assert.equal(h.calls.filter(row=>row.method==='delete').length,0)
    assert.deepEqual(h.messages,[]);h.close()
})
test('cancelled confirmation never sends a delete and hiding resets the selected preset',async()=>{
    const h=harness();await nextTick();await nextTick()
    h.instance.selected.value='one'
    const deleting=h.instance.removeSelected()
    h.confirmation.reject(Error('cancel'));await deleting
    h.props.visible=false;await nextTick()
    assert.equal(h.instance.selected.value,'')
    assert.equal(h.calls.filter(row=>row.method!=='get').length,0);h.close()
})
