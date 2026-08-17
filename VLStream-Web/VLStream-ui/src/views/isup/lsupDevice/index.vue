<template>
  <DeviceClassificationLayout protocol-type="ISUP" :selected-device-keys="classificationDeviceKeys" @filter-change="handleClassificationFilter" @assigned="getList">
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="设备ID" prop="deviceId">
        <el-input
            v-model="queryParams.deviceId"
            placeholder="请输入设备ID"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="设备名称" prop="name">
        <el-input
            v-model="queryParams.name"
            placeholder="请输入设备名称"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="IP地址" prop="ipAddress">
        <el-input
            v-model="queryParams.ipAddress"
            placeholder="请输入设备的IP地址"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择在线状态" style="width: 196px;"
                   default-first-option>
          <el-option label="在线" value="ON"></el-option>
          <el-option label="离线" value="OFFLINE"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['isup:lsupDevice:edit']"
        >修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['isup:lsupDevice:remove']"
        >删除
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="lsupDeviceList" @selection-change="handleSelectionChange" border>
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="设备ID" align="center" prop="deviceId"/>
      <el-table-column label="设备名称" align="center" prop="name"/>
      <el-table-column label="地址" align="center" prop="addressMap"/>
      <el-table-column label="IP地址" align="center" prop="ipAddress"/>
      <el-table-column label="用户名" align="center" prop="userName"/>
      <el-table-column label="密码" align="center" prop="password">
        <template #default="scope">
          <div class="password-container">
            <span v-if="!passwordVisibility[scope.row.id]">******</span>
            <span v-else>{{ scope.row.password }}</span>
            <el-icon class="eye-icon" @click="togglePasswordVisibility(scope.row.id)">
              <component :is="passwordVisibility[scope.row.id] ? 'Hide' : 'View'"/>
            </el-icon>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 'ON'" type="success">在线</el-tag>
          <el-tag v-if="scope.row.status === 'OFFLINE'" type="danger">离线</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="播放类型" align="center" prop="playType">
        <template #default="scope">
          <dict-tag :options="play_type" :value="scope.row.playType"/>
        </template>
      </el-table-column>
      <el-table-column key="streamId" label="流id" prop="streamId" min-width="150" align="center"/>
      <el-table-column label="备注" align="center" prop="remark"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="300">
        <template #default="scope">
          <div style="display:flex; align-items: center;justify-content: center">
            <el-button link type="primary" icon="View" @click="handleSDKPlay(scope.row)"
                       v-hasPermi="['isup:lsupDevice:start']">SDK播放
            </el-button>
            <el-button link type="primary" icon="View" @click="handleStartPlay(scope.row)"
                       v-hasPermi="['isup:lsupDevice:start']">播放
            </el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                       v-hasPermi="['isup:lsupDevice:edit']">修改
            </el-button>
            <el-dropdown @command="(command)=>{moreClick(command, scope.row)}"
                         v-if="checkPermi(['isup:lsupDevice:edit'])">
             <span class="el-dropdown-link">
              <el-button type="text">
                更多
                <el-icon>
                  <arrow-down/>
                </el-icon>
              </el-button>
            </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="handleMap" v-if="checkPermi(['isup:lsupDevice:edit'])">修改位置
                  </el-dropdown-item>
                  <el-dropdown-item command="handleDelete" v-if="checkPermi(['isup:lsupDevice:remove'])">删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />

    <!-- 添加或修改isup设备对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="lsupDeviceRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="form.deviceId" disabled placeholder="请输入设备ID"/>
        </el-form-item>
        <el-form-item label="IP地址" prop="ipAddress">
          <el-input v-model="form.ipAddress" disabled placeholder="请输入设备的 IP 地址"/>
        </el-form-item>
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入设备名称"/>
        </el-form-item>
        <el-form-item label="播放类型" prop="playType">
          <el-radio-group v-model="form.playType">
            <el-radio
                v-for="dict in play_type"
                :key="dict.value"
                :value="dict.value"
            >{{ dict.label }}
            </el-radio>
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
            <el-input v-model="form.userName" placeholder="请输入用户名"/>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" placeholder="请输入密码" show-password/>
          </el-form-item>
          <el-form-item label="通道" prop="channel">
            <template #label>
              <span>
                 <el-tooltip content="海康isup不支持获取通道" placement="top">
                    <el-icon><question-filled /></el-icon>
                 </el-tooltip>
                 通道
              </span>
            </template>
            <el-input v-model="form.channel" placeholder="请输入通道"/>
          </el-form-item>
        </div>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="播放视频" v-model="openPlay" width="835px" append-to-body>
      <div>
        <Hikvision :rtsp="videoUrl" v-if="openPlay && (playType === '1' || playType === '3' || playType === '4') "/>

        <el-row :gutter="10" style="margin-top: 20px" v-if="openPlay && (playType === '1' || playType === '3' || playType === '4') ">
          <el-col :span="4"><span style="width: 100px; line-height: 40px; text-align: right;">播放地址：</span></el-col>
          <el-col :span="20">
            <el-input v-model="videoUrl" :disabled="true">
              <template #append>
                <el-button type="primary" :icon="DocumentCopy" @click="copyToClipboard(videoUrl)"/>
              </template>
            </el-input>
          </el-col>
        </el-row>

        <div style="display: grid; height: 180px; overflow: auto" v-if="openPlay && (playType === '1' || playType === '3' || playType === '4') ">
            <!-- 左侧控制区域 -->
            <div style="display: grid; grid-template-columns: 100px auto;">
              <!-- 方向控制 -->
              <div class="control-wrapper">
                <div class="control-btn control-top" @mousedown="ptzCtrlStartFun(3)" @mouseup="ptzCtrlEndFun()">
                  <el-icon class="icon">
                    <CaretTop/>
                  </el-icon>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-left" @mousedown="ptzCtrlStartFun(2)" @mouseup="ptzCtrlEndFun()">
                  <el-icon class="icon">
                    <CaretLeft/>
                  </el-icon>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-bottom" @mousedown="ptzCtrlStartFun(4)" @mouseup="ptzCtrlEndFun()">
                  <el-icon class="icon">
                    <CaretBottom/>
                  </el-icon>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-right" @mousedown="ptzCtrlStartFun(1)" @mouseup="ptzCtrlEndFun()">
                  <el-icon class="icon">
                    <CaretRight/>
                  </el-icon>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-round">
                  <div class="control-round-inner"><i class="fa fa-pause-circle"></i></div>
                </div>
                <!-- 速度控制 -->
                <div class="contro-speed" style="position: absolute; left: 4px; top: 112px; width: 100px;">
                  <el-slider v-model="controSpeed" :min="1" :max="15"></el-slider>
                </div>
              </div>

              <!-- 变倍、聚焦、光圈控制 -->
              <div>
                <div style="margin-left: 20px;width: 300px;">
                  聚焦
                  <el-slider v-model="controSpeedFocus" :max="100" :min="-100" :show-input="true"
                             @change="focusCamera"/>
                </div>
              </div>
            </div>
          </div>
      </div>

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

          <el-tab-pane v-if="checkPermi(['isup:lsupDevice:ptzCtrl'])" label="云台控制" name="control">
            <div style="display: grid; height: 180px; overflow: auto">
              <!-- 左侧控制区域 -->
              <div style="display: grid; grid-template-columns: 100px auto;">
                <!-- 方向控制 -->
                <div class="control-wrapper">
                  <div class="control-btn control-top" @mousedown="ptzCtrlStartFun(3)" @mouseup="ptzCtrlEndFun()">
                    <el-icon class="icon">
                      <CaretTop/>
                    </el-icon>
                    <div class="control-inner-btn control-inner"></div>
                  </div>
                  <div class="control-btn control-left" @mousedown="ptzCtrlStartFun(2)" @mouseup="ptzCtrlEndFun()">
                    <el-icon class="icon">
                      <CaretLeft/>
                    </el-icon>
                    <div class="control-inner-btn control-inner"></div>
                  </div>
                  <div class="control-btn control-bottom" @mousedown="ptzCtrlStartFun(4)" @mouseup="ptzCtrlEndFun()">
                    <el-icon class="icon">
                      <CaretBottom/>
                    </el-icon>
                    <div class="control-inner-btn control-inner"></div>
                  </div>
                  <div class="control-btn control-right" @mousedown="ptzCtrlStartFun(1)" @mouseup="ptzCtrlEndFun()">
                    <el-icon class="icon">
                      <CaretRight/>
                    </el-icon>
                    <div class="control-inner-btn control-inner"></div>
                  </div>
                  <div class="control-round">
                    <div class="control-round-inner"><i class="fa fa-pause-circle"></i></div>
                  </div>
                  <!-- 速度控制 -->
                  <div class="contro-speed" style="position: absolute; left: 4px; top: 112px; width: 100px;">
                    <el-slider v-model="controSpeed" :min="1" :max="15"></el-slider>
                  </div>
                </div>

                <!-- 变倍、聚焦、光圈控制 -->
                <div>
                  <div style="margin-left: 20px;width: 300px;">
                    聚焦
                    <el-slider v-model="controSpeedFocus" :max="100" :min="-100" :show-input="true"
                               @change="focusCamera"/>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <el-dialog title="修改地址" v-model="showMap" width="800px" append-to-body>
      <MapGaoDe ref="MapContainer" @update-value="updateDialogMap" :position="position" :toponym="form.address"/>
    </el-dialog>

    <el-dialog title="SDK播放" v-model="openPlaySDK" width="835px" append-to-body @close="closeSDK">
      <div>
        <Hikvision :rtsp="videoUrl"/>
      </div>
    </el-dialog>
  </div>
