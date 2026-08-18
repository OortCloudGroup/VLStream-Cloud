<template>
  <div class="group-management tenant_Page draHeaPB">
    <el-tabs v-model="activeName" class="tenanat-tabs" @tab-change="handleClick">
      <el-tab-pane label="行政区划" name="region" />
      <el-tab-pane label="业务分组" name="group" />
    </el-tabs>
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <div v-show="!treeCollapsed" v-yResize class="police_aside_use">
          <div class="treeTitle">{{ activeName === 'region' ? '行政区划' : '业务分组' }}</div>
          <div class="tree_search_content flexRowAC">
            <el-input
              v-model="treeSearchKeyword"
              placeholder="搜索"
              clearable
              prefix-icon="Search"
            />
          </div>
          <el-tree
            ref="leftTreeRef"
            style="background: #fff;"
            :data="currentTreeData"
            :props="{ label: 'name', children: 'children' }"
            :filter-node-method="filterNode"
            :expand-on-click-node="false"
            default-expand-all
            highlight-current
            node-key="id"
            @node-click="handleTreeNodeClick"
          >
            <template #default="{ node, data }">
              <div
                class="custom-tree-node flexRowAC"
                @mouseenter="hoveredTreeNodeId = data.id || data.name"
                @mouseleave="hoveredTreeNodeId = null"
              >
                <div class="tree-node-main flexRowAC">
                  <el-icon class="tree-icon"><Folder /></el-icon>
                  <el-tooltip :open-delay="500" effect="light" :content="node.label" placement="top">
                    <div class="tree-node-label">{{ node.label }}</div>
                  </el-tooltip>
                </div>
                <div
                  v-show="hoveredTreeNodeId === (data.id || data.name) || (currentTreeNode && currentTreeNode.id === data.id)"
                  class="tree-node-actions flexRowAC"
                  @click.stop
                >
                  <oort-svg-icon v-if="data.id" width="14" height="14" name="delete" color="red" class="tree-action-icon" @click="handleToolbarDelete(data)" />
                  <oort-svg-icon width="14" height="14" name="add" class="tree-action-icon" @click="handleToolbarAdd(data)" />
                </div>
              </div>
            </template>
          </el-tree>
        </div>

        <div class="tableTenItU">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <CollapseToggle
                v-if="treeCollapsed"
                class="expand-device-tree-btn"
                :is-expanded="false"
                @toggle="treeCollapsed = false"
              />
              <div class="exportBtnBox flexRowAC">
                <button type="button" class="exportBtn newBtn flexRowAC" @click="handleToolbarAdd()">
                  <el-icon class="BtnImg"><Plus /></el-icon>新建
                </button>
                <button-group :button-list="toolbarButtonList" />
              </div>
            </div>
            <div class="searchHeight_out flexRowAC">
              <search-height-box
                keyword="keyword"
                placeholder="搜索"
                :data="searchData"
                @handle="searchResetFn"
              />
              <export-excel-pdf />
            </div>
          </div>

          <TableSelf
            class="new_table"
            header-cell-class-name="header_tenant_cell"
            stripe
            v-loading="loading"
            :data="tableRows"
            row-key="id"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" :width="clacPXToVW(55)" />
            <el-table-column label="序号" :width="clacPXToVW(65)">
              <template #default="scope">
                {{ scope.$index + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" show-overflow-tooltip />
            <el-table-column prop="deviceId" label="设备编号" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" show-overflow-tooltip />
            <el-table-column label="操作" align="right" fixed="right" :width="clacPXToVW(220)">
              <template #default="scope">
                <div class="operateAppBox flexRowAC" @click.stop>
                  <div v-if="scope.row.id && checkEditPermi" class="new_table_svg_group" @click="handleRowUpdate(scope.row)">
                    <span>修改</span>
                  </div>
                  <div v-if="checkAddPermi" class="new_table_svg_group" @click="handleToolbarAdd(scope.row)">
                    <span>新增</span>
                  </div>
                  <div v-if="scope.row.id && checkDeletePermi" class="new_table_svg_group" @click="handleToolbarDelete(scope.row)">
                    <span>删除</span>
                  </div>
                </div>
              </template>
            </el-table-column>
          </TableSelf>
        </div>
      </div>
    </div>

    <el-dialog :title="title" v-model="openRegion" width="60%" append-to-body>
          <el-tabs v-model="activeKeyRegion" style="padding: 0 1rem; margin: auto 0" @tab-click="getRegionList">
            <el-tab-pane name="0">
              <template #label>
                <div class="show-code-item">{{ allValRegion[0].val }}</div>
                <div style="text-align: center">{{ allValRegion[0].meaning }}</div>
              </template>
              <el-radio v-for="item in regionList" v-model="allValRegion[0].val" :key="item.deviceId" :name="item.name"
                        :label="item.deviceId" @change="deviceChange(item)" style="line-height: 2rem">
                {{ item.name }} - {{ item.deviceId }}
              </el-radio>
            </el-tab-pane>
            <el-tab-pane name="1">
              <template #label>
                <div class="show-code-item">{{ allValRegion[1].val ? allValRegion[1].val : "--" }}</div>
                <div style="text-align: center">{{ allValRegion[1].meaning }}</div>
              </template>
              <el-radio :key="-1" v-model="allValRegion[1].val" @change="deviceChange" label=""
                        style="line-height: 2rem">
                不添加
              </el-radio>
              <el-radio v-for="item in regionList" v-model="allValRegion[1].val" @change="deviceChange(item)"
                        :key="item.deviceId" :label="item.deviceId.substring(2)" style="line-height: 2rem">
                {{ item.name }} - {{ item.deviceId.substring(2) }}
              </el-radio>
            </el-tab-pane>
            <el-tab-pane name="2">
              <template #label>
                <div class="show-code-item">{{ allValRegion[2].val ? allValRegion[2].val : "--" }}</div>
                <div style="text-align: center">{{ allValRegion[2].meaning }}</div>
              </template>
              <el-radio :key="-1" label="" v-model="allValRegion[2].val" style="line-height: 2rem"
                        @change="deviceChange">
                不添加
              </el-radio>
              <el-radio v-for="item in regionList" v-model="allValRegion[2].val" @change="deviceChange(item)"
                        :key="item.deviceId" :label="item.deviceId.substring(4)" style="line-height: 2rem">
                {{ item.name }} - {{ item.deviceId.substring(4) }}
              </el-radio>
            </el-tab-pane>
            <el-tab-pane name="3">

              <template #label>
                <div class="show-code-item">{{ allValRegion[3].val ? allValRegion[3].val : "--" }}</div>
                <div style="text-align: center">{{ allValRegion[3].meaning }}</div>
              </template>
              <el-input
                  style="width: 400px"
                  type="text"
                  placeholder="请手动输入基层接入单位编码,两位数字"
                  v-model="allValRegion[3].val"
                  maxlength="2"
                  :disabled="allValRegion[3].lock"
                  show-word-limit
                  @input="deviceChange"
              >
              </el-input>
            </el-tab-pane>
          </el-tabs>
          <el-divider/>
          <el-form ref="formRegionRef" :model="formRegion" :rules="rulesRegion" label-width="80px">
            <el-row>
              <el-col :span="12">
                <el-form-item label="名称" prop="name">
                  <el-input v-model="formRegion.name" autocomplete="off" placeholder="请输入名称"></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="编号" prop="deviceId">
                  <el-input v-model="formRegion.deviceId" disabled autocomplete="off"></el-input>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <template #footer>
            <div class="dialog-footer">
              <el-button type="primary" @click="submitFormRegion" class="common_btn">确 定</el-button>
              <el-button @click="cancel" class="common_btn">取 消</el-button>
            </div>
          </template>
        </el-dialog>

        <el-dialog :title="title" v-model="openGroup" width="60%" append-to-body>
          <el-form ref="formGroupRef" :model="formGroup" :rules="rulesGroup" label-width="80px">
            <el-form-item label="节点编号" prop="deviceId">
              <el-input v-model="formGroup.deviceId" placeholder="请输入编码">
                <template #append>
                  <el-button @click="buildDeviceIdCode(formGroup.deviceId)">生成</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="节点名称" prop="name">
              <el-input v-model="formGroup.name" clearable></el-input>
            </el-form-item>
            <el-form-item label="行政区划" prop="civilCode">
              <el-input v-model="formGroup.civilCode">
                <template #append>
                  <el-button @click="chooseCivilCodeFun(formGroup.civilCode)">选择</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-form>
          <template #footer>
            <div class="dialog-footer">
              <el-button type="primary" @click="submitFormGroup" class="common_btn">确 定</el-button>
              <el-button @click="cancel" class="common_btn">取 消</el-button>
            </div>
          </template>
        </el-dialog>

        <ChannelCode ref="channelCodeRef" @handleOk="handleOk"></ChannelCode>
        <ChooseCivilCode ref="chooseCivilCodeRef" @onSubmit="gbCivilCodeOnSubmit"></ChooseCivilCode>
  </div>
</template>

<script setup name="AdministrativeGrouping">
import { clacPXToVW } from "@/utils/wvpCompat";
import { checkPermi } from "@/utils/wvpPermission";
import CollapseToggle from "@/components/CollapseToggle.vue";
import ChannelCode from "../../components/common/channelCode.vue"
import ChooseCivilCode from "../../components/common/chooseCivilCode.vue"
import {addRegion, deleteRegion, getAllChild, queryForTree, updateRegion} from "../../../api/wvp/region.js";
import {addGroup, deleteGroup, queryForTree as queryGroupForTree, updateGroup} from "../../../api/wvp/group.js";

const {proxy} = getCurrentInstance();

const treeRegionData = ref([])
const loading = ref(true);
const activeName = ref('region')
const title = ref("");
const openRegion = ref(false);
const activeKeyRegion = ref('0');
const regionList = ref([]);
const allValRegion = ref([]);

const data = reactive({
  formRegion: {},
  rulesRegion: {
    name: [{required: true, message: "请输入名称", trigger: "blur"}],
  },
  formGroup: {},
  rulesGroup: {
    name: [{required: true, message: "请输入节点名称", trigger: "blur"}],
    deviceId: [{required: true, message: "请选择节点编号", trigger: "change"}],
    civilCode: [{required: true, message: "请选择行政区划", trigger: "change"}],
  }
});

const treeGroupData = ref([])
const openGroup = ref(false);
const channelCodeRef = ref(null);
const chooseCivilCodeRef = ref(null);

const treeCollapsed = ref(false)
const hoveredTreeNodeId = ref(null)
const treeSearchKeyword = ref('')
const leftTreeRef = ref()
const currentTreeNode = ref(null)
const selectedTableRows = ref([])
const searchKeyword = ref('')
const searchDeviceId = ref('')
const searchData = ref([
  { label: '名称', value: 'keyword', type: 'text', default: '' },
  { label: '设备编号', value: 'deviceId', type: 'text', default: '' }
])
const currentTreeData = computed(() => activeName.value === 'region' ? treeRegionData.value : treeGroupData.value)
const tableRows = computed(() => {
  const node = currentTreeNode.value
  const children = node ? (node.children || []) : (currentTreeData.value[0]?.children || [])
  return children.filter(row => {
    const matchName = !searchKeyword.value || (row.name || '').includes(searchKeyword.value)
    const matchId = !searchDeviceId.value || (row.deviceId || '').includes(searchDeviceId.value)
    return matchName && matchId
  })
})
const checkAddPermi = computed(() => activeName.value === 'region' ? checkPermi(['wvp:region:add']) : checkPermi(['wvp:group:add']))
const checkEditPermi = computed(() => activeName.value === 'region' ? checkPermi(['wvp:region:edit']) : checkPermi(['wvp:group:edit']))
const checkDeletePermi = computed(() => activeName.value === 'region' ? checkPermi(['wvp:region:delete']) : checkPermi(['wvp:group:delete']))
const toolbarButtonList = computed(() => [
  { name: '编辑', svg: 'table_edit', clickFn: handleToolbarEdit },
  { name: '删除', svg: 'table_del', clickFn: () => handleToolbarDelete() }
])

watch(treeSearchKeyword, val => {
  leftTreeRef.value?.filter(val)
})

function filterNode(value, data) {
  if (!value) return true
  return (data.name || '').indexOf(value) !== -1
}

function handleTreeNodeClick(data) {
  currentTreeNode.value = data
}

function searchResetFn(val) {
  searchKeyword.value = val?.keyword || ''
  searchDeviceId.value = val?.deviceId || ''
}

function handleSelectionChange(selection) {
  selectedTableRows.value = selection
}

function handleToolbarAdd(row) {
  const target = row && row.name !== undefined ? row : (currentTreeNode.value || currentTreeData.value[0] || {})
  if (activeName.value === 'region') handleRegionAdd(target)
  else handleGroupAdd(target)
}

function handleToolbarEdit() {
  if (selectedTableRows.value.length !== 1) {
    proxy.$modal.msgWarning('请选择一条要修改的数据')
    return
  }
  handleRowUpdate(selectedTableRows.value[0])
}

function handleRowUpdate(row) {
  if (activeName.value === 'region') handleRegionUpdate(row)
  else handleGroupUpdate(row)
}

function handleToolbarDelete(row) {
  const target = row && row.id ? row : selectedTableRows.value[0]
  if (!target?.id) {
    proxy.$modal.msgWarning('请选择要删除的数据')
    return
  }
  if (activeName.value === 'region') handleRegionDelete(target)
  else handleGroupDelete(target)
}

const {formRegion, rulesRegion, formGroup, rulesGroup} = toRefs(data);

function getList() {
  loading.value = true

  if (activeName.value === 'region') {
    queryForTree().then(res => {
      let data = [
        {
          name: "根资源组",
          children: []
        }
      ]
      data[0].children = proxy.handleTree(res.data, "id")
      treeRegionData.value = data
      loading.value = false
    })
  } else if (activeName.value === 'group') {
    queryGroupForTree().then(res => {
      let data = [
        {
          name: "根资源组",
          children: []
        }
      ]
      data[0].children = proxy.handleTree(res.data, "id")
      treeGroupData.value = data
      loading.value = false
    })
  }
}

function handleRegionAdd(row) {
  resetRegion()
  formRegion.value.parentId = row.id
  openRegion.value = true
  title.value = "新增行政区划"
  getRegionList()
  allValRegion.value = [
    {
      id: [1, 2],
      meaning: '省级编码',
      val: '11',
      type: '中心编码',
      lock: false,
    },
    {
      id: [3, 4],
      meaning: '市级编码',
      val: '',
      type: '中心编码',
      lock: false,
    },
    {
      id: [5, 6],
      meaning: '区级编码',
      val: '',
      type: '中心编码',
      lock: false,
    },
    {
      id: [7, 8],
      meaning: '基层接入单位编码',
      val: '',
      type: '中心编码',
      lock: false,
    }
  ]
}

function handleRegionUpdate(row) {
  resetRegion()
  openRegion.value = true
  title.value = "修改行政区划"
  getRegionList()
  formRegion.value = JSON.parse(JSON.stringify(row))

  allValRegion.value = [
    {
      id: [1, 2],
      meaning: '省级编码',
      val: '11',
      type: '中心编码',
      lock: false,
    },
    {
      id: [3, 4],
      meaning: '市级编码',
      val: '',
      type: '中心编码',
      lock: false,
    },
    {
      id: [5, 6],
      meaning: '区级编码',
      val: '',
      type: '中心编码',
      lock: false,
    },
    {
      id: [7, 8],
      meaning: '基层接入单位编码',
      val: '',
      type: '中心编码',
      lock: false,
    }
  ]

  activeKeyRegion.value = '0'
  if (formRegion.value.deviceId) {
    if (formRegion.value.deviceId.length >= 2 && allValRegion.value[0]) {
      allValRegion.value[0].val = formRegion.value.deviceId.substring(0, 2)
    }
    if (formRegion.value.deviceId.length >= 4 && allValRegion.value[1]) {
      allValRegion.value[1].val = formRegion.value.deviceId.substring(2, 4)
    }
    if (formRegion.value.deviceId.length >= 6 && allValRegion.value[2]) {
      allValRegion.value[2].val = formRegion.value.deviceId.substring(4, 6)
    }
    if (formRegion.value.deviceId.length === 8 && allValRegion.value[3]) {
      allValRegion.value[3].val = formRegion.value.deviceId.substring(6, 8)
    }
  } else {
    if (formRegion.value.parentDeviceId) {
      if (formRegion.value.parentDeviceId.length >= 2) {
        allValRegion.value[0].val = formRegion.value.parentDeviceId.substring(0, 2)
        activeKeyRegion.value = "1"
      }
      if (formRegion.value.parentDeviceId.length >= 4) {
        allValRegion.value[1].val = formRegion.value.parentDeviceId.substring(2, 4)
        activeKeyRegion.value = "2"
      }
      if (formRegion.value.parentDeviceId.length >= 6) {
        allValRegion.value[2].val = formRegion.value.parentDeviceId.substring(4, 6)
        activeKeyRegion.value = "3"
      }
    }
  }
}

function queryChildList(parent) {
  getAllChild({parent: parent,}).then((res) => {
    regionList.value = res.data
  })
}

function resetRegion() {
  formRegion.value = {
    deviceId: undefined,
    name: undefined,
  };
  proxy.resetForm("formRegionRef");
}

/** 取消按钮 */
function cancel() {
  openRegion.value = false;
  openGroup.value = false;
  resetRegion();
  resetGroup()
}

function handleRegionDelete(row) {
  proxy.$modal.confirm('是否确认删除名称为"' + row.name + '"的数据项?').then(function () {
    deleteRegion(row.id).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");

    })
  })
}

