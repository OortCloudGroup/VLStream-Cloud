<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关键字" prop="searchSrt">
        <el-input
            v-model="queryParams.searchSrt"
            placeholder="请输入设备名称"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="通道类型" prop="channelType">
        <el-select v-model="queryParams.channelType" placeholder="请选择通道类型" style="width: 250px;"
                   default-first-option>
          <el-option label="设备" value="false"></el-option>
          <el-option label="子目录" value="true"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="在线状态" prop="online">
        <el-select v-model="queryParams.online" placeholder="请选择在线状态" style="width: 250px;"
                   default-first-option>
          <el-option label="在线" value="true"></el-option>
          <el-option label="离线" value="false"></el-option>
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
            icon="Back"
            @click="handleBack"
        >返回</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="initData"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="channelList" ref="channelListTable" border>
      <el-table-column prop="name" label="名称" min-width="180" align="center"/>
      <el-table-column prop="deviceId" label="编号" min-width="180" align="center"/>
      <el-table-column label="快照" min-width="100" align="center">
        <template #default="scope">
          <ImagePreview :src="getSnap(scope.row)"></ImagePreview>
        </template>
      </el-table-column>
      <el-table-column prop="subCount" label="子节点数" min-width="100" align="center"/>
      <el-table-column prop="channelType" label="通道类型" min-width="100" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.channelType === 0">国标设备</el-tag>
          <el-tag v-if="scope.row.channelType === 1">推流设备</el-tag>
          <el-tag v-if="scope.row.channelType === 2">拉流代理</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="manufacturer" label="厂家" min-width="100" align="center"/>
      <el-table-column label="位置信息" min-width="120" align="center">
        <template #default="scope">
          <span
              v-if="scope.row.longitude && scope.row.latitude">{{ scope.row.longitude }}<br/>{{
              scope.row.latitude
            }}</span>
          <span v-if="!scope.row.longitude || !scope.row.latitude">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="ptzType" label="云台类型" min-width="100" align="center">
        <template #default="scope">
          <div>{{ scope.row.ptzTypeText }}</div>
        </template>
      </el-table-column>
      <el-table-column label="开启音频" min-width="100" align="center">
        <template #default="scope">
          <el-switch @change="updateChannel(scope.row)" v-model="scope.row.hasAudio" active-color="#409EFF">
          </el-switch>
        </template>
      </el-table-column>
      <el-table-column label="码流类型" min-width="180" align="center">
        <template #default="scope">
          <div v-if="checkPermi(['wvp:device:channelStreamIdentification'])">
            <el-select @change="channelSubStreamChange(scope.row)"
                       v-model="scope.row.streamIdentification"
                       placeholder="请选择码流类型" default-first-option

            >
              <el-option label="stream:0(主码流)" value="stream:0"></el-option>
              <el-option label="stream:1(子码流)" value="stream:1"></el-option>
              <el-option label="streamnumber:0(主码流-2022)" value="streamnumber:0"></el-option>
              <el-option label="streamnumber:1(子码流-2022)" value="streamnumber:1"></el-option>
              <el-option label="streamprofile:0(主码流-大华)" value="streamprofile:0"></el-option>
              <el-option label="streamprofile:1(子码流-大华)" value="streamprofile:1"></el-option>
              <el-option label="streamMode:main(主码流-水星+TP-LINK)" value="streamMode:main"></el-option>
              <el-option label="streamMode:sub(子码流-水星+TP-LINK)" value="streamMode:sub"></el-option>
            </el-select>
          </div>
          <div v-else>
            <el-tag v-if="scope.row.streamIdentification === 'stream:0'">stream:0(主码流)</el-tag>
            <el-tag v-if="scope.row.streamIdentification === 'stream:1'">stream:1(子码流)</el-tag>
            <el-tag v-if="scope.row.streamIdentification === 'streamnumber:0'">streamnumber:0(主码流-2022)</el-tag>
            <el-tag v-if="scope.row.streamIdentification === 'streamnumber:1'">streamnumber:1(子码流-2022)</el-tag>
            <el-tag v-if="scope.row.streamIdentification === 'streamprofile:0'">streamprofile:0(主码流-大华)</el-tag>
            <el-tag v-if="scope.row.streamIdentification === 'streamprofile:1'">streamprofile:1(子码流-大华)</el-tag>
            <el-tag v-if="scope.row.streamIdentification === 'streamMode:main'">streamMode:main(主码流-水星+TP-LINK)
            </el-tag>
            <el-tag v-if="scope.row.streamIdentification === 'streamMode:sub'">streamMode:sub(子码流-水星+TP-LINK)
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="100" align="center">
        <template #default="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag v-if="scope.row.status === 'ON'">在线</el-tag>
            <el-tag type="info" v-if="scope.row.status !== 'ON'">离线</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button v-bind:disabled="device == null || device.online === 0"
                     v-if="checkPermi(['wvp:play:start'])"
                     type="text" @click="start(scope.row)">播放
          </el-button>
          <el-button v-bind:disabled="device == null || device.online === 0"
                     v-hasPermi="['wvp:play:stop']"
                     type="text" style="color: #f56c6c" v-if="!!scope.row.streamId"
                     @click="stopDevicePush(scope.row)">停止
          </el-button>
          <el-button
              type="text"
              @click="handleEdit(scope.row)"
              v-hasPermi="['wvp:channel:edit']"
          >
            编辑
          </el-button>

          <el-dropdown @command="(command)=>{moreClick(command, scope.row)}"
                       v-if="checkPermi(['wvp:control:recordApi'])">
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
                <el-dropdown-item command="records" v-bind:disabled="device == null || device.online === 0">
                  设备录像
                </el-dropdown-item>
                <el-dropdown-item command="cloudRecords" v-bind:disabled="device == null || device.online === 0"
                                  v-if="checkPermi(['wvp:record:list'])">
                  云端录像
                </el-dropdown-item>
                <el-dropdown-item command="record" v-bind:disabled="device == null || device.online === 0"
                                  v-if="checkPermi(['wvp:control:recordApi'])">
                  设备录像控制-开始
                </el-dropdown-item>
                <el-dropdown-item command="stopRecord" v-bind:disabled="device == null || device.online === 0"
                                  v-if="checkPermi(['wvp:control:recordApi'])">
                  设备录像控制-停止
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>

    </el-table>

    <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="initData"
    />

    <el-dialog title="编辑通道" v-model="open" width="1000px" append-to-body>
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
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
          <el-button v-if="form.dataType === 1" @click="resetData">重置</el-button>
        </div>
      </template>
    </el-dialog>

    <ChannelCode ref="channelCode" @handleOk="handleOk"></ChannelCode>

    <ChooseCivilCode ref="chooseCivilCodeRef" @onSubmit="gbCivilCodeOnSubmit"></ChooseCivilCode>

    <ChooseGroup ref="chooseGroupRef" @onSubmit="gbParentOnSubmit"></ChooseGroup>

    <el-dialog title="播放视频" v-model="openPlay" width="1000px" append-to-body @close="initData">
      <el-tabs v-model="activeName" type="card" :stretch="true">
        <el-tab-pane label="flv播放" name="flv">
          <el-row>
            <el-col :span="24">
              <div class="player" v-if="activeName === 'flv'">
                <Jessibuca v-if="activeName === 'flv'" ref="flv" :visible.sync="showVideoDialog"
                           :videoUrl="vUrl" :error="videoError" :message="videoError" height="100px"
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
              <el-input v-model="vUrl" :disabled="true" v-show="activeName === 'flv'">
                <template #prepend>flv地址</template>
                <template #append>
                  <el-button type="primary" :icon="DocumentCopy" @click="copyToClipboard(vUrl)"/>
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
        <el-tab-pane label="云台控制" name="control">
          <div style="display: grid; grid-template-columns: 240px auto; height: 180px; overflow: auto">
            <!-- 左侧控制区域 -->
            <div style="display: grid; grid-template-columns: 100px auto;">
              <!-- 方向控制 -->
              <div class="control-wrapper">
                <div class="control-btn control-top" @mousedown="ptzCamera('up')" @mouseup="ptzCamera('stop')">
                  <el-icon class="icon"><CaretTop /></el-icon>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-left" @mousedown="ptzCamera('left')" @mouseup="ptzCamera('stop')">
                  <el-icon class="icon"><CaretLeft /></el-icon>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-bottom" @mousedown="ptzCamera('down')" @mouseup="ptzCamera('stop')">
                  <el-icon class="icon"><CaretBottom /></el-icon>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-right" @mousedown="ptzCamera('right')" @mouseup="ptzCamera('stop')">
                  <el-icon class="icon"><CaretRight /></el-icon>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-round">
                  <div class="control-round-inner"><i class="fa fa-pause-circle"></i></div>
                </div>
                <!-- 速度控制 -->
                <div class="contro-speed" style="position: absolute; left: 4px; top: 112px; width: 100px;">
                  <el-slider v-model="controSpeed" :min="1"></el-slider>
                </div>
              </div>
              <!-- 变倍、聚焦、光圈控制 -->
              <div>
                <div class="ptz-btn-box">
                  <div @mousedown="ptzCamera('zoomin')" @mouseup="ptzCamera('stop')" title="变倍+">
                    <el-icon class="control-zoom-btn" style="font-size: 24px;"><ZoomIn /></el-icon>
                  </div>
                  <div @mousedown="ptzCamera('zoomout')" @mouseup="ptzCamera('stop')" title="变倍-">
                    <el-icon class="control-zoom-btn" style="font-size: 24px;"><ZoomOut /></el-icon>
                  </div>
                </div>
                <div class="ptz-btn-box">
                  <div @mousedown="focusCamera('near')" @mouseup="focusCamera('stop')" title="聚焦+">
                    <i class="iconfont icon-bianjiao-fangda control-zoom-btn" style="font-size: 24px;"></i>
                  </div>
                  <div @mousedown="focusCamera('far')" @mouseup="focusCamera('stop')" title="聚焦-">
                    <i class="iconfont icon-bianjiao-suoxiao control-zoom-btn" style="font-size: 24px;"></i>
                  </div>
                </div>
                <div class="ptz-btn-box">
                  <div @mousedown="irisCamera('in')" @mouseup="irisCamera('stop')" title="光圈+">
                    <i class="iconfont icon-guangquan control-zoom-btn" style="font-size: 24px;"></i>
                  </div>
                  <div @mousedown="irisCamera('out')" @mouseup="irisCamera('stop')" title="光圈-">
                    <i class="iconfont icon-guangquan- control-zoom-btn" style="font-size: 24px;"></i>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧功能选择区域 -->
            <div style="text-align: left">
              <el-select
                  v-model="ptzMethod"
                  style="width: 100%"
                  placeholder="请选择云台功能"
              >
                <el-option label="预置点" value="preset"></el-option>
                <el-option label="巡航组" value="cruise"></el-option>
                <el-option label="自动扫描" value="scan"></el-option>
                <el-option label="雨刷" value="wiper"></el-option>
                <el-option label="辅助开关" value="switch"></el-option>
              </el-select>


              <PtzPreset :channel-device-id="channelId" :device-id="deviceId" v-if="ptzMethod === 'preset'" style="margin-top: 16px"></PtzPreset>
              <PtzCruising :channel-device-id="channelId" :device-id="deviceId" v-if="ptzMethod === 'cruise'" style="margin-top: 16px"></PtzCruising>
              <PtzScan :channel-device-id="channelId" :device-id="deviceId" v-if="ptzMethod === 'scan'" style="margin-top: 16px"></PtzScan>
              <PtzWiper :channel-device-id="channelId" :device-id="deviceId" v-if="ptzMethod === 'wiper'" style="margin-top: 16px"></PtzWiper>
              <PtzSwitch :channel-device-id="channelId" :device-id="deviceId" v-if="ptzMethod === 'switch'" style="margin-top: 16px"></PtzSwitch>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="语音对讲" name="broadcast">
          <div style="padding: 0 10px">
            <el-radio-group v-model="broadcastMode" :disabled="broadcastStatus !== -1">
              <el-radio :label="true" >喊话(Broadcast)</el-radio>
              <el-radio :label="false" >对讲(Talk)</el-radio>
            </el-radio-group>
          </div>

          <div class="trank" style="text-align: center;">
            <el-button @click="broadcastStatusClick()" :type="getBroadcastStatus()" :disabled="broadcastStatus === -2"
                       circle :icon="Microphone" style="font-size: 32px; padding: 24px;margin-top: 24px;"/>
            <p>
              <span v-if="broadcastStatus === -2">正在释放资源</span>
              <span v-if="broadcastStatus === -1">点击开始对讲</span>
              <span v-if="broadcastStatus === 0">等待接通中...</span>
              <span v-if="broadcastStatus === 1">请说话</span>
            </p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup name="Channel">
