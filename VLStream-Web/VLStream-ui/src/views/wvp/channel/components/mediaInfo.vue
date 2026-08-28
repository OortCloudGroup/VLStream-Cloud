<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div id="mediaInfo">
    <!-- new button -->
    <el-button
        style="position: absolute; right: 1rem;"
        icon="RefreshRight"
        circle
        size="mini"
        @click="getMediaInfo"
    ></el-button>

    <!-- info -->
    <el-descriptions size="mini" :column="3" title="概况">
      <el-descriptions-item label="观看人数">{{ info.readerCount }}</el-descriptions-item>
      <el-descriptions-item label="网络">{{ formatByteSpeed() }}</el-descriptions-item>
      <el-descriptions-item label="持续时间">{{ formatAliveSecond() }}</el-descriptions-item>
    </el-descriptions>

    <!-- and info -->
    <div style="display: grid; grid-template-columns: 1fr 1fr">
      <!-- info -->
      <el-descriptions size="mini" v-if="info.videoCodec" :column="2" title="视频信息">
        <el-descriptions-item label="编码">{{ info.videoCodec }}</el-descriptions-item>
        <el-descriptions-item label="分辨率">
          {{ info.width }}x{{ info.height }}
        </el-descriptions-item>
        <el-descriptions-item label="FPS">{{ info.fps }}</el-descriptions-item>
        <el-descriptions-item label="丢包率">{{ info.loss }}</el-descriptions-item>
      </el-descriptions>

      <!-- info -->
      <el-descriptions size="mini" v-if="info.audioCodec" :column="2" title="音频信息">
        <el-descriptions-item label="编码">{{ info.audioCodec }}</el-descriptions-item>
        <el-descriptions-item label="采样率">{{ info.audioSampleRate }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import {getServerMediaInfo} from "@/api/wvp/channel.js";

// Props
const props = defineProps({
  app: {
    type: String,
    required: true
  },
  stream: {
    type: String,
    required: true
  },
  mediaServerId: {
    type: String,
    required: true
  }
});

// data
const info = ref({});
const task = ref(null);

// Get info
const getMediaInfo = async () => {
  const params = {
    app: props.app,
    stream: props.stream,
    mediaServerId: props.mediaServerId
  }
  const res= await getServerMediaInfo(params);
  info.value = res.data;
};

// Format
const formatByteSpeed = () => {
  const bytesSpeed = info.value.bytesSpeed || 0;
  const num = 1024.0;

  if (bytesSpeed < num) return `${bytesSpeed} B/S`;
  if (bytesSpeed < Math.pow(num, 2)) return `${(bytesSpeed / num).toFixed(2)} KB/S`;
  if (bytesSpeed < Math.pow(num, 3)) return `${(bytesSpeed / Math.pow(num, 2)).toFixed(2)} MB/S`;
  if (bytesSpeed < Math.pow(num, 4)) return `${(bytesSpeed / Math.pow(num, 3)).toFixed(2)} G/S`;
  return `${(bytesSpeed / Math.pow(num, 4)).toFixed(2)} T/S`;
};

// Format
const formatAliveSecond = () => {
  const aliveSecond = info.value.aliveSecond || 0;
  const h = Math.floor(aliveSecond / 3600);
  const minute = Math.floor((aliveSecond / 60) % 60);
  const second = Math.ceil(aliveSecond % 60);

  const hours = h > 0 ? `${h}小时` : '';
  const minutes = minute < 10 ? `0${minute}` : `${minute}`;
  const seconds = second < 10 ? `0${second}` : `${second}`;

  return `${hours}${minutes}分${seconds}秒`;
};

// task
const startTask = () => {
  task.value = setInterval(getMediaInfo, 1000);
};

// task
const stopTask = () => {
  if (task.value) {
    clearInterval(task.value);
    task.value = null;
  }
};

// sub
onMounted(() => {
  getMediaInfo(); // Initialize Get data
  startTask(); // task
});

onUnmounted(() => {
  stopTask();
});
</script>

<style scoped>
.channel-form {
  display: grid;
  background-color: #ffffff;
  padding: 1rem 2rem 0 2rem;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}
</style>