function submitFormRegion() {
  proxy.$refs["formRegionRef"].validate(valid => {
    if (valid) {
      if (formRegion.value.id) {
        updateRegion(formRegion.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          openRegion.value = false;
          getList();
        })
      } else {
        addRegion(formRegion.value).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          openRegion.value = false;
          getList();
        })
      }
    }
  })
}

function getRegionList() {
  nextTick(() => {
    if (activeKeyRegion.value === '0') {
      queryChildList();
    } else if (activeKeyRegion.value === '1' || activeKeyRegion.value === '2') {
      let parent = ''
      if (activeKeyRegion.value === '1') {
        parent = allValRegion.value[0].val
      }
      if (activeKeyRegion.value === '2') {
        if (allValRegion.value[1].val === "") {
          parent = ""
        } else {
          parent = allValRegion.value[0].val + allValRegion.value[1].val
        }
      }
      if (activeKeyRegion.value !== '0' && parent === '') {
        proxy.$modal.msgError("请先选择上级行政区划");
      }
      if (parent !== "") {
        queryChildList(parent);
      } else {
        regionList.value = []
      }
    }
  })
}

function deviceChange(item) {
  nextTick(() => {
    let code = allValRegion.value[0].val

    if (allValRegion.value[1].val) {
      code += allValRegion.value[1].val
      if (allValRegion.value[2].val) {
        code += allValRegion.value[2].val
        if (allValRegion.value[3].val) {
          code += allValRegion.value[3].val
        }
      } else {
        allValRegion.value[3].val = ""
      }
    } else {
      allValRegion.value[2].val = ""
      allValRegion.value[3].val = ""
    }
    formRegion.value.deviceId = code
  })
}

