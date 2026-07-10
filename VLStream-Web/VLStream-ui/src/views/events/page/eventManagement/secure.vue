<!-- eslint-disable no-unused-vars -->

<template>
  <div class="tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <!-- 左侧菜单 -->
        <div class="left_menu_panel">
          <el-tabs v-model="leftTabActive" class="left-tabs" @tab-change="handleLeftTabChange">
            <el-tab-pane label="区域" name="region" />
            <el-tab-pane label="分组" name="group" />
            <el-tab-pane label="标签" name="tag" />
          </el-tabs>
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
                  <oort-img :src="data?.dept_photo" style="width: 1rem;height:1rem;margin: 0 0.25rem;border-radius: 50%" default-type="dept" />
                  <el-tooltip :open-delay="500" class="item" effect="light" :content="node.label" placement="top">
                    <div :class="{'delete_text': data.oort_status === 9, 'activeDept': data.uid === currentTreeNodeId}">
                      {{ node.label }}
                    </div>
                  </el-tooltip>
                </div>
              </template>
            </el-tree>
          </div>
        </div>
        <!--table-->
        <div class="tableTenIt">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <div class="exportBtnBox flexRowAC">
                <div v-if="editShow" class="exportBtn newBtn flexRowAC">
                  <el-icon class="BtnImg">
                    <Plus />
                  </el-icon>新增事件
                </div>
                <div v-if="showMoreDelete" class="exportBtn newBtn flexRowAC">
                  <el-icon class="BtnImg">
                    <UploadFilled />
                  </el-icon>导出
                </div>
                <div class="oort_button_group">
                  <div v-if="editShow" class="oort_button_group_item">
                    <el-icon>
                      <Edit />
                    </el-icon>
                    <span>编辑</span>
                  </div>
                  <div v-if="editShow" class="oort_button_group_line" />
                  <!--                  <div class="oort_button_group_item" @click="deleteTask">-->
                  <!--                    <el-icon>-->
                  <!--                      <Delete />-->
                  <!--                    </el-icon>-->
                  <!--                    <span>删除</span>-->
                  <!--                  </div>-->
                  <button-group
                    :button-list="[
                      {svg: 'table_del', name: '删除' ,clickFn: deleteTask},
                      {svg: 'table_incomingRecord', name: itemTemp||'事件类型' ,clickFn: '',children:eventItemList},
                    ]"
                  />
                </div>
              </div>
            </div>
            <div class="tabs-btn" style="position: absolute;right:25%;">
              <el-radio-group v-model="tabActive" size="default" @change="tabsAChange">
                <el-radio-button value="0">
                  全部
                </el-radio-button>
                <el-radio-button value="2">
                  正在处理
                </el-radio-button>
                <el-radio-button value="1">
                  已完成
                </el-radio-button>
              </el-radio-group>
            </div>
            <div class="searchHeight_out flexRowAC">
              <search-height-box keyword="keyword" placeholder="搜索" :data="searchData" @handle="searchResetFn" />
              <export-excel-pdf />
            </div>
          </div>
          <TableSelf
            class="new_table"
            header-cell-class-name="header_tenant_cell"
            stripe
            :row-class-name="tableRowClassName"
            :data="tableData"
            current-row-key="user_id"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" :width="clacPXToVW(55)" />
            <el-table-column label="序号" :width="clacPXToVW(60)">
              <template #default="scope">
                {{ scope.$index + (page - 1) * pagesize + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="completionTime" label="事件时间" :width="clacPXToVW(160)">
              <template #default="scope">
                {{ scope.row.created_at }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" :width="clacPXToVW(120)">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)">
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="position" label="事件位置" :width="clacPXToVW(160)">
              <template #default="scope">
                {{ scope.row.point?.address }}
              </template>
            </el-table-column>
            <el-table-column prop="item" label="事件类型" :width="clacPXToVW(120)" :show-overflow-tooltip="true" />
            <el-table-column prop="pics" label="抓拍照片" :width="clacPXToVW(120)">
              <template #default="scope">
                <!-- <div v-if="scope.row.pics.length > 0">
                  <img :src="scope.row.pics[0]" alt="抓拍照片" class="eventImg" />
                  <span class="eventImgText">+{{ scope.row.pics.length }}</span>
                </div> -->
                <div v-if="scope.row.pics.length > 0">
                  <el-image
                    :src="covertCurrentLocationURL(scope.row.pics[0])"
                    :preview-src-list="scope.row.pics.map((pic: any) => covertCurrentLocationURL(pic))"
                    :initial-index="0"
                    class="eventImg"
                    fit="cover"
                    :preview-teleported="true"
                  />
                  <span class="eventImgText">+{{ scope.row.pics.length }}</span>
                </div>
                <span v-else>暂无图片</span>
              </template>
            </el-table-column>
            <el-table-column prop="fileUrl" label="录制视频" :width="clacPXToVW(120)">
              <template #default="scope">
                <div v-if="scope.row.videoUrl" class="video-container" @click="openVideoDialog(scope.row)">
                </div>
                <div v-else class="no-video">
                  <span>暂无视频</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="device_name" label="设备名称" :width="clacPXToVW(160)" :show-overflow-tooltip="true" />
            <el-table-column prop="device_id" label="设备编号" />
            <el-table-column prop="describe" label="事件描述" :width="clacPXToVW(120)" :show-overflow-tooltip="true" />
            <el-table-column prop="device_tag" label="标签" :width="clacPXToVW(160)" :show-overflow-tooltip="true" />
            <el-table-column v-if="false" prop="status" label="告警状态" :width="clacPXToVW(140)">
              <template #default="scope">
                <el-tag :type="getStatusType2(scope.row.mod_status)">
                  {{ getStatusText2(scope.row.mod_status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="finish_at" label="完成时间" :width="clacPXToVW(160)" />
            <el-table-column fixed="right" align="right" label="操作" :width="clacPXToVW(180)">
              <template #default="scope">
                <div class="operateAppBox flexRowAC">
                  <div v-if="scope.row.status !== 1 && scope.row.mod_status === 0&&false" class="new_table_svg_group" @click="confirmAlert(scope.row, 'confirm')">
                    <oort-svg-icon
                      width="20"
                      height="20"
                      name="enable"
                      class="new_table_svg_group_svg"
                    />
                    <span>确认事件</span>
                  </div>
                  <template v-else>
                    <div class="new_table_svg_group" @click="phoneFBackFn(scope.row)">
                      <oort-svg-icon width="20" height="20" name="table_incoming" class="new_table_svg_group_svg" />
                      <span>反馈</span>
                    </div>
                    <div class="new_table_svg_group" @click="confirmAlert(scope.row, 'ignore')">
                      <oort-svg-icon
                        width="20"
                        height="20"
                        name="table_detail"
                        class="new_table_svg_group_svg"
                      />
                    </div>
                  </template>
                  <div v-if="scope.row.status === 2&&false" class="new_table_svg_group" @click="taskAssignment(scope.row)">
                    <oort-svg-icon
                      width="20"
                      height="20"
                      name="allocation"
                      class="new_table_svg_group_svg"
                    />
                    <span>任务分配</span>
                  </div>
                </div>
              </template>
            </el-table-column>
          </TableSelf>
          <div class="paginationBox flexRowAC">
            <el-pagination
              background
              :page-sizes="[10, 20, 50, 100]"
              :page-size="pagesize"
              layout="total, prev, pager, next, sizes"
              :total="count"
              class="justifyAlign"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </div>
    </div>
    <!-- 确认事件/忽略事件 弹框-->
    <!-- <ConfirmAlertDialog
      v-model:visible="confirmAlertVis"
      :data="currentItem"
      :dialog-type="dialogType"
      @success="sucessFun"
      @close="confirmAlertVis = false"
    /> -->
    <ConfirmEventDialog v-model:visible="confirmAlertVis" :data="currentItem" @refresh="getList" />
    <!-- 事件反馈 弹框 -->
    <event-phone-f-back v-model:visible="eventDetailsFBVis" :data="currentItem" @close="eventDetailsFBVis = false" />
    <!-- 详情 弹框 -->
    <event-details-dialog v-model="eventDetailsVis" :data="currentItem" @close="eventDetailsVis = false" />
    <!-- 确认事件 弹框 -->
    <event-phone-confirm v-model:visible="eventDetailsCoVis" :data="currentItem" @close="eventDetailsCoVis = false" @refresh="getList" />
    <!-- 视频播放弹窗 -->
    <el-dialog
      v-model="videoDialogVisible"
      title="视频播放"
      width="80%"
      :before-close="closeVideoDialog"
    >
      <div class="video-dialog-container">
        <OortVideoPlayer v-if="currentVideoUrl" :video="{ url: currentVideoUrl }" />
      </div>
    </el-dialog>
    <!-- 任务分配弹框 -->
    <SchedulingDialog
      v-model:visible="schedulingVisible"
      :current-item="currentItem"
      @refresh="getList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import type Node from 'element-plus/es/components/tree/src/model/node'
// import { Delete } from '@element-plus/icons-vue'
import SchedulingDialog from './components/schedulingDialog.vue'
import ConfirmEventDialog from './components/confirmEventDialog/index.vue'
import eventDetailsDialog from './components/eventDetailsDialog/index.vue'
import OortVideoPlayer from '@/components/oortVideoPlayer.vue'
import { delEvent, eventList, event_item_list, event_group_list } from '@/api/smartCity/events'
import { clacPXToVW } from '@/utils/index'
import { useUserStore } from '@/store/modules/useraPaas'
import EventPhoneFBack from '@/pages/events/views/page/eventManagement/eventPhoneFBack.vue'
import EventPhoneConfirm from '@/pages/events/views/page/eventManagement/eventPhoneConfirm.vue'
import AppConfig from '@/config/AppConfig'
import { covertCurrentLocationURL } from '@/utils/converGatewayPath'
import dayjs from 'dayjs'

const store = useUserStore()
const getAccessToken = () => store.userInfo?.accessToken || store.token || sessionStorage.getItem('token') || sessionStorage.getItem('accessToken') || localStorage.getItem('apaas_token') || localStorage.getItem('accessToken') || ''
const editShow = ref(false)
const showMoreDelete = ref(false)
const eventDetailsFBVis = ref(false) // 事件反馈
const eventDetailsCoVis = ref(false) // 确认事件
const tabActive = ref('0')
const currentItem = ref<any>(null)
const eventDetailsVis = ref(false)
const videoDialogVisible = ref(false)
const currentVideoUrl = ref<string>('')
const selectedRows = ref<any[]>([])
const eventItemList = ref<any[]>([]) // 事件类型过滤
const itemTemp = ref<any>('') // 事件类型过滤 传值
const tableRowClassName = ({ row }) => {
  if (row.status === 2) {
    return 'blueFont'
  } else {
    return ''
  }
}
const tabsAChange = () => {
  params.status = Number(tabActive.value)
  params.page = 1
  getList()
}

const getStatusType = (status: string | number) => {
  const statusMap = {
    '0': 'warning',
    // '1': 'success',
    '1': 'plain1',
    '2': 'primary'
  }
  return statusMap[status] || 'info'
}
const schedulingVisible = ref(false)

const taskAssignment = (data) => {
  currentItem.value = data
  schedulingVisible.value = true
}

const getStatusText = (status) => {
  const statusTextMap = {
    '1': '已完成',
    '2': '正在处理'
  }
  return statusTextMap[String(status)] || '未知状态'
}
const getStatusType2 = (status: string | number) => {
  const statusMap = {
    '0': 'warning',
    '1': 'success',
    '2': 'primary'
  }
  return statusMap[status] || 'info'
}

const getStatusText2 = (status: string | number) => {
  const statusTextMap = {
    '0': '待确认',
    '1': '真实告警',
    '2': '维保',
    '3': '误报'
  }
  return statusTextMap[String(status)] || '未知状态'
}

const confirmAlertVis = ref(false)
const dialogType = ref('confirm')

const count = ref<any>(0)
const page = ref<number>(1)
const pagesize = ref<number>(10)
const tableData = ref<any[]>([])
// const sucessFun = () => {
// }
const params = reactive({
  accessToken: store.token,
  keyword: '',
  page: 1,
  pagesize: 10,
  status: 0, // 1:已完成 2:正在处理 0:全部
  mod_type: 2 // 主动安全
})
const getList = async() => {
  try {
    let data = {
      ...params,
      item: itemTemp.value,
      group_uid: currentTreeNodeId.value
    }
    if (datePickerTime.value && datePickerTime.value?.length !== 0) {
      data['start_at'] = dayjs(datePickerTime.value[0]).format('YYYY-MM-DD HH:mm:ss')
      data['end_at'] = dayjs(datePickerTime.value[1]).format('YYYY-MM-DD 23:59:59')
    }
    const res:any = await eventList(data)
    if (res.code === 200) {
      tableData.value = res.data.list
      count.value = res.data.count
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取数据失败')
  }
}
const handleSizeChange = (val: number) => {
  pagesize.value = val
  params.pagesize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  page.value = val
  params.page = val
  getList()
}

const _editTask = (data) => {
  console.log(data)
}

// 处理表格选择变化
const handleSelectionChange = (selection: any[]) => {
  selectedRows.value = selection
}

const deleteTask = async() => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的事件')
    return
  }

  // 确认删除
  await ElMessageBox.confirm(
    `确定要删除选中的 ${selectedRows.value.length} 个事件吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  // 批量删除
  const deletePromises = selectedRows.value.map(row => {
    const params = {
      accessToken: store.token,
      id: row.id
    }
    return delEvent(params)
  })

  await Promise.all(deletePromises)

  ElMessage.success('删除成功')
  // 重新获取列表
  getList()
}

// 事件反馈
const phoneFBackFn = (data) => {
  eventDetailsFBVis.value = true
  currentItem.value = data
}

const confirmAlert = (data, type = 'confirm') => {
  currentItem.value = data
  dialogType.value = type
  if (type === 'confirm') {
    eventDetailsCoVis.value = true
  } else {
    confirmAlertVis.value = true
  }
}

const openVideoDialog = (data) => {
  if (data.videoUrl) {
    currentVideoUrl.value = data.videoUrl
    videoDialogVisible.value = true
  }
}

const closeVideoDialog = () => {
  videoDialogVisible.value = false
  currentVideoUrl.value = ''
}

// 左侧菜单相关
const leftTabActive = ref('region')
const leftTreeRef = ref<InstanceType<typeof ElTree>>()
const treeData = ref<any>([]) // 部门树date
const currentTreeNodeId = ref<any>('') // 当前节点id
// 配置选项
const defaultProps = {
  label: 'name',
  children: 'children',
  isLeaf: (data: any) => {
    return !data.is_subtenant && !data.children
  }
}
const defaultExpandedKeys = ref<string[]>([''])

interface Tree {
  name: string
  uid: string
  puid: string
  label?: string
  id?: string
  hadchild?: number
  children?: Tree[]
  dept_photo?: string
}

const activeGroupType = computed(() => (leftTabActive.value === 'tag' ? 3 : leftTabActive.value === 'group' ? 2 : 1))

// 部门树列表
const getDeptTreeUI = async(node: Node, resolve: (data: Tree[]) => void) => {
  const params: any = {
    accessToken: store.userInfo?.accessToken,
    app_id: AppConfig.events?.appID,
    group_type: activeGroupType.value, // 区域为1，分组为2
    mod_type: 2 // 主动安全
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
      // 转换数据格式，将 name 映射为 label，uid 映射为 id
      const treeData: Tree[] = list.map((item: any) => ({
        name: item.name,
        label: item.name,
        uid: item.uid,
        id: item.uid,
        puid: item.puid,
        hadchild: item.hadchild || 0,
        dept_photo: item.dept_photo,
        children: []
      }))
      resolve(treeData)

      // 如果是第一级节点,展开第二级
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
    // 获取树形数据失败
    resolve([])
  }
}

// 搜索相关
const datePickerTime = ref([])
const searchData = ref<any>([])
searchData.value = [
  { label: '设备名称', value: 'keyword', type: 'text', default: '' },
  { label: '时间', value: 'datePickerTime', type: 'daterange', default: [] }
]
const searchResetFn = (val: any, reset) => {
  if (reset) {
    params.page = 1
  }
  params.keyword = val?.keyword || ''
  datePickerTime.value = val.datePickerTime
  getList()
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
  // 展开根节点,触发懒加载
  nextTick(() => {
    setTimeout(() => {
      const rootNode = leftTreeRef.value?.getNode('')
      if (rootNode) {
        rootNode.expand()
      }
    }, 100)
  })
}

const handleLeftTabChange = (_tabName: string) => {
  itemTemp.value = ''
  // 切换标签时重新加载树数据
  reloadTree()
  getList()
}

// 部门树选中
const handleTreeNodeClick = (data: any) => {
  currentTreeNodeId.value = data.uid || data.id
  getList()
}

// 事件类型过滤-刷新
const itemFn = (tt) => {
  itemTemp.value = tt.item
  getList()
}

// 事件类型过滤
const event_item_listFn = async() => {
  let data = {
    accessToken: getAccessToken(),
    mod_type: 2 // 事件类型 1:事件拍传 2:主动安全
  }
  let res: any = await event_item_list(data)
  if (res.code === 200) {
    let list = res.data?.list?.map(tt => ({
      ...tt,
      name: `${tt.item}`,
      svg: 'table_incomingRecord',
      clickFn: () => itemFn(tt)
    }))
    eventItemList.value = list || []
  }
}

onMounted(() => {
  event_item_listFn() // 事件类型过滤-list
  getList()
  // 初始化树数据
  reloadTree()
})
</script>
<style>
.blueFont .el-table__cell .cell{
  color: blue !important;
}

</style>
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
  width: 280px;
  min-width: 280px;
  height: calc(100vh - 120px);
  max-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
  border-right: 1px solid #E8E8E8;
}

.left-tabs {
  margin-bottom: 16px;
  flex-shrink: 0;

  :deep(.el-tabs__header) {
    margin: 0;
    border-bottom: 1px solid #e4e7ed;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__nav) {
    display: flex;
    width: 100%;
  }

  :deep(.el-tabs__item) {
    flex: 1;
    text-align: center;
    padding: 0;
    height: 40px;
    line-height: 40px;
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

  // 树选中
  :deep(.el-tree-node) {
    .el-tree-node.is-current.is-focusable > .el-tree-node__content {
      background-color: var(--el-color-primary-hb);
      color: var(--el-color-primary);
    }
  }
}

.custom-tree-node {
  display: flex;
  align-items: center;
  width: 100%;
}

.tableTenIt {
  flex: 1;
  min-width: 0;
}

:deep(.depNameBox_out) {
  padding: 0 20px;

  .el-tabs__item {
    color: #999999;
  }

  .el-tabs__item.is-active {
    color: var(--el-color-primary);
  }

  .el-radio-group {
    border-radius: 40px !important;
    background-color: #fafafa;

    .el-radio-button {
      border-radius: 40px !important;
      overflow: hidden;
      background-color: #fafafa;
      border: none;
    }

    .el-radio-button__inner {
      background-color: #fafafa;
      border: none;
    }

    .is-active {
      .el-radio-button__inner {
        background-color: var(--el-color-primary);
      }
    }
  }
}

.tabs-btn {
  position: absolute;
  right: 50%;
}

:deep(.tableTenBox) {
  align-items: flex-start;

  .header_tenant_cell {
    background: #F8F8F9;
  }
}

.exportBtnBox {
  gap: 12px;
}

.exportBtn {
  cursor: pointer;
  justify-content: center;
  padding: 0 12px;
  height: 32px;
  background: var(--el-color-primary);
  border-radius: 5px;
  border: 1px solid var(--el-color-primary);
  font-size: 14px;
  color: #FFFFFF;
  box-shadow: 0px 2px 0px 0px rgba(0, 0, 0, 0.04);

  .BtnImg {
    width: 14px;
    height: 14px;
    margin-right: 8px;
    background-size: cover;
  }

  &.Btn2 {
    background: #FFFFFF;
    color: #333333;
    border: 1px solid rgba(217, 217, 217, 1);
  }

  .line {
    width: 1px;
    height: 14px;
    margin-right: 8px;
    background-color: var(--el-border-color);
  }
}

.paginationBox {
  justify-content: right;
  height: 100px;
}

.operateAppBox {
  justify-content: flex-end;
  gap: 2px;

  .appBtn {
    line-height: 22px;
    cursor: pointer;
    padding: 6px 12px;
    border: 0.5px solid rgba(220, 223, 230, 1);
    border-radius: 2px;
  }
}

.depNameBox {
  font-size: 16px;
  color: #333333;
  font-weight: 700;
  gap: 8px;

  >span {
    font-size: 12px;
    color: #939393;
    font-weight: 400;
  }

  >.depNameImg {
    width: 24px;
    height: 24px;
    margin: 0 0.25rem;
    border-radius: 50%
  }

  &_out {
    justify-content: space-between;
    padding-bottom: 10px;
  }

  &.column {
    flex-direction: column;
    align-items: flex-start;
    gap: 0;
  }
}

.searchHeight_out {
  gap: 20px;
}

.oort_button_group {
  border-radius: 20px;
  background: var(--el-menu-hover-bg-color);
  height: 36px;
  display: flex;
  align-items: center;

  &_item {
    padding: 0 12px;
    height: 100%;
    cursor: pointer;
    display: flex;
    align-items: center;
    flex-direction: row;

    span {
      color: var(--el-color-primary);
      font-size: 14px;
      margin: 0 4px;
      font-weight: 500;
    }
  }

  &_item:first-child:hover {
    background: var(--el-color-primary-hb);
    border-top-left-radius: 20px;
    border-bottom-left-radius: 20px;
  }

  &_item:hover {
    background: var(--el-color-primary-hb);
  }

  &_item:last-child:hover {
    background: var(--el-color-primary-hb);
    border-top-right-radius: 20px;
    border-bottom-right-radius: 20px;
  }

  &_line {
    width: 1px;
    height: 16px;
    background: var(--el-color-primary-hb2);
  }
}

.delete_row {
  text-decoration: line-through;
  color: #999;
}

.new_table_svg_group {
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;

  &:hover {
    background-color: var(--el-color-primary-hb);
  }

  .new_table_svg_group_svg {
    fill: var(--el-color-primary) !important;

    svg {
      fill: var(--el-color-primary) !important;
    }

    use {
      fill: var(--el-color-primary) !important;
    }

    path {
      fill: var(--el-color-primary) !important;
    }

    * {
      fill: var(--el-color-primary) !important;
    }
  }
}
.oort_button_group_item{
  color: var(--el-color-primary);
}

.eventImg {
  width: 48px;
  height: 48px;
  border-radius: 4px;
}

.eventImgText {
  /* 点击穿透 */
  pointer-events: none;
  position: absolute;
  top: 20px;
  left: 28px;
  font-family: PingFangSC-Regular;
  font-size: 14px;
  color: #FFFFFF;
  font-weight: 400;
}
.eventImg {
  width: 48px;
  height: 48px;
  border-radius: 4px;
}

.video-container {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  overflow: hidden;
  background: #0a0a0a;
  position: relative;
  cursor: pointer;

  :deep(.xgplayer) {
    width: 100% !important;
    height: 100% !important;
  }

  :deep(.xgplayer-video) {
    width: 100% !important;
    height: 100% !important;
    object-fit: cover;
  }

  :deep(.xgplayer-controls) {
    display: none; // 隐藏控制条以节省空间
  }

  .video-play-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.3);
    opacity: 0;
    transition: opacity 0.3s ease;

    .play-icon {
      color: white;
      font-size: 16px;
    }
  }

  &:hover .video-play-overlay {
    opacity: 1;
  }
}

.no-video {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: #f5f5f5;
  border-radius: 4px;
  color: #999;
  font-size: 10px;
}

.video-dialog-container {
  width: 100%;
  height: 800px;
  border-radius: var(--common-border-radius);
  overflow: hidden;
  background: #000;

  :deep(.xgplayer) {
    width: 100% !important;
    height: 100% !important;
  }

  :deep(.xgplayer-video) {
    width: 100% !important;
    height: 100% !important;
  }
}
.el-table :deep(.xgplayer .xgplayer-start) {
  width: 30px !important;
  height: 30px !important;
}

</style>
<!-- <style>
.el-dialog {
  border-radius: 20px !important;
}

.el-dialog__header.show-close {
  border-bottom: 1.5px solid #e5e7eb !important;
  margin-bottom: 15px !important;
}
</style> -->
