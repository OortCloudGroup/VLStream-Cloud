<template>
  <div class="app-container">
    <el-tabs v-model="activeName" class="work-tabs" @tab-click="handleClick">
      <el-tab-pane label="行政区划" name="region">
        <Region/>
      </el-tab-pane>
      <el-tab-pane label="业务分组" name="group">
        <Group/>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="Thoroughfare">
import Region from "./region.vue"
import Group from "./group.vue"

const activeName = ref('region')

const handleClick = () => {
  nextTick(() => {

  })
}
</script>

<style scoped lang="scss">
:deep(.work-tabs) {
  --el-tabs-header-height: 32px;

  .el-tabs__header {
    margin: 0 0 12px;
    border-bottom: 1px solid #f0f0f0;
  }

  .el-tabs__nav-wrap::after {
    display: none;
  }

  .el-tabs__item {
    width: 96px;
    height: 32px;
    padding: 0;
    line-height: 32px;
    text-align: center;
    color: #333;
    font-size: 14px;
    font-weight: 400;
    box-sizing: border-box;
  }

  .el-tabs__item.is-active {
    color: var(--el-color-primary);
  }

  .el-tabs__item:hover {
    color: var(--el-color-primary);
  }

  .el-tabs__active-bar {
    height: 2px;
    background-color: var(--el-color-primary);
  }
}
</style>