const handleClick = () => {
  currentTreeNode.value = null
  selectedTableRows.value = []
  nextTick(() => {
    getList()
  })
}

function resetGroup() {
  formGroup.value = {
    deviceId: undefined,
    name: undefined,
    civilCode: undefined,
    businessGroup: "",
  };
  proxy.resetForm("formGroupRef");
}

function handleGroupAdd(row) {
  resetGroup()
  formGroup.value.parentId = row.id
  formGroup.value.businessGroup = row.deviceId
  openGroup.value = true;
  title.value = "新增分组";
}

function handleGroupUpdate(row) {
  resetGroup()
  openGroup.value = true
  title.value = "修改分组"
  formGroup.value = JSON.parse(JSON.stringify(row))
}

function buildDeviceIdCode(deviceId) {
  let lockContent = formGroup.value.businessGroup ? "216":"215"
  channelCodeRef.value.openDialog(code => {

  }, deviceId, 5 , lockContent);
}

function handleOk(code) {
  formGroup.value.deviceId = code
}

function chooseCivilCodeFun() {
  chooseCivilCodeRef.value.openDialog(code => {

  });
}

function gbCivilCodeOnSubmit(data) {
  formGroup.value.civilCode = data;
}

function submitFormGroup() {
  proxy.$refs["formGroupRef"].validate(valid => {
    if (valid) {
      if (formGroup.value.id) {
        updateGroup(formGroup.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          openGroup.value = false;
          getList();
        })
      } else {
        addGroup(formGroup.value).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          openGroup.value = false;
          getList();
        })
      }
    }
  })
}