import useClipboard from 'vue-clipboard3'
import {checkPermi} from "@/utils/wvpPermission";
import { CaretTop } from '@element-plus/icons-vue'
import ChannelCode from "../../components/common/channelCode.vue"
import ChooseCivilCode from "../../components/common/chooseCivilCode.vue"
import ChooseGroup from "../../components/dialog/chooseGroup.vue"
import CusPlayer from "@/components/flv/CusPlayer.vue";
import Jessibuca from "@/components/jessibuca/index.vue";
import RtcPlayer from "@/components/rtcPlayer/index.vue";
import H265web from "@/components/H265web/index.vue";
import StreamDropdown from "./components/streamDropdown.vue";
import MediaInfo from "./components/mediaInfo.vue";
import PtzPreset from "./components/ptzPreset.vue";
import PtzCruising from "./components/ptzCruising.vue";
import PtzScan from "./components/ptzScan.vue";
import PtzWiper from "./components/ptzWiper.vue";
import PtzSwitch from "./components/ptzSwitch.vue";
import {playStop} from "../../../api/wvp/play.js";
import {ElMessage, ElMessageBox} from 'element-plus'
import {DocumentCopy, Microphone} from '@element-plus/icons-vue'
import {
  changeAudio,
  getDeviceById,
  listDeviceChannel,
  subChannels,
  updateChannelStreamIdentification
} from "../../../api/wvp/device.js";
import {
  GetBroadcast,
  getCommonChannel, getFocusCamera, getIrIsCamera,
  getPtzCamera,
  resetChannel,
  sendDevicePush,
  updateChannelData
} from "../../../api/wvp/channel.js";
import {recordApi} from "../../../api/wvp/control.js";
import router from "@/router";
import {useRoute} from "vue-router";
import CryptoJS from 'crypto-js';
import {ZLMRTCClient} from '@/components/rtcPlayer/js/ZLMRTCClient';
const { toClipboard } = useClipboard()