</DeviceClassificationLayout>
</template>

<script setup name="LsupDevice">
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import {checkPermi} from "@/utils/wvpPermission";
import {delLsupDevice, getLsupDevice, start as startSDK, stopRealPlay, updateLsupDevice} from "@/api/isup/lsupDevice";
import {
  listIsupDevice,
  ptzCtrlEnd,
  ptzCtrlFocus,
  ptzCtrlStart
} from "../../../api/isup/lsupDevice.js";
import Hikvision from "@/components/Hikvision/index.vue";
import MapGaoDe from "@/components/MapGaoDe/index.vue";

import Jessibuca from "@/components/jessibuca/index.vue";
import RtcPlayer from "@/components/rtcPlayer/index.vue";
import H265web from "@/components/H265web/index.vue";
import StreamDropdown from "@/views/wvp/channel/components/streamDropdown.vue";
import MediaInfo from "@/views/wvp/channel/components/mediaInfo.vue";
import {DocumentCopy} from '@element-plus/icons-vue'
import {ElLoading, ElMessage} from "element-plus";
import {startPlay} from "../../../api/wvp/push.js";
import useClipboard from "vue-clipboard3";
const { toClipboard } = useClipboard()

const {proxy} = getCurrentInstance();
const {play_type} = proxy.useDict('play_type');


