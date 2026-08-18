<template>
  <div class="tenant_Page draHeaPB">
    <el-tabs v-model="queryParams.hasLink" class="tenanat-tabs" @tab-click="handleClick">
      <el-tab-pane label="未关联" name="false"/>
      <el-tab-pane label="已关联" name="true"/>
    </el-tabs>
    <div class="tenant_content">
      <div class="tableTenBox">
      <div class="depNameBox_out flexRowAC">
        <div class="depNameBox flexRowAC">
          <div class="exportBtnBox flexRowAC">
          <button
            v-if="queryParams.hasLink !== 'true'"
            type="button"
            class="exportBtn newBtn flexRowAC"
            :disabled="multiple"
            @click="handleAdd"
            v-hasPermi="['wvp:record:channelAdd']"
          >
            <el-icon class="BtnImg"><Plus /></el-icon>新增
          </button>
          <button
            v-else
            type="button"
            class="exportBtn newBtn flexRowAC"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['wvp:record:channelDelete']"
          >
            <el-icon class="BtnImg"><Delete /></el-icon>删除
          </button>
          <button-group :button-list="toolbarButtons" />
          </div>
        </div>
        <div class="searchHeight_out flexRowAC">
          <search-height-box keyword="query" placeholder="请输入关键字" :data="searchData" @handle="searchResetFn" />
          <export-excel-pdf />
        </div>
      </div>

      <table-self v-loading="loading" :data="channelList" @selection-change="handleSelectionChange" class="new_table" header-cell-class-name="header_tenant_cell" stripe>
        <el-table-column type="selection" :width="clacPXToVW(55)" align="center"/>
        <el-table-column prop="gbName" label="名称" :min-width="clacPXToVW(180)" align="center"/>
        <el-table-column prop="gbDeviceId" label="编号" :min-width="clacPXToVW(180)" align="center"/>
        <el-table-column prop="gbManufacturer" label="厂家" :min-width="clacPXToVW(100)" align="center"/>
        <el-table-column label="类型" :min-width="clacPXToVW(100)" align="center">
          <template #default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag effect="plain" v-if="scope.row.dataType === 1">国标设备</el-tag>
              <el-tag effect="plain" type="success" v-else-if="scope.row.dataType === 2">推流设备</el-tag>
              <el-tag effect="plain" type="warning" v-else-if="scope.row.dataType === 3">拉流代理</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" :min-width="clacPXToVW(100)" align="center">
          <template #default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.gbStatus === 'ON'">在线</el-tag>
              <el-tag type="info" v-if="scope.row.gbStatus !== 'ON'">离线</el-tag>
            </div>
          </template>
        </el-table-column>
      </table-self>

      <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
      />

      <el-dialog :title="title" v-model="open" width="800px" append-to-body>
        <div class="searchHeight_out flexRowAC" style="margin-bottom: 12px; justify-content: flex-end;">
          <search-height-box keyword="name" placeholder="请输入设备名称等关键词" :data="deviceSearchData" @handle="deviceSearchResetFn" />
          <export-excel-pdf />
        </div>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain :disabled="multipleDevice" @click="handleSure">确定</el-button>
          </el-col>
        </el-row>

        <table-self v-loading="loading" :data="deviceList" @selection-change="handleSelectionDeviceChange" class="new_table" header-cell-class-name="header_tenant_cell" stripe>
          <el-table-column type="selection" :width="clacPXToVW(55)" align="center"/>
          <el-table-column prop="name" label="名称" align="center"/>
          <el-table-column prop="deviceId" label="设备编号" align="center"/>
          <el-table-column prop="channelCount" label="通道数" align="center"/>
          <el-table-column prop="manufacturer" label="厂家" align="center"/>
          <el-table-column label="地址" align="center">
            <template #default="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag v-if="scope.row.hostAddress" size="medium">{{ scope.row.hostAddress }}</el-tag>
                <el-tag v-if="!scope.row.hostAddress" size="medium">未知</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center">
            <template #default="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag v-if="scope.row.onLine">在线</el-tag>
                <el-tag type="info" v-if="!scope.row.onLine">离线</el-tag>
              </div>
            </template>
          </el-table-column>
        </table-self>

        <pagination
            v-show="totalDevice > 0"
            :total="totalDevice"
            v-model:page="queryParamsDevice.pageNum"
            v-model:limit="queryParamsDevice.pageSize"
            @pagination="getDeviceList"
        />
      </el-dialog>
      </div>
    </div>
  </div>
</template>

<script setup name="RecordPlan">
import { clacPXToVW } from "@/utils/wvpCompat";
import {link, listPlanRecord} from "../../../api/wvp/record.js";
import {useRoute} from "vue-router";
import {listDevice} from "../../../api/wvp/device.js";

const {proxy} = getCurrentInstance();
const route = useRoute();

const loading = ref(false)
const channelList = ref([])
const total = ref(0);
const multiple = ref(true);
const selectionList = ref([]);
const open = ref(false);
const title = ref("");

const loadingDevice = ref(false)
const deviceList = ref([])
const totalDevice = ref(0);
const multipleDevice = ref(true);
const selectionDeviceList = ref([]);
const typeDevice = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    query: undefined,
    online: undefined,
    channelType: undefined,
    hasLink: 'false',
  },
  rules: {},
  queryParamsDevice: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    status: undefined,
    ip: undefined,
    manufacturer: undefined,
  }
});

const {queryParams, form, rules, queryParamsDevice} = toRefs(data);

