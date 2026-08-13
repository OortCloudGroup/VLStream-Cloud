<template>
  <div id="ptzScan">
    <el-form size="mini" :inline="true">
      <el-form-item>
        <el-input
            min="1"
            max="4095"
            placeholder="开关编号"
            addonBefore="开关编号"
            addonAfter="(2-255)"
            v-model="switchId"
            size="mini"
        >
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button size="mini" @click="() => open('on')">开启</el-button>
        <el-button size="mini" @click="() => open('off')">关闭</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage, ElLoading } from "element-plus";
import {GetPtzSwitch} from "@/api/wvp/channel.js";

const props = defineProps({
  channelDeviceId: String,
  deviceId: String,
});

const url = ref({
  deviceId: props.deviceId,
  channelDeviceId: props.channelDeviceId,
});

const switchId = ref(1);

const open = async (command) => {
  const loading = ElLoading.service({
    lock: true,
    fullscreen: true,
    text: "正在发送指令",
    spinner: "el-icon-loading",
    background: "rgba(0, 0, 0, 0.7)",
  });

  await GetPtzSwitch(url.value, {command: command,switchId: switchId.value}).then(() => {
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
