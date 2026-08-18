<template>
  <div class="live-page tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <div class="police_aside_use">
          <div class="treeTitle">通道列表</div>
          <div class="live-switch-row flexRowAC">
            <el-switch
              v-model="activeValue"
              active-text="行政区划"
              inactive-text="业务分组"
              @change="onSwitch"
            />
          </div>
          <el-tree
            :data="treeData"
            :props="defaultProps"
            lazy
            :load="loadNode"
            highlight-current
            :expand-on-click-node="false"
            style="background: #fff;"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="custom-tree-node flexRowAC">
                <div class="tree-node-main flexRowAC">
                  <el-icon class="tree-icon">
                    <VideoCamera v-if="data.leaf" />
                    <Folder v-else />
                  </el-icon>
                  <el-tooltip :open-delay="500" effect="light" :content="node.label" placement="top">
                    <div class="tree-node-label">{{ node.label }}</div>
                  </el-tooltip>
                </div>
              </div>
            </template>
          </el-tree>
        </div>

        <div class="tableTenItU live-right">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <span class="live-toolbar-label">分屏</span>
              <div class="live-split-btns flexRowAC">
                <svg-icon
                  :class="['flex-icon', { active: splitShow === 1 }]"
                  icon-class="splitOne"
                  @click="spiltIndex(1)"
                />
                <svg-icon
                  :class="['flex-icon', { active: splitShow === 4 }]"
                  icon-class="splitFour"
                  @click="spiltIndex(4)"
                />
                <svg-icon
                  :class="['flex-icon', { active: splitShow === 6 }]"
                  icon-class="splitSix"
                  @click="spiltIndex(6)"
                />
                <svg-icon
                  :class="['flex-icon', { active: splitShow === 9 }]"
                  icon-class="splitNine"
                  @click="spiltIndex(9)"
                />
              </div>
            </div>
          </div>

          <div class="live-player-grid" :class="'split-' + splitShow">
            <div
              v-for="(item, index) in splitLayouts[splitShow]"
              :key="index"
              :class="['player-cell', { active: activePlayerIndex === index }]"
              @click="setActivePlayer(index)"
            >
              <CusPlayer :ref="'video' + index" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="WVPLive">
import { Folder, VideoCamera } from '@element-plus/icons-vue'
import {queryForTree} from "@/api/wvp/region";
import {queryListByCivilCode, queryListByParentId, sendDevicePush} from "@/api/wvp/channel.js";
import {queryForTree as groupQueryForTree} from "@/api/wvp/group.js";
import CusPlayer from "@/components/flv/CusPlayer.vue";
import { start as playPush} from "@/api/wvp/push.js";
import { start as playProxy } from "@/api/wvp/proxy.js";

const {proxy} = getCurrentInstance();

const queryParams = ref({
  pageNum: 1,
  pageSize: 200,
})

const treeData = ref([]);

const defaultProps = {
  children: 'children',
  label: 'name',
  isLeaf: 'leaf'
};

const splitLayouts = {
  1: [1],
  4: [1, 2, 3, 4],
  6: [1, 2, 3, 4, 5, 6],
  9: [1, 2, 3, 4, 5, 6, 7, 8, 9],
};

async function onSwitch() {
  if (activeValue.value) {
    await getTreeData();
  } else {
    await getGroupQueryForTree();
  }
}

const loadNode = async (node, resolve) => {
  if (node.level === 0) {
    return resolve(treeData.value);
  } else if (node.level === 1) {
    return resolve(treeData.value[node.level - 1].children);
  } else if (node.level === 2) {
    if (activeValue.value) {
      queryParams.value.civilCode = node.data.deviceId;
      const response = await queryListByCivilCode(queryParams.value);
      const children = response.rows.map(item => ({
        ...item,
        leaf: true,
        name: item.gbName
      }));
      resolve(children);
    } else {
      queryParams.value.groupDeviceId = node.data.deviceId;
      const response = await queryListByParentId(queryParams.value);
      const children = response.rows.map(item => ({
        ...item,
        leaf: true,
        name: item.gbName
      }));
      resolve(children);
    }
  }
};

