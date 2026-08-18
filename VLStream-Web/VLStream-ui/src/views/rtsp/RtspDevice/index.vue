<template>
  <DeviceClassificationLayout protocol-type="RTSP" :selected-device-keys="classificationDeviceKeys" @filter-change="handleClassificationFilter" @assigned="getList">
  <div class="device-table-panel">
    <div class="depNameBox_out flexRowAC">
      <div class="depNameBox flexRowAC">
        <div class="exportBtnBox flexRowAC">
          <button type="button" class="exportBtn newBtn flexRowAC" @click="handleAdd" v-hasPermi="['rtsp:RtspDevice:add']">
            <el-icon class="BtnImg"><Plus /></el-icon>新增
          </button>
          <button-group :button-list="toolbarButtonList" />
        </div>
      </div>
      <div class="searchHeight_out flexRowAC">
        <search-height-box
          keyword="name"
          placeholder="搜索摄像头名称"
          :data="searchData"
          @handle="searchResetFn"
        />
        <export-excel-pdf :item="exportItem" @handle="handleExport" />
      </div>
    </div>

    <table-self
      class="new_table"
      header-cell-class-name="header_tenant_cell"
      stripe
      v-loading="loading"
      :data="RtspDeviceList"
      current-row-key="id"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" :width="clacPXToVW(55)" />
      <el-table-column label="ip" prop="ip" show-overflow-tooltip />
      <el-table-column label="摄像头名称" prop="name" show-overflow-tooltip />
      <el-table-column label="地址" prop="addressMap" show-overflow-tooltip />
      <el-table-column label="用户名" prop="userName" show-overflow-tooltip />
      <el-table-column label="密码" prop="password">
        <template #default="scope">
          <div class="password-container">
            <span v-if="!passwordVisibility[scope.row.id]">******</span>
            <span v-else>{{ scope.row.password }}</span>
            <el-icon class="eye-icon" @click.stop="togglePasswordVisibility(scope.row.id)">
              <component :is="passwordVisibility[scope.row.id] ? 'Hide' : 'View'"/>
            </el-icon>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="通道号" prop="channel" :width="clacPXToVW(90)" />
      <el-table-column label="设备厂商" prop="firm" :width="clacPXToVW(120)">
        <template #default="scope">
          <dict-tag :options="rtsp_manufacturer" :value="scope.row.firm"/>
        </template>
      </el-table-column>
      <el-table-column label="播放类型" prop="playType" :width="clacPXToVW(100)">
        <template #default="scope">
          <el-tag type="primary" v-if="scope.row.playType === '1'">本地</el-tag>
          <el-tag type="primary" v-if="scope.row.playType === '2'">推流</el-tag>
          <el-tag type="primary" v-if="scope.row.playType === '3'">EasyNTS</el-tag>
        </template>
      </el-table-column>
      <el-table-column key="streamId" label="流id" prop="streamId" min-width="150" show-overflow-tooltip />
      <el-table-column key="remark" label="备注" prop="remark" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" align="right" fixed="right" :width="clacPXToVW(260)">
        <template #default="scope">
          <div class="operateAppBox flexRowAC" @click.stop>
            <div class="new_table_svg_group" @click="handleView(scope.row)" v-hasPermi="['rtsp:RtspDevice:view']">
              <span>播放</span>
            </div>
            <div class="new_table_svg_group" @click="handleUpdate(scope.row)" v-hasPermi="['rtsp:RtspDevice:edit']">
              <span>修改</span>
            </div>
            <div class="new_table_svg_group" @click="handleDelete(scope.row)" v-hasPermi="['rtsp:RtspDevice:remove']">
              <span>删除</span>
            </div>
            <el-dropdown @command="(command)=>{moreClick(command, scope.row)}"
                         v-if="checkPermi(['rtsp:RtspDevice:edit', 'rtsp:RtspDevice:Avatar', 'rtsp:RtspDevice:AlarmClock'])">
              <div class="new_table_svg_group">
                <span>更多</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="handleMap" v-if="checkPermi(['rtsp:RtspDevice:edit'])">修改位置</el-dropdown-item>
                  <el-dropdown-item command="handleAI" v-if="checkPermi(['rtsp:RtspDevice:Avatar'])">AI播放</el-dropdown-item>
                  <el-dropdown-item command="handleAlarmClock" v-if="checkPermi(['rtsp:RtspDevice:AlarmClock'])">历史播放</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </table-self>

    <div class="paginationBox flexRowAC">
      <pagination
          v-show="total>0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
      />
    </div>

    <!-- 添加或修改rtsp设备对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="RtspDeviceRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="ip" prop="ip">
          <el-input v-model="form.ip" placeholder="请输入ip" maxlength="50" show-word-limit/>
        </el-form-item>
        <el-form-item label="摄像头名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入摄像头名称" maxlength="30" show-word-limit/>
        </el-form-item>
        <el-form-item label="设备厂商" prop="firm">
          <el-select v-model="form.firm" placeholder="请选择设备厂商">
            <el-option
                v-for="dict in rtsp_manufacturer"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="播放类型" prop="playType">
          <el-radio-group v-model="form.playType">
            <el-radio value="1">本地</el-radio>
            <el-radio value="2">推流</el-radio>
            <el-radio value="3">EasyNTS</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="流id" prop="streamId" v-if="form.playType === '2'">
          <el-input v-model="form.streamId" placeholder="请输入流id" maxlength="100" show-word-limit/>
        </el-form-item>
        <el-form-item label="EasyNTS地址" prop="easyNTSUrl" v-if="form.playType === '3'">
          <el-input v-model="form.easyNTSUrl" type="textarea" placeholder="请输入EasyNTS地址" maxlength="200"
                    show-word-limit/>
        </el-form-item>
        <div v-if="form.playType === '1'">
          <el-form-item label="用户名" prop="userName">
            <el-input v-model="form.userName" placeholder="请输入用户名" maxlength="20" show-word-limit/>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" placeholder="请输入密码" maxlength="50"/>
          </el-form-item>
          <el-form-item label="通道号" prop="channel">
            <el-input v-model="form.channel" placeholder="请输入通道号" maxlength="10" show-word-limit/>
          </el-form-item>
        </div>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" maxlength="255" show-word-limit/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 播放弹窗 对话框 -->
    <el-dialog :title="title" v-model="showPaly" width="835px">
      <div>
        <Hikvision :rtsp="rtspURL" v-if="showPaly && (playType === '1' || playType === '3') "/>

        <el-row :gutter="10" style="margin-top: 20px" v-if="showPaly && (playType === '1' || playType === '3') ">
          <el-col :span="4"><span style="width: 100px; line-height: 40px; text-align: right;">播放地址：</span></el-col>
          <el-col :span="20">
            <el-input v-model="rtspURL" :disabled="true">
              <template #append>
                <el-button type="primary" :icon="DocumentCopy" @click="copyToClipboard(rtspURL)"/>
              </template>
            </el-input>
          </el-col>
        </el-row>

        <div v-if="playType === '2'">
          <el-tabs v-model="activeName" type="card" :stretch="true">
            <el-tab-pane label="flv播放" name="flv">
              <el-row>
                <el-col :span="24">
                  <div class="player" v-if="activeName === 'flv'">
                    <Jessibuca v-if="activeName === 'flv'" ref="flv" :visible.sync="showVideoDialog"
                               :videoUrl="flvUrl" :error="videoError" :message="videoError" height="100px"
                               :hasAudio="hasAudio" fluent autoplay live></Jessibuca>
                  </div>
                </el-col>
              </el-row>
            </el-tab-pane>
            <el-tab-pane label="webRtc" name="webRtc">
              <RtcPlayer v-if="activeName === 'webRtc'" ref="webRTC" :visible.sync="showVideoDialog"
                         :videoUrl="rtcUrl" :error="videoError" :message="videoError" height="100px"
                         :hasAudio="hasAudio" fluent autoplay live></RtcPlayer>
            </el-tab-pane>
            <el-tab-pane label="H265" name="H265">
              <H265web v-if="activeName === 'H265'" ref="h265web" :visible.sync="showVideoDialog"
                       :videoUrl="wsUrl" :error="videoError" :message="videoError" height="100px"></H265web>
            </el-tab-pane>
          </el-tabs>

          <el-tabs v-model="tabActiveName" type="card" :stretch="true" style="margin-top: 10px;">
            <el-tab-pane label="实时视频" name="media">
              <el-row :gutter="10">
                <el-col :span="2"><span style="width: 80px; line-height: 40px; text-align: right;">播放地址：</span>
                </el-col>
                <el-col :span="18">
                  <el-input v-model="flvUrl" :disabled="true" v-show="activeName === 'flv'">
                    <template #prepend>flv地址</template>
                    <template #append>
                      <el-button type="primary" :icon="DocumentCopy" @click="copyToClipboard(flvUrl)"/>
                    </template>
                  </el-input>
                  <el-input v-model="rtcUrl" :disabled="true" v-show="activeName === 'webRtc'">
                    <template #prepend>rtcUrl地址</template>
                    <template #append>
                      <el-button type="primary" :icon="DocumentCopy" @click="copyToClipboard(rtcUrl)"/>
                    </template>
                  </el-input>
                  <el-input v-model="wsUrl" :disabled="true" v-show="activeName === 'H265'">
                    <template #prepend>wsUrl地址</template>
                    <template #append>
                      <el-button type="primary" :icon="DocumentCopy" @click="copyToClipboard(wsUrl)"/>
                    </template>
                  </el-input>
                </el-col>
                <el-col :span="2">
                  <StreamDropdown :stream-info="streamInfo"/>
                </el-col>
              </el-row>
            </el-tab-pane>

            <el-tab-pane label="编码信息" name="codec">
              <MediaInfo v-if="tabActiveName === 'codec'" ref="mediaInfo" :app="streamInfo.app"
                         :stream="streamInfo.stream" :mediaServerId="streamInfo.mediaServerId"></MediaInfo>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-dialog>

    <el-dialog title="选择时间" v-model="showAlarmClock" width="505px" append-to-body>
      <el-form :model="alarmClockOptions" :rules="rulesAlarm" ref="alarmClockFormRef" label-width="100px">
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
              v-model="alarmClockOptions.startTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择日期时间">
          </el-date-picker>
        </el-form-item>

        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
              v-model="alarmClockOptions.endTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择日期时间">
          </el-date-picker>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFormAlarmClock">确 定</el-button>
          <el-button @click="showAlarmClock = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="选择ai模式" v-model="showAI" width="505px">
      <div>
        <el-button type="primary" style="width: 100%;" @click="handlDetection('detection')">目标检测</el-button>
      </div>
      <div style="margin-top: 10px;">
        <el-button type="primary" style="width: 100%;">车牌检测</el-button>
      </div>
    </el-dialog>

    <el-dialog title="目标检测算法" v-model="showAIPaly" width="835px" @close="closeAI">
      <div style="width: 800px; height: 600px;">
        <el-row>
          <el-col :span="24">
            <div class="selectVideo">
              <CusPlayer ref="cusPlayer" v-if="cusPlayerShow"></CusPlayer>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-dialog>

    <el-dialog title="修改地址" v-model="showMap" width="800px" append-to-body>
      <MapGaoDe ref="MapContainer" @update-value="updateDialogMap" :position="position" :toponym="form.address"/>
    </el-dialog>

  </div>
