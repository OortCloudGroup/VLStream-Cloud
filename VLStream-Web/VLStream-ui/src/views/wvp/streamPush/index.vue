<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关键字" prop="query">
        <el-input
            v-model="queryParams.query"
            placeholder="请输入关键字"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="流媒体" prop="query">
        <el-select @change="getPushList" style="width: 250px" v-model="queryParams.mediaServerId"
                   placeholder="请选择流媒体" default-first-option>
          <el-option
              v-for="item in mediaServerList"
              :key="item.id"
              :label="item.id"
              :value="item.id">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="推流状态" prop="query">
        <el-select @change="getPushList" style="width: 250px" v-model="queryParams.pushing"
                   placeholder="请选择推流状态" default-first-option>
          <el-option label="推流中" value="true"></el-option>
          <el-option label="已停止" value="false"></el-option>
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
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            v-hasPermi="['wvp:push:add']"
        >新增
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getPushList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="pushList" border :row-key="(row)=> row.app + row.stream">
      <el-table-column prop="gbName" label="名称" min-width="150" align="center"/>
      <el-table-column prop="app" label="应用名" min-width="100" align="center"/>
      <el-table-column prop="stream" label="流ID" min-width="100" align="center"/>
      <el-table-column label="推流状态" min-width="100" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.pushing">推流中</el-tag>
          <el-tag type="info" v-if="!scope.row.pushing">已停止</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="gbDeviceId" label="国标编码" min-width="150" align="center"/>
      <el-table-column label="位置信息" min-width="150" align="center">
        <template #default="scope">
          <span
              v-if="scope.row.gbLongitude && scope.row.gbLatitude">{{
              scope.row.gbLongitude
            }}<br/>{{ scope.row.gbLatitude }}</span>
          <span v-if="!scope.row.gbLongitude || !scope.row.gbLatitude">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="mediaServerId" label="流媒体" min-width="150" align="center"/>
      <el-table-column label="开始时间" min-width="200" align="center">
        <template #default="scope">
          <el-button-group v-if="scope.row.pushTime && scope.row.pushTime">
            {{ scope.row.pushTime == null ? "-" : scope.row.pushTime }}
          </el-button-group>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button @click="playPush(scope.row)" type="text" v-hasPermi="['wvp:push:play']">播放</el-button>
          <el-button type="text" @click="handleChannelConfiguration(scope.row)" v-hasPermi="['wvp:channel:edit']">
            通道配置
          </el-button>
          <el-button type="text" @click="handleEdit(scope.row)" v-hasPermi="['wvp:push:edit']">
            编辑
          </el-button>
          <el-button type="text" @click="handleDelete(scope.row)" v-hasPermi="['wvp:push:remove']">
            删除
          </el-button>
          <el-button type="text" @click="queryCloudRecords(scope.row)" v-hasPermi="['wvp:record:list']">云端录像
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getPushList"
    />

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="应用名" prop="app">
          <el-input v-model="form.app" placeholder="请输入应用名"></el-input>
        </el-form-item>
        <el-form-item label="流ID" prop="stream">
          <el-input v-model="form.stream" placeholder="请输入流ID"></el-input>
        </el-form-item>
        <el-form-item label="拉起离线推流">
          <el-checkbox v-model="form.startOfflinePush"></el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="编辑通道" v-model="openChannel" width="1000px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="名称">
              <el-input v-model="form.gbName" placeholder="请输入通道名称"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="编码">
              <el-input v-model="form.gbDeviceId" placeholder="请输入通道编码">
                <template v-slot:append>
                  <el-button @click="buildDeviceIdCode(form.gbDeviceId)">生成</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="设备厂商">
              <el-input v-model="form.gbManufacturer" placeholder="请输入设备厂商"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备型号">
              <el-input v-model="form.gbModel" placeholder="请输入设备型号"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="行政区域">
              <el-input v-model="form.gbCivilCode" placeholder="请输入行政区域">
                <template v-slot:append>
                  <el-button @click="chooseCivilCodeFun()">选择</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安装地址">
              <el-input v-model="form.gbAddress" placeholder="请输入安装地址"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="子设备">
              <el-select v-model="form.gbParental" style="width: 100%" placeholder="请选择是否有子设备">
                <el-option label="有" :value="1"></el-option>
                <el-option label="无" :value="0"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父节点编码">
              <el-input v-model="form.gbParentId" placeholder="请输入父节点编码或选择所属虚拟组织">
                <template v-slot:append>
                  <el-button @click="chooseGroupFun()">选择</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="设备状态">
              <el-select v-model="form.gbStatus" style="width: 100%" placeholder="请选择设备状态">
                <el-option label="在线" value="ON"></el-option>
                <el-option label="离线" value="OFF"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input v-model="form.gbLongitude" placeholder="请输入经度"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="纬度">
              <el-input v-model="form.gbLatitude" placeholder="请输入纬度"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="云台类型">
              <el-select v-model="form.gbPtzType" style="width: 100%" placeholder="请选择云台类型">
                <el-option label="球机" :value="1"></el-option>
                <el-option label="半球" :value="2"></el-option>
                <el-option label="固定枪机" :value="3"></el-option>
                <el-option label="遥控枪机" :value="4"></el-option>
                <el-option label="遥控半球" :value="5"></el-option>
                <el-option label="多目设备的全景/拼接通道" :value="6"></el-option>
                <el-option label="多目设备的分割通道" :value="7"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="警区">
              <el-input v-model="form.gbBlock" placeholder="请输入警区"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备归属">
              <el-input v-model="form.gbOwner" placeholder="请输入设备归属"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="信令安全模式">
              <el-select v-model="form.gbSafetyWay" style="width: 100%" placeholder="请选择信令安全模式">
                <el-option label="不采用" :value="0"></el-option>
                <el-option label="S/MIME签名" :value="2"></el-option>
                <el-option label="S/MIME加密签名同时采用" :value="3"></el-option>
                <el-option label="数字摘要" :value="4"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="注册方式">
              <el-select v-model="form.gbRegisterWay" style="width: 100%" placeholder="请选择注册方式">
                <el-option label="IETFRFC3261标准" :value="1"></el-option>
                <el-option label="基于口令的双向认证" :value="2"></el-option>
                <el-option label="基于数字证书的双向认证注册" :value="3"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="证书序列号">
              <el-input type="number" v-model="form.gbCertNum" placeholder="请输入证书序列号"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证书有效标识">
              <el-select v-model="form.gbCertifiable" style="width: 100%" placeholder="请选择证书有效标识">
                <el-option label="有效" :value="1"></el-option>
                <el-option label="无效" :value="0"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="无效原因码">
              <el-input type="errCode" v-model="form.gbCertNum" placeholder="请输入无效原因码"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证书终止有效期">
              <el-date-picker
                  v-model="form.gbEndTime"
                  type="datetime"
                  placeholder="选择日期时间"
                  style="width: 100%">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="保密属性">
              <el-select v-model="form.gbSecrecy" style="width: 100%" placeholder="请选择保密属性">
                <el-option label="不涉密" :value="0"></el-option>
                <el-option label="涉密" :value="1"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="IP地址">
              <el-input v-model="form.gbIpAddress" placeholder="请输入IP地址"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="端口">
              <el-input type="number" v-model="form.gbPort" placeholder="请输入端口"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备口令">
              <el-input v-model="form.gbPassword" placeholder="请输入设备口令"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="业务分组编号">
              <el-input v-model="form.gbBusinessGroupId" placeholder="请输入业务分组编号"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="位置类型">
              <el-select v-model="form.gbPositionType" style="width: 100%" placeholder="请选择位置类型">
                <el-option label="省际检查站" :value="1"></el-option>
                <el-option label="党政机关" :value="2"></el-option>
                <el-option label="车站码头" :value="3"></el-option>
                <el-option label="中心广场" :value="4"></el-option>
                <el-option label="体育场馆" :value="5"></el-option>
                <el-option label="商业中心" :value="6"></el-option>
                <el-option label="宗教场所" :value="7"></el-option>
                <el-option label="校园周边" :value="8"></el-option>
                <el-option label="治安复杂区域" :value="9"></el-option>
                <el-option label="交通干线" :value="10"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="室外/室内">
              <el-select v-model="form.gbRoomType" style="width: 100%" placeholder="请选择位置类型">
                <el-option label="室外" :value="1"></el-option>
                <el-option label="室内" :value="2"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用途">
              <el-select v-model="form.gbUseType" style="width: 100%" placeholder="请选择位置类型">
                <el-option label="治安" :value="1"></el-option>
                <el-option label="交通" :value="2"></el-option>
                <el-option label="重点" :value="3"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="补光">
              <el-select v-model="form.gbSupplyLightType" style="width: 100%" placeholder="请选择位置类型">
                <el-option label="无补光" :value="1"></el-option>
                <el-option label="红外补光" :value="2"></el-option>
                <el-option label="白光补光" :value="3"></el-option>
                <el-option label="激光补光" :value="4"></el-option>
                <el-option label="其他" :value="9"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="监视方位">
              <el-select v-model="form.gbDirectionType" style="width: 100%" placeholder="请选择位置类型">
                <el-option label="东(西向东)" :value="1"></el-option>
                <el-option label="西(东向西)" :value="2"></el-option>
                <el-option label="南(北向南)" :value="3"></el-option>
                <el-option label="北(南向北)" :value="4"></el-option>
                <el-option label="东南(西北到东南)" :value="5"></el-option>
                <el-option label="东北(西南到东北)" :value="6"></el-option>
                <el-option label="西南(东北到西南)" :value="7"></el-option>
                <el-option label="西北(东南到西北)" :value="8"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="分辨率">
              <el-input v-model="form.gbResolution" placeholder="请输入分辨率"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下载倍速">
              <el-select v-model="form.gbDownloadSpeedArray" multiple style="width: 100%" placeholder="请选择位置类型">
                <el-option label="1倍速" value="1"></el-option>
                <el-option label="2倍速" value="2"></el-option>
                <el-option label="4倍速" value="4"></el-option>
                <el-option label="8倍速" value="8"></el-option>
                <el-option label="16倍速" value="16"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="空域编码能力">
              <el-select v-model="form.gbSvcSpaceSupportMod" style="width: 100%" placeholder="请选择空域编码能力">
                <el-option label="1级增强" value="1"></el-option>
                <el-option label="2级增强" value="2"></el-option>
                <el-option label="3级增强" value="3"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时域编码能力">
              <el-select v-model="form.gbSvcTimeSupportMode" style="width: 100%" placeholder="请选择空域编码能力">
                <el-option label="1级增强" value="1"></el-option>
                <el-option label="2级增强" value="2"></el-option>
                <el-option label="3级增强" value="3"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFormChannel">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <ChannelCode ref="channelCode" @handleOk="handleOk"></ChannelCode>

    <ChooseCivilCode ref="chooseCivilCodeRef" @onSubmit="gbCivilCodeOnSubmit"></ChooseCivilCode>

    <ChooseGroup ref="chooseGroupRef" @onSubmit="gbParentOnSubmit"></ChooseGroup>

    <el-dialog title="播放视频" v-model="openView" width="1000px" append-to-body>
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
            <el-col :span="2"><span style="width: 80px; line-height: 40px; text-align: right;">播放地址：</span></el-col>
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
            <el-col :span="2"><StreamDropdown :stream-info="streamInfo"/></el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="编码信息" name="codec">
          <MediaInfo v-if="tabActiveName === 'codec'" ref="mediaInfo" :app="streamInfo.app" :stream="streamInfo.stream" :mediaServerId="streamInfo.mediaServerId"></MediaInfo>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

  </div>