const lsupDeviceList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const daterangeCreateTime = ref([]);
const daterangeUpdateTime = ref([]);
const passwordVisibility = ref({});

const openPlay = ref(false);
const openPlaySDK = ref(false);
const videoUrl = ref('');

const controSpeed = ref(5);

const controSpeedFocus = ref(0);

const position = ref(null);
const MapContainer = ref(null);
const toponym = ref('');
const showMap = ref(false);

const ptzCameraQueryParams = ref({
  id: null,
});

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
    deviceId: null,
    ipAddress: null,
    deviceSerial: null,
    status: null,
    name: null,
  },
  rules: {
    userName: [
      {required: true, message: "用户名不能为空", trigger: "blur"}
    ],
    password: [
      {required: true, message: "密码不能为空", trigger: "blur"}
    ],
    name: [
      {required: true, message: "请输入设备名称", trigger: "blur"}
    ],
    channel: [
      {required: true, message: "请选择通道", trigger: "change"}
    ],
    streamId: [
      {required: true, message: "流id不能为空", trigger: "blur"}
    ],
    easyNTSUrl: [
      {required: true, message: "EasyNTS播放地址不能为空", trigger: "blur"}
    ],
  },
});

const {queryParams, form, rules} = toRefs(data);
const classificationDeviceKeys = ref([]);
function handleClassificationFilter(filter) { Object.assign(queryParams.value, filter, { pageNum: 1 }); getList(); }

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

const luserId = ref(0);
const app = ref("");
const stream = ref("");

function closeSDK() {
  stopRealPlay(luserId.value).then(() => {
    proxy.$modal.msgSuccess("关闭播放");
  })
}

/**
 * SDK 播放
 * @param row
 */