</DeviceClassificationLayout>
</template>

<script setup name="RtspDevice">
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import {
  addDetection,
  addRtspDevice,
  alarmClockRtspDevice,
  delRtspDevice,
  getRtspDevice,
  listRtspDevice,
  stopDetection,
  updateRtspDevice
} from "@/api/rtsp/RtspDevice";
import Hikvision from "@/components/Hikvision/index.vue";
import CusPlayer from "@/components/flv/CusPlayer.vue";
import {ElLoading, ElMessage} from "element-plus";
import MapGaoDe from "@/components/MapGaoDe/index.vue";
import {checkPermi} from "@/utils/wvpPermission";
import {startPlay} from "../../../api/wvp/push.js";
import {DocumentCopy} from '@element-plus/icons-vue'

import Jessibuca from "@/components/jessibuca/index.vue";
import RtcPlayer from "@/components/rtcPlayer/index.vue";
import H265web from "@/components/H265web/index.vue";
import StreamDropdown from "@/views/wvp/channel/components/streamDropdown.vue";
import MediaInfo from "@/views/wvp/channel/components/mediaInfo.vue";
import { clacPXToVW } from "@/utils/wvpCompat";
import useClipboard from "vue-clipboard3";
const { toClipboard } = useClipboard()

const {proxy} = getCurrentInstance();
const {rtsp_manufacturer} = proxy.useDict('rtsp_manufacturer');

