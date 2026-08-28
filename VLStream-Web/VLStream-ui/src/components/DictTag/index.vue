<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div>
    <template v-for="(item, index) in options">
      <template v-if="values.includes(item.value)">
        <span
          v-if="(item.elTagType == 'default' || item.elTagType == '') && (item.elTagClass == '' || item.elTagClass == null)"
          :key="item.value"
          :index="index"
          :class="item.elTagClass"
        >{{ item.label + " " }}</span>
        <el-tag
          v-else
          :disable-transitions="true"
          :key="item.value + ''"
          :index="index"
          :type="item.elTagType"
          :class="item.elTagClass"
        >{{ item.label + " " }}</el-tag>
      </template>
    </template>
    <template v-if="unmatch && showValue">
      {{ unmatchArray | handleArray }}
    </template>
  </div>
</template>

<script setup>
// record not item
const unmatchArray = ref([]);

const props = defineProps({
  // data
  options: {
    type: Array,
    default: null,
  },
  // current value
  value: [Number, String, Array],
  // not data , value
  showValue: {
    type: Boolean,
    default: true,
  },
  separator: {
    type: String,
    default: ",",
  }
});

const values = computed(() => {
  if (props.value === null || typeof props.value === 'undefined' || props.value === '') return [];
  return Array.isArray(props.value) ? props.value.map(item => '' + item) : String(props.value).split(props.separator);
});

const unmatch = computed(() => {
  unmatchArray.value = [];
  // value
  if (props.value === null || typeof props.value === 'undefined' || props.value === '' || !Array.isArray(props.options) || props.options.length === 0) return false
  // value to array
  let unmatch = false // Check whether not item
  values.value.forEach(item => {
    if (!props.options.some(v => v.value === item)) {
      unmatchArray.value.push(item)
      unmatch = true // if not item , Set to true
    }
  })
  return unmatch // value
});

function handleArray(array) {
  if (array.length === 0) return "";
  return array.reduce((pre, cur) => {
    return pre + " " + cur;
  });
}
</script>

<style scoped>
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
