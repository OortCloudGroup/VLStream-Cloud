<!-- eslint-disable no-unused-vars,vue/html-indent -->

<template>
  <div class="tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <!-- 左侧菜单 -->
        <div class="left_menu_panel">
          <div class="left-menu-header">
            <div class="custom-tabs">
              <div class="custom-tab-item" :class="leftTabActive==='region'?'active':''" @click="handleTabClick('region')">
                <span>区域</span>
                <div class="header-actions">
                  <div class="auto-dispatch-switch">
                    <el-switch
                        v-model="regionAutoDispatch"
                        size="small"
                        inline-prompt
                        active-text="自动派单"
                        inactive-text="自动派单"
                        @change="handleRegionAutoDispatchChange"
                    />
                  </div>
                  <oort-svg-icon
                      width="20"
                      height="20"
                      name="setting"
                      style="cursor:pointer"
                      @click="handleHeaderSettingsClick"
                  />
                </div>
              </div>
              <div class="custom-tab-item" :class="leftTabActive==='group'?'active':''" @click="handleTabClick('group')">
                <span>分组</span>
                <div class="header-actions">
                  <div class="auto-dispatch-switch">
                    <el-switch
                        v-model="groupAutoDispatch"
                        size="small"
                        inline-prompt
                        active-text="自动派单"
                        inactive-text="自动派单"
                        @change="handleGroupAutoDispatchChange"
                    />
                  </div>
                  <oort-svg-icon
                      width="20"
                      height="20"
                      name="setting"
                      style="cursor:pointer"
                      @click="handleHeaderSettingsClick"
                  />
                </div>
              </div>
              <div class="custom-tab-item" :class="{active:leftTabActive==='tag'}" @click="handleTabClick('tag')">
                <span>标签</span>
                <div class="header-actions">
                  <div class="auto-dispatch-switch">
                    <el-switch
                      v-model="groupAutoDispatch"
                      size="small"
                      inline-prompt
                      active-text="自动派单"
                      inactive-text="自动派单"
                      @change="handleGroupAutoDispatchChange"
                    />
                  </div>
                  <oort-svg-icon
                    width="20"
                    height="20"
                    name="setting"
                    style="cursor:pointer"
                    @click="handleHeaderSettingsClick"
                  />
                </div>
              </div>
            </div>
          </div>
          <div class="tree-container">
            <el-tree
                ref="leftTreeRef"
                style="background: #fff;"
                :load="getDeptTreeUI"
                :data="treeData"
                lazy
                highlight-current
                node-key="id"
                :default-expanded-keys="defaultExpandedKeys"
                :props="defaultProps"
                :expand-on-click-node="false"
                @node-click="handleTreeNodeClick"
            >
              <template #default="{ node, data }">
                <div class="custom-tree-node flexRowAC">
                  <oort-img :src="data?.company_logo || data?.dept_photo || data?.photo" style="width: 1rem;height:1rem;margin: 0 0.25rem;border-radius: 50%" default-type="dept" />
                  <el-tooltip :open-delay="500" class="item" effect="light" :content="node.label" placement="top">
                    <div class="tree-node-label" :class="{'delete_text': data.oort_status === 9, 'activeDept': data.id === currentTreeNodeId}">
                      {{ node.label }}
                    </div>
                  </el-tooltip>
                  <div v-if="data.id !== ''" class="tree-node-actions">
                    <div class="switch-wrapper">
                      <el-switch
                          v-model="data.autoDispatch"
                          size="small"
                          inline-prompt
                          active-text="自动派单"
                          inactive-text="自动派单"
                          @change="handleAutoDispatchChange(data)"
                          @click.stop
                      />
                    </div>
                    <oort-svg-icon
                        width="20"
                        height="20"
                        name="setting"
                        style="cursor:pointer"
                        @click="handleSettingsClick(data)"
                    />
                  </div>
                </div>
              </template>
            </el-tree>
          </div>
        </div>
      </div>
    </div>
    <AutoDispatchSettingDialog
        v-model="settingDialogVisible"
        :info-text="dialogInfoText"
        :process-id="currentProcessId"
        @confirm="handleSettingConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, computed, onMounted } from 'vue'
