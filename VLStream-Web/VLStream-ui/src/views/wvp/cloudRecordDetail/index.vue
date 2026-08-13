<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="3">
        <div class="head-container">
          <el-date-picker
              style="width: 100%"
              v-model="chooseDate"
              type="date"
              value-format="YYYY-MM-DD"
              @change="dateChange()"
              :clearable="false"
              placeholder="日期">
          </el-date-picker>
        </div>

        <div class="head-container" v-loading="loading">
          <InfiniteList
              style="margin-top: 20px"
              v-if="detailFiles.length >0"
              :data="detailFiles"
              :width="'100%'"
              :height="840"
              :itemSize="40"
              v-slot="{ item, index }"
          >
            <div style="display: flex;align-items: center;justify-content: center;cursor: pointer">
              <el-tag @click="chooseFile(item)" size="large" :type="choosedFile === item.fileName ? 'danger' : ''">
                <div style="display: flex;align-items: center">
                  <span> <VideoCamera style="width: 15px;"/></span>
                  <span> {{ moment(item.startTime).format('HH:mm:ss') }}-{{
                      moment(item.endTime).format('HH:mm:ss')
                    }}</span>
                </div>
              </el-tag>
              <Download style="margin-left: 10px;width: 15px;color: var(--el-color-primary);"
                        @click="downloadFile(item)"/>
            </div>
          </InfiniteList>

          <el-empty v-if="detailFiles.length === 0" :image-size="50" description="暂无数据"/>
        </div>
      </el-col>

      <el-col :span="21">
        <div style="height:890px">
          <easy-player class="player" :video-url="videoUrl" autoplay :live="true"></easy-player>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="CloudRecordDetail">
import InfiniteList from 'vue3-infinite-list';
import {useRoute} from "vue-router";
import moment from "moment";
import {getPlayUrlPath, listDateRecord, openRtpServer} from "../../../api/wvp/record.js";

const route = useRoute();

const queryDate = ref(new Date());
const dateFilesObj = ref([]);
const pageNum = ref(1);
const pageSize = ref(10000);
const total = ref(0);
const chooseDate = ref(null);
const detailFiles = ref([]);
const streamId = ref(null);
const videoUrl = ref(null);
const app = ref(null);
const mediaServerId = ref(null);
const timeFormat = ref('00:00:00');
const mediaServerList = ref('00:00:00');
const choosedFile = ref(null);
const loading = ref(false);

function dateChange() {
  detailFiles.value = [];
  pageNum.value = 1;
  let chooseFullDate = new Date(chooseDate.value + " " + timeFormat.value);
  if (chooseFullDate.getFullYear() !== queryDate.value.getFullYear()
      || chooseFullDate.getMonth() !== queryDate.value.getMonth()) {
    queryDate.value = chooseFullDate;
    getDateInYear()
  }
  queryRecordDetails()
}

function queryRecordDetails(callback) {
  openRtpServer({
    app: app.value,
    stream: streamId.value,
    startTime: chooseDate.value + " 00:00:00",
    endTime: chooseDate.value + " 23:59:59",
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    mediaServerId: mediaServerId.value
  }).then((res) => {
    total.value = res.total;
    detailFiles.value = detailFiles.value.concat(res.rows);
    let temp = new Set()
    for (let i = 0; i < detailFiles.value.length; i++) {
      temp.add(detailFiles.value[i].mediaServerId)
    }
    mediaServerList.value = Array.from(temp)
    if (mediaServerList.value.length === 1) {
      mediaServerId.value = mediaServerList.value[0]
    }
    loading.value = false
    if (callback) callback();
  })
}

function getDateInYear(callback) {
  dateFilesObj.value = {};

  listDateRecord({
    app: app.value,
    stream: streamId.value,
    year: queryDate.value.getFullYear(),
    month: queryDate.value.getMonth() + 1,
    mediaServerId: mediaServerId.value,
  }).then((res) => {
    if (res.data.length > 0) {
      for (let i = 0; i < res.data.length; i++) {
        dateFilesObj.value[res.data[i]] = res.data[i]
      }
    }

    if (callback) callback();
  })
}

function chooseFile(file) {
  if (file == null) {
    videoUrl.value = "";
    choosedFile.value = "";
  } else {
    choosedFile.value = file.fileName;
    getPlayUrlPath({recordId: file.id}).then((res) => {
      if (location.protocol === "https:") {
        videoUrl.value = res.data.httpsPath;
      } else {
        videoUrl.value = res.data.httpPath;
      }
    })
  }
}

function downloadFile(file) {
  getPlayUrlPath({recordId: file.id}).then((res) => {
    const link = document.createElement('a');
    link.target = "_blank";

    if (location.protocol === "https:") {
      link.href = res.data.httpsPath + "&save_name=" + file.fileName;
    } else {
      link.href = res.data.httpPath + "&save_name=" + file.fileName;
      link.click();
    }
  })
}

onMounted(async () => {
  loading.value = true
  app.value = route.query && route.query.app;
  streamId.value = route.query && route.query.streamId;
  getDateInYear(() => {
    if (Object.values(dateFilesObj.value).length > 0) {
      chooseDate.value = Object.values(dateFilesObj.value)[Object.values(dateFilesObj.value).length - 1];
    }else {
      chooseDate.value = moment().format('YYYY-MM-DD')
    }
    dateChange();
  })
})
</script>

<style>

</style>
