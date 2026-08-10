<template>
  <div class="app-container">
    <div class="toolbar-with-search">
      <div class="toolbar-left" />
      <div class="searchHeight_out flexRowAC">
        <search-height-box
          keyword="query"
          placeholder="请输入关键字"
          :data="searchData"
          @handle="searchResetFn"
        />
        <export-excel-pdf />
      </div>
    </div>

    <table-self
      class="new_table"
      header-cell-class-name="header_tenant_cell"
      stripe
      v-loading="loading"
      :data="recordList"
      current-row-key="id"
    >
      <el-table-column prop="app" label="应用名" align="center" show-overflow-tooltip/>
      <el-table-column prop="stream" label="流ID" align="center" show-overflow-tooltip/>
      <el-table-column label="开始时间" align="center" show-overflow-tooltip>
        <template #default="scope">
          {{ formatTimeStamp(scope.row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" show-overflow-tooltip>
        <template #default="scope">
          {{ formatTimeStamp(scope.row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="时长" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.timeLen">{{ formatTime(scope.row.timeLen) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名称" align="center" show-overflow-tooltip/>
      <el-table-column prop="mediaServerId" label="流媒体" align="center" show-overflow-tooltip/>
      <el-table-column label="操作" align="right" fixed="right" :width="clacPXToVW(180)">
        <template #default="scope">
          <div class="operateAppBox flexRowAC" style="justify-content: flex-end;">
            <div
              class="new_table_svg_group"
              @click.stop="play(scope.row)"
              v-hasPermi="['wvp:record:play']"
            >
              <el-icon><View /></el-icon>
              <span>播放</span>
            </div>
            <div
              class="new_table_svg_group"
              @click.stop="downloadFile(scope.row)"
              v-hasPermi="['wvp:record:download']"
            >
              <el-icon><Download /></el-icon>
              <span>下载</span>
            </div>
          </div>
        </template>
      </el-table-column>
    </table-self>

    <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getRecordList"
    />

    <el-dialog title="播放视频" v-model="openPlay" width="1000px" append-to-body>
      <div class="player">
        <easy-player class="player" :video-url="videoUrl" autoplay :live="true"></easy-player>
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="CloudRecord">
import {getOnlineMediaServerList} from "../../../api/wvp/wvpMediaServer.js";
import {getPlayUrlPath, openRtpServer} from "../../../api/wvp/record.js";
import moment from 'moment'
import { clacPXToVW } from "@/utils/wvpCompat";
const {proxy} = getCurrentInstance();
const mediaServerList = ref([])
const recordList = ref([])
const loading = ref(false)
const openPlay = ref(false)
const total = ref(0);
const searchData = ref([]);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    startTime: undefined,
    endTime: undefined,
    mediaServerId: undefined,
    app: '',
    stream: '',
    query: '',
  },
});

const {queryParams} = toRefs(data);

/** 高级搜索 / 重置 */
function searchResetFn(val) {
  queryParams.value.pageNum = 1;
  queryParams.value.query = val.query || '';
  if (val.dateRange && val.dateRange.length === 2) {
    queryParams.value.startTime = val.dateRange[0];
    queryParams.value.endTime = val.dateRange[1];
  } else {
    queryParams.value.startTime = undefined;
    queryParams.value.endTime = undefined;
  }
  queryParams.value.mediaServerId = val.mediaServerId || undefined;
  getRecordList();
}

function initSearchData() {
  searchData.value = [
    {
      label: '时间范围',
      value: 'dateRange',
      type: 'daterange',
      startP: '开始时间',
      endP: '结束时间',
      format: 'YYYY-MM-DD HH:mm:ss',
      default: []
    },
    {
      label: '节点选择',
      value: 'mediaServerId',
      type: 'select',
      option: (mediaServerList.value || []).map(item => ({ label: item.id, value: item.id })),
      default: undefined
    }
  ];
}

function getRecordList() {
  loading.value = true
  openRtpServer(queryParams.value).then((res) => {
    loading.value = false
    recordList.value = res.rows
    total.value = res.total;
  })
}

function getMediaServerList() {
  getOnlineMediaServerList().then((res) => {
    mediaServerList.value = res.data
    initSearchData();
  })
}

function formatTimeStamp(time) {
  return moment.unix(time / 1000).format('yyyy-MM-DD HH:mm:ss')
}

function formatTime(time) {
  const h = parseInt(time / 3600 / 1000)
  const minute = parseInt((time - h * 3600 * 1000) / 60 / 1000)
  let second = Math.ceil((time - h * 3600 * 1000 - minute * 60 * 1000) / 1000)
  if (second < 0) {
    second = 0;
  }
  return (h > 0 ? h + `小时` : '') + (minute > 0 ? minute + '分' : '') + (second > 0 ? second + '秒' : '')
}

function downloadFile(file) {
  getPlayUrlPath({recordId: file.id}).then((res) => {
    const link = document.createElement('a');
    link.target = "_blank";
    if (location.protocol === "https:") {
      link.href = res.data.httpsPath + "&save_name=" + file.fileName;
    } else {
      link.href = res.data.httpPath + "&save_name=" + file.fileName;
    }
    link.click();
  })
}

const videoUrl = ref('');
async function play(row) {
  const res = await getPlayUrlPath({recordId: row.id});
  if (location.protocol === "https:") {
    videoUrl.value = res.data.httpsPath;
  } else {
    videoUrl.value = res.data.httpPath;
  }
  openPlay.value = true;
}

onMounted(() => {
  getRecordList();
  getMediaServerList();
})
</script>

<style scoped>
.player {
  width: 100%;
  height: 600px;
}
</style>
