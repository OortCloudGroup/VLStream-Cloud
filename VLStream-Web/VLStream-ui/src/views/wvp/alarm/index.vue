<template>
  <div class="tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox">
        <div class="depNameBox_out flexRowAC">
          <div class="depNameBox flexRowAC" />
          <div class="searchHeight_out flexRowAC">
            <search-height-box
              keyword="deviceId"
              placeholder="搜索设备编号"
              :data="searchData"
              @handle="searchResetFn"
            />
            <export-excel-pdf />
          </div>
        </div>

        <TableSelf
          class="new_table"
          header-cell-class-name="header_tenant_cell"
          stripe
          v-loading="loading"
          :data="alarmList"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" :width="clacPXToVW(55)" />
          <el-table-column label="编号" prop="id" :width="clacPXToVW(80)" />
          <el-table-column label="设备编号" prop="deviceId" show-overflow-tooltip />
          <el-table-column label="通道编号" prop="channelId" show-overflow-tooltip />
          <el-table-column label="报警等级" prop="alarmPriority" :width="clacPXToVW(110)" />
          <el-table-column label="报警方式" prop="alarmMethodDescription" show-overflow-tooltip />
          <el-table-column label="报警时间" prop="alarmTime" show-overflow-tooltip />
          <el-table-column label="报警类型" prop="alarmTypeDescription" show-overflow-tooltip />
        </TableSelf>

        <div class="paginationBox flexRowAC">
          <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="Alarm">
import { delAlarm, listAlarm } from "@/api/wvp/alarm";
import { clacPXToVW } from "@/utils/wvpCompat";

const { proxy } = getCurrentInstance();
const { alarm_priority, alarm_method } = proxy.useDict("alarm_priority", "alarm_method");

const alarmList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const searchData = computed(() => [
  { label: '通道编号', value: 'channelId', type: 'text', default: '' },
  { label: '报警等级', value: 'alarmPriority', type: 'select', option: alarm_priority.value || [], default: '' },
  { label: '报警方式', value: 'alarmMethod', type: 'select', option: alarm_method.value || [], default: '' },
  { label: '时间范围', value: 'dateRange', type: 'daterange', startP: '开始时间', endP: '结束时间', format: 'YYYY-MM-DD HH:mm:ss', default: [] }
])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    deviceId: null,
    channelId: null,
    alarmPriority: null,
    alarmMethod: null,
    alarmTime: null,
    alarmDescription: null,
    longitude: null,
    latitude: null,
    alarmType: null,
    startTime: null,
    endTime: null
  },
  rules: {
    deviceId: [
      { required: true, message: "设备id不能为空", trigger: "blur" }
    ],
    channelId: [
      { required: true, message: "通道id不能为空", trigger: "blur" }
    ],
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" }
    ]
  }
});

const { queryParams, form, rules } = toRefs(data);

function searchResetFn(val) {
  queryParams.value.pageNum = 1;
  queryParams.value.deviceId = val?.deviceId || null;
  queryParams.value.channelId = val?.channelId || null;
  queryParams.value.alarmPriority = val?.alarmPriority || null;
  queryParams.value.alarmMethod = val?.alarmMethod || null;
  if (val?.dateRange && val.dateRange.length === 2) {
    queryParams.value.startTime = val.dateRange[0];
    queryParams.value.endTime = val.dateRange[1];
  } else {
    queryParams.value.startTime = null;
    queryParams.value.endTime = null;
  }
  getList();
}

/** 查询报警列表 */
function getList() {
  loading.value = true;
  listAlarm(queryParams.value).then(response => {
    alarmList.value = response.list;
    total.value = response.total;
    loading.value = false;
  });
}

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    id: null,
    deviceId: null,
    channelId: null,
    alarmPriority: null,
    alarmMethod: null,
    alarmTime: null,
    alarmDescription: null,
    longitude: null,
    latitude: null,
    alarmType: null,
    createTime: null
  };
  proxy.resetForm("alarmRef");
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('确认删除？').then(function () {
    return delAlarm(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

getList();
</script>