import { ElTree, ElMessage } from 'element-plus'
import type Node from 'element-plus/es/components/tree/src/model/node'
import AutoDispatchSettingDialog from './components/AutoDispatchSettingDialog.vue'
import {
  workflowConfigGet,
  workflowConfigSet,
  event_group_setting_save,
  event_group_status,
  event_group_list
} from '@/api/smartCity/events'
import { useUserStore } from '@/store/modules/useraPaas'
import AppConfig from '@/config/AppConfig'

const store: any = useUserStore()

// 左侧菜单相关
const leftTabActive = ref('region')
const regionAutoDispatch = ref(false)
const groupAutoDispatch = ref(false)
const leftTreeRef = ref<InstanceType<typeof ElTree>>()
const treeData = ref<any>([]) // 部门树date
const currentTreeNodeId = ref<any>('') // 当前节点id
const currentTreeNodeLabel = ref('')
// 配置选项
const defaultProps = {
  label: 'name',
  children: 'children',
  isLeaf: (data) => {
    return !data.is_subtenant && !data.children
  }
}
const defaultExpandedKeys = ref([''])

interface Tree {
  name?: string
  label: string
  id: any
  uid?: string
  puid?: string
  hadchild?: number
  leaf?: boolean
  company_logo?: string
  dept_photo?: string
  photo?: string
  is_subtenant?: boolean
  children?: Tree[]
  autoDispatch?: boolean
  config?: {
    app_id?: string
    process_id?: string
    auto_to_work?: boolean
  }
}

const activeGroupType = computed(() => (leftTabActive.value === 'tag' ? 3 : leftTabActive.value === 'group' ? 2 : 1))

// 部门树列表（与 secure.vue 一致，从接口获取）
const getDeptTreeUI = async(node: Node, resolve: (data: Tree[]) => void) => {
  const params: any = {
    accessToken: store.userInfo?.accessToken,
    app_id: AppConfig.events?.appID,
    group_type: activeGroupType.value,
    mod_type: 2
  }

  if (node.level === 0) {
    return resolve([{
      name: '全部',
      label: '全部',
      uid: '',
      id: '',
      puid: '',
      hadchild: 1
    }])
  } else {
    params.puid = node.data?.uid
  }

  try {
    const res: any = await event_group_list(params)
    if (res?.code === 200 && res?.data?.list) {
      const list = res.data.list || []
      const treeData: Tree[] = list.map((item: any) => ({
        name: item.name,
        label: item.name,
        uid: item.uid,
        id: item.uid,
        puid: item.puid,
        hadchild: item.hadchild || 0,
        dept_photo: item.dept_photo,
        children: [],
        config: item.config,
        autoDispatch: item.config?.auto_to_work ?? false
      }))
      resolve(treeData)

      if (node.level === 1 && list.length > 0) {
        setTimeout(() => {
          list.forEach((item: any) => {
            const childNode = leftTreeRef.value?.getNode(item.uid)
            if (childNode && item.hadchild > 0) {
              childNode.expand()
            }
          })
        }, 100)
      }
    } else {
      resolve([])
    }
  } catch (error) {
    resolve([])
  }
}

// 重新加载树数据
const reloadTree = () => {
  const rootDept = {
    name: '全部',
    label: '全部',
    uid: '',
    id: '',
    puid: '',
    hadchild: 1
  }
  treeData.value = [rootDept]
  defaultExpandedKeys.value = ['']
  nextTick(() => {
    setTimeout(() => {
      const rootNode = leftTreeRef.value?.getNode('')
      if (rootNode) {
        rootNode.expand()
      }
    }, 100)
  })
}

const settingDialogVisible = ref(false)
const currentProcessId = ref<string>('') // 当前配置的 process_id
const currentGroupType = ref<number>(1) // 当前分组类型：1-区域，2-分组
const currentAutoToWork = ref<boolean>(false) // 当前自动转工单状态
const currentNodeUid = ref<string>('')

const dialogContext = ref({
  regionName: '中国',
  nodeLabel: '目标追踪类AI主动安全事件'
})

