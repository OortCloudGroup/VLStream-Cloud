<template>
  <basic-container>
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
      <template #menu-left>
        <el-button
            type="danger"
            icon="el-icon-delete"
            plain
            @click="handleDelete"
        >删 除
        </el-button>
        <el-button type="info" plain icon="el-icon-sort" @click="handleSync"
        >数 据 同 步
        </el-button>
      </template>
      <template #menu="scope">
        <el-button type="primary" text icon="el-icon-video-play" @click="handleRun(scope.row)"
        >运 行
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
  </basic-container>
</template>

<script>
import option from '@/option/job/jobinfo';
import 'nprogress/nprogress.css';
import func from '@/utils/func';

export default {
  data() {
    return {
      form: {},
      query: {},
      search: {},
      loading: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      option: option,
      data: [],
    };
  },
  computed: {
    ids() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
  },
  methods: {
    // 当前页面暂时禁用接口交互，只保留本地空态和操作提示。
    disableRemoteCall(message = '当前页面接口调用已暂时关闭') {
      this.$message.info(message);
    },
    rowSave(row, done, loading) {
      if (func.isArrayAndNotEmpty(row.lifecycle)) {
        const lifecycleStart = row.lifecycle[0];
        const lifecycleEnd = row.lifecycle[1];
        if (!func.isUndefined(lifecycleStart) && !func.isUndefined(lifecycleEnd)) {
          row.lifecycle = lifecycleStart + ',' + lifecycleEnd;
        }
      } else {
        row.lifecycle = '';
      }
      this.disableRemoteCall();
      done();
    },
    rowUpdate(row, index, done, loading) {
      if (func.isArrayAndNotEmpty(row.lifecycle)) {
        const lifecycleStart = row.lifecycle[0];
        const lifecycleEnd = row.lifecycle[1];
        if (!func.isUndefined(lifecycleStart) && !func.isUndefined(lifecycleEnd)) {
          row.lifecycle = lifecycleStart + ',' + lifecycleEnd;
        }
      } else {
        row.lifecycle = '';
      }
      this.disableRemoteCall();
      done();
    },
    rowDel(row) {
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
          .then(() => {
            this.disableRemoteCall();
          });
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
          .then(() => {
            this.disableRemoteCall();
            this.clearCrudSelection();
          });
    },
    handleSync() {
      this.$confirm('确定进行数据双向同步?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
          .then(() => {
            this.disableRemoteCall('数据同步接口调用已暂时关闭');
          });
    },
    handleRun(row) {
      this.$confirm('运行后将创建一个实例执行，是否继续?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
          .then(() => {
            this.disableRemoteCall('运行接口调用已暂时关闭');
          });
    },
    slotChange(row) {
      if (!row.id) {
        return;
      }
      this.disableRemoteCall('启停接口调用已暂时关闭');
    },
    beforeOpen(done, type) {
      done();
    },
    searchReset() {
      this.query = {};
      this.onLoad(this.page);
    },
    searchChange(params, done) {
      this.query = params;
      this.page.currentPage = 1;
      this.onLoad(this.page, params);
      done();
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    clearCrudSelection() {
      const crud = this.$refs.crud;
      if (crud && typeof crud.toggleSelection === 'function') {
        crud.toggleSelection();
      }
    },
    selectionClear() {
      this.selectionList = [];
      this.clearCrudSelection();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
    },
    refreshChange() {
      this.onLoad(this.page, this.query);
    },
    onLoad(page) {
      this.loading = true;
      this.page.total = 0;
      this.data = [];
      this.loading = false;
      this.selectionClear();
    },
  },
};
</script>

<style></style>