const handleNodeClick = async (data) => {
  if(activePlayerIndex.value == null){
    proxy.$modal.msgError("请先选择一个播放窗口");
    return
  }

  if(data.dataType === 1){
    if (data.gbDeviceId && data.gbParentId) {
      const params = {
        deviceId: data.gbParentId,
        channelId: data.gbDeviceId
      }
      const res = await sendDevicePush(params);

      const videoRef = proxy.$refs[`video${activePlayerIndex.value}`];
      if (videoRef && videoRef[0]) {
        if (location.protocol === "https:") {
          videoRef[0].createPlayer(res.data.https_flv, 0);
        } else {
          videoRef[0].createPlayer(res.data.flv, 0);
        }
      } else {
        proxy.$modal.msgError("请选择播放器");
      }
    } else {
      proxy.$modal.msgError('通道或设备不存在')
    }
  }

  if(data.dataType === 2) {
    const ans = await playPush({id: data.dataDeviceId});
    const videoRef = proxy.$refs[`video${activePlayerIndex.value}`];
    if (videoRef && videoRef[0]) {
      if (location.protocol === "https:") {
        videoRef[0].createPlayer(ans.data.https_flv, 0);
      } else {
        videoRef[0].createPlayer(ans.data.flv, 0);
      }
    } else {
      proxy.$modal.msgError("请选择播放器");
    }
  }

  if(data.dataType === 3) {
    const ans = await playProxy({id: data.dataDeviceId});
    const videoRef = proxy.$refs[`video${activePlayerIndex.value}`];
    if (videoRef && videoRef[0]) {
      if (location.protocol === "https:"){
        videoRef[0].createPlayer(ans.data.https_flv, 0);
      } else {
        videoRef[0].createPlayer(ans.data.flv, 0);
      }
    } else {
      proxy.$modal.msgError("请选择播放器");
    }
  }

};

const splitShow = ref(1)
const activePlayerIndex = ref(null);
const activeValue = ref(true);

function setActivePlayer(index) {
  activePlayerIndex.value = index;
}

async function getTreeData() {
  const res = await queryForTree();
  let data = [
    {
      name: "根资源组",
      children: []
    }
  ]
  data[0].children = proxy.handleTree(res.data, "id")
  treeData.value = data;
}

async function getGroupQueryForTree() {
  const res = await groupQueryForTree();
  let data = [
    {
      name: "根资源组",
      children: []
    }
  ]
  data[0].children = proxy.handleTree(res.data, "id")
  treeData.value = data;
}

function spiltIndex(index){
  splitShow.value = index;
  activePlayerIndex.value = null;
}

onMounted(async () => {
  await getTreeData();
});

</script>

<style scoped lang="scss">
.live-page {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  overflow: hidden;

  .tenant_content {
    flex: 1;
    min-height: 0;
    width: 100%;
    display: flex;
    flex-direction: column;
  }

  .tableTenBox {
    padding: 20px;
    width: 100%;
    height: 100%;
    min-height: 0;
    flex: 1;
    background: #fff;
    align-items: stretch;
    box-sizing: border-box;
  }
}

.police_aside_use {
  width: 300px;
  padding-right: 20px;
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .treeTitle {
    color: var(--el-color-primary);
    padding: 4px 0 16px;
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;

    &::before {
      content: "";
      width: 3px;
      height: 18px;
      background-color: var(--el-color-primary);
    }
  }

  :deep(.el-tree-node__content) {
    --el-tree-node-hover-bg-color: var(--el-menu-hover-bg-color);
    height: 38px;
    font-size: 14px;
    color: #333;

    .custom-tree-node {
      width: 100%;
      justify-content: space-between;
      padding-right: 4px;
    }
  }

  :deep(.el-tree) {
    flex: 1;
    min-height: 0;
    overflow: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }
}

.live-switch-row {
  margin-bottom: 12px;
  flex-shrink: 0;
}

.custom-tree-node {
  flex: 1;
  min-width: 0;
  gap: 4px;

  .tree-node-main {
    flex: 1;
    min-width: 0;
    gap: 4px;
    overflow: hidden;
  }

  .tree-node-label {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .tree-icon {
    flex-shrink: 0;
    font-size: 14px;
    color: var(--el-color-primary);
  }
}

.live-right {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.depNameBox_out {
  padding-bottom: 16px;
}

.live-toolbar-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.live-split-btns {
  gap: 10px;
}

.live-player-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  gap: 2px;
  background: #000;
  border-radius: 4px;
  overflow: hidden;

  &.split-1 {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr;
  }

  &.split-4 {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: 1fr 1fr;
  }

  &.split-6 {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: 1fr 1fr 1fr;
  }

  &.split-9 {
    grid-template-columns: 1fr 1fr 1fr;
    grid-template-rows: 1fr 1fr 1fr;
  }
}

.player-cell {
  position: relative;
  min-width: 0;
  min-height: 0;
  background: #000;
  border: 2px solid #409eff;
  box-sizing: border-box;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s ease;

  &.active {
    border-color: #67c23a;
    z-index: 2;
  }

  :deep(.wvp-flv-player) {
    width: 100%;
    height: 100%;
    max-height: none;
    object-fit: contain;
    display: block;
    background: #000;
  }
}

.flex-icon {
  cursor: pointer;
  font-size: 20px;
  color: #909399;
  transition: color 0.2s ease, transform 0.2s ease;

  &.active {
    color: #409eff;
    transform: scale(1.15);
  }

  &:hover {
    color: #409eff;
  }
}
</style>