const dialogInfoText = computed(() => {
  const { regionName, nodeLabel } = dialogContext.value
  return `AI主动安全告警事件触发后，系统自动派单。区域为 ${regionName} 的 ${nodeLabel}，自动派单设置为`
})

const handleTabClick = (tabName: string) => {
  leftTabActive.value = tabName
  reloadTree()
}

// 部门树选中
const handleTreeNodeClick = (data: any) => {
  currentTreeNodeId.value = data.id
  currentTreeNodeLabel.value = data.label
}

// 自动派单开关变化
const handleAutoDispatchChange = async(data: any) => {
  const nextVal = data.autoDispatch
  const params = {
    accessToken: store.userInfo?.accessToken,
    app_id: AppConfig.events?.appID,
    uid: data.uid || data.id,
    auto_to_work: nextVal
  }
  try {
    const res = await event_group_status(params) as any
    if (res?.code === 200) {
      ElMessage.success('自动派单状态更新成功')
    } else {
      data.autoDispatch = !nextVal
    }
  } catch (error) {
    data.autoDispatch = !nextVal
  }
}

// 设置图标点击
const handleSettingsClick = async(data: any) => {
  currentNodeUid.value = data.uid || data.id
  currentGroupType.value = activeGroupType.value
  dialogContext.value = {
    regionName: leftTabActive.value === 'region' ? '区域' : '分组',
    nodeLabel: data?.label || ''
  }
  currentProcessId.value = data?.config?.process_id || ''
  currentAutoToWork.value = data?.config?.auto_to_work ?? data?.autoDispatch ?? false
  settingDialogVisible.value = true
}

// 区域自动派单开关变化
const handleRegionAutoDispatchChange = async(val: boolean) => {
  try {
    const params = {
      accessToken: store.userInfo?.accessToken,
      group_type: 1,
      mod_type: 2,
      auto_to_work: val
    }
    const res = await workflowConfigSet(params) as any
    if (res?.code === 200) {
      ElMessage.success('区域自动派单设置保存成功')
    } else {
      regionAutoDispatch.value = !val
    }
  } catch (error) {
    regionAutoDispatch.value = !val
  }
}

// 分组自动派单开关变化
const handleGroupAutoDispatchChange = async(val: boolean) => {
  try {
    const params = {
      accessToken: store.userInfo?.accessToken,
      group_type: 2,
      mod_type: 2,
      auto_to_work: val
    }
    const res = await workflowConfigSet(params) as any
    if (res?.code === 200) {
      ElMessage.success('分组自动派单设置保存成功')
    } else {
      groupAutoDispatch.value = !val
    }
  } catch (error) {
    groupAutoDispatch.value = !val
  }
}

// 顶部设置图标点击
const handleHeaderSettingsClick = async() => {
  dialogContext.value = {
    regionName: leftTabActive.value === 'region' ? '区域' : '分组',
    nodeLabel: currentTreeNodeLabel.value || '目标追踪类AI主动安全事件'
  }

  // 根据当前标签页确定 group_type
  const groupType = leftTabActive.value === 'region' ? 1 : 2
  currentGroupType.value = groupType

  // 调用接口获取配置
  try {
    const params = {
      accessToken: store.userInfo?.accessToken,
      group_type: groupType,
      mod_type: 2
    }
    const res = await workflowConfigGet(params) as any
    if (res?.code === 200 && res?.data) {
      currentProcessId.value = res.data.process_id || ''
      currentAutoToWork.value = res.data.auto_to_work ?? false
    } else {
      currentProcessId.value = ''
      currentAutoToWork.value = false
    }
  } catch (error) {
    currentProcessId.value = ''
    currentAutoToWork.value = false
  }

  settingDialogVisible.value = true
}

const handleSettingConfirm = async(value: any) => {
  const { definitionId } = value
  const params = {
    accessToken: store.userInfo?.accessToken,
    uid: currentNodeUid.value,
    app_id: AppConfig.events?.appID,
    config: {
      app_id: AppConfig.events?.appID,
      auto_to_work: currentAutoToWork.value,
      process_id: definitionId
    }
  }
  const res = await event_group_setting_save(params) as any
  if (res?.code === 200) {
    ElMessage.success('工单配置保存成功')
    currentProcessId.value = definitionId
  }
}

