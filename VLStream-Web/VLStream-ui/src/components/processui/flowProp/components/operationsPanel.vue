<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div class="flow_opration_panel">
    <el-checkbox-group v-model="copyActiveChooseData.operations" @change="checkChange">
      <el-checkbox
        v-for="(item,index) in options"
        :key="index"
        :value="item.dictValue"
        :label="item.dictLabel"
      />
    </el-checkbox-group>
  </div>
</template>

<script setup>
import { defineProps, inject, onMounted, ref, watch } from 'vue'
import { getApprovalButton, getApprovalButton_wk } from '@/api/processui'
import { ElCheckbox, ElCheckboxGroup } from 'element-plus'

const props = defineProps({
  activeChooseData: {
    type: Object,
    default: () => ({})
  }
})
const flowDesignerPage = inject('flowDesignerPage') // notificationnode: work order ,workflow

const copyActiveChooseData = ref(props.activeChooseData)
// watch nodeConfig
watch(() => props.activeChooseData, () => {
  copyActiveChooseData.value = props.activeChooseData
})

const options = ref([])

async function getOpertaList() {
  // operation : work orderwk_approval_button,workflowwf_approval_button
  let res = flowDesignerPage?.notifyNode ? await getApprovalButton_wk() : await getApprovalButton()
  if (res.code === 200) {
    options.value = res.data
  }
}

const emits = defineEmits(['update:activeChooseData', 'close'])
function checkChange(data) {
  copyActiveChooseData.value.operations = data
  emits('update:activeChooseData', copyActiveChooseData.value)
}

onMounted(() => {
  getOpertaList()
})

</script>

<style lang="scss" scoped>
    .flow_opration_panel{

    }
</style>
