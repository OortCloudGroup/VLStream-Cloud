<template>
  <DeviceClassificationLayout protocol-type="ONVIF" :selected-device-keys="classificationDeviceKeys" @filter-change="handleClassificationFilter" @assigned="getList">
  <div class="app-container">
    <el-alert title="ONVIF协议 16的设备可以使用Digest/WS,2.20版本使用WS" type="success" style="margin-bottom: 10px;" />
    <el-card>
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
        <el-form-item label="ip" prop="ip">
          <el-input
              v-model="queryParams.ip"
              placeholder="请输入ip"
              clearable
              @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input
              v-model="queryParams.name"
              placeholder="请输入名称"
              clearable
              @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="设备厂商" prop="firm">
          <el-input
              v-model="queryParams.firm"
              placeholder="请输入设备厂商"
              clearable
              @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="m-1">
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
              type="primary"
              plain
              icon="Search"
              @click="handleWSDiscovery"
              v-hasPermi="['onvif:device:WSDiscovery']"
          >发现设备
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
              type="success"
              plain
              icon="Edit"
              :disabled="single"
              @click="handleUpdate"
              v-hasPermi="['onvif:device:edit']"
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
              v-hasPermi="['onvif:device:remove']"
          >删除
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
              type="warning"
              plain
              icon="Download"
              @click="handleExport"
              v-hasPermi="['onvif:device:export']"
          >导出
          </el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="deviceList" @selection-change="handleSelectionChange" border>
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column label="名称" align="center" prop="name"/>
        <el-table-column label="ip" align="center" prop="ip"/>
        <el-table-column label="地址" align="center" prop="addressMap"/>
        <el-table-column label="设备厂商" align="center" prop="firm"/>
        <el-table-column label="设备型号" align="center" prop="model"/>
        <el-table-column label="用户名" align="center" prop="userName" width="100"/>
        <el-table-column label="密码" align="center" prop="password" width="150">
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
        <el-table-column label="播放类型" align="center" prop="playType">
          <template #default="scope">
            <el-tag type="primary" v-if="scope.row.playType === '1'">本地</el-tag>
            <el-tag type="primary" v-if="scope.row.playType === '2'">推流</el-tag>
            <el-tag type="primary" v-if="scope.row.playType === '3'">EasyNTS</el-tag>
          </template>
        </el-table-column>
        <el-table-column key="streamId" label="流id" prop="streamId" min-width="150" align="center"/>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="250">
          <template #default="scope">
            <div style="display:flex; align-items: center;justify-content: center">
              <el-button link type="primary" icon="View" @click="handleView(scope.row)"
                         v-hasPermi="['onvif:device:play']">播放
              </el-button>

              <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                         v-hasPermi="['onvif:device:edit']">修改
              </el-button>
              <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
                         v-hasPermi="['onvif:device:remove']">删除
              </el-button>

              <el-dropdown @command="(command)=>{moreClick(command, scope.row)}"
                           v-if="checkPermi(['onvif:device:edit'])">
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
                    <el-dropdown-item command="viewUrls">全部地址</el-dropdown-item>
                    <el-dropdown-item command="handleMap" v-if="checkPermi(['onvif:device:edit'])">修改位置
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
    </el-card>

    <!-- 修改onvif 设备对话框 -->
    <el-dialog :title="title" v-model="open" width="1000px" append-to-body>
      <el-form ref="deviceRef" :model="form" :rules="rules" label-width="120px">

        <el-row :gutter="20">
          <el-col :span="12">
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="userName">
              <el-input v-model="form.name" placeholder="请输入设备名称"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="ip" prop="ip">
              <el-input v-model="form.ip" placeholder="请输入ip"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入用户名"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" placeholder="请输入密码"/>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="固件版本" prop="firmwareVersion">
              <el-input v-model="form.firmwareVersion" placeholder="请输入固件版本" disabled/>
            </el-form-item>
          </el-col>

        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备厂商" prop="firm">
              <el-input v-model="form.firm" placeholder="请输入设备厂商" disabled/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备型号" prop="model">
              <el-input v-model="form.model" placeholder="请输入设备型号" disabled/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="播放类型" prop="playType">
              <el-radio-group v-model="form.playType">
                <el-radio value="1">本地</el-radio>
                <el-radio value="2">推流</el-radio>
                <el-radio value="3">EasyNTS</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="流id" prop="streamId" v-if="form.playType === '2'">
              <el-input v-model="form.streamId" placeholder="请输入流id" maxlength="100" show-word-limit/>
            </el-form-item>
            <el-form-item label="EasyNTS地址" prop="easyNTSUrl" v-if="form.playType === '3'">
              <el-input v-model="form.easyNTSUrl" type="textarea" placeholder="请输入EasyNTS地址" maxlength="200"
                        show-word-limit/>
            </el-form-item>
            <el-form-item label="默认播放地址" prop="url" v-if="form.playType === '1'">
              <el-select
                  v-model="form.url"
                  class="m-2"
                  placeholder="请选择直播流地址"
                  size="large"
              >
                <el-option
                    v-for="item in form.streamUris"
                    :key="item"
                    :label="item"
                    :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="通道" prop="channel">
              <el-select
                  v-model="form.channel"
                  class="m-2"
                  placeholder="请选择通道"
                  size="large"
              >
                <el-option
                    v-for="item in channelList"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                >
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"/>
          </el-form-item>
        </el-col>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看直播流地址 对话框 -->
    <el-dialog :title="title" v-model="showUrl" width="800px" append-to-body>
      <div v-for="(item,index) in urls" :key="index" style="margin-bottom: 10px;">
        <el-text type="primary" style="cursor: pointer;" @click="copyToClipboard(item)">{{ item }}</el-text>
      </div>
    </el-dialog>

    <!-- 播放弹窗 对话框 -->
    <el-dialog :title="title" v-model="showPaly" width="835px" @opened="openedPaly" @close="closePaly">
      <div style="background: #000000">
        <Hikvision :rtsp="rtspUrl" v-if="showPaly && (playType === '1' || playType === '3') "/>
      </div>
      <div>
        <el-tabs v-if="playType === '2'" v-model="activeName" type="card" :stretch="true">
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

          <el-tab-pane label="WS控制台" name="control">
            <el-tabs v-model="playTabsName" style="width: 100%;" @tab-click="handleplayTabsClick">
              <el-tab-pane label="云台" name="absolute">
                <div style="display: grid; height: 180px; overflow: auto">
                  <!-- 左侧控制区域 -->
                  <div style="display: grid; grid-template-columns: 100px auto;">
                    <!-- 方向控制 -->
                    <div class="control-wrapper">
                      <div class="control-btn control-top" @mousedown="onvifPtzCtrlStartFun('upper')"
                           @mouseup="onvifPtzCtrlEndFun()">
                        <el-icon class="icon">
                          <CaretTop/>
                        </el-icon>
                        <div class="control-inner-btn control-inner"></div>
                      </div>
                      <div class="control-btn control-left" @mousedown="onvifPtzCtrlStartFun('left')"
                           @mouseup="onvifPtzCtrlEndFun()">
                        <el-icon class="icon">
                          <CaretLeft/>
                        </el-icon>
                        <div class="control-inner-btn control-inner"></div>
                      </div>
                      <div class="control-btn control-bottom" @mousedown="onvifPtzCtrlStartFun('below')"
                           @mouseup="onvifPtzCtrlEndFun()">
                        <el-icon class="icon">
                          <CaretBottom/>
                        </el-icon>
                        <div class="control-inner-btn control-inner"></div>
                      </div>
                      <div class="control-btn control-right" @mousedown="onvifPtzCtrlStartFun('right')"
                           @mouseup="onvifPtzCtrlEndFun()">
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
                        <el-slider v-model="controSpeed" :step="0.1" :min="0.1" :max="1"></el-slider>
                      </div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
              <el-tab-pane label="预置点" name="presets">
                <div style="display: flex; align-items: center;">
                  <el-input style="width: 300px; margin-right: 10px;" v-model="presetName"
                            placeholder="请输入预置点名称"/>
                  <el-button type="primary" @click="presetAdd">添 加</el-button>
                </div>
                <div style="margin-top: 10px;">
                  <el-tag v-loading="showPresets" closable @close="closePresets(item)" @click="gotoPresets(item)"
                          style="margin-right: 10px; cursor: pointer;" v-for="item in optionsPresetsToken"
                          :key="item.token">{{ item.name }}
                  </el-tag>
                </div>

              </el-tab-pane>
            </el-tabs>
          </el-tab-pane>

          <el-tab-pane label="onvif-java控制台" name="onvifJava">
            <div style="display: grid; height: 180px; overflow: auto">
              <!-- 左侧控制区域 -->
              <div style="display: grid; grid-template-columns: 100px auto;">
                <!-- 方向控制 -->
                <div class="control-wrapper">
                  <div class="control-btn control-top" @mousedown="onvifPtzCtrlStartFun('upper')"
                       @mouseup="onvifPtzCtrlEndFun()">
                    <el-icon class="icon">
                      <CaretTop/>
                    </el-icon>
                    <div class="control-inner-btn control-inner"></div>
                  </div>
                  <div class="control-btn control-left" @mousedown="onvifPtzCtrlStartFun('left')"
                       @mouseup="onvifPtzCtrlEndFun()">
                    <el-icon class="icon">
                      <CaretLeft/>
                    </el-icon>
                    <div class="control-inner-btn control-inner"></div>
                  </div>
                  <div class="control-btn control-bottom" @mousedown="onvifPtzCtrlStartFun('below')"
                       @mouseup="onvifPtzCtrlEndFun()">
                    <el-icon class="icon">
                      <CaretBottom/>
                    </el-icon>
                    <div class="control-inner-btn control-inner"></div>
                  </div>
                  <div class="control-btn control-right" @mousedown="onvifPtzCtrlStartFun('right')"
                       @mouseup="onvifPtzCtrlEndFun()">
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
                    <el-slider v-model="controSpeed" :step="0.1" :min="0.1" :max="1"></el-slider>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <el-dialog :title="title" v-model="openAdd" width="800px" append-to-body>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form ref="probeRef" :model="probeForm" :rules="rules" label-width="120px">
            <el-form-item label="设备名称" prop="name">
              <el-input v-model="probeForm.name" placeholder="请输入设备名称"/>
            </el-form-item>
            <el-form-item label="ONVIF IP" prop="ip">
              <el-input v-model="probeForm.ip" placeholder="请输入ONVIF IP"/>
            </el-form-item>
            <el-form-item label="ONVIF用户名" prop="username">
              <el-input v-model="probeForm.username" placeholder="请输入ONVIF用户名"/>
            </el-form-item>
            <el-form-item label="ONVIF密码" prop="password">
              <el-input v-model="probeForm.password" placeholder="请输入ONVIF密码"/>
            </el-form-item>
          </el-form>
          <div style="display: flex; justify-content: space-around;">
            <el-button type="primary" @click="submitDetectionForm" v-hasPermi="['onvif:service:getInfo']">探 测
            </el-button>
          </div>
        </el-col>
        <el-col :span="12">
          <el-form ref="resultRef" :model="resultForm" :rules="rulesResult" label-width="120px">
            <el-form-item label="厂商" prop="firm">
              <el-input v-model="resultForm.firm" placeholder="请输入厂商" disabled/>
            </el-form-item>
            <el-form-item label="型号" prop="model">
              <el-input v-model="resultForm.model" placeholder="请输入型号" disabled/>
            </el-form-item>
            <el-form-item label="固件版本" prop="firmwareVersion">
              <el-input v-model="resultForm.firmwareVersion" placeholder="请输入固件版本" disabled/>
            </el-form-item>
            <el-form-item label="通道" prop="channel">
              <el-select
                  v-model="resultForm.channel"
                  class="m-2"
                  placeholder="请选择通道"
                  size="large"
                  :disabled="disabledAdd"
              >
                <el-option
                    v-for="item in channelList"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                >
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="播放类型" prop="playType">
              <el-radio-group v-model="resultForm.playType">
                <el-radio value="1">本地</el-radio>
                <el-radio value="2">推流</el-radio>
                <el-radio value="3">EasyNTS</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="流id" prop="streamId" v-if="resultForm.playType === '2'">
              <el-input v-model="resultForm.streamId" placeholder="请输入流id" maxlength="100" show-word-limit/>
            </el-form-item>
            <el-form-item label="EasyNTS地址" prop="easyNTSUrl" v-if="resultForm.playType === '3'">
              <el-input v-model="resultForm.easyNTSUrl" type="textarea" placeholder="请输入EasyNTS地址" maxlength="200"
                        show-word-limit/>
            </el-form-item>
            <el-form-item label="直播流地址" prop="url" v-if="resultForm.playType === '1'">
              <el-select
                  v-model="resultForm.url"
                  class="m-2"
                  placeholder="请选择直播流地址"
                  size="large"
                  :disabled="disabledAdd"
              >
                <el-option
                    v-for="item in resultForm.streamUris"
                    :key="item"
                    :label="item"
                    :value="item"
                >
                </el-option>
              </el-select>
            </el-form-item>
            <div style="display: flex;justify-content: center">
              <el-button type="primary" @click="getChannel" :disabled="disabledAdd"
                         v-hasPermi="['onvif:service:getChannelToken']">获取通道
              </el-button>
              <el-button type="primary" @click="submitResultForm" :disabled="disabledAdd"
                         v-hasPermi="['onvif:device:add']">添 加
              </el-button>
            </div>
          </el-form>
        </el-col>
      </el-row>
    </el-dialog>

    <el-dialog title="修改地址" v-model="showMap" width="800px" append-to-body>
      <MapGaoDe ref="MapContainer" @update-value="updateDialogMap" :position="position" :toponym="form.address"/>
    </el-dialog>

    <!--发现设备-->
    <el-dialog title="WSDiscovery" v-model="showWS" width="1000px" append-to-body>
      <el-steps :active="stepsName" finish-status="success" align-center>
        <el-step title="设备认证" />
        <el-step title="添加设备" />
      </el-steps>
      <el-table :data="listWS" border v-if="stepsName === 0">
        <el-table-column label="ip" align="center" prop="ip" show-overflow-tooltip/>
        <el-table-column label="访问地址" align="center" prop="hostName" show-overflow-tooltip />
        <el-table-column label="用户名" align="center">
          <template #default="{ row }">
            <el-input v-model="row.username" placeholder="请输入用户名"></el-input>
          </template>
        </el-table-column>
        <el-table-column label="密码" align="center">
          <template #default="{ row }">
            <el-input v-model="row.password" placeholder="请输入密码" show-password></el-input>
          </template>
        </el-table-column>
        <el-table-column label="认证方式" align="center">
          <template #default="{ row }">
            <el-radio-group v-model="row.auth">
              <el-radio value="1" size="large">WS-Usemame token</el-radio>
              <el-radio value="2" size="large">Digest</el-radio>
            </el-radio-group>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="180">
          <template #default="scope">
            <div style="display:flex; align-items: center;justify-content: center">
              <el-button link type="primary" icon="Check" @click="handleAuth(scope.row)"
                         v-hasPermi="['onvif:device:auth']">认证
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="stepsName === 1">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form ref="resultRef" :model="resultForm" :rules="rulesResult2" label-width="120px">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="设备名称" prop="name">
                    <el-input v-model="resultForm.name" placeholder="请输入设备名称"/>
                  </el-form-item>
                  <el-form-item label="ONVIF IP" prop="ip">
                    <el-input v-model="resultForm.ip" placeholder="请输入ONVIF IP"/>
                  </el-form-item>
                  <el-form-item label="ONVIF用户名" prop="username">
                    <el-input v-model="resultForm.username" placeholder="请输入ONVIF用户名"/>
                  </el-form-item>
                  <el-form-item label="ONVIF密码" prop="password">
                    <el-input v-model="resultForm.password" placeholder="请输入ONVIF密码"/>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="厂商" prop="firm">
                    <el-input v-model="resultForm.firm" placeholder="请输入厂商" disabled/>
                  </el-form-item>
                  <el-form-item label="型号" prop="model">
                    <el-input v-model="resultForm.model" placeholder="请输入型号" disabled/>
                  </el-form-item>
                  <el-form-item label="固件版本" prop="firmwareVersion">
                    <el-input v-model="resultForm.firmwareVersion" placeholder="请输入固件版本" disabled/>
                  </el-form-item>
                  <el-form-item label="通道" prop="channel">
                    <el-select
                        v-model="resultForm.channel"
                        class="m-2"
                        placeholder="请选择通道"
                        size="large"
                    >
                      <el-option
                          v-for="item in channelList"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                      >
                      </el-option>
                    </el-select>
                  </el-form-item>
                  <el-form-item label="播放类型" prop="playType">
                    <el-radio-group v-model="resultForm.playType">
                      <el-radio value="1">本地</el-radio>
                      <el-radio value="2">推流</el-radio>
                      <el-radio value="3">EasyNTS</el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="流id" prop="streamId" v-if="resultForm.playType === '2'">
                    <el-input v-model="resultForm.streamId" placeholder="请输入流id" maxlength="100" show-word-limit/>
                  </el-form-item>
                  <el-form-item label="EasyNTS地址" prop="easyNTSUrl" v-if="resultForm.playType === '3'">
                    <el-input v-model="resultForm.easyNTSUrl" type="textarea" placeholder="请输入EasyNTS地址" maxlength="200"
                              show-word-limit/>
                  </el-form-item>
                  <el-form-item label="直播流地址" prop="url" v-if="resultForm.playType === '1'">
                    <el-select
                        v-model="resultForm.url"
                        class="m-2"
                        placeholder="请选择直播流地址"
                        size="large"
                    >
                      <el-option
                          v-for="item in resultForm.streamUris"
                          :key="item"
                          :label="item"
                          :value="item"
                      >
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>

              <div style="display: flex;justify-content: center">
                <el-button type="primary" @click="submitResultForm2" v-hasPermi="['onvif:device:add']">添 加
                </el-button>
              </div>
            </el-form>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</DeviceClassificationLayout>