const route = useRoute();
const {proxy} = getCurrentInstance();
const channelList = ref([]);
const loading = ref(true);
const total = ref(0);
const deviceId = ref('');
const parentChannelId = ref('');
const rtcUrl = ref('');
const wsUrl = ref('');
const activeName = ref('flv');
const tabActiveName = ref('media');
const device = ref({});
const showTree = ref(false);
const open = ref(false);
const openPlay = ref(false);
const deviceChannelList = ref([])
const showSearch = ref(true);
const loadSnap = ref({});
const channelListTable = ref(null);
const channelCode = ref(null);
const chooseCivilCodeRef = ref(null);
const chooseGroupRef = ref(null);
const broadcastRtc = ref(null);
const video = ref(null);
const vUrl = ref(null);
const showVideoDialog = ref(false);
const hasAudio = ref(false);
const streamInfo = ref({});
const controSpeed = ref(30);
const channelId = ref('');
const ptzMethod = ref('preset');
const broadcastMode = ref(true);
const broadcastStatus = ref(-1); // -2 正在释放资源 -1 默认状态 0 等待接通 1 接通成功

const broadcastStatusClick = async () => {
  if (broadcastStatus.value == -1){
    broadcastStatus.value = 0;
    const url = {
      deviceId: deviceId.value,
      channelId: channelId.value,
    }
    await GetBroadcast(url, {timeout: 30, broadcastMode: broadcastMode.value}).then((res) => {
      let streamInfo = res.streamInfo
      if (document.location.protocol.includes("https")) {
        startBroadcast(streamInfo.rtcs);
      } else {
        startBroadcast(streamInfo.rtc);
      }
    }).catch(() => {
      ElMessage({
        showClose: true,
        message: '网络开小差了',
        type: 'error',
      });
    })
  } else if (broadcastStatus.value === 1) {
    broadcastStatus.value = -1;
    broadcastRtc.value.close()
  }
}

