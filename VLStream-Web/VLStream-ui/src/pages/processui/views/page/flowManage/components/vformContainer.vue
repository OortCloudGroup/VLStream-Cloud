<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div>
    <VFormRender
      ref="preForm"
      :form-json="formJson"
      :form-data="formData"
      :preview-state="previewState"
      :option-data="optionData"
      :global-dsv="globalDsv"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'

const props = defineProps({
  formJson: {
    type: Object,
    required: true
  },
  formData: {
    type: Object,
    required: true
  },
  previewState: {
    type: Boolean,
    required: true
  },
  optionData: {
    type: Object,
    required: true
  },
  globalDsv: {
    type: Object,
    required: true
  },
  nodeFormProperties: {
    type: Object
  }
})

const preForm = ref(null)
const formJson = ref(props.formJson)
const formData = ref(props.formData)
const previewState = ref(props.previewState)
const optionData = ref(props.optionData)
const globalDsv = ref(props.globalDsv)

onMounted(() => {
  // form
  if (formJson.value) {
    // TODO form
    nextTick(() => {
      preForm.value?.disableForm()
      preForm.value?.setFormItemAuth(props.nodeFormProperties)
    })
  }
})

</script>