</template>

<script setup name="CameraManage">
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import {checkPermi} from "@/utils/wvpPermission";
import {
  addDevice, addOnvif,
  addPreset,
  delDevice,
  getChannelToken,
  getDevice,
  getGotoPreset,
  getPresetList,
  listDevice,
  removePreset,
  updateDevice, WSDiscovery
} from "@/api/onvif/device";
import Hikvision from "@/components/Hikvision/index.vue";
import {ref} from "vue";
import {probe} from "../../../api/onvif/addCamera.js";
import MapGaoDe from "@/components/MapGaoDe/index.vue";
import {ElLoading} from 'element-plus';
import Jessibuca from "@/components/jessibuca/index.vue";
import RtcPlayer from "@/components/rtcPlayer/index.vue";
import H265web from "@/components/H265web/index.vue";
import StreamDropdown from "@/views/wvp/channel/components/streamDropdown.vue";
import MediaInfo from "@/views/wvp/channel/components/mediaInfo.vue";
import {DocumentCopy} from '@element-plus/icons-vue'
import {startPlay} from "../../../api/wvp/push.js";
import {onvifPZTEnd, onvifPZTStart} from "../../../api/onvif/device.js";
import {ptzControlUpEnd} from "../../../api/dahua/device.js";

const {proxy} = getCurrentInstance();