// 获取区域和分组的自动派单配置
const fetchAutoDispatchConfig = async() => {
  // 获取区域配置 (group_type: 1)
  const regionParams = {
    accessToken: store.userInfo?.accessToken,
    group_type: 1,
    mod_type: 2
  }
  const regionRes = await workflowConfigGet(regionParams) as any
  if (regionRes?.code === 200 && regionRes?.data) {
    regionAutoDispatch.value = regionRes.data.auto_to_work ?? false
  }

  // 获取分组配置 (group_type: 2)
  const groupParams = {
    accessToken: store.userInfo?.accessToken,
    group_type: 2,
    mod_type: 2
  }
  const groupRes = await workflowConfigGet(groupParams) as any
  if (groupRes?.code === 200 && groupRes?.data) {
    groupAutoDispatch.value = groupRes.data.auto_to_work ?? false
  }
}

onMounted(() => {
  fetchAutoDispatchConfig()
  reloadTree()
})
</script>

<style lang="scss" scoped>
.tenant_Page {
  height: 100%;
  width: 100%;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0px 0px;
  background: #F7F7F7;

  .tenant_content {
    width: 100%;
    height: calc(100% - 66px);
    height: 100%;
    border-radius: var(--common-border-radius) var(--common-border-radius) 0px 0px;
    overflow: auto;
  }

  .tableTenBox {
    padding: 20px;
    width: 100%;
    height: 100%;
    border-radius: var(--common-border-radius) var(--common-border-radius) 0px 0px;
    flex: 1;
    background: #fff;
    gap: 20px;
  }
}

// 左侧菜单样式
.left_menu_panel {
  width: 460px;
  min-width: 360px;
  height: calc(100vh - 120px);
  max-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
}

.left-menu-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-shrink: 0;
  border-bottom: 1px solid #e4e7ed;
  height: 40px;
}

.custom-tabs {
  display: flex;
  align-items: center;
  flex: 1;
  height: 100%;

  .custom-tab-item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 5px;
    position: relative;
    height: 40px;
    line-height: 40px;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s;
    border-bottom: 2.5px solid transparent;

    &.active {
      color: var(--el-color-primary);
      border-bottom-color:var(--el-color-primary);
    }
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;

  .auto-dispatch-switch {
    display: flex;
    align-items: center;
    gap: 6px;

    :deep(.el-switch) {
      height: 20px;
      min-width: 64px;

      .el-switch__core {
        height: 20px;
        min-width: 64px;
      }
    }
  }

  .header-settings-icon {
    cursor: pointer;
    color: #606266;
    font-size: 16px;
    transition: color 0.3s;
    padding: 4px;

    &:hover {
      color: var(--el-color-primary);
    }
  }
}

.tree-container {
  flex: 1;
  overflow: auto;

  :deep(.el-tree-node__content) {
    --el-tree-node-hover-bg-color: var(--el-menu-hover-bg-color);
    height: 38px;
    font-size: 14px;
    color: #333;
  }

  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    background-color: #f5f5dc;
  }

  :deep(.el-tree-node__expand-icon) {
    color: #606266;
    font-size: 12px;
  }

  :deep(.el-tree-node__expand-icon.is-leaf) {
    color: transparent;
  }
}

.custom-tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;

  .tree-node-label {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #333;
    font-size: 14px;
    margin-left: 8px;

    &.activeDept {
      color: #333;
    }
  }

  .tree-node-actions {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
    margin-left: auto;

    .switch-wrapper {
      display: flex;
      align-items: center;
      gap: 6px;

      :deep(.el-switch) {
        height: 20px;
        min-width: 64px;

        .el-switch__core {
          height: 20px;
          min-width: 64px;
        }
      }
    }
  }
}

:deep(.tableTenBox) {
  align-items: flex-start;

  .header_tenant_cell {
    background: #F8F8F9;
  }
}
</style>