const toolbarButtons = computed(() => {
  if (queryParams.value.hasLink === 'true') {
    return [
      { name: '按设备移除', svg: 'delete', permi: ['wvp:record:channelDelete'], clickFn: () => handleRemoveByDevice() },
      { name: '移除所有通道', svg: 'delete', permi: ['wvp:record:channelDelete'], clickFn: () => handleRemoveAll() }
    ]
  }
  return [
    { name: '按设备添加', svg: 'edit', permi: ['wvp:record:channelAdd'], clickFn: () => handleAddByDevice() },
    { name: '添加所有通道', svg: 'edit', permi: ['wvp:record:channelAdd'], clickFn: () => handleAddAll() }
  ]
});

const searchData = [
  {
    label: '在线状态',
    value: 'online',
    type: 'select',
    option: [
      { label: '在线', value: 'true' },
      { label: '离线', value: 'false' }
    ],
    default: undefined
  },
  {
    label: '类型',
    value: 'channelType',
    type: 'select',
    option: [
      { label: '国标设备', value: 1 },
      { label: '推流设备', value: 2 },
      { label: '拉流代理', value: 3 }
    ],
    default: undefined
  }
];

const deviceSearchData = [
  { label: '地址', value: 'ip', type: 'text', default: '' },
  { label: '厂家', value: 'manufacturer', type: 'text', default: '' },
  {
    label: '在线状态',
    value: 'status',
    type: 'select',
    option: [
      { label: '在线', value: 'true' },
      { label: '离线', value: 'false' }
    ],
    default: undefined
  }
];

function getList() {
  loading.value = true
  listPlanRecord(queryParams.value).then((res) => {
    channelList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function searchResetFn(val) {
  queryParams.value.pageNum = 1;
  queryParams.value.query = val.query || undefined;
  queryParams.value.online = val.online || undefined;
  queryParams.value.channelType = val.channelType || undefined;
  getList();
}

function deviceSearchResetFn(val) {
  queryParamsDevice.value.pageNum = 1;
  queryParamsDevice.value.name = val.name || undefined;
  queryParamsDevice.value.ip = val.ip || undefined;
  queryParamsDevice.value.manufacturer = val.manufacturer || undefined;
  queryParamsDevice.value.status = val.status || undefined;
  getDeviceList();
}

/** 选择条数  */
function handleSelectionChange(selection) {
  selectionList.value = selection
  multiple.value = !selection.length;
}

function handleAdd() {
  let channels = []
  for (let i = 0; i < selectionList.value.length; i++) {
    channels.push(selectionList.value[i].gbId)
  }
  if (channels.length === 0) {
    proxy.$modal.msgError("请选择要关联的通道");
    return;
  }
  linkPlan({
    planId: queryParams.value.planId,
    channelIds: channels
  })
  proxy.$modal.msgSuccess("关联成功");
}

function linkPlan(data) {
  link(data).then((res) => {
    getList();
  })
}

function handleDelete() {
  let channels = []
  for (let i = 0; i < selectionList.value.length; i++) {
    channels.push(selectionList.value[i].gbId)
  }
  if (channels.length === 0) {
    proxy.$modal.msgError("请选择要关联的通道");
    return;
  }
  linkPlan({
    channelIds: channels
  })
  proxy.$modal.msgSuccess("取消关联成功");
}

function handleAddAll() {
  proxy.$modal.confirm('添加所有通道将包括已经添加到其他计划的通道，确定添加所有通道？').then(function () {
    return linkPlan({
      planId: queryParams.value.planId,
      allLink: true
    })
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("添加成功");
  }).catch(() => {
  });
}

function handleRemoveAll() {
  proxy.$modal.confirm('确定移除所有通道？').then(function () {
    return linkPlan({
      planId: queryParams.value.planId,
      allLink: false
    })
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("移除成功");
  }).catch(() => {
  });
}

function handleAddByDevice() {
  open.value = true
  title.value = "添加国标设备通道"
  getDeviceList()
  typeDevice.value = "addDevice"
}

function handleRemoveByDevice() {
  open.value = true
  title.value = "移除国标设备通道"
  getDeviceList()
  typeDevice.value = "removeDevice"
}

function getDeviceList() {
  loadingDevice.value = true
  listDevice(queryParamsDevice.value).then((res) => {
    deviceList.value = res.rows
    totalDevice.value = res.total
    loadingDevice.value = false
  })
}

function handleSelectionDeviceChange(selection){
  selectionDeviceList.value = selection
  multipleDevice.value = !selection.length;
}

function handleSure(){
  let deviceIds = []
  for (let i = 0; i < selectionDeviceList.value.length; i++) {
    deviceIds.push(selectionDeviceList.value[i].id)
  }
  if(typeDevice.value === "addDevice"){
    linkPlan({
      planId: queryParams.value.planId,
      deviceDbIds: deviceIds
    })
    proxy.$modal.msgSuccess("关联成功");
  }else if(typeDevice.value === "removeDevice"){
    linkPlan({
      deviceDbIds: deviceIds
    })
    proxy.$modal.msgSuccess("取消关联成功");
  }
  open.value = false
}

const handleClick = () => {
  nextTick(() => {
    getList();
  })
}


onMounted(() => {
  queryParams.value.planId = route.params && route.params.planId;
  getList();
})
</script>

<style scoped lang="scss">
.tenant_Page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  overflow: hidden;
}

.tenant_content {
  width: 100%;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.tableTenBox {
  padding: 20px;
  width: 100%;
  height: 100%;
  flex: 1;
  background: #fff;
  min-height: 0;
}
</style>
