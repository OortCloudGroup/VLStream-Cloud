<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="home">
    <div class="depNameBox_out flexRowAC">
      <div class="depNameBox flexRowAC">
        <div class="exportBtnBox flexRowAC">
          <div class="exportBtn newBtn flexRowAC" @click="newEditClick('')">
            <el-icon class="BtnImg">
              <Plus />
            </el-icon>
            新建
          </div>
        </div>
      </div>
      <!--搜索-->
      <div class="searchHeight_out flexRowAC">
        <search-height-box keyword="modelName" placeholder="流程名称" :data="searchData" @handle="searchResetFn" />
        <export-excel-pdf />
      </div>
    </div>
    <div class="start_content new_table">
      <div class="startBox flexRowAC">
        <div
          v-for="(item,i) in tableData"
          :key="i"
          class="start_item flexRowAC"
          :class="{act:isAct===i}"
          @click="startClick(item,i)"
        >
          <oort_img class="start_img" :src="item.iconId" alt="" />
          <div class="start_t">
            {{ item['modelName'] }}
          </div>
          <el-dropdown>
            <div class="new_table_svg_group " @click.stop>
              <oort-svg-icon width="20" height="20" name="table_more" class="new_table_svg_group_svg" />
              <span>更多</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <div class="new_table_svg_group" @click.stop="formClick(item,'1')">
                    <oort-svg-icon width="20" height="20" name="table_detail" class="new_table_svg_group_svg" />
                    <span>详情</span>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item>
                  <div class="new_table_svg_group" @click.stop="formClick(item,'1')">
                    <oort-svg-icon width="20" height="20" name="table_edit" class="new_table_svg_group_svg" />
                    <span>编辑</span>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item>
                  <div class="new_table_svg_group" @click.stop="formClick(item,'2')">
                    <oort-svg-icon width="20" height="20" name="table_design" class="new_table_svg_group_svg" />
                    <span>表单设计</span>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item>
                  <div class="new_table_svg_group" @click.stop="formClick(item, '3')">
                    <oort-svg-icon width="20" height="20" name="taskCenter_btn_design" class="new_table_svg_group_svg" />
                    <span>流程设计</span>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item>
                  <div class="new_table_svg_group" @click.stop="delClick(item)">
                    <oort-svg-icon width="20" height="20" name="table_del" class="new_table_svg_group_svg" />
                    <span>删除</span>
                  </div>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
    <el-dialog
      v-model="dVisi"
      :title="dItem?.modelId?'编辑模型':'新建模型'"
      width="46%"
      :close-on-click-modal="false"
    >
      <!--模型 0 新增 、1，copy ， 2 编辑-->
      <work-order-built v-if="dVisi" :is-type="1" :type="dItem?.modelId?2:0" :app="appObj" :item="dItem" @close="dVisi=false" @handle="getListFn" />
    </el-dialog>
    <el-dialog
      v-model="fVisi"
      :title="fVisi?.formId?'编辑表单':'新建表单'"
      width="46%"
      :close-on-click-modal="false"
    >
      <form-designer-built v-if="fVisi" :app="appObj" :item="dItem" @close="fVisi=false" @handle="getListFn" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'
import ExportExcelPdf from '@/components/exportExcelPdf.vue'
import { deleteModel, listModel } from '@/api/processui'
import Oort_img from '@/components/oort_img.vue'
import WorkOrderBuilt from '@/pages/task_center/views/page/components/workOrderBuilt.vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import FormDesignerBuilt from '@/pages/task_center/views/page/components/formDesignerBuilt.vue'
import { resolveWorkOrderAppContext } from '@/utils/workOrderAppContext'

const store: any = useUserStore()
const getAccessToken = () => store.userInfo?.accessToken || store.token || sessionStorage.getItem('token') || sessionStorage.getItem('accessToken') || localStorage.getItem('apaas_token') || localStorage.getItem('accessToken') || ''
const router = useRouter()
const props: any = defineProps(['act', 'app'])
const tableData = ref<any>([])
let appObj = ref<any>(props?.app)
const dVisi = ref<boolean>(false)// 新建工单
const fVisi = ref<boolean>(false)// 新建表单
let dItem = ref<any>('')// 详情 obj
let isAct = ref('')
const searchData = ref<any>([]) // 高级搜索框内容
searchData.value = [
  { label: '模型名称', value: 'modelName', type: 'text', default: '' },
  { label: '模型标识', value: 'modelKey', type: 'text', default: '' }
]
const queryParams = reactive({
  pageNum: 1,
  pageSize: 99,
  modelName: undefined,
  category: undefined,
  modelKey: undefined
})