const deviceList = ref([]);
const open = ref(false);
const openAdd = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const showPresets = ref(false);
const showWS = ref(false);
const total = ref(0);
const title = ref("");
const brand = ref("");
const token = ref("");
const stepsName = ref(0)
const playTabsName = ref("absolute");
const presetName = ref("");
const urls = ref({});
const showUrl = ref(false);
const showPaly = ref(false);
const video = ref(null);
const rtspUrl = ref({});
const dahuaPlayer = ref(null);
const optionsPresetsToken = ref([]);
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
    userName: null,
    password: null,
    url: null,
    firm: null,
    model: null,
    firmwareVersion: null,
    streamUris: null,
  },
  rules: {
    id: [{required: true, message: "编号不能为空", trigger: "blur"}],
    ip: [{required: true, message: "ip不能为空", trigger: "blur"}],
    userName: [{required: true, message: "用户名不能为空", trigger: "blur"}],
    password: [{required: true, message: "密码不能为空", trigger: "blur"}],
    name: [{required: true, message: "名称不能为空", trigger: 'blur'}],
    url: [{required: true, message: "直播流地址不能为空", trigger: "blur"}],
    streamId: [
      {required: true, message: "流id不能为空", trigger: "blur"}
    ],
    easyNTSUrl: [
      {required: true, message: "EasyNTS播放地址不能为空", trigger: "blur"}
    ],
    channel: [{required: true, message: "通道不能为空", trigger: "blur"}],
  },
  probeForm: {},
  rulesResult: {
    firm: [{required: true, message: "设备厂商不能为空", trigger: "blur"}],
    model: [{required: true, message: "设备型号不能为空", trigger: "blur"}],
    firmwareVersion: [{required: true, message: "固件版本不能为空", trigger: "blur"}],
    url: [{required: true, message: "直播流地址不能为空", trigger: "blur"}],
    streamId: [
      {required: true, message: "流id不能为空", trigger: "blur"}
    ],
    easyNTSUrl: [
      {required: true, message: "EasyNTS播放地址不能为空", trigger: "blur"}
    ],
    channel: [{required: true, message: "通道不能为空", trigger: "blur"}],
  },
  rulesResult2: {
    ip: [{required: true, message: "ip不能为空", trigger: "blur"}],
    userName: [{required: true, message: "用户名不能为空", trigger: "blur"}],
    password: [{required: true, message: "密码不能为空", trigger: "blur"}],
    name: [{required: true, message: "名称不能为空", trigger: 'blur'}],
    firm: [{required: true, message: "设备厂商不能为空", trigger: "blur"}],
    model: [{required: true, message: "设备型号不能为空", trigger: "blur"}],
    firmwareVersion: [{required: true, message: "固件版本不能为空", trigger: "blur"}],
    url: [{required: true, message: "直播流地址不能为空", trigger: "blur"}],
    streamId: [
      {required: true, message: "流id不能为空", trigger: "blur"}
    ],
    easyNTSUrl: [
      {required: true, message: "EasyNTS播放地址不能为空", trigger: "blur"}
    ],
    channel: [{required: true, message: "通道不能为空", trigger: "blur"}],
  }
});
const {queryParams, form, rules, probeForm, rulesResult, rulesResult2} = toRefs(data);
const classificationDeviceKeys = ref([]);
function handleClassificationFilter(filter) { Object.assign(queryParams.value, filter, { pageNum: 1 }); getList(); }
const passwordVisibility = ref({});
const url = ref('');
const disabledAdd = ref(true);
const resultForm = ref({});
const position = ref(null);
const MapContainer = ref(null);
const toponym = ref('');
const showMap = ref(false);
const channelList = ref([])
const controSpeed = ref(0.1);
const onvifDeviceId = ref(null);
const listWS = ref([]);

