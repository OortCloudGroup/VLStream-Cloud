<template>
  <div id="ptzScan">
    <div style="display: grid; grid-template-columns: 80px auto; line-height: 28px; margin-bottom: 10px;">
      <span>扫描组号: </span>
      <el-input
          min="1"
          max="255"
          placeholder="扫描组号"
          addonBefore="扫描组号"
          addonAfter="(1-255)"
          v-model="scanId"
          size="mini"
      >
      </el-input>
    </div>

    <el-button size="mini" @click="setScanLeft">设置左边界</el-button>
    <el-button size="mini" @click="setScanRight">设置右边界</el-button>

    <el-form size="mini" :inline="true" v-if="setSpeedVisible" style="margin-top: 5px">
      <el-form-item>
        <el-input
            min="1"
            max="4095"
            placeholder="巡航速度"
            addonBefore="巡航速度"
            addonAfter="(1-4095)"
            v-if="setSpeedVisible"
            v-model="speed"
            size="mini"
        >
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="setSpeed">保存</el-button>
        <el-button @click="cancelSetSpeed">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="mini" @click="setSpeedVisible = true">设置扫描速度</el-button>
    <el-button size="mini" @click="startScan">开始自动扫描</el-button>
    <el-button size="mini" @click="stopScan">停止自动扫描</el-button>
  </div>
</template>

<script setup>
import {ref} from 'vue';
import {ElLoading, ElMessage, ElInput, ElButton, ElForm, ElFormItem} from 'element-plus';
import {GetSetScanLeft, GetSetScanRight, GetSetSpeed, GetStartScan, GetStopScan} from "@/api/wvp/channel.js";

const props = defineProps({
  channelDeviceId: String,
  deviceId: String,
});

const scanId = ref(1);
const setSpeedVisible = ref(false);
const speed = ref('');
const url = ref({
  deviceId: props.deviceId,
  channelDeviceId: props.channelDeviceId,
});

const loading = (options) => ElLoading.service(options);

const showMessage = ({message, type}) => {
  ElMessage({
    showClose: true,
    message,
    type,
  });
};

const setSpeed = async () => {
  const loader = loading({
    lock: true,
    fullscreen: true,
    text: '正在发送指令',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  await GetSetSpeed(url.value, {scanId: scanId.value, speed: speed.value,}).then((res) => {
    showMessage({message: "保存成功", type: 'success'});
  }).catch((error) => {
    showMessage({message: error, type: 'error'});
  }).finally(() => {
    speed.value = '';
    setSpeedVisible.value = false;
    loader.close();
  });
};

const cancelSetSpeed = () => {
  speed.value = '';
  setSpeedVisible.value = false;
};

const setScanLeft = async () => {
  const loader = loading({
    lock: true,
    fullscreen: true,
    text: '正在发送指令',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  await GetSetScanLeft(url.value, {scanId: scanId.value}).then((res) => {
    showMessage({message: "保存成功", type: 'success'});
  }).catch((error) => {
    showMessage({message: error, type: 'error'});
  }).finally(() => {
    loader.close();
  });
};

const setScanRight = async () => {
  const loader = loading({
    lock: true,
    fullscreen: true,
    text: '正在发送指令',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  await GetSetScanRight(url.value, {scanId: scanId.value}).then((res) => {
    showMessage({message: "保存成功", type: 'success'});
  }).catch((error) => {
    showMessage({message: error, type: 'error'});
  }).finally(() => {
    setSpeedVisible.value = false;
    speed.value = '';
    loader.close();
  });
};

const startScan = async () => {
  const loader = loading({
    lock: true,
    fullscreen: true,
    text: '正在发送指令',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  await GetStartScan(url.value, {scanId: scanId.value}).then((res) => {
    showMessage({message: "发送成功", type: 'success'});
  }).catch((error) => {
    showMessage({message: error, type: 'error'});
  }).finally(() => {
    loader.close();
  });
};

const stopScan = async () => {
  const loader = loading({
    lock: true,
    fullscreen: true,
    text: '正在发送指令',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  await GetStopScan(url.value, {scanId: scanId.value}).then((res) => {
    showMessage({message: "发送成功", type: 'success'});
  }).catch((error) => {
    showMessage({message: error, type: 'error'});
  }).finally(() => {
    loader.close();
  });
};
</script>