</template>

<script setup name="StreamPush">
import ChannelCode from "../../components/common/channelCode.vue"
import ChooseCivilCode from "../../components/common/chooseCivilCode.vue"
import ChooseGroup from "../../components/dialog/chooseGroup.vue"
import {getOnlineMediaServerList} from "../../../api/wvp/wvpMediaServer.js";
import {addPush, listPush, removePush, start, updatePush} from "../../../api/wvp/push.js";
import {addChannelData, updateChannelData} from "../../../api/wvp/channel.js";
import {ElMessage} from "element-plus";
import Jessibuca from "@/components/jessibuca/index.vue";
import RtcPlayer from "@/components/rtcPlayer/index.vue";
import H265web from "@/components/H265web/index.vue";
import StreamDropdown from "@/views/wvp/channel/components/streamDropdown.vue";
import MediaInfo from "@/views/wvp/channel/components/mediaInfo.vue";
import {DocumentCopy, Microphone} from '@element-plus/icons-vue'
import useClipboard from "vue-clipboard3";
const { toClipboard } = useClipboard()

import router from "@/router";
const {proxy} = getCurrentInstance();

const pushList = ref([]);
const mediaServerList = ref([]);
const loading = ref(false);
const openView = ref(false);
const showVideoDialog = ref(false);
const hasAudio = ref(false);
const showSearch = ref(true);
const total = ref(0);
const title = ref("");
const rtcUrl = ref("");
const flvUrl = ref("");
const wsUrl = ref('');
const open = ref(false);
const openChannel = ref(false);
const channelCode = ref(null);
const chooseCivilCodeRef = ref(null);
const chooseGroupRef = ref(null);
const activeName = ref('flv');
const tabActiveName = ref('media');
const streamInfo = ref({});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    query: undefined,
    mediaServerId: undefined,
    pushing: undefined,
  },
  rules: {
    app: [{required: true, message: "请输入应用名", trigger: "blur"}],
    stream: [{required: true, message: "请输入流ID", trigger: "blur"}],
  }
});
const videoError = (e) => {
  console.log("播放器错误：" + JSON.stringify(e));
}

