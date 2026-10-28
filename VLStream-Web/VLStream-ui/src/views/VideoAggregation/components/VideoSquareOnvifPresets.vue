<template>
  <section v-if="visible && source?.protocol === 'ONVIF'" class="square-onvif-presets">
    <strong>{{ $tp('预置点') }}</strong>
    <el-alert v-if="state.error" :title="$tp(state.error)" type="warning" :closable="false" />
    <el-button size="small" :disabled="!canRead || state.loading || state.busy" @click="resource.load()">{{ $tp('刷新') }}</el-button>
    <el-select v-model="selected" size="small" :loading="state.loading" :disabled="!state.ready || state.busy" :placeholder="$tp('请选择预置点')">
      <el-option v-for="row in state.rows" :key="row.token" :value="row.token" :label="row.name || row.token" />
    </el-select>
    <div class="preset-actions">
      <el-button size="small" :disabled="!canGo || !selected || !state.ready || state.busy" @click="run('goTo',selected)">{{ $tp('调用') }}</el-button>
      <el-button size="small" :disabled="!canRemove || !selected || !state.ready || state.busy" @click="removeSelected">{{ $tp('删除') }}</el-button>
    </div>
    <div class="preset-actions">
      <el-input v-model="name" size="small" :maxlength="128" :disabled="!canCreate || !state.ready || state.busy" :placeholder="$tp('请输入预置点名称')" />
      <el-button size="small" :disabled="!canCreate || !name.trim() || !state.ready || state.busy" @click="run('create',name.trim())">{{ $tp('添加') }}</el-button>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/wvpRequest'
import { checkPermi } from '@/utils/wvpPermission'
import { createOnvifDevicePresetApi, createOnvifPresetController } from '@/utils/onvifDevicePresets.mjs'
import { translatePhrase } from '@/i18n'
const props=defineProps({source:{type:Object,default:null},visible:Boolean,viewOnly:Boolean,capable:Boolean,beforeAction:{type:Function,default:()=>{}}})
const state=ref({rows:[],ready:false,loading:false,busy:false,error:''})
const selected=ref(''), name=ref('')
let contextRevision=0
const allowed = permission => props.visible && !props.viewOnly && props.capable && props.source?.protocol==='ONVIF' && checkPermi([permission])
const canRead=computed(()=>allowed('onvif:service:getPresets'))
const canGo=computed(()=>allowed('onvif:service:gotoPreset'))
const canCreate=computed(()=>allowed('onvif:service:addPreset'))
const canRemove=computed(()=>allowed('onvif:service:removePreset'))
const resource=createOnvifPresetController({api:createOnvifDevicePresetApi(request),beforeAction:()=>props.beforeAction(),onChange:value=>{state.value=value}})
watch(()=>[props.source?.rowId,props.source?.protocol,props.visible,props.viewOnly,props.capable,canRead.value,canGo.value,canCreate.value,canRemove.value],()=>{
  contextRevision++
  selected.value=''; name.value=''
  if(canRead.value) resource.open({id:props.source.rowId,canRead:true,goTo:canGo.value,create:canCreate.value,remove:canRemove.value})
  else resource.close()
},{immediate:true})
watch(()=>state.value.rows,rows=>{if(!rows.some(row=>row.token===selected.value))selected.value=''})
async function run(kind,value) {
  if(await resource.action(kind,value)) {
    if(kind==='create')name.value=''
    ElMessage.success(translatePhrase('指令已提交'))
  }
}
async function removeSelected() {
  const token=selected.value, revision=contextRevision
  try {await ElMessageBox.confirm(translatePhrase('确认删除所选预置点？'),translatePhrase('提示'),{type:'warning'})} catch {return}
  if(revision===contextRevision && canRemove.value)await run('remove',token)
}
onBeforeUnmount(()=>{contextRevision++; resource.close()})
</script>

<style scoped>
.square-onvif-presets { display:flex; flex-direction:column; gap:8px; margin-top:16px; }
.preset-actions { display:flex; gap:8px; align-items:center; }
.preset-actions .el-input { min-width:0; }
</style>
