<template>
  <div class="tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox">
        <div class="depNameBox_out flexRowAC">
          <div class="depNameBox flexRowAC">
            <div class="exportBtnBox flexRowAC">
              <button type="button" class="exportBtn newBtn flexRowAC" @click="handleAdd" v-hasPermi="['wvp:server:add']">
                <el-icon class="BtnImg"><Plus /></el-icon>新增
              </button>
            </div>
          </div>
          <div class="searchHeight_out flexRowAC">
            <search-height-box keyword="keyword" placeholder="搜索节点" :data="searchData" @handle="searchResetFn" />
            <export-excel-pdf />
          </div>
        </div>

        <TableSelf
          class="new_table"
          header-cell-class-name="header_tenant_cell"
          stripe
          v-loading="loading"
          :data="filteredServerList"
        >
          <el-table-column label="节点ID" prop="id" show-overflow-tooltip />
          <el-table-column label="类型" :width="clacPXToVW(140)">
            <template #default="scope">
              <el-tag v-if="scope.row.type === 'zlm'">ZLMediaKit</el-tag>
              <el-tag v-else-if="scope.row.type === 'abl'">ABLMediaServer</el-tag>
              <span v-else>{{ scope.row.type }}</span>
            </template>
          </el-table-column>
          <el-table-column label="IP" prop="ip" show-overflow-tooltip />
          <el-table-column label="默认节点" :width="clacPXToVW(110)">
            <template #default="scope">
              <el-tag v-if="scope.row.defaultServer" type="success">默认</el-tag>
              <el-tag v-else type="info">否</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="right" fixed="right" :width="clacPXToVW(200)">
            <template #default="scope">
              <div class="operateAppBox flexRowAC" @click.stop>
                <div class="new_table_svg_group" @click="handleView(scope.row)" v-hasPermi="['wvp:server:view']">
                  <span>查看</span>
                </div>
                <div v-if="!scope.row.defaultServer" class="new_table_svg_group" @click="handleUpdate(scope.row)" v-hasPermi="['wvp:server:edit']">
                  <span>编辑</span>
                </div>
                <div v-if="!scope.row.defaultServer" class="new_table_svg_group" @click="handleDelete(scope.row)" v-hasPermi="['wvp:server:delete']">
                  <span>移除</span>
                </div>
              </div>
            </template>
          </el-table-column>
        </TableSelf>

    <el-dialog title="媒体节点" v-model="openView" width="60%" append-to-body>
      <el-descriptions border>
        <el-descriptions-item label="媒体服务IP">
          {{ rowData.ip }}
        </el-descriptions-item>
        <el-descriptions-item label="HTTP端口">
          {{ rowData.httpPort }}
        </el-descriptions-item>
        <el-descriptions-item label="SECRET">
          {{ rowData.secret }}
        </el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag type="primary" v-if="rowData.type === 'zlm'">ZLMediaKit</el-tag>
          <el-tag type="primary" v-if="rowData.type === 'abl'">ABLMediaServer</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="媒体服务RTMP_PORT">
          {{ rowData.rtmpPort }}
        </el-descriptions-item>
        <el-descriptions-item label="媒体服务RTMPS_PORT">
          {{ rowData.rtmpSSlPort }}
        </el-descriptions-item>
        <el-descriptions-item label="媒体服务HOOK_IP">
          {{ rowData.hookIp }}
        </el-descriptions-item>
        <el-descriptions-item label="媒体服务SDP_IP">
          {{ rowData.sdpIp }}
        </el-descriptions-item>
        <el-descriptions-item label="自动配置媒体服务">
          <el-tag type="primary" v-if="rowData.autoConfig === 1">是</el-tag>
          <el-tag type="primary" v-if="rowData.autoConfig === 0">否</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="媒体服务流IP">
          {{ rowData.streamIp }}
        </el-descriptions-item>
        <el-descriptions-item label="收流端口模式">
          <el-tag type="primary" v-if="rowData.rtpEnable === 1">多端口</el-tag>
          <el-tag type="primary" v-if="rowData.rtpEnable === 0">单端口</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="媒体服务HTTPS_PORT">
          {{ rowData.httpSSlPort }}
        </el-descriptions-item>
        <el-descriptions-item label="收流端口">
          {{ rowData.rtpPortRange }}
        </el-descriptions-item>
        <el-descriptions-item label="媒体服务RTSP_PORT">
          {{ rowData.rtspPort }}
        </el-descriptions-item>
        <el-descriptions-item label="录像管理服务端口">
          {{ rowData.recordAssistPort }}
        </el-descriptions-item>
        <el-descriptions-item label="媒体服务RTSPS_PORT">
          {{ rowData.rtspSSLPort }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
      </div>
    </div>
  </div>
</template>

<script setup name="WvpMediaServer">
import {delWvpMediaServer, listWvpMediaServer} from "@/api/wvp/wvpMediaServer";
import router from "@/router";
import { clacPXToVW } from "@/utils/wvpCompat";

const {proxy} = getCurrentInstance();

const wvpMediaServerList = ref([]);
const openView = ref(false);
const loading = ref(true);
const rowData = ref({});
const keyword = ref('')
const searchData = ref([])
const filteredServerList = computed(() => {
  if (!keyword.value) return wvpMediaServerList.value
  return wvpMediaServerList.value.filter(item =>
    (item.id || '').includes(keyword.value) || (item.ip || '').includes(keyword.value)
  )
})

function searchResetFn(val) {
  keyword.value = val?.keyword || ''
}

/** 查询媒体服务器列表 */
function getList() {
  loading.value = true;
  listWvpMediaServer().then(response => {
    wvpMediaServerList.value = response.data;
    loading.value = false;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 新增按钮操作 */
function handleAdd() {
  router.push(`/gbmanger/node/addMediaServer/index`);
}

/** 修改按钮操作 */
function handleUpdate(row) {
  router.push({
    path: '/gbmanger/node/updateMediaServer/index',
    query: {id: row.id}
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id
  proxy.$modal.confirm('确认删除此节点？').then(function () {
    return delWvpMediaServer(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/**
 * 查看按钮操作
 *
 * @param row
 */
function handleView(row) {
  openView.value = true
  rowData.value = row;
}

getList();
</script>

<style lang="scss" scoped>
.server-card {
  position: relative;
  margin-bottom: 20px;
}

.card-img-zlm {
  width: 200px;
  height: 200px;
  background: url('../../../assets/images/zlm-log.png') no-repeat center;
  background-position: center;
  background-size: contain;
  margin: 0 auto;
}

.card-img-abl {
  width: 200px;
  height: 200px;
  background: url('../../../assets/images/zlm-log.png') no-repeat center;
  background-position: center;
  background-size: contain;
  margin: 0 auto;
}

.server-card-status-online {
  position: absolute;
  right: 20px;
  top: 20px;
  font-size: 18px;
}

.server-card-status-offline {
  position: absolute;
  right: 20px;
  top: 20px;
  font-size: 18px;
}

.server-card-default {
  position: absolute;
  left: 20px;
  top: 20px;
  color: #808080;
  font-size: 18px;
}

.server-card:hover {
  border: 1px solid #adadad;
}
</style>