// 搜索重置
function searchResetFn(val, reset) {
  if (reset) {
    queryParams.pageNum = 1
    queryParams.category = undefined
  }
  queryParams.modelName = val.modelName
  queryParams.modelKey = val.modelKey
  getListFn()
}

// 选中
const startClick = (item, index) => {
  isAct.value = index
  formClick(item, '1')
}

// 新建
function newEditClick(row) {
  if (!appObj.value?.appId) {
    ElMessage.warning('工单应用分类未初始化，暂不能新建模型')
    return
  }
  dVisi.value = true
  dItem.value = row || ''
}

// 表单
const formClick = (item, step) => {
  if (step === '2' && !item.formId) {
    fVisi.value = true
    appObj.value['modelId'] = item.modelId
    appObj.value['formName'] = item.modelName
    return
  }
  router.push({
    path: '/workOrderDetails',
    query: {
      // 通知节点：工单有,流程无(workOrderAppAll:true)
      workOrderAppAll: item.workOrderAppAll,
      modelId: item.modelId,
      formId: item.formId,
      step: step
    }
  })
}

// 删除
function delClick(row) {
  ElMessageBox.confirm('是否确定删除该模型?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async() => {
    const data = {
      modelIds: row.modelId
    }
    let res: any = await deleteModel(data)
    if (res.code === 200) {
      // 列表 刷新
      getListFn()
      ElMessage.success('模型删除成功')
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 模型列表
function getListFn() {
  if (!appObj.value?.appId) {
    tableData.value = []
    return
  }
  let data = {
    accessToken: getAccessToken(),
    ...queryParams,
    workOrderCategory: appObj.value?.appId
  }
  listModel(data).then((response: any) => {
    tableData.value = response.rows || []
    tableData.value.map((item: any) => {
      item['categoryName'] = appObj.value?.['applicationName'] || '--'
    })
  })
}

onMounted(async() => {
  appObj.value = props?.app?.appId ? props.app : await resolveWorkOrderAppContext()
  if (!appObj.value?.appId) {
    ElMessage.warning('未找到工单应用分类，请先初始化工单应用')
  }
  getListFn()
})
</script>
<style lang="scss" scoped>
.home {
  height: 100%;
  padding: 20px;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0px 0px;
  overflow: auto;
  background-color: var(--common-bg-color);

  .start_content {
    //height: 100%;
    background-color: var(--common-bg-color);
  }

  .startBox {
    gap: 16px;
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .start_item {
    cursor: pointer;
    padding: 12px;
    width: calc(20% - 16px);
    background: #FFFFFF;
    border-radius: var(--common-border-radius);
    border: 1px solid transparent;

    &.act, &:hover {
      background: #FFFFFF;
      border: 1px solid var(--el-color-primary);
      box-shadow: 0px 4px 10px 0px rgba(240, 90, 31, 0.08);
      border-radius: var(--common-border-radius);

      .el-dropdown {
        visibility: inherit;
      }
    }

    .el-dropdown {
      visibility: hidden;
    }
  }

  .start_img {
    flex-shrink: 0;
    width: 40px;
    height: 40px;
    margin-right: 8px;
    &.h{
      width: 1px;
    }
  }

  .start_t {
    flex: 1;
    white-space: nowrap;
    text-overflow: ellipsis;
    overflow: hidden;
    font-size: 14px;
    color: #333333;
    line-height: 22px;
    font-weight: 400;
  }
}

// 紧急程度
:deep(.priority) {
  .el-select__selected-item > span {
    color: var(--el-color-primary);
    position: relative;
    padding-left: 12px;

    &:before {
      content: "";
      display: flex;
      position: absolute;
      top: 4px;
      left: 0px;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--el-color-primary);
    }
  }
}

// 新增
.exportBtnBox {
  gap: 12px;
  padding: 0;

  // 新增
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
  }
}
</style>