function handleSDKPlay(row) {
  luserId.value = row.luserId;
  const loadingService = ElLoading.service({
    lock: true,
    fullscreen: true,
    text: 'java-cv 推流中...',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  startSDK(row.luserId).then(res => {
    // app.value = res.data.app;
    // stream.value = res.data.stream;
    // videoUrl.value = "rtsp://192.168.2.199:554/" + app.value + "/" + stream.value;
    setTimeout(() => {
      openPlaySDK.value = true;
      loadingService.close();
    }, 3000);
  });
}


/** 查询isup设备列表 */
function getList() {
  loading.value = true;
  queryParams.value.params = {};
  if (null != daterangeCreateTime && '' != daterangeCreateTime) {
    queryParams.value.params["beginCreateTime"] = daterangeCreateTime.value[0];
    queryParams.value.params["endCreateTime"] = daterangeCreateTime.value[1];
  }
  if (null != daterangeUpdateTime && '' != daterangeUpdateTime) {
    queryParams.value.params["beginUpdateTime"] = daterangeUpdateTime.value[0];
    queryParams.value.params["endUpdateTime"] = daterangeUpdateTime.value[1];
  }
  listIsupDevice(queryParams.value).then(response => {
    lsupDeviceList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

const togglePasswordVisibility = (id) => {
  passwordVisibility.value[id] = !passwordVisibility.value[id];
};

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
    name: null,
    channel: null,
    dwSize: null,
    dwNetUnitType: null,
    firmwareVersion: null,
    ipAddress: null,
    port: null,
    deviceRes: null,
    devType: null,
    manufacture: null,
    userName: null,
    password: null,
    deviceSerial: null,
    reliableTransmission: null,
    websocketTransmission: null,
    supportRedirect: null,
    devProtocolVersion: null,
    sessionKey: null,
    res: null,
    marketType: null,
    luserId: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null,
    addressMap: null,
    lng: null,
    lat: null,
    playType: '1',
    url: null,
    streamId: null,
    easyNTSUrl: null,
  };
  proxy.resetForm("lsupDeviceRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  daterangeCreateTime.value = [];
  daterangeUpdateTime.value = [];
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

function moreClick(command, itemData) {
  if (command === "handleMap") {
    handleMap(itemData)
  } else if (command === "handleDelete") {
    handleDelete(itemData)
  }
}


/**
 * 修改位置
 *
 * @param row
 */
function handleMap(row) {
  form.value = row;
  position.value = [form.value.lng, form.value.lat];
  toponym.value = form.value.addressMap;
  showMap.value = true;
  Create();
}

const Create = () => {
  MapContainer.value?.inGaDeMap();
};

const Destruction = () => {
  MapContainer.value?.Destruction();
};

const updateDialogMap = (value) => {
  form.value.addressMap = value.address + value.detailedStreet;
  form.value.lng = value.lng;
  form.value.lat = value.lat;
  position.value = [form.value.lng, form.value.lat];
  toponym.value = form.value.addressMap;
  updateLsupDevice(form.value).then(res => {
    showMap.value = false;
    Destruction();
    proxy.$modal.msgSuccess("操作成功");
  }).catch(() => {
    proxy.$modal.msgError("操作失败");
  })
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getLsupDevice(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改isup设备";
  });
}

async function handleStartPlay(row) {
  playType.value = row.playType;
  if (row.playType === '2') {
    ptzCameraQueryParams.value.id = row.id;
    const ans = await startPlay(row.streamId);
    streamInfo.value = ans.data;

    flvUrl.value = ans.data.flv;
    rtcUrl.value = ans.data.rtc;
    wsUrl.value = ans.data.ws_flv;
    openPlay.value = true;
  } else if (row.playType === '1') {
    videoUrl.value = row.url;
    ptzCameraQueryParams.value.id = row.id;
    openPlay.value = true;
  } else if (row.playType === '3') {
    videoUrl.value = row.easyNTSUrl;
    ptzCameraQueryParams.value.id = row.id;
    openPlay.value = true;
  }


}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["lsupDeviceRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateLsupDevice(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除isup设备编号为"' + _ids + '"的数据项？').then(function () {
    return delLsupDevice(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/**
 * 云台控制（开始）
 */
function ptzCtrlStartFun(direction) {
  ptzCtrlStart(ptzCameraQueryParams.value.id, direction, controSpeed.value)
}

/**
 * 云台控制（结束）
 */
function ptzCtrlEndFun() {
  ptzCtrlEnd(ptzCameraQueryParams.value.id)
}

/**
 * 聚焦
 */
function focusCamera() {
  ptzCtrlFocus(ptzCameraQueryParams.value.id, controSpeedFocus.value)
}

getList();
</script>

<style scoped>
::v-deep(.el-icon) {
  height: auto !important;
}

.control-wrapper {
  position: relative;
  width: 100px; /* 6.25rem * 16 = 100px */
  height: 100px; /* 6.25rem * 16 = 100px */
  max-width: 100px; /* 6.25rem * 16 = 100px */
  max-height: 100px; /* 6.25rem * 16 = 100px */
  border-radius: 100%;
  margin-top: 24px; /* 1.5rem * 16 = 24px */
  margin-left: 8px; /* 0.5rem * 16 = 8px */
  float: left;
}

.control-btn {
  display: flex;
  justify-content: center;
  position: absolute;
  width: 44%;
  height: 44%;
  border-radius: 5px;
  border: 1px solid #78aee4;
  box-sizing: border-box;
  transition: all 0.3s linear;
}


.control-btn:hover {
  cursor: pointer;
}

.control-btn .icon {
  width: 100%;
  font-size: 20px;
  color: #78aee4;
  display: flex;
  justify-content: center;
  align-items: center;
}

.control-btn .icon:hover {
  cursor: pointer;
}

.control-zoom-btn:hover {
  cursor: pointer;
}

.control-round {
  position: absolute;
  top: 21%;
  left: 21%;
  width: 58%;
  height: 58%;
  background: #fff;
  border-radius: 100%;
}

.control-round-inner {
  position: absolute;
  left: 13%;
  top: 13%;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 70%;
  height: 70%;
  font-size: 40px;
  color: #78aee4;
  border: 1px solid #78aee4;
  border-radius: 100%;
  transition: all 0.3s linear;
}

.control-inner-btn {
  position: absolute;
  width: 60%;
  height: 60%;
  background: #fafafa;
}

.control-top {
  top: -12px; /* -8% of 100px ≈ -12px */
  left: 27%;
  transform: rotate(-45deg);
  border-radius: 5px 100% 5px 0;
}

.control-top .icon {
  transform: rotate(45deg);
  border-radius: 5px 100% 5px 0;
}

.control-top .control-inner {
  left: -1px;
  bottom: 0;
  border-top: 1px solid #78aee4;
  border-right: 1px solid #78aee4;
  border-radius: 0 100% 0 0;
}

.control-top .fa {
  transform: rotate(45deg) translateY(-7px);
}

.control-left {
  top: 27%;
  left: -12px; /* -8% of 100px ≈ -12px */
  transform: rotate(45deg);
  border-radius: 5px 0 5px 100%;
}

.control-left .icon {
  transform: rotate(-45deg);
}

.control-left .control-inner {
  right: -1px;
  top: -1px;
  border-bottom: 1px solid #78aee4;
  border-left: 1px solid #78aee4;
  border-radius: 0 0 0 100%;
}

.control-left .fa {
  transform: rotate(-45deg) translateX(-7px);
}

.control-right {
  top: 27%;
  right: -12px; /* -8% of 100px ≈ -12px */
  transform: rotate(45deg);
  border-radius: 5px 100% 5px 0;
}

.control-right .icon {
  transform: rotate(-45deg);
}

.control-right .control-inner {
  left: -1px;
  bottom: -1px;
  border-top: 1px solid #78aee4;
  border-right: 1px solid #78aee4;
  border-radius: 0 100% 0 0;
}

.control-right .fa {
  transform: rotate(-45deg) translateX(7px);
}

.control-bottom {
  left: 27%;
  bottom: -12px; /* -8% of 100px ≈ -12px */
  transform: rotate(45deg);
  border-radius: 0 5px 100% 5px;
}

.control-bottom .icon {
  transform: rotate(-45deg);
}

.control-bottom .control-inner {
  top: -1px;
  left: -1px;
  border-bottom: 1px solid #78aee4;
  border-right: 1px solid #78aee4;
  border-radius: 0 0 100% 0;
}

.control-bottom .fa {
  transform: rotate(-45deg) translateY(7px);
}

.trank {
  width: 80%;
  height: 180px;
  text-align: left;
  padding: 0 10%;
  overflow: auto;
}

.trankInfo {
  width: 80%;
  padding: 0 10%;
}

.el-dialog__body {
  padding: 10px 20px;
}

.ptz-btn-box {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 0 32px; /* 2rem * 16 = 32px */
  height: 48px; /* 3rem * 16 = 48px */
  line-height: 64px; /* 4rem * 16 = 64px */
}
</style>