function handleGroupDelete(row) {
  proxy.$modal.confirm('是否确认删除名称为"' + row.name + '"的数据项?').then(function () {
    deleteGroup(row.id).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
  })
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.show-code-item {
  text-align: center;
  font-size: 3rem;
}

.group-management {
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

.tableTenBox {
  padding: 20px;
  width: 100%;
  height: 100%;
  flex: 1;
  background: #fff;
  align-items: flex-start;
  min-height: 0;
}

.police_aside_use {
  width: 300px;
  padding-right: 20px;
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;

  .treeTitle {
    color: var(--el-color-primary);
    padding-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 12px;
    padding-top: 4px;

    &::before {
      content: '';
      width: 3px;
      height: 18px;
      background-color: var(--el-color-primary);
    }
  }

  .tree_search_content {
    justify-content: center;
    padding-bottom: 10px;

    :deep(.el-input__wrapper) {
      background: #fff;
      box-shadow: none;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
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
    height: calc(100% - 80px);
    overflow: auto;
  }
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

  .tree-node-actions {
    flex-shrink: 0;
    gap: 8px;
    margin-left: 8px;
  }

  .tree-action-icon {
    cursor: pointer;
  }

  .tree-icon {
    flex-shrink: 0;
    font-size: 14px;
    color: var(--el-color-primary);
  }
}

.tableTenItU {
  flex: 1;
  height: 100%;
  overflow: auto;
  min-width: 0;

  :deep(.header_tenant_cell) {
    background: #F8F8F9;
  }
}
</style>

