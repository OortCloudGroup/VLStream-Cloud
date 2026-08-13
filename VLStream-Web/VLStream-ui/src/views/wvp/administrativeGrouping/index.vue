<template>
  <div class="app-container">
    <el-tabs v-model="activeName" class="work-tabs" @tab-click="handleClick">
      <el-tab-pane label="行政区划" name="region">

        <div class="toolbar-with-search">
          <div class="toolbar-left">
            <button-group :button-list="toolbarButtons" />
          </div>
          <right-toolbar :search="false" @queryTable="getList"></right-toolbar>
        </div>

        <table-self
            v-if="refreshTable"
            v-loading="loading"
            :data="treeRegionData"
            row-key="id"
            :default-expand-all="isExpandAll"
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
         class="new_table" header-cell-class-name="header_tenant_cell" stripe>
          <el-table-column prop="name" label="名称" :show-overflow-tooltip="true"></el-table-column>
          <el-table-column prop="deviceId" label="设备编号" :show-overflow-tooltip="true"
          ></el-table-column>
          <el-table-column prop="createTime" label="创建时间" :show-overflow-tooltip="true"
          ></el-table-column>
          <el-table-column label="操作" align="right" fixed="right" :width="clacPXToVW(220)">
            <template #default="scope">
              <div class="operateAppBox flexRowAC" style="justify-content: flex-end;">
                <div class="new_table_svg_group" @click.stop="handleRegionUpdate(scope.row)" v-if="scope.row.id" v-hasPermi="['wvp:region:edit']">
                  <el-icon><Edit /></el-icon>
                  <span>修改</span>
                </div>
                <div class="new_table_svg_group" @click.stop="handleRegionAdd(scope.row)" v-hasPermi="['wvp:region:add']">
                  <el-icon><Plus /></el-icon>
                  <span>新增</span>
                </div>
                <div class="new_table_svg_group" @click.stop="handleRegionDelete(scope.row)" v-if="scope.row.id" v-hasPermi="['wvp:region:delete']">
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </div>
              </div>
            </template>
          </el-table-column>
        </table-self>

        <el-dialog :title="title" v-model="openRegion" width="1000px" append-to-body>
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
              <el-button type="primary" @click="submitFormRegion">确 定</el-button>
              <el-button @click="cancel">取 消</el-button>
            </div>
          </template>
        </el-dialog>
      </el-tab-pane>
      <el-tab-pane label="业务分组" name="group">
        <div class="toolbar-with-search">
          <div class="toolbar-left">
            <button-group :button-list="toolbarButtons" />
          </div>
          <right-toolbar :search="false" @queryTable="getList"></right-toolbar>
        </div>

        <table-self
            v-if="refreshTable"
            v-loading="loading"
            :data="treeGroupData"
            row-key="id"
            :default-expand-all="isExpandAll"
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
         class="new_table" header-cell-class-name="header_tenant_cell" stripe>
          <el-table-column prop="name" label="名称" :show-overflow-tooltip="true"></el-table-column>
          <el-table-column prop="deviceId" label="设备编号" :show-overflow-tooltip="true"
          ></el-table-column>
          <el-table-column prop="createTime" label="创建时间" :show-overflow-tooltip="true"
          ></el-table-column>
          <el-table-column label="操作" align="right" fixed="right" :width="clacPXToVW(220)">
            <template #default="scope">
              <div class="operateAppBox flexRowAC" style="justify-content: flex-end;">
                <div class="new_table_svg_group" @click.stop="handleGroupUpdate(scope.row)" v-if="scope.row.id" v-hasPermi="['wvp:group:edit']">
                  <el-icon><Edit /></el-icon>
                  <span>修改</span>
                </div>
                <div class="new_table_svg_group" @click.stop="handleGroupAdd(scope.row)" v-hasPermi="['wvp:group:add']">
                  <el-icon><Plus /></el-icon>
                  <span>新增</span>
                </div>
                <div class="new_table_svg_group" @click.stop="handleGroupDelete(scope.row)" v-if="scope.row.id" v-hasPermi="['wvp:group:delete']">
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </div>
              </div>
            </template>
          </el-table-column>
        </table-self>

        <el-dialog :title="title" v-model="openGroup" width="1000px" append-to-body>
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
              <el-button type="primary" @click="submitFormGroup">确 定</el-button>
              <el-button @click="cancel">取 消</el-button>
            </div>
          </template>
        </el-dialog>

        <ChannelCode ref="channelCodeRef" @handleOk="handleOk"></ChannelCode>

        <ChooseCivilCode ref="chooseCivilCodeRef" @onSubmit="gbCivilCodeOnSubmit"></ChooseCivilCode>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="AdministrativeGrouping">
import { clacPXToVW } from "@/utils/wvpCompat";
import ChannelCode from "../../components/common/channelCode.vue"
import ChooseCivilCode from "../../components/common/chooseCivilCode.vue"
import {addRegion, deleteRegion, getAllChild, queryForTree, updateRegion} from "../../../api/wvp/region.js";
import {addGroup, deleteGroup, queryForTree as queryGroupForTree, updateGroup} from "../../../api/wvp/group.js";

const {proxy} = getCurrentInstance();

const treeRegionData = ref([])
const isExpandAll = ref(false);
const refreshTable = ref(true);
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

const toolbarButtons = computed(() => [
  { name: '展开/折叠', svg: 'list', clickFn: () => toggleExpandAll() }
]);

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

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
}

const handleClick = () => {
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