const {queryParams, form, rules} = toRefs(data);

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

async function playPush(row) {
  try {
    const ans = await start({id: row.id});
    streamInfo.value = ans.data;
    if (location.protocol === "https:") {
      flvUrl.value = ans.data.https_flv;
      rtcUrl.value = ans.data.rtcs;
      wsUrl.value = ans.data.wss_flv;
    } else {
      flvUrl.value = ans.data.flv;
      rtcUrl.value = ans.data.rtc;
      wsUrl.value = ans.data.ws_flv;
    }
    openView.value = true;
  } catch (e) {
    console.log(e)
  }
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getPushList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 表单重置 */
function reset() {
  form.value = {
    app: undefined,
    stream: undefined,
    startOfflinePush: undefined,
    gbName: undefined,
    gbDeviceId: undefined,
    gbManufacturer: undefined,
    gbModel: undefined,
    gbCivilCode: undefined,
    gbAddress: undefined,
    gbParental: undefined,
    gbParentId: undefined,
    gbStatus: undefined,
    gbLongitude: undefined,
    gbLatitude: undefined,
    gbPtzType: undefined,
    gbBlock: undefined,
    gbOwner: undefined,
    gbSafetyWay: undefined,
    gbRegisterWay: undefined,
    gbCertNum: undefined,
    gbCertifiable: undefined,
    gbEndTime: undefined,
    gbSecrecy: undefined,
    gbIpAddress: undefined,
    gbPort: undefined,
    gbPassword: undefined,
    gbBusinessGroupId: undefined,
    gbPositionType: undefined,
    gbRoomType: undefined,
    gbUseType: undefined,
    gbSupplyLightType: undefined,
    gbDirectionType: undefined,
    gbResolution: undefined,
    gbDownloadSpeedArray: undefined,
    gbSvcSpaceSupportMod: undefined,
    gbSvcTimeSupportMode: undefined,
  };
  proxy.resetForm("formRef");
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  openChannel.value = false;
  reset();
}

function initData() {
  getOnlineMediaServerList().then((res) => {
    mediaServerList.value = res.data;
  })
  getPushList();
}

function getPushList() {
  loading.value = true;

  listPush(queryParams.value).then((res) => {
    total.value = res.total;
    res.rows.forEach(e => {
      e.location = ''
      if (e.gbLongitude && e.gbLatitude) {
        e.location = e.gbLongitude + "," + e.gbLatitude;
      }
    });
    pushList.value = res.rows;
    loading.value = false;
  })
}

function handleAdd() {
  reset()
  open.value = true;
  title.value = "新增推流";
}

function handleEdit(row) {
  reset()
  open.value = true;
  title.value = "修改推流";
  form.value = JSON.parse(JSON.stringify(row))
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除该推流？').then(function () {
    return removePush(row.id)
  }).then(() => {
    getPushList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/** 编辑通道按钮 */
function submitFormChannel() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      if (form.value.gbDownloadSpeedArray) {
        form.value.gbDownloadSpeed = form.value.gbDownloadSpeedArray.join("/")
      }
      if (form.value.gbId) {
        updateChannelData(form.value).then(() => {
          ElMessage({
            type: 'success',
            message: '保存成功',
          })
          openChannel.value = false
          getPushList()
        })
      } else {
        addChannelData(form.value).then(() => {
          ElMessage({
            type: 'success',
            message: '保存成功',
          })
          openChannel.value = false
          getPushList()
        })
      }
    }
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updatePush(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getPushList()
        })
      } else {
        addPush(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getPushList()
        })
      }
    }
  });
}