const next = () => {
  if (stepsName.value++ > 2) stepsName.value = 0
}

function handleWSDiscovery(){
  stepsName.value = 0;
  const loadingService = ElLoading.service({
    lock: true,
    fullscreen: true,
    text: '正在发送指令...',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  WSDiscovery().then(res => {
    listWS.value = res.data;
    showWS.value = true;
  }).finally(() => {
    loadingService.close();
  })
}

const videoError = (e) => {
  console.log("播放器错误：" + JSON.stringify(e));
}


function moreClick(command, itemData) {
  if (command === "viewUrls") {
    viewUrls(itemData.streamUris)
  } else if (command === "handleMap") {
    handleMap(itemData)
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
  updateDevice(form.value).then(res => {
    showMap.value = false;
    Destruction();
    proxy.$modal.msgSuccess("操作成功");
  }).catch(() => {
    proxy.$modal.msgError("操作失败");
  })
}

const submitDetectionForm = () => {
  proxy.$refs["probeRef"].validate(async valid => {
    if (valid) {
      const query = {
        ip: probeForm.value.ip,
        username: probeForm.value.username,
        password: probeForm.value.password,
      };
      const res = await probe(query);
      resultForm.value.firm = res.data.firm;
      resultForm.value.model = res.data.model;
      resultForm.value.firmwareVersion = res.data.firmwareVersion;
      resultForm.value.streamUris = res.data.streamUris;
      disabledAdd.value = false;
      proxy.$modal.msgSuccess("操作成功");
    }
  });
};

const submitResultForm = () => {
  proxy.$refs["resultRef"].validate(async valid => {
    if (valid) {
      const data = {
        id: null,
        name: probeForm.value.name,
        ip: probeForm.value.ip,
        userName: probeForm.value.username,
        password: probeForm.value.password,
        firm: resultForm.value.firm,
        url: resultForm.value.url,
        model: resultForm.value.model,
        firmwareVersion: resultForm.value.firmwareVersion,
        streamUris: resultForm.value.streamUris,
        playType: resultForm.value.playType,
        streamId: resultForm.value.streamId,
        easyNTSUrl: resultForm.value.easyNTSUrl,
        channel: resultForm.value.channel,
      };
      await addDevice(data);
      proxy.$modal.msgSuccess("操作成功");
      probeForm.value = {};
      resultForm.value = {};
      openAdd.value = false;
      getList();
    }
  });
}

const submitResultForm2 = () => {
  proxy.$refs["resultRef"].validate(async valid => {
    if (valid) {
      const data = {
        id: null,
        name: resultForm.value.name,
        ip: resultForm.value.ip,
        userName: resultForm.value.username,
        password: resultForm.value.password,
        firm: resultForm.value.firm,
        url: resultForm.value.url,
        model: resultForm.value.model,
        firmwareVersion: resultForm.value.firmwareVersion,
        streamUris: resultForm.value.streamUris,
        playType: resultForm.value.playType,
        streamId: resultForm.value.streamId,
        easyNTSUrl: resultForm.value.easyNTSUrl,
        channel: resultForm.value.channel,
      };
      await addDevice(data);
      proxy.$modal.msgSuccess("操作成功");
      resultForm.value = {};
      showWS.value = false;
      stepsName.value = 0;
      getList();
    }
  });
}

const presetAdd = async () => {
  const params = {
    ip: streamInfo.value.ip,
    username: streamInfo.value.userName,
    password: streamInfo.value.password,
    profileToken: token.value,
    presetToken: presetName.value,
  }
  await addPreset(params).then(() => {
    getAllPreset();
    proxy.$modal.msgSuccess("操作成功");
  })
}

const gotoPresets = async (row) => {
  const params = {
    ip: streamInfo.value.ip,
    username: streamInfo.value.userName,
    password: streamInfo.value.password,
    profileToken: token.value,
    presetToken: row.token,
  }
  await getGotoPreset(params);
  proxy.$modal.msgSuccess("操作成功");
}

const closePresets = async (row) => {
  const params = {
    ip: streamInfo.value.ip,
    username: streamInfo.value.userName,
    password: streamInfo.value.password,
    profileToken: token.value,
    presetToken: row.token,
  }
  await removePreset(params).then(() => {
    getAllPreset();
    proxy.$modal.msgSuccess("操作成功");
  });
}
const handleplayTabsClick = (tab, event) => {
  if (tab.props.label === "预置点") {
    presetName.value = "";
    getAllPreset();
  }
}

const getAllPreset = async () => {
  showPresets.value = true;
  const params = {
    ip: streamInfo.value.ip,
    username: streamInfo.value.userName,
    password: streamInfo.value.password,
    profileToken: token.value,
  }
  const res = await getPresetList(params);
  optionsPresetsToken.value = res.data;
  showPresets.value = false;
}

const togglePasswordVisibility = (id) => {
  passwordVisibility.value[id] = !passwordVisibility.value[id];
};


function closePaly() {
  token.value = null;
  showPaly.value = false;
}

async function handleView(row) {
  playType.value = row.playType;
  onvifDeviceId.value = row.id;
  if (row.playType === '2') {
    tabActiveName.value = 'media'
    const ans = await startPlay(row.streamId);
    streamInfo.value = ans.data;
    flvUrl.value = ans.data.flv;
    rtcUrl.value = ans.data.rtc;
    wsUrl.value = ans.data.ws_flv;

    brand.value = row.firm;
    title.value = "视频播放";
    showPaly.value = true;

  } else if (row.playType === '1') {
    tabActiveName.value = 'control'
    streamInfo.value = row;
    brand.value = row.firm;
    rtspUrl.value = row.url;
    title.value = "视频播放";
    showPaly.value = true;
  } else if (row.playType === '3') {
    tabActiveName.value = 'control'
    streamInfo.value = row;
    brand.value = row.firm;
    rtspUrl.value = row.easyNTSUrl;
    title.value = "视频播放";
    showPaly.value = true;
  }
}

function openedPaly() {
  if (brand.value === 'Dahua') {
    if (dahuaPlayer.value) {
      dahuaPlayer.value.playerPlay();
    }
  }
}


function copyToClipboard(text) {
  navigator.clipboard.writeText(text).then(() => {
    proxy.$modal.msgSuccess("复制成功！");
  }).catch((err) => {
    proxy.$modal.msgError("复制失败，请重试！");
  });
}

function viewUrls(row) {
  title.value = "查看直播流地址";
  urls.value = JSON.parse(row);
  showUrl.value = true;
}

/** 查询onvif 设备列表 */
function getList() {
  loading.value = true;
  listDevice(queryParams.value).then(response => {
    deviceList.value = response.rows;
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
    userName: null,
    password: null,
    playType: '1',
    url: null,
    streamId: null,
    easyNTSUrl: null,
    firm: null,
    model: null,
    firmwareVersion: null,
    streamUris: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null,
    addressMap: null,
    lng: null,
    lat: null,
  };
  proxy.resetForm("deviceRef");
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


function handleAuth(row) {
  if (!row.password || row.password.trim() === '') {
    proxy.$modal.msgError("请输入用户名");
    return
  }
  if (!row.password || row.password.trim() === '') {
    proxy.$modal.msgError("请输入密码");
    return
  }
  if (!row.auth || !['1', '2'].includes(row.auth)) {
    proxy.$modal.msgError("请选择认证方式");
    return
  }
  addOnvif(row).then(res => {
    stepsName.value = 1;
    const keys = [];
    const rtspUrls = [];
    Object.entries(res.data.streamUris).map(([key, value]) => {
      keys.push({
        label: key,
        value: key,
      });
      rtspUrls.push(value);
    });
    channelList.value = keys;
    resultForm.value = res.data;
    resultForm.value.ip = res.data.ip;
    resultForm.value.username = row.username;
    resultForm.value.password = row.password;
  })
}
function handleAdd(row) {
  title.value = "添加onvif 设备";
  probeForm.value = {
    name: null,
    ip: null,
    username: null,
    password: null,
  }
  resultForm.value = {
    firm: null,
    playType: '1',
    url: null,
    streamId: null,
    easyNTSUrl: null,
    model: null,
    firmwareVersion: null,
    streamUris: null,
    channel: null,
  };
  openAdd.value = true
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getDevice(_id).then(response => {
    form.value = response.data;
    form.value.streamUris = JSON.parse(response.data.streamUris);
    open.value = true;
    title.value = "修改onvif 设备";

    getChannelToken({
      ip: form.value.ip,
      username: form.value.userName,
      password: form.value.password
    }).then(res => {
      channelList.value = res.data
    })
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["deviceRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateDevice(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addDevice(form.value).then(response => {
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
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除onvif 设备编号为"' + _ids + '"的数据项？').then(function () {
    return delDevice(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('onvif/device/export', {
    ...queryParams.value
  }, `device_${new Date().getTime()}.xlsx`)
}

/**
 * 获取通道
 */
function getChannel() {
  getChannelToken({
    ip: probeForm.value.ip,
    username: probeForm.value.username,
    password: probeForm.value.password
  }).then(res => {
    channelList.value = res.data
  })
}

/**
 * 云台开始
 *
 * @param direction
 */
function onvifPtzCtrlStartFun(direction) {
  onvifPZTStart({
    direction: direction,
    id: onvifDeviceId.value,
    controSpeed: controSpeed.value,
  });
}

/**
 * 云台结束
 */
function onvifPtzCtrlEndFun() {
  setTimeout(() => {
    onvifPZTEnd({
      id: onvifDeviceId.value,
    });
  }, 200)
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