const RtspDeviceList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const cusPlayerShow = ref(false);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const rtspURL = ref("");
const showPaly = ref(false);
const showAIPaly = ref(false);
const showAI = ref(false);
const showMap = ref(false);
const showAlarmClock = ref(false);
const cusPlayer = ref(null);
const alarmClockFormRef = ref(null);
const position = ref(null);
const MapContainer = ref(null);
const toponym = ref('');

const rtcUrl = ref("");
const flvUrl = ref("");
const wsUrl = ref('');
const playType = ref('');
const showVideoDialog = ref(false);
const activeName = ref('flv');
const tabActiveName = ref('media');
const streamInfo = ref({});
const hasAudio = ref(false);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    ip: null,
    name: null,
    firm: null,
  },
  rules: {
    ip: [
      {required: true, message: "ip不能为空", trigger: "blur"}
    ],
    name: [
      {required: true, message: "摄像头名称不能为空", trigger: "blur"}
    ],
    userName: [
      {required: true, message: "用户名不能为空", trigger: "blur"}
    ],
    password: [
      {required: true, message: "密码不能为空", trigger: "blur"}
    ],
    channel: [
      {required: true, message: "通道号不能为空", trigger: "blur"}
    ],
    streamId: [
      {required: true, message: "流id不能为空", trigger: "blur"}
    ],
    easyNTSUrl: [
      {required: true, message: "EasyNTS播放地址不能为空", trigger: "blur"}
    ],
  }
});