const startBroadcast = async (url) => {
  const pushKey = "3e80d1762a324d5b0ff636e0bd16f1e3";
  const sign = CryptoJS.MD5(pushKey).toString(CryptoJS.enc.Hex);
  url += "&sign=" + sign;
  console.log("开始语音喊话： " + url)
  broadcastRtc.value = new ZLMRTCClient.Endpoint({
    debug: true,
    zlmsdpUrl: url,
    simulecast: false,
    useCamera: true,
    audioEnable: true,
    videoEnable: false,
    recvOnly: false,
  })

  broadcastRtc.value.on(ZLMRTCClient.Events.WEBRTC_NOT_SUPPORT, (e) => {
    console.error('offer anwser 交换失败', e);
    ElMessage({
      showClose: true,
      message: 'offer anwser 交换失败' + e,
      type: 'error',
    });
    broadcastStatus.value = -1;
  })

  broadcastRtc.value.on(ZLMRTCClient.Events.WEBRTC_ON_CONNECTION_STATE_CHANGE, (e) => {
    console.log('状态改变', e)
    if (e === "connecting") {
      broadcastStatus.value = 0;
    } else if (e === "connected") {
      broadcastStatus.value = 1;
    } else if (e === "disconnected") {
      broadcastStatus.value = -1;
    }
  });

  broadcastRtc.value.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, (e) => {
    console.error('ICE 协商出错')
    ElMessage({
      showClose: true,
      message: 'ICE 协商出错',
      type: 'error'
    });
    broadcastStatus.value = -1;
  });

  broadcastRtc.value.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, (e) => {
    console.error('offer anwser 交换失败', e)
    ElMessage({
      showClose: true,
      message: 'offer anwser 交换失败' + e,
      type: 'error'
    });
    broadcastStatus.value = -1;
  });

  broadcastRtc.value.on(ZLMRTCClient.Events.WEBRTC_ON_CONNECTION_STATE_CHANGE, (e) => {
    console.log('状态改变', e)
    if (e === "connecting") {
      broadcastStatus.value = 0;
    } else if (e === "connected") {
      broadcastStatus.value = 1;
    } else if (e === "disconnected") {
      broadcastStatus.value = -1;
    }
  });

  broadcastRtc.value.on(ZLMRTCClient.Events.CAPTURE_STREAM_FAILED, (e) => {
    console.log('捕获流失败', e)
    ElMessage({
      showClose: true,
      message: '捕获流失败' + e,
      type: 'error'
    });
    broadcastStatus.value = -1;
  });

}

