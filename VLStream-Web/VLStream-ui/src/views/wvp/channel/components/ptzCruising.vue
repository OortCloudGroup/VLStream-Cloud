<template>
  <div id="ptzCruising">
    <div style="display: grid; grid-template-columns: 80px auto; line-height: 28px">
      <span>巡航组号: </span>
      <el-input
          min="1"
          max="255"
          placeholder="巡航组号"
          addonBefore="巡航组号"
          addonAfter="(1-255)"
          v-model="cruiseId"
          size="mini"
      >
      </el-input>
    </div>
    <p>
      <el-tag
          v-for="(item, index) in presetList"
          :key="item.presetId"
          closable
          @close="delPreset(item, index)"
          style="margin-right: 1rem; cursor: pointer"
      >
        {{ item.presetName ? item.presetName : item.presetId }}
      </el-tag>
    </p>

    <el-form size="mini" :inline="true" v-if="selectPresetVisible">
      <el-form-item>
        <el-select v-model="selectPreset" placeholder="请选择预置点">
          <el-option
              v-for="item in allPresetList"
              :key="item.presetId"
              :label="item.presetName"
              :value="item"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="addCruisePoint">保存</el-button>
        <el-button type="primary" @click="cancelAddCruisePoint">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button size="mini" v-else @click="selectPresetVisible = true">添加巡航点</el-button>

    <el-form size="mini" :inline="true" v-if="setSpeedVisible">
      <el-form-item>
        <el-input
            min="1"
            max="4095"
            placeholder="巡航速度"
            addonBefore="巡航速度"
            addonAfter="(1-4095)"
            v-model="cruiseSpeed"
            size="mini"
        >
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="setCruiseSpeed">保存</el-button>
        <el-button @click="cancelSetCruiseSpeed">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="mini" @click="setSpeedVisible = true">设置巡航速度</el-button>

    <el-form size="mini" :inline="true" v-if="setTimeVisible">
      <el-form-item>
        <el-input
            min="1"
            max="4095"
            placeholder="巡航停留时间(秒)"
            addonBefore="巡航停留时间(秒)"
            addonAfter="(1-4095)"
            style="width: 100%;"
            v-model="cruiseTime"
        >
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="setCruiseTime">保存</el-button>
        <el-button @click="cancelSetCruiseTime">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="mini" @click="setTimeVisible = true">设置巡航时间</el-button>
    <el-button size="mini" @click="startCruise">开始巡航</el-button>
    <el-button size="mini" @click="stopCruise">停止巡航</el-button>
    <el-button size="mini" type="danger" @click="deleteCruise">删除巡航</el-button>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue';
import {ElMessage, ElMessageBox, ElLoading} from 'element-plus';
import {GetAddCruisePoint, GetDeleteCruisePoint, gotoPresetList} from "@/api/wvp/channel.js";

const props = defineProps(['channelDeviceId', 'deviceId']);

const cruiseId = ref(1);
const presetList = ref([]);
const allPresetList = ref([]);
const selectPreset = ref("");
const selectPresetVisible = ref(false);
const setSpeedVisible = ref(false);
const setTimeVisible = ref(false);
const cruiseSpeed = ref("");
const cruiseTime = ref("");
const url = ref({
  deviceId: props.deviceId,
  channelDeviceId: props.channelDeviceId,
});

const getPresetList = async () => {
  try {
    const res = await gotoPresetList(url.value);
    allPresetList.value = res.data;
  } catch (error) {
    console.error(error);
  }
};

const addCruisePoint = async () => {
  const loading = ElLoading.service({
    lock: true,
    text: '正在发送指令',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  try {
    await GetAddCruisePoint(url.value, {
      cruiseId: cruiseId.value,
      presetId: selectPreset.value.presetId,
    }).then(() => {
      presetList.value.push(selectPreset.value);
    })
  } catch (error) {
    ElMessage.error(error.toString());
  } finally {
    selectPreset.value = "";
    selectPresetVisible.value = false;
    loading.close();
  }
};

const cancelAddCruisePoint = () => {
  selectPreset.value = "";
  selectPresetVisible.value = false;
};

const delPreset = async (preset, index) => {
  const loading = ElLoading.service({
    lock: true,
    text: '正在发送指令',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  try {
    await GetDeleteCruisePoint(url.value,
        {
          cruiseId: cruiseId.value,
          presetId: preset.presetId,
        }).then(() => {
      presetList.value.splice(index, 1);
    })

  } catch (error) {
    ElMessage.error(error.toString());
  } finally {
    loading.close();
  }
};

const deleteCruise = async () => {
  try {
    await ElMessageBox.confirm("确定删除此巡航组", "提示", {
      dangerouslyUseHTMLString: true,
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    });
    const loading = ElLoading.service({
      lock: true,
      text: "正在发送指令",
      background: "rgba(0, 0, 0, 0.7)",
    });
    await GetDeleteCruisePoint(url.value, {
        cruiseId: cruiseId.value,
        presetId: 0,
      }).then(() => {
      presetList.value = [];
    })
    loading.close();
  } catch (error) {
    console.error(error);
  }
};

// Similar implementations for setCruiseSpeed, cancelSetCruiseSpeed, setCruiseTime, cancelSetCruiseTime, startCruise, stopCruise...

onMounted(() => {
  getPresetList();
});
</script>

<style>
.channel-form {
  display: grid;
  background-color: #ffffff;
  padding: 1rem 2rem 0 2rem;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}
</style>
