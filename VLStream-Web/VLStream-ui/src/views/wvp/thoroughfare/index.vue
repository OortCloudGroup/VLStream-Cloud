<template>
  <div class="thoroughfare-page tenant_Page draHeaPB">
    <el-tabs v-model="activeName" class="tenanat-tabs">
      <el-tab-pane label="行政区划" name="region" />
      <el-tab-pane label="业务分组" name="group" />
    </el-tabs>
    <div class="tenant_content">
      <Region v-if="activeName === 'region'" />
      <Group v-if="activeName === 'group'" />
    </div>
  </div>
</template>

<script setup name="Thoroughfare">
import Region from "./region.vue"
import Group from "./group.vue"

const activeName = ref('region')
</script>

<style scoped lang="scss">
.thoroughfare-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  overflow: hidden;
}

.tenant_content {
  width: 100%;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
