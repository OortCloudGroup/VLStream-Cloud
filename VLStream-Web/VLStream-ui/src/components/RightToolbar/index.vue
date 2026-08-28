<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="top-right-btn" :style="style">
    <el-row>
      <el-tooltip class="item" effect="dark" :content="showSearch ? '隐藏搜索' : '显示搜索'" placement="top" v-if="search">
        <el-button circle icon="Search" @click="toggleSearch()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="刷新" placement="top">
        <el-button circle icon="Refresh" @click="refresh()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="显隐列" placement="top" v-if="columns">
        <el-button circle icon="Menu" @click="showColumn()" v-if="showColumnsType == 'transfer'"/>
        <el-dropdown trigger="click" :hide-on-click="false" style="padding-left: 12px" v-if="showColumnsType == 'checkbox'">
          <el-button circle icon="Menu" />
          <template #dropdown>
            <el-dropdown-menu>
              <template v-for="item in columns" :key="item.key">
                <el-dropdown-item>
                  <el-checkbox :checked="item.visible" @change="checkboxChange($event, item.label)" :label="item.label" />
                </el-dropdown-item>
              </template>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-tooltip>
    </el-row>
    <el-dialog :title="title" v-model="open" append-to-body>
      <el-transfer
        :titles="['显示', '隐藏']"
        v-model="value"
        :data="columns"
        @change="dataChange"
      ></el-transfer>
    </el-dialog>
  </div>
</template>

<script setup>
const props = defineProps({
  /* whether */
  showSearch: {
    type: Boolean,
    default: true,
  },
  /* info */
  columns: {
    type: Array,
  },
  /* whether */
  search: {
    type: Boolean,
    default: true,
  },
  /* (transfer 、checkbox ) */
  showColumnsType: {
    type: String,
    default: "checkbox",
  },
  /*  */
  gutter: {
    type: Number,
    default: 10,
  },
})

const emits = defineEmits(['update:showSearch', 'queryTable']);

// data
const value = ref([]);
// layer
const title = ref("显示/隐藏");
// whether layer
const open = ref(false);

const style = computed(() => {
  const ret = {};
  if (props.gutter) {
    ret.marginRight = `${props.gutter / 2}px`;
  }
  return ret;
});

//
function toggleSearch() {
  emits("update:showSearch", !props.showSearch);
}

// new
function refresh() {
  emits("queryTable");
}

// element
function dataChange(data) {
  for (let item in props.columns) {
    const key = props.columns[item].key;
    props.columns[item].visible = !data.includes(key);
  }
}

// dialog
function showColumn() {
  open.value = true;
}

if (props.showColumnsType == 'transfer') {
  //
  for (let item in props.columns) {
    if (props.columns[item].visible === false) {
      value.value.push(parseInt(item));
    }
  }
}

//
function checkboxChange(event, label) {
  props.columns.filter(item => item.label == label)[0].visible = event;
}

</script>

<style lang='scss' scoped>
:deep(.el-transfer__button) {
  border-radius: 50%;
  display: block;
  margin-left: 0px;
}
:deep(.el-transfer__button:first-child) {
  margin-bottom: 10px;
}
:deep(.el-dropdown-menu__item) {
  line-height: 30px;
  padding: 0 17px;
}
</style>
