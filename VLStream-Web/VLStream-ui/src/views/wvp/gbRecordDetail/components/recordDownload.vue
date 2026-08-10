<template>
  <div id="recordDownload">
    <el-dialog
        :title="title"
        v-if="showDialog"
        width="45rem"
        :append-to-body="true"
        :close-on-click-modal="false"
        :model-value="showDialog"
        :destroy-on-close="true"
        @close="close()"
        center
    >
      <el-row>
        <el-col :span="18" style="padding-top: 7px;">
          <el-progress :percentage="percentage"></el-progress>
        </el-col>
        <el-col :span="6">
          <el-button
              icon="el-icon-download"
              v-if="downloadFile"
              size="mini"
              title="点击下载"
              @click="downloadFileClientEvent()"
          >
            下载
          </el-button>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, onMounted, onBeforeUnmount} from 'vue'
import {progressStop, progress} from "@/api/wvp/gb_record.js";

const title = ref("下载中...")
const deviceId = ref("")
const channelId = ref("")
const app = ref("")
const stream = ref("")
const mediaServerId = ref("")
const showDialog = ref(false)
const scale = ref(1)
const percentage = ref(0.00)
const streamInfo = ref(null)
const taskId = ref(null)
const getProgressRun = ref(false)
const timer = ref(null)
const downloadFile = ref(null)

const openDialog = (pDeviceId, pChannelId, pApp, pStream, pMediaServerId) => {
  deviceId.value = pDeviceId
  channelId.value = pChannelId
  app.value = pApp
  stream.value = pStream
  mediaServerId.value = pMediaServerId
  showDialog.value = true
  getProgressRun.value = true
  percentage.value = 0.0
  downloadFile.value = null
  getProgressTimer()
}

const getProgressTimer = () => {
  if (!getProgressRun.value) return
  if (downloadFile.value) return

  setTimeout(() => {
    console.log("11111", Boolean(!showDialog.value))
    if (!showDialog.value) return
    getProgress(getProgressTimer)
  }, 5000)
}

const getProgress = (callback) => {
  const params = {
    deviceId: deviceId.value,
    channelId: channelId.value,
    stream: stream.value
  }
  progress(params).then((res) => {
    streamInfo.value = res.data
    if (parseFloat(res.data.progress) === 1) {
      percentage.value = 100
    } else {
      percentage.value = (parseFloat(res.data.progress) * 100).toFixed(1)
    }
    if (streamInfo.value.downLoadFilePath) {
      if (location.protocol === "https:") {
        downloadFile.value = streamInfo.value.downLoadFilePath.httpsPath
      } else {
        downloadFile.value = streamInfo.value.downLoadFilePath.httpPath
      }
      percentage.value = 100
      getProgressRun.value = false
      downloadFileClientEvent()
    }
    if (callback) callback()
  }).catch((e) => {
    close()
  })
}

const close = () => {
  if (streamInfo.value?.progress < 1) {
    stopDownloadRecord()
  }

  if (timer.value !== null) {
    window.clearTimeout(timer.value)
    timer.value = null
  }
  showDialog.value = false
  getProgressRun.value = false
}

const gbScale = (sScale) => {
  scale.value = sScale
}

const stopDownloadRecord = (callback) => {
  if (deviceId.value && channelId.value && stream.value) {
    const params = {
      deviceId: deviceId.value,
      channelId: channelId.value,
      stream: stream.value
    }
    progressStop(params).then(res => {
      if (callback) callback(res)
    })
  }
}

const downloadFileClientEvent = () => {
  const x = new XMLHttpRequest()
  x.open("GET", downloadFile.value, true)
  x.responseType = 'blob'
  x.onload = (e) => {
    const url = window.URL.createObjectURL(x.response)
    const a = document.createElement('a')
    a.href = url
    a.download = `${deviceId.value}-${channelId.value}.mp4`
    a.click()
  }
  x.send()
}

// 生命周期钩子
onMounted(() => {
  window.addEventListener('beforeunload', stopDownloadRecord)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', stopDownloadRecord)
})

// 暴露方法给父组件使用
defineExpose({
  openDialog,
  close,
  gbScale
})
</script>