const getBroadcastStatus = () => {
  if (broadcastStatus.value == -2) {
    return "primary"
  }
  if (broadcastStatus.value == -1) {
    return "primary"
  }
  if (broadcastStatus.value == 0) {
    return "warning"
  }
  if (broadcastStatus.value == 1) {
    return "danger"
  }
}

const ptzCamera = async (command) => {
  const url = {
    deviceId: deviceId.value,
    channelId: channelId.value,
  }
  const params = {
    command: command,
    horizonSpeed: parseInt(controSpeed.value * 255/100),
    verticalSpeed: parseInt(controSpeed.value * 255/100),
    zoomSpeed: parseInt(controSpeed.value * 16/100),
  }
  await getPtzCamera(url, params);
}

const focusCamera = async (command) => {
  const url = {
    deviceId: deviceId.value,
    channelId: channelId.value,
  }
  const params = {
    command: command,
    speed: parseInt(controSpeed.value * 255/100),
  }
  await getFocusCamera(url, params);
  ElMessage.success('操作成功！');
}

const irisCamera = async (command) => {
  const url = {
    deviceId: deviceId.value,
    channelId: channelId.value,
  }
  const params = {
    command: command,
    speed: parseInt(controSpeed.value * 255/100),
  }
  await getIrIsCamera(url, params);
  ElMessage.success('操作成功！');
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

const videoError = (e) => {
  console.log("播放器错误：" + JSON.stringify(e));
}

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    searchSrt: undefined,
    channelType: undefined,
    online: undefined,
  },
  rules: {}
});

const {queryParams, form, rules} = toRefs(data);

async function start(itemData) {
  const params = {
    deviceId: deviceId.value,
    channelId: itemData.deviceId
  }
  channelId.value = itemData.deviceId;
  const res = await sendDevicePush(params);
  streamInfo.value = res.data;
  if (location.protocol === "https:") {
    vUrl.value = res.data.https_flv;
    rtcUrl.value = res.data.rtcs;
    wsUrl.value = res.data.wss_flv;
  } else {
    vUrl.value = res.data.flv;
    rtcUrl.value = res.data.rtc;
    wsUrl.value = res.data.ws_flv;
  }
  openPlay.value = true;
}

function paly() {
  video.value.createPlayer(vUrl.value, 0)
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  initData();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 表单重置 */
function reset() {
  form.value = {
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

/** 提交按钮 */
function submitForm() {
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
          open.value = false
        })
      }
    }
  });
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
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

/**
 * 初始化数据
 */
