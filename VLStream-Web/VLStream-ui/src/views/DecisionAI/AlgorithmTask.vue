<template>
  <div class="algorithm-task tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <div class="tableTenItU">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <div class="exportBtnBox flexRowAC">
                <button type="button" class="exportBtn newBtn flexRowAC" @click="handleAdd">
                  <el-icon class="BtnImg">
                    <Plus />
                  </el-icon>
                  新增
                </button>
                <button-group :button-list="toolbarButtonList" />
              </div>
            </div>
            <!--搜索-->
            <div class="searchHeight_out flexRowAC">
              <search-height-box
                keyword="jobName"
                placeholder="任务名称"
                :data="searchData"
                @handle="searchResetFn"
              />
              <export-excel-pdf :item="exportItem" @handle="handleExport" />
            </div>
          </div>

          <avue-crud
            :option="option"
            v-model:search="search"
            v-model:page="page"
            v-model="form"
            :table-loading="loading"
            :data="data"
            :before-open="beforeOpen"
            ref="crud"
            @row-update="rowUpdate"
            @row-save="rowSave"
            @row-del="rowDel"
            @search-change="searchChange"
            @search-reset="searchReset"
            @selection-change="selectionChange"
            @current-change="currentChange"
            @size-change="sizeChange"
            @refresh-change="refreshChange"
            @on-load="onLoad"
          >
            <template #menu="scope">
              <el-button
                type="primary"
                text
                icon="el-icon-video-play"
                @click="handleRun(scope.row)"
              >
                运 行
              </el-button>
            </template>
            <template #enable="{ row }">
              <el-switch
                v-model="row.enable"
                inline-prompt
                @change="slotChange(row)"
                active-text="启用"
                inactive-text="暂停"
                :active-value="1"
                :inactive-value="0"
              />
            </template>
          </avue-crud>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import option from '@/option/job/jobinfo'
import 'nprogress/nprogress.css'
import func from '@/utils/func'
import { Plus } from '@element-plus/icons-vue'

export default {
  components: {
    Plus
  },
  data() {
    return {
      form: {},
      query: {},
      search: {},
      loading: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0
      },
      selectionList: [],
      option: {
        ...option,
        searchShow: false,
        searchShowBtn: false,
        searchBtn: false,
        addBtn: false
      },
      data: [],
      exportItem: {
        isDisabledExcel: false
      },
      searchData: [
        {
          label: '任务应用',
          value: 'jobServerId',
          type: 'select',
          default: '',
          option: []
        },
        { label: '任务ID', value: 'jobId', type: 'text', default: '' },
        { label: '任务名称', value: 'jobName', type: 'text', default: '' }
      ]
    }
  },
  computed: {
    ids() {
      return this.selectionList.map((ele) => ele.id).join(',')
    },
    toolbarButtonList() {
      return [
        { name: '删除', svg: 'table_del', clickFn: this.handleDelete },
        { name: '数据同步', svg: 'allocation', clickFn: this.handleSync }
      ]
    }
  },
  methods: {
    // 当前页面暂时禁用接口交互，只保留本地空态和操作提示。
    disableRemoteCall(message = '当前页面接口调用已暂时关闭') {
      this.$message.info(message)
    },
    handleAdd() {
      const crud = this.$refs.crud
      if (crud && typeof crud.rowAdd === 'function') {
        crud.rowAdd()
        return
      }
      this.$message.info('暂无法打开新增弹窗')
    },
    handleExport() {
      this.$message.success('导出数据')
    },
    searchResetFn(val, reset) {
      if (reset && !(val && (val.jobName || val.jobId || val.jobServerId))) {
        this.searchReset()
        return
      }
      this.query = { ...(val || {}) }
      this.page.currentPage = 1
      this.onLoad(this.page, this.query)
    },
    rowSave(row, done) {
      if (func.isArrayAndNotEmpty(row.lifecycle)) {
        const lifecycleStart = row.lifecycle[0]
        const lifecycleEnd = row.lifecycle[1]
        if (!func.isUndefined(lifecycleStart) && !func.isUndefined(lifecycleEnd)) {
          row.lifecycle = lifecycleStart + ',' + lifecycleEnd
        }
      } else {
        row.lifecycle = ''
      }
      this.disableRemoteCall()
      done()
    },
    rowUpdate(row, index, done) {
      if (func.isArrayAndNotEmpty(row.lifecycle)) {
        const lifecycleStart = row.lifecycle[0]
        const lifecycleEnd = row.lifecycle[1]
        if (!func.isUndefined(lifecycleStart) && !func.isUndefined(lifecycleEnd)) {
          row.lifecycle = lifecycleStart + ',' + lifecycleEnd
        }
      } else {
        row.lifecycle = ''
      }
      this.disableRemoteCall()
      done()
    },
    rowDel() {
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.disableRemoteCall()
      })
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据')
        return
      }
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.disableRemoteCall()
        this.clearCrudSelection()
      })
    },
    handleSync() {
      this.$confirm('确定进行数据双向同步?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.disableRemoteCall('数据同步接口调用已暂时关闭')
      })
    },
    handleRun() {
      this.$confirm('运行后将创建一个实例执行，是否继续?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.disableRemoteCall('运行接口调用已暂时关闭')
      })
    },
    slotChange(row) {
      if (!row.id) {
        return
      }
      this.disableRemoteCall('启停接口调用已暂时关闭')
    },
    beforeOpen(done) {
      done()
    },
    searchReset() {
      this.query = {}
      this.onLoad(this.page)
    },
    searchChange(params, done) {
      this.query = params
      this.page.currentPage = 1
      this.onLoad(this.page, params)
      done()
    },
    selectionChange(list) {
      this.selectionList = list
    },
    clearCrudSelection() {
      const crud = this.$refs.crud
      if (crud && typeof crud.toggleSelection === 'function') {
        crud.toggleSelection()
      }
    },
    selectionClear() {
      this.selectionList = []
      this.clearCrudSelection()
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize
    },
    refreshChange() {
      this.onLoad(this.page, this.query)
    },
    onLoad() {
      this.loading = true
      this.page.total = 0
      this.data = []
      this.loading = false
      this.selectionClear()
    }
  }
}
</script>

<style scoped lang="scss">
.tenant_Page {
  height: 100%;
  width: 100%;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  background: #f0f2f5;

  .tenant_content {
    width: 100%;
    height: 100%;
    border-radius: 8px;
  }

  .tableTenBox {
    padding: 20px;
    width: 100%;
    height: 100%;
    border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
    flex: 1;
    background: #fff;
    align-items: flex-start;
  }
}

.tableTenItU {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: auto;
}

.algorithm-task {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  overflow: hidden;

  /* 隐藏 avue-crud 自带搜索区域与搜索切换按钮 */
  :deep(.avue-crud__search) {
    display: none !important;
  }
}
</style>