const {queryParams, form, rules} = toRefs(data);
const classificationDeviceKeys = ref([]);
function handleClassificationFilter(filter) { Object.assign(queryParams.value, filter, { pageNum: 1 }); getList(); }

const exportItem = ref({ isDisabledExcel: false })
const searchData = ref([
  { label: 'ip', value: 'ip', type: 'text', default: '' },
  { label: '摄像头名称', value: 'name', type: 'text', default: '' },
  { label: '设备厂商', value: 'firm', type: 'select', option: [], default: '' },
])
const toolbarButtonList = computed(() => [
  { name: '修改', svg: 'table_edit', clickFn: handleToolbarUpdate },
  { name: '删除', svg: 'table_del', clickFn: handleToolbarDelete },
])
watch(rtsp_manufacturer, (val) => {
  const item = searchData.value.find(i => i.value === 'firm')
  if (item) item.option = val || []
}, { immediate: true, deep: true })
function searchResetFn(val) {
  queryParams.value.pageNum = 1
  queryParams.value.ip = val?.ip || null
  queryParams.value.name = val?.name || null
  queryParams.value.firm = val?.firm || null
  getList()
}
function handleToolbarUpdate() {
  if (single.value) {
    proxy.$modal.msgWarning('请选择一条要修改的数据')
    return
  }
  handleUpdate({ id: Array.isArray(ids.value) ? ids.value[0] : ids.value })
}
function handleToolbarDelete() {
  if (multiple.value) {
    proxy.$modal.msgWarning('请选择要删除的数据')
    return
  }
  handleDelete()
}

const videoError = (e) => {
  console.log("播放器错误：" + JSON.stringify(e));
}

const copyToClipboard = async (text) => {
  if (!text) {
    ElMessage.error('内容为空，无法复制');
    return;
  }

  try {
    await toClipboard(text)
    ElMessage.success('成功拷贝到粘贴板');
  } catch (e) {
    console.error(e)
  }
};



function moreClick(command, itemData) {
  if (command === "handleMap") {
    handleMap(itemData)
  } else if (command === "handleAI") {
    handleAI(itemData)
  } else if (command === "handleAlarmClock") {
    handleAlarmClock(itemData)
  }
}

const updateDialogMap = (value) => {
  form.value.addressMap = value.address + value.detailedStreet;
  form.value.lng = value.lng;
  form.value.lat = value.lat;
  position.value = [form.value.lng, form.value.lat];
  toponym.value = form.value.addressMap;
  updateRtspDevice(form.value).then(res => {
    showMap.value = false;
    Destruction();
    proxy.$modal.msgSuccess("操作成功");
  }).catch(() => {
    proxy.$modal.msgError("操作失败");
  })
}

