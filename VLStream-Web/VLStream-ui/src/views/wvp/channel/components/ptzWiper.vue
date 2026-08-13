<template>
  <div id="ptzWiper">
    <el-button size="mini" @click="() => open('on')">开启</el-button>
    <el-button size="mini" @click="() => open('off')">关闭</el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElLoading, ElMessage } from 'element-plus';
import {GetPtzWiper} from "@/api/wvp/channel.js";

// 接收 props
const props = defineProps({
  channelDeviceId: String,
  deviceId: String,
});

const url = ref({
  deviceId: props.deviceId,
  channelDeviceId: props.channelDeviceId,
});
// 定义方法
const open = async (command) => {
  const loading = ElLoading.service({
    lock: true,
    fullscreen: true,
    text: '正在发送指令',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });

  await GetPtzWiper(url.value, {command}).then(() => {
    ElMessage({
      showClose: true,
      message: '保存成功',
      type: 'success',
    });
  }).catch((error) => {
    ElMessage({
      showClose: true,
      message: error?.message || '请求失败',
      type: 'error',
    });
  }).finally(() => {
    loading.close();
  })
};
</script>
