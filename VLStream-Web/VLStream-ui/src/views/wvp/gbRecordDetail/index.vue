<template>
  <div class="app-container">
    <div style="margin-bottom: 10px;">
      <el-alert style="margin-bottom: 10px;" title="日期和时间不要选择太大要不然会很卡，解决方法：自行搭配el-table-v2" type="error" />
      <el-alert title="关于分页问题自行查看国标文件9.7，能否分页取决于厂家是否支持分页功能" type="error" />
    </div>
    <div class="toolbar-with-search">
      <div class="toolbar-left" />
      <div class="searchHeight_out flexRowAC">
        <search-height-box
          keyword="query"
          placeholder="请通过高级筛选选择时间与类型"
          :data="searchData"
          @handle="searchResetFn"
        />
        <export-excel-pdf />
      </div>
    </div>
    <table-self :data="detailFiles" height="600" class="new_table" header-cell-class-name="header_tenant_cell" stripe>
      <el-table-column label="设备ID" align="center" prop="deviceId" />
      <el-table-column label="位置" align="center" prop="address">
        <template #default="scope">
          <span v-if="scope.row.address">{{scope.row.address}}</span>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" />
      <el-table-column label="结束时间" align="center" prop="endTime" />
      <el-table-column label="类型" align="center" prop="type">
        <template #default="scope">
          <el-tag v-for="item in getMatchingType(scope.row.type)" :key="item.value">
            {{ item.label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="right" fixed="right" :width="clacPXToVW(180)">
        <template #default="scope">
          <div class="operateAppBox flexRowAC" style="justify-content: flex-end;">
            <div class="new_table_svg_group" @click.stop="playRecord(scope.row)" v-hasPermi="['gb:playback:start']">
              <el-icon><View /></el-icon>
              <span>播放</span>
            </div>
            <div class="new_table_svg_group" @click.stop="downloadFun(scope.row)" v-hasPermi="['gb:record:download']">
              <el-icon><Download /></el-icon>
              <span>下载</span>
            </div>
          </div>
        </template>
      </el-table-column>
    </table-self>
    <div style="margin-top: 20px; display: flex; justify-content: flex-end;">总条数： {{detailFiles.length}}</div>

    <el-dialog title="播放视频" v-model="openPlay" width="1000px" append-to-body>
      <div class="player">
        <easy-player class="player" :video-url="videoUrl" autoplay :live="true"></easy-player>
      </div>
    </el-dialog>
  </div>

  <RecordDownload ref="recordDownloadRef" />

</template>

<script setup name="GbRecordDetail">
import { clacPXToVW } from "@/utils/wvpCompat";
import {useRoute} from "vue-router";
import {download, progress, recordinfo} from "../../../api/wvp/gb_record.js";
import {playStop, start} from "../../../api/wvp/playback.js";
import {ElLoading, ElMessage} from 'element-plus';
import RecordDownload from "./components/recordDownload.vue"

const route = useRoute();

const deviceId = ref(null);
const channelId = ref(null);
const detailFiles = ref([]);
const startTime = ref(null);
const endTime = ref(null);
const streamId = ref(null);
const videoUrl = ref(null);
const streamInfo = ref(null);
const app = ref(null);
const mediaServerId = ref(null);
const recordDownloadRef = ref(null);
const ssrc = ref(null);
const dataInterval = ref([]);
const defaultTime = ref([
  new Date(2025, 1, 1, 10, 10),
  new Date(2025, 3, 1, 10, 10),
])
const openPlay = ref(false)

const optionsType = ref([
  {
    value: 'time',
    label: '定时录像',
  },
  {
    value: 'alarm',
    label: '报警触发录像',
  },
  {
    value: 'manual',
    label: '手动录像',
  },
  {
    value: 'all',
    label: '全部录像',
  },
])

function buildDefaultInterval() {
  const now = new Date();
  const currentHour = now.getHours();
  let startHour = currentHour - 3;
  let startDate = new Date(now);
  if (startHour < 0) {
    startDate.setDate(startDate.getDate() - 1);
    startHour = 24 + startHour;
  }
  const startOfDay = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate(), startHour, 0, 0);
  const endOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate(), currentHour, 59, 59);
  return [startOfDay, endOfDay];
}

const searchData = computed(() => [
  {
    label: '日期/时间',
    value: 'dataInterval',
    type: 'datetimerange',
    startP: '开始时间',
    endP: '结束时间',
    default: dataInterval.value?.length ? dataInterval.value : buildDefaultInterval(),
    defaultTime: defaultTime.value
  },
  {
    label: '类型',
    value: 'type',
    type: 'select',
    option: optionsType.value,
    default: 'alarm'
  }
]);