const Destruction = () => {
  MapContainer.value?.Destruction();
};

const passwordVisibility = ref({});


const rulesAlarm = ref({
  startTime: [
    {required: true, message: '请选择开始时间', trigger: 'change'}
  ],
  endTime: [
    {required: true, message: '请选择结束时间', trigger: 'change'}
  ]
});

const alarmClockOptions = ref({
  id: undefined,
  startTime: undefined,
  endTime: undefined,
});

const handleMap = (row) => {
  form.value = row;
  position.value = [form.value.lng, form.value.lat];
  toponym.value = form.value.addressMap;
  showMap.value = true;
  Create();
};

const Create = () => {
  MapContainer.value?.inGaDeMap();
};


const submitFormAlarmClock = () => {
  proxy.$refs["alarmClockFormRef"].validate(valid => {
    if (valid) {
      alarmClockRtspDevice(alarmClockOptions.value).then(res => {
        rtspURL.value = res.msg;
        showPaly.value = true;
      }).finally(() => {
        showAlarmClock.value = false;
      });
    }
  });
}

const handleAlarmClock = (row) => {
  resetAlarmClock();
  alarmClockOptions.value.id = row.id;
  showAlarmClock.value = true;
}

const closeAI = () => {
  const params = {
    type: "detection",
    url: streamInfo.value.url,
    mediaId: null,
  }
  stopDetection(params);
}

const handleAI = (row) => {
  streamInfo.value = row;
  showAI.value = true;
}

function resetAlarmClock() {
  alarmClockOptions.value.id = undefined;
  alarmClockOptions.value.startTime = undefined;
  alarmClockOptions.value.endTime = undefined;
}

const handlDetection = async (str) => {
  const loadingService = ElLoading.service({
    lock: true,
    fullscreen: true,
    text: '正在发送指令',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  cusPlayerShow.value = false;
  let mediaId = Math.floor(Math.random() * 900) + 100;
  const params = {
    type: str,
    url: streamInfo.value.url,
    mediaId: mediaId,
  }
  await addDetection(params).then(res => {
    showAI.value = false;
    cusPlayerShow.value = true;
    showAIPaly.value = true;
    setTimeout(() => {
      cusPlayer.value.createPlayer("http://127.0.0.1:7080/live/livestream/" + mediaId + ".flv", true, 0);
      loadingService.close();
    }, 3000);
  }).finally(() => {
    loadingService.close();
  });
}

const togglePasswordVisibility = (id) => {
  passwordVisibility.value[id] = !passwordVisibility.value[id];
};

const handleView = async (row) => {
  playType.value = row.playType;
  if (row.playType === '2') {
    const ans = await startPlay(row.streamId);
    streamInfo.value = ans.data;
    flvUrl.value = ans.data.flv;
    rtcUrl.value = ans.data.rtc;
    wsUrl.value = ans.data.ws_flv;
    showPaly.value = true;
  } else if (row.playType === '1') {
    rtspURL.value = row.url;
    title.value = row.name;
    showPaly.value = true;
  } else if (row.playType === '3') {
    rtspURL.value = row.easyNTSUrl;
    title.value = row.name;
    showPaly.value = true;
  }
}

/** 查询rtsp设备列表 */
function getList() {
  loading.value = true;
  listRtspDevice(queryParams.value).then(response => {
    RtspDeviceList.value = response.rows;
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
    ip: null,
    name: null,
    userName: null,
    password: null,
    channel: null,
    playType: '1',
    url: null,
    streamId: null,
    easyNTSUrl: null,
    firm: '1',
    lat: null,
    lng: null,
    addressMap: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("RtspDeviceRef");
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
  classificationDeviceKeys.value = selection.map(item => String(item.id));
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加rtsp设备";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row?.id || ids.value
  getRtspDevice(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改rtsp设备";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["RtspDeviceRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateRtspDevice(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addRtspDevice(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row?.id || ids.value;
  proxy.$modal.confirm('是否确认删除rtsp设备编号为"' + _ids + '"的数据项？').then(function () {
    return delRtspDevice(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('rtsp/RtspDevice/export', {
    ...queryParams.value
  }, `RtspDevice_${new Date().getTime()}.xlsx`)
}

getList();
</script>

<style scoped>
.selectVideo {
  height: 600px;
  width: 800px;
}
</style>
