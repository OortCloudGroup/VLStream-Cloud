<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="设备编号" prop="deviceId">
        <el-input
            v-model="queryParams.deviceId"
            placeholder="请输入设备编号"
            style="width: 200px"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="通道编号" prop="channelId">
        <el-input
            v-model="queryParams.channelId"
            placeholder="请输入通道编号"
            style="width: 200px"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="报警等级" prop="alarmPriority">
        <el-select v-model="queryParams.alarmPriority" placeholder="报警等级" clearable style="width: 200px">
          <el-option
              v-for="dict in alarm_priority"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="报警方式 " prop="alarmMethod">
        <el-select v-model="queryParams.alarmMethod" placeholder="报警方式" clearable style="width: 200px">
          <el-option
              v-for="dict in alarm_method"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker
            v-model="queryParams.startTime"
            style="width: 200px"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择日期时间">
        </el-date-picker>
      </el-form-item>

      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker
            v-model="queryParams.endTime"
            type="datetime"
            style="width: 200px"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择日期时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">

<!--      <el-col :span="1.5">-->
<!--        <el-button-->
<!--            type="danger"-->
<!--            plain-->
<!--            icon="Delete"-->
<!--            size="mini"-->
<!--            :disabled="multiple"-->
<!--            @click="handleDelete"-->
<!--            v-hasPermi="['wvp:alarm:delete']"-->
<!--        >删除-->
<!--        </el-button>-->
<!--      </el-col>-->
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="alarmList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="编号" align="center" prop="id"/>
      <el-table-column label="设备编号" align="center" prop="deviceId"/>
      <el-table-column label="通道编号" align="center" prop="channelId"/>
      <el-table-column label="报警等级" align="center" prop="alarmPriority"/>
      <el-table-column label="报警方式" align="center" prop="alarmMethodDescription">

      </el-table-column>
      <el-table-column label="报警时间" align="center" prop="alarmTime"/>

      <el-table-column label="报警类型" align="center" prop="alarmTypeDescription"/>
<!--      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">-->
<!--        <template #default="scope">-->
<!--          <el-button-->
<!--              size="mini"-->
<!--              type="text"-->
<!--              icon="Delete"-->
<!--              @click="handleDelete(scope.row)"-->
<!--              v-hasPermi="['wvp:alarm:delete']"-->
<!--          >删除-->
<!--          </el-button>-->
<!--        </template>-->
<!--      </el-table-column>-->
    </el-table>

    <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"/>


  </div>
</template>


<script setup name="Alarm">
import {delAlarm, listAlarm} from "@/api/wvp/alarm";


const {proxy} = getCurrentInstance();
const {alarm_priority, alarm_method} = proxy.useDict("alarm_priority", "alarm_method");

const alarmList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    deviceId: null,
    channelId: null,
    alarmPriority: null,
    alarmMethod: null,
    alarmTime: null,
    alarmDescription: null,
    longitude: null,
    latitude: null,
    alarmType: null,
    startTime: null,
    endTime: null
  },
  rules: {
    deviceId: [
      {required: true, message: "设备id不能为空", trigger: "blur"}
    ],
    channelId: [
      {required: true, message: "通道id不能为空", trigger: "blur"}
    ],
    createTime: [
      {required: true, message: "创建时间不能为空", trigger: "blur"}
    ]
  }
});

const {queryParams, form, rules} = toRefs(data);

/** 查询报警列表 */
function getList() {
  loading.value = true;
  listAlarm(queryParams.value).then(response => {
    alarmList.value = response.list;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    deviceId: null,
    channelId: null,
    alarmPriority: null,
    alarmMethod: null,
    alarmTime: null,
    alarmDescription: null,
    longitude: null,
    latitude: null,
    alarmType: null,
    createTime: null
  };
  proxy.resetForm("alarmRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}


/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('确认删除？').then(function () {
    return delAlarm(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

getList();
</script>