function handleChannelConfiguration(row) {
  openChannel.value = true
  form.value = JSON.parse(JSON.stringify(row))
}

function buildDeviceIdCode(deviceId) {
  channelCode.value.openDialog(code => {

  }, deviceId);
}

function handleOk(code) {
  form.value.gbDeviceId = code
}

function chooseCivilCodeFun() {
  chooseCivilCodeRef.value.openDialog(code => {

  });
}

function gbCivilCodeOnSubmit(data) {
  form.value.gbCivilCode = data;
}

function chooseGroupFun() {
  chooseGroupRef.value.openDialog((deviceId, businessGroupId) => {

  });
}

function gbParentOnSubmit(deviceId, businessGroupId) {
  form.value.gbBusinessGroupId = businessGroupId;
  form.value.gbParentId = deviceId;
}

function queryCloudRecords(row) {
  router.push({
    path: `/channel/cloudRecordDetail/index`,
    query: {
      app: row.app,
      streamId: row.stream,
    }
  });
}

onMounted(() => {
  initData();
})
</script>

<style scoped>
.example-showcase .el-dropdown-link {
  cursor: pointer;
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
}

.player {
  width: 100%;
  height: 450px;
}

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

.control-panel {
  position: relative;
  top: 0;
  left: 80px; /* 5rem * 16 = 80px */
  height: 176px; /* 11rem * 16 = 176px */
  max-height: 176px; /* 11rem * 16 = 176px */
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