function initData() {
  if (typeof (parentChannelId.value) == "undefined" || parentChannelId.value === '0') {
    getDeviceChannelList();
  } else {
    showSubchannels();
  }
}

/**
 * 显示子通道
 */
function showSubchannels() {
  if (!showTree.value) {
    subChannels({
      pageNum: queryParams.value.pageNum,
      pageSize: queryParams.value.pageSize,
      query: queryParams.value.searchSrt,
      online: queryParams.value.online,
      channelType: queryParams.value.channelType
    }).then((res) => {
      total.value = res.total;
      deviceChannelList.value = res.rows;
      deviceChannelList.value.forEach(e => {
        e.ptzType = e.ptzType + "";
      });

      // 防止出现表格错位
      nextTick(() => {
        channelListTable.value.doLayout();
      })
    })
  } else {

  }
}

/** 获取设备通道列表 */
function getDeviceChannelList() {
  loading.value = true;
  listDeviceChannel(
      {
        pageNum: queryParams.value.pageNum,
        pageSize: queryParams.value.pageSize,
        deviceId: deviceId.value,
        query: queryParams.value.searchSrt,
        online: queryParams.value.online,
        channelType: queryParams.value.channelType,
      }
  ).then(response => {
    channelList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  })
}

function getSnap(row) {
  return '/api/device/query/snap/' + deviceId.value + '/' + row.deviceId;
}

/**
 * 开启音频
 *
 * @param row
 */
function updateChannel(row) {
  changeAudio({
    channelId: row.id,
    audio: row.hasAudio
  });
}

/**
 * 码流类型
 *
 * @param row
 */
function channelSubStreamChange(row) {
  updateChannelStreamIdentification({
    deviceDbId: row.deviceDbId,
    id: row.id,
    streamIdentification: row.streamIdentification
  });
}

/**
 * 停止设备推流
 *
 * @param itemData
 */
function stopDevicePush(itemData) {
  playStop(deviceId.value, itemData.deviceId).then(() => {
    initData();
    ElMessage({
      type: 'success',
      message: '停止成功',
    })
  }).catch(function (error) {
    initData();
  });
}

function resetData() {
  ElMessageBox.confirm(
      '确定重置为默认内容?',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(() => {
        resetChannel(form.value.gbId).then((res) => {
          ElMessage({
            type: 'success',
            message: '重置成功 已保存',
          })
        })
        getCommonChannelFun(form.value.gbId)
      })
      .catch(() => {

      })
}

// 编辑
function handleEdit(row) {
  getCommonChannelFun(row.id)
}

function getCommonChannelFun(id) {
  getCommonChannel(id).then((res) => {
    if (res.data.gbDownloadSpeed) {
      res.data.gbDownloadSpeedArray = res.data.gbDownloadSpeed.split("/")
    }
    form.value = res.data;
    open.value = true
  })
}

function moreClick(command, itemData) {
  if (command === "records") {
    queryRecords(itemData)
  } else if (command === "cloudRecords") {
    queryCloudRecords(itemData)
  } else if (command === "record") {
    startRecord(itemData)
  } else if (command === "stopRecord") {
    stopRecord(itemData)
  }
}

function queryRecords(itemData) {
  router.push(`/channel/gbRecordDetail/index/${deviceId.value}/${itemData.deviceId}`);
}

function queryCloudRecords(itemData) {
  router.push({
    path: "/channel/cloudRecordDetail/index",
    query: {
      app: "rtp",
      streamId: deviceId.value+'_'+itemData.deviceId,
    }
  });
}

function startRecord(itemData) {
  recordApi({
    deviceId: deviceId.value,
    recordCmdStr: "Record",
    channelId: itemData.deviceId,
  }).then(() => {
    ElMessage({
      type: 'success',
      message: '开始录像成功',
    })
  })
}

function stopRecord(itemData) {
  recordApi({
    deviceId: deviceId.value,
    recordCmdStr: "StopRecord",
    channelId: itemData.deviceId,
  }).then(() => {
    ElMessage({
      type: 'success',
      message: '停止录像成功',
    })
  })
}

function handleBack(){
  proxy.$tab.closeOpenPage({path: "/gbmanger/device"});
}

onMounted(() => {
  deviceId.value = route.params && route.params.deviceId;
  parentChannelId.value = route.params && route.params.parentChannelId;
  if (deviceId.value) {
    getDeviceById(deviceId.value).then(response => {
      device.value = response.data;
    })
  }
  initData()
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