const handleQuery = () => {
  dateChange()
}

function searchResetFn(val) {
  if (val.dataInterval && val.dataInterval.length === 2) {
    dataInterval.value = val.dataInterval;
  } else {
    getDefaultTime();
  }
  queryParams.value.type = val.type || 'alarm';
  dateChange();
}

const resetQuery = () => {
  getDefaultTime();
  queryParams.value.deviceId = deviceId.value;
  queryParams.value.channelId = channelId.value;
  queryParams.value.startTime = '';
  queryParams.value.endTime = '';
  queryParams.value.type = 'alarm';
  dateChange()
}

const getMatchingType = (type) => {
  return optionsType.value.filter(item => item.value === type);
};

const queryParams = ref({
  deviceId: undefined,
  channelId: undefined,
  startTime: undefined,
  endTime: undefined,
  type: 'alarm',
});

function getDefaultTime(){
  dataInterval.value = buildDefaultInterval();
}

const playTime = ref(null);

const loading = ref(false);

function dateChange() {
  const loadingService = ElLoading.service({
    lock: true,
    fullscreen: true,
    text: '正在发送指令',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  queryParams.value.deviceId = deviceId.value;
  queryParams.value.channelId = channelId.value;
  queryParams.value.startTime = setTime(dataInterval.value[0]);
  queryParams.value.endTime = setTime(dataInterval.value[1]);
  detailFiles.value = [];
  recordinfo(queryParams.value).then(res => {
    detailFiles.value = res.recordList;
  }).finally(() => {
      loadingService.close();
    })
}

function setTime(dateStr) {
  const date = new Date(dateStr);
  const formattedDate = date.getFullYear() + '-' +
      String(date.getMonth() + 1).padStart(2, '0') + '-' +
      String(date.getDate()).padStart(2, '0') + ' ' +
      String(date.getHours()).padStart(2, '0') + ':' +
      String(date.getMinutes()).padStart(2, '0') + ':' +
      String(date.getSeconds()).padStart(2, '0');
  return formattedDate;
}

async function playRecord(row) {
  if (streamId.value) {
    await stopPlayRecord()
    streamId.value = "";
    startFun(row);
  } else {
    await startFun(row)
  }
}

function startFun(row) {
  const loadingService = ElLoading.service({
    lock: true,
    fullscreen: true,
    text: '正在发送指令',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  start({
    deviceId: deviceId.value,
    channelId: channelId.value,
    startTime: row.startTime,
    endTime: row.endTime,
  }).then((res) => {
    streamInfo.value = res.data;
    app.value = streamInfo.value.app;
    streamId.value = streamInfo.value.stream;
    mediaServerId.value = streamInfo.value.mediaServerId;
    ssrc.value = streamInfo.value.ssrc;
    getUrlByStreamInfo();
    openPlay.value = true;
  }).finally(() => {
    loadingService.close();
  })
}

function getUrlByStreamInfo() {
  if (location.protocol === "https:") {
    videoUrl.value = streamInfo.value["https_fmp4"]
  } else {
    videoUrl.value = streamInfo.value["fmp4"]
  }
}

function downloadFun(row) {
  console.log(row)
  if(streamId.value !== null && streamId.value !== ''){
    stopPlayRecord().then(() => {
      streamId.value = '';
      downloadFun();
    });
  } else {
    const loadingService = ElLoading.service({
      lock: true,
      fullscreen: true,
      text: '正在发送指令',
      spinner: 'el-icon-loading',
      background: 'rgba(0, 0, 0, 0.7)',
    });
    download({
      deviceId: deviceId.value,
      channelId: channelId.value,
      startTime: row.startTime,
      endTime: row.endTime,
      downloadSpeed: '1',
    }).then((res) => {
      recordDownloadRef.value.openDialog(deviceId.value, channelId.value, res.data.app, res.data.stream, res.data.mediaServerId);
    }) .catch(() => {
      loadingService.close();
    }).finally(() => {
      loadingService.close();
    });
  }
}

function stopPlayRecord() {
  return new Promise((resolve) => {
    playStop({
      deviceId: deviceId.value,
      channelId: channelId.value,
      streamId: streamId.value,
    }).then(() => {
      resolve();
    })
  });

}

function getTimeForFile(file) {
  let startTime = new Date(file.startTime);
  let endTime = new Date(file.endTime);
  return [startTime, endTime, endTime.getTime() - startTime.getTime()];
}

onMounted(() => {
  getDefaultTime();
  deviceId.value = route.params && route.params.deviceId;
  channelId.value = route.params && route.params.channelId;
  dateChange()
})
</script>

<style scoped>
.player {
  width: 100%;
  height: 600px;
}
</style>
