<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div id="ptzPreset" style="width: 100%">
    <!--  -->
    <el-tag
        v-if="presetList.length > 0"
        v-for="item in presetList"
        :key="item.presetId"
        closable
        @close="delPreset(item)"
        @click="gotoPreset(item)"
        size="small"
        style="margin-right: 1rem; cursor: pointer; margin-bottom: 0.6rem"
    >
      {{ item.presetName || item.presetId }}
    </el-tag>

    <!--  -->
    <el-input
        v-if="inputVisible"
        v-model="ptzPresetId"
        placeholder="预置位编号"
        addon-before="预置位编号"
        addon-after="(1-255)"
        style="width: 500px; vertical-align: bottom;"
        ref="saveTagInput"
        size="small"
    >
      <template #append>
        <el-button @click="addPreset" style="width: 100px;">保存</el-button>
        |
        <el-button @click="cancel" style="width: 100px;">取消</el-button>
      </template>
    </el-input>

    <!-- button -->
    <el-button v-else size="small" @click="showInput">+ 添加</el-button>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue';
import axios from 'axios';
import {ElMessage, ElLoading, ElMessageBox} from 'element-plus';
import {callPreset, deletePreset, getAddPreset, gotoPresetList} from "@/api/wvp/channel.js";

// Props
const props = defineProps({
  channelDeviceId: {
    type: String,
    required: true
  },
  deviceId: {
    type: String,
    required: true
  }
});

// data
const presetList = ref([]);
const inputVisible = ref(false);
const ptzPresetId = ref('');
const saveTagInput = ref(null);

// sub
onMounted(() => {
  getPresetList();
});

// Get
const getPresetList = async () => {
  const url = {
    deviceId: props.deviceId,
    channelDeviceId: props.channelDeviceId,
  }
  const res = await gotoPresetList(url);
  presetList.value = res
};

//
const showInput = () => {
  inputVisible.value = true;
  nextTick(() => {
    saveTagInput.value.focus();
  });
};

//
const addPreset = async () => {
  const loading = ElLoading.service({
    lock: true,
    text: '正在发送指令',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)'
  });

  try {
    const url = {
      deviceId: props.deviceId,
      channelDeviceId: props.channelDeviceId,
    }
    await getAddPreset(url, {presetId: ptzPresetId.value} ).then(() => {
      setTimeout(() => {
        loading.close();
        inputVisible.value = false;
        ptzPresetId.value = '';
        getPresetList();
      }, 1000);
    }).catch(() => {
      loading.close();
      ElMessage({
        showClose: true,
        message: res.data.msg,
        type: 'error'
      });
    })
  } catch (error) {
    loading.close();
    ElMessage({
      showClose: true,
      message: error.message || '添加失败',
      type: 'error'
    });
  }
};


//
const cancel = () => {
  inputVisible.value = false;
  ptzPresetId.value = '';
};

//
const gotoPreset = async (preset) => {
  try {
    const url = {
      deviceId: props.deviceId,
      channelDeviceId: props.channelDeviceId,
    }
    await callPreset(url, {presetId: preset.presetId}).then(() => {
      ElMessage({
        showClose: true,
        message: '调用成功',
        type: 'success'
      });
    }).catch(() => {
      ElMessage({
        showClose: true,
        message: res.data.msg,
        type: 'error'
      });
    })
  } catch (error) {
    ElMessage({
      showClose: true,
      message: error.message || '调用失败',
      type: 'error'
    });
  }
};

// Delete
const delPreset = (preset) => {
  ElMessageBox.confirm('确定删除此预置位？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
      .then(async () => {
        const loading = ElLoading.service({
          lock: true,
          text: '正在发送指令',
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0.7)'
        });

        try {
          const url = {
            deviceId: props.deviceId,
            channelDeviceId: props.channelDeviceId,
          }
          await deletePreset(url, {presetId: preset.presetId}).then(() => {
            setTimeout(() => {
              loading.close();
              getPresetList();
            }, 1000);
          }).catch(() => {
            loading.close();
            ElMessage({
              showClose: true,
              message: res.data.msg,
              type: 'error'
            });
          })
        } catch (error) {
          loading.close();
          ElMessage({
            showClose: true,
            message: error.message || '删除失败',
            type: 'error'
          });
        }
      })
      .catch(() => {
        // user operation
      });
};
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
