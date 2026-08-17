<template>
  <DeviceClassificationLayout protocol-type="DAHUA" :selected-device-keys="classificationDeviceKeys" @filter-change="handleClassificationFilter" @assigned="getList">
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="ip" prop="ip">
        <el-input
            v-model="queryParams.ip"
            placeholder="请输入ip"
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
            v-hasPermi="['dahua:device:add']"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['dahua:device:edit']"
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
            v-hasPermi="['dahua:device:remove']"
        >删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="Download"
            @click="handleExport"
            v-hasPermi="['dahua:device:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="deviceList" @selection-change="handleSelectionChange" border>
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="ip" align="center" prop="ip"/>
      <el-table-column label="设备名称" align="center" prop="name"/>
      <el-table-column label="地址" align="center" prop="addressMap"/>
      <el-table-column label="端口" align="center" prop="port"/>
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
      <el-table-column label="播放类型" align="center" prop="playType">
        <template #default="scope">
          <dict-tag :options="play_type" :value="scope.row.playType"/>
        </template>
      </el-table-column>
      <el-table-column key="streamId" label="流id" prop="streamId" min-width="150" align="center"/>
      <el-table-column label="备注" align="center" prop="remark"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="250"  fixed="right">
        <template #default="scope">
          <div style="display:flex; align-items: center;justify-content: center;flex-wrap: wrap;">
<!--            <el-button link type="primary" icon="VideoPlay" @click="handleSDKPlay(scope.row)"-->
<!--                       v-hasPermi="['dahua:zlmApi:play']">SDK播放-->
<!--            </el-button>-->
<!--            <el-button link type="primary" icon="VideoPlay" @click="handleProxyPlay(scope.row)"-->
<!--                       v-hasPermi="['dahua:zlmApi:play']">代理播放-->
<!--            </el-button>-->
            <el-button link type="primary" icon="VideoPlay" @click="handleStartPlay(scope.row)"
                       v-hasPermi="['dahua:device:start']">播放
            </el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                       v-hasPermi="['dahua:device:edit']">修改
            </el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
                       v-hasPermi="['dahua:device:remove']">删除
            </el-button>

            <el-dropdown @command="(command)=>{moreClick(command, scope.row)}"
                         v-if="checkPermi(['dahua:device:edit'])">
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
                  <el-dropdown-item command="handleMap" v-if="checkPermi(['dahua:device:edit'])">修改位置
                  </el-dropdown-item>
                  <el-dropdown-item command="snapPictureList" v-if="checkPermi(['dahua:device:listScreenshot'])">
                    抓图列表
                  </el-dropdown-item>
                  <el-dropdown-item command="snapPicture" v-if="checkPermi(['dahua:device:snapPicture'])">
                    抓图
                  </el-dropdown-item>
                  <el-dropdown-item command="timerCapturePicture"
                                    v-if="checkPermi(['dahua:device:timerCapturePicture'])">
                    定时抓图
                  </el-dropdown-item>
                  <el-dropdown-item command="stopCapturePicture" v-if="checkPermi(['dahua:device:stopCapturePicture'])">
                    停止定时抓图
                  </el-dropdown-item>
                  <el-dropdown-item command="control" v-if="checkPermi(['dahua:device:control'])">
                    设备控制
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

    <!-- 添加或修改大华设备对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="deviceRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入摄像头名称" maxlength="30" show-word-limit/>
        </el-form-item>
        <el-form-item label="设备id" prop="deviceId">
          <el-input v-model="form.deviceId" placeholder="请输入设备id" maxlength="30" show-word-limit disabled/>
        </el-form-item>
        <el-form-item label="ip" prop="ip">
          <el-input v-model="form.ip" placeholder="请输入ip" maxlength="50" show-word-limit disabled/>
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input v-model="form.port" placeholder="请输入端口" maxlength="10" show-word-limit disabled/>
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
        <div v-if="form.playType === '1'">
          <el-form-item label="用户名" prop="userName">
            <el-input v-model="form.userName" placeholder="请输入用户名" maxlength="20" show-word-limit/>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" placeholder="请输入密码" maxlength="50" show-word-limit show-password/>
          </el-form-item>
          <el-form-item>
            <template #label>
            <span>
               <el-tooltip content="登录之后才能获取通道，必须要输入用户名和密码才能获取" placement="top">
                  <el-icon><question-filled/></el-icon>
               </el-tooltip>
            </span>
            </template>
            <el-button type="primary" @click="login">登录</el-button>
          </el-form-item>
          <el-form-item label="通道号" prop="channel">
            <el-radio-group v-model="form.channel">
              <el-radio :value="item" v-for="(item,index) in channelList" :key="index">通道-{{ item }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
        <el-form-item label="流id" prop="streamId" v-if="form.playType === '2'">
          <el-input v-model="form.streamId" placeholder="请输入流id" maxlength="100" show-word-limit/>
        </el-form-item>
        <el-form-item label="EasyNTS地址" prop="easyNTSUrl" v-if="form.playType === '3'">
          <el-input v-model="form.easyNTSUrl" type="textarea" placeholder="请输入EasyNTS地址" maxlength="200"
                    show-word-limit/>
        </el-form-item>
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

    <el-dialog title="选择设备" v-model="openDevice" width="600px" append-to-body>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
              type="danger"
              plain
              icon="Delete"
              :disabled="multipleRegisterDevice"
              @click="handleDeviceDelete"
              v-hasPermi="['dahua:device:remove']"
          >删除
          </el-button>
        </el-col>
        <right-toolbar :search="false" @queryTable="getRegisterDeviceListFun"></right-toolbar>
      </el-row>
      <el-table v-loading="registerDeviceLoading" :data="registerDeviceList" border height="500px" @selection-change="handleRegisterDeviceSelectionChange">
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column label="ip" align="center" prop="ip"/>
        <el-table-column label="设备id" align="center" prop="deviceId"/>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="Plus" v-hasPermi="['dahua:device:add']" @click="handleDeviceAdd(scope.row)">新增</el-button>
            <el-button link type="primary" icon="Delete" v-hasPermi="['dahua:device:remove']" @click="handleDeviceDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog title="修改地址" v-model="showMap" width="800px" append-to-body>
      <MapGaoDe ref="MapContainer" @update-value="updateDialogMap" :position="position" :toponym="form.address"/>
    </el-dialog>

    <el-dialog title="播放视频" v-model="openPlay" width="835px" append-to-body  @close="closeProxyPlay">
      <div>
        <Hikvision :rtsp="videoUrl" v-if="openPlay && (playType === '1' || playType === '3' || playType === '4') "/>
        <div>
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
        </div>

        <div v-if="checkPermi(['dahua:device:ptzCtrl'])">
          <div style="display: grid; grid-template-columns: 240px auto; height: 180px; overflow: auto"
               v-if="openPlay && (playType === '1' || playType === '3' || playType === '4') ">
            <!-- 左侧控制区域 -->
            <div style="display: grid; grid-template-columns: 100px auto;">
              <!-- 方向控制 -->
              <div class="control-wrapper">
                <div class="control-btn control-top" @mousedown="ptzControlUpStartFun('up')"
                     @mouseup="ptzControlUpEndFun('up')">
                  <el-icon class="icon">
                    <CaretTop/>
                  </el-icon>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-left" @mousedown="ptzControlUpStartFun('left')"
                     @mouseup="ptzControlUpEndFun('left')">
                  <el-icon class="icon">
                    <CaretLeft/>
                  </el-icon>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-bottom" @mousedown="ptzControlUpStartFun('down')"
                     @mouseup="ptzControlUpEndFun('down')">
                  <el-icon class="icon">
                    <CaretBottom/>
                  </el-icon>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-right" @mousedown="ptzControlUpStartFun('right')"
                     @mouseup="ptzControlUpEndFun('right')">
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
                <div class="ptz-btn-box">
                  <div @mousedown="ptzControlUpStartFun('doubling+')" @mouseup="ptzControlUpEndFun('doubling+')"
                       title="变倍+">
                    <el-icon class="control-zoom-btn" style="font-size: 24px;">
                      <ZoomIn/>
                    </el-icon>
                  </div>
                  <div @mousedown="ptzControlUpStartFun('doubling-')" @mouseup="ptzControlUpEndFun('doubling-')"
                       title="变倍-">
                    <el-icon class="control-zoom-btn" style="font-size: 24px;">
                      <ZoomOut/>
                    </el-icon>
                  </div>
                </div>
                <div class="ptz-btn-box">
                  <div @mousedown="ptzControlUpStartFun('zoom+')" @mouseup="ptzControlUpEndFun('zoom+')" title="聚焦+">
                    <i class="iconfont icon-bianjiao-fangda control-zoom-btn" style="font-size: 24px;"></i>
                  </div>
                  <div @mousedown="ptzControlUpStartFun('zoom-')" @mouseup="ptzControlUpEndFun('zoom-')" title="聚焦-">
                    <i class="iconfont icon-bianjiao-suoxiao control-zoom-btn" style="font-size: 24px;"></i>
                  </div>
                </div>
                <div class="ptz-btn-box">
                  <div @mousedown="ptzControlUpStartFun('aperture+')" @mouseup="ptzControlUpEndFun('aperture+')"
                       title="光圈+">
                    <i class="iconfont icon-guangquan control-zoom-btn" style="font-size: 24px;"></i>
                  </div>
                  <div @mousedown="ptzControlUpStartFun('aperture-')" @mouseup="ptzControlUpEndFun('aperture-')"
                       title="光圈-">
                    <i class="iconfont icon-guangquan- control-zoom-btn" style="font-size: 24px;"></i>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="playType === '2'">
          <el-tabs v-model="activeName" type="card" :stretch="true" v-if="playType === '2'">
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

            <el-tab-pane v-if="checkPermi(['dahua:device:ptzCtrl'])" label="云台控制" name="control">
              <div style="display: grid; grid-template-columns: 240px auto; height: 180px; overflow: auto">
                <!-- 左侧控制区域 -->
                <div style="display: grid; grid-template-columns: 100px auto;">
                  <!-- 方向控制 -->
                  <div class="control-wrapper">
                    <div class="control-btn control-top" @mousedown="ptzControlUpStartFun('up')"
                         @mouseup="ptzControlUpEndFun('up')">
                      <el-icon class="icon">
                        <CaretTop/>
                      </el-icon>
                      <div class="control-inner-btn control-inner"></div>
                    </div>
                    <div class="control-btn control-left" @mousedown="ptzControlUpStartFun('left')"
                         @mouseup="ptzControlUpEndFun('left')">
                      <el-icon class="icon">
                        <CaretLeft/>
                      </el-icon>
                      <div class="control-inner-btn control-inner"></div>
                    </div>
                    <div class="control-btn control-bottom" @mousedown="ptzControlUpStartFun('down')"
                         @mouseup="ptzControlUpEndFun('down')">
                      <el-icon class="icon">
                        <CaretBottom/>
                      </el-icon>
                      <div class="control-inner-btn control-inner"></div>
                    </div>
                    <div class="control-btn control-right" @mousedown="ptzControlUpStartFun('right')"
                         @mouseup="ptzControlUpEndFun('right')">
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
                    <div class="ptz-btn-box">
                      <div @mousedown="ptzControlUpStartFun('doubling+')" @mouseup="ptzControlUpEndFun('doubling+')"
                           title="变倍+">
                        <el-icon class="control-zoom-btn" style="font-size: 24px;">
                          <ZoomIn/>
                        </el-icon>
                      </div>
                      <div @mousedown="ptzControlUpStartFun('doubling-')" @mouseup="ptzControlUpEndFun('doubling-')"
                           title="变倍-">
                        <el-icon class="control-zoom-btn" style="font-size: 24px;">
                          <ZoomOut/>
                        </el-icon>
                      </div>
                    </div>
                    <div class="ptz-btn-box">
                      <div @mousedown="ptzControlUpStartFun('zoom+')" @mouseup="ptzControlUpEndFun('zoom+')" title="聚焦+">
                        <i class="iconfont icon-bianjiao-fangda control-zoom-btn" style="font-size: 24px;"></i>
                      </div>
                      <div @mousedown="ptzControlUpStartFun('zoom-')" @mouseup="ptzControlUpEndFun('zoom-')" title="聚焦-">
                        <i class="iconfont icon-bianjiao-suoxiao control-zoom-btn" style="font-size: 24px;"></i>
                      </div>
                    </div>
                    <div class="ptz-btn-box">
                      <div @mousedown="ptzControlUpStartFun('aperture+')" @mouseup="ptzControlUpEndFun('aperture+')"
                           title="光圈+">
                        <i class="iconfont icon-guangquan control-zoom-btn" style="font-size: 24px;"></i>
                      </div>
                      <div @mousedown="ptzControlUpStartFun('aperture-')" @mouseup="ptzControlUpEndFun('aperture-')"
                           title="光圈-">
                        <i class="iconfont icon-guangquan- control-zoom-btn" style="font-size: 24px;"></i>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-dialog>

    <el-dialog :title="`【${screenshotQueryParams.name}】设备抓图列表`" v-model="openSnapPicture" width="600px"
               append-to-body>
      <el-row :gutter="10" class="mb8">
        <right-toolbar :search="false" @queryTable="getListScreenshotFun"></right-toolbar>
      </el-row>

      <el-table :data="screenshotList" border>
        <el-table-column label="抓图路径" align="center" prop="image">
          <template #default="scope">
            <image-preview :src="scope.row.image" :width="50" :height="50"/>
          </template>
        </el-table-column>
        <el-table-column label="抓图时间" align="center" prop="createTime"/>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="Delete" @click="handleRemoveScreenshot(scope.row)"
                       v-hasPermi="['dahua:device:removeScreenshot']">删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
          v-show="screenshotTotal>0"
          :total="screenshotTotal"
          v-model:page="screenshotQueryParams.pageNum"
          v-model:limit="screenshotQueryParams.pageSize"
          @pagination="getListScreenshotFun"
      />
    </el-dialog>

    <el-dialog :title="`【${controlQueryParams.name}】设备控制`" v-model="openControl" width="600px" append-to-body>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
              type="primary"
              plain
              icon="RefreshRight"
              @click="handleReboot"
          >重启
          </el-button>
        </el-col>

        <el-col :span="1.5">
          <el-button
              type="primary"
              plain
              icon="Clock"
              @click="handleGetTime"
          >获取时间
          </el-button>
        </el-col>
      </el-row>

      <el-row :gutter="10" class="mb8">
        设备时间：{{ deviceTime }}
      </el-row>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-checkbox v-model="controlQueryParams.type" label="当前时间" size="large" />
        </el-col>

        <el-col :span="1.5">
          <el-date-picker :disabled="controlQueryParams.type" v-model="controlQueryParams.date" value-format="YYYY-MM-DD hh:mm:ss" type="datetime"></el-date-picker>
        </el-col>

        <el-col :span="1.5">
          <el-button
              type="primary"
              plain
              icon="Setting"
              @click="handleSetTime"
          >设置时间
          </el-button>
        </el-col>
      </el-row>
    </el-dialog>

<!--    <el-dialog title="播放视频" v-model="openProxyPlay" width="835px" append-to-body @close="closeProxyPlay">-->
<!--      <Hikvision :rtsp="videoUrl"/>-->
<!--    </el-dialog>-->



  </div>
</DeviceClassificationLayout>
</template>

<script setup name="DahuaDevice">
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import {
  addDevice,
  delDevice,
  getDevice,
  listDevice,
  startRealPlay,
  stopRealPlay,
  updateDevice
} from "@/api/dahua/device";
import {
  dahuaLogin, delRegisterDevice,
  getRegisterDeviceList, getTime,
  listScreenshot,
  ptzControlUpEnd,
  ptzControlUpStart,
  reboot,
  removeScreenshot, setTime,
  snapPicture,
  stopCapturePicture,
  timerCapturePicture
} from "../../../api/dahua/device.js";
import {checkPermi} from "@/utils/wvpPermission";
import MapGaoDe from "@/components/MapGaoDe/index.vue";
import moment from "moment";
import Hikvision from "@/components/Hikvision/index.vue";


import Jessibuca from "@/components/jessibuca/index.vue";
import RtcPlayer from "@/components/rtcPlayer/index.vue";
import H265web from "@/components/H265web/index.vue";
import StreamDropdown from "@/views/wvp/channel/components/streamDropdown.vue";
import MediaInfo from "@/views/wvp/channel/components/mediaInfo.vue";
import {DocumentCopy} from '@element-plus/icons-vue'
import {ElMessage} from "element-plus";
import {startPlay} from "../../../api/wvp/push.js";
import {proxyPlay, stopProxy} from "@/api/dahua/zlmApi.js";
import useClipboard from "vue-clipboard3";
const { toClipboard } = useClipboard()

const {proxy} = getCurrentInstance();
const {play_type} = proxy.useDict('play_type');



const channelList = ref([]);
const passwordVisibility = ref({});

const registerDeviceList = ref([]);
const openDevice = ref(false);
const registerDeviceLoading = ref(false);
const openProxyPlay = ref(false);

const deviceList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const position = ref(null);
const MapContainer = ref(null);
const toponym = ref('');
const showMap = ref(false);

const videoUrl = ref('');
const openPlay = ref(false);
const tabActiveName = ref('media');
const controSpeed = ref(5);
const dahuaDeviceId = ref(null);

const openSnapPicture = ref(false);
const screenshotQueryParams = ref({
  pageNum: 1,
  pageSize: 10,
  dahuaDeviceId: null,
  name: null,
});
const screenshotList = ref([]);
const screenshotTotal = ref(0);

const controlQueryParams = ref({
  dahuaDeviceId: null,
  name: null,
  type: false,
  date: moment(new Date()).format('YYYY-MM-DD HH:mm:ss'),
})
const openControl = ref(false);

const deviceTime = ref(null);

const rtcUrl = ref("");
const flvUrl = ref("");
const wsUrl = ref('');
const playType = ref('');
const showVideoDialog = ref(false);
const activeName = ref('flv');
const streamInfo = ref({});
const hasAudio = ref(false);


const ips = ref([]);
const multipleRegisterDevice = ref(true);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    ip: null,
    name: null,
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
    deviceId: [
      {required: true, message: "设备id不能为空", trigger: "blur"}
    ],
    port: [
      {required: true, message: "端口不能为空", trigger: "blur"}
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
const statusPlay = ref("");
const SDKID = ref("");
function handleSDKPlay(row){
  statusPlay.value = "SDK";
  SDKID.value = row.id;
  startRealPlay(row.id).then(res => {
    videoUrl.value = res.data.rtsp;
    openProxyPlay.value = true;
  })
}

const videoError = (e) => {
  console.log("播放器错误：" + JSON.stringify(e));
}

function closeProxyPlay(){
  if (statusPlay.value === "SDK"){
    stopRealPlay(SDKID.value).then(() => {
      openPlay.value = false;
    });
  } else {
    openPlay.value = false;
  }
}

const rowID = ref("");
const handleProxyPlay = async (row) => {
  statusPlay.value = "Proxy";
  let newUrl = row.url.replace(/(channel=)(\d+)/, function(_, p1, p2) {
    return p1 + (parseInt(p2) + 1);
  });
  rowID.value = row.id;
  const data = {
    id: row.id,
    name: row.name,
    url: newUrl,
  }
  const res = await proxyPlay(data);
  videoUrl.value = res.data.rtsp;
  openProxyPlay.value = true;
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
  } else if (command === "snapPicture") {
    handleSnapPicture(itemData)
  } else if (command === 'snapPictureList') {
    handleSnapPictureList(itemData)
  } else if (command === 'timerCapturePicture') {
    handleTimerCapturePicture(itemData)
  } else if (command === 'stopCapturePicture') {
    handleStopCapturePicture(itemData)
  } else if (command === 'control') {
    handleControl(itemData)
  }
}

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


const Destruction = () => {
  MapContainer.value?.Destruction();
};

/** 查询大华设备列表 */
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
    deviceId: null,
    ip: null,
    name: null,
    port: null,
    userName: null,
    password: null,
    channel: null,
    playType: '1',
    url: null,
    streamId: null,
    easyNTSUrl: null,
    lat: null,
    lng: null,
    addressMap: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
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

/** 新增按钮操作 */
function handleAdd() {
  reset();
  openDevice.value = true;
  getRegisterDeviceListFun()
}

/**
 * 获取自动注册设备列表
 */
function getRegisterDeviceListFun() {
  getRegisterDeviceList().then((res) => {
    registerDeviceList.value = res.data
  })
}

/**
 * 设备新增按钮操作
 */
function handleDeviceAdd(row) {
  form.value.ip = row.ip
  channelList.value = []
  form.value.deviceId = row.deviceId
  form.value.port = row.port
  open.value = true
  title.value = "添加大华设备";
}

/**
 * 设备新增删除按钮操作
 */
function handleDeviceDelete(row){
 const iplist = row.ip || ips.value;
  proxy.$modal.confirm('是否确认删除大华新增设备"' + iplist + '"的数据项？').then(function () {
    return delRegisterDevice(iplist)
  }).then(() => {
    getRegisterDeviceListFun();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  channelList.value = []
  const _id = row.id || ids.value
  getDevice(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改大华设备";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["deviceRef"].validate(valid => {
    if (valid) {
      if(form.value.playType === '1'){
        if (form.value.channel === null) {
          proxy.$modal.msgError("请选择通道号");
          return
        }
      }
      if (form.value.id != null) {
        updateDevice(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          openDevice.value = false
          getList();
        });
      } else {
        addDevice(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          openDevice.value = false
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除大华设备编号为"' + _ids + '"的数据项？').then(function () {
    return delDevice(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('dahua/device/export', {
    ...queryParams.value
  }, `device_${new Date().getTime()}.xlsx`)
}

function login() {
  proxy.$refs["deviceRef"].validate(valid => {
    if (valid) {
      let data = {
        ip: form.value.ip,
        port: form.value.port,
        userName: form.value.userName,
        password: form.value.password,
        deviceId: form.value.deviceId,
      }

      dahuaLogin(data).then(res => {
        channelList.value = res.data
      })
    }
  })
}

const togglePasswordVisibility = (id) => {
  passwordVisibility.value[id] = !passwordVisibility.value[id];
};

/**
 * 开始播放
 *
 * @param row
 */
async function handleStartPlay(row) {
  statusPlay.value = "";
  playType.value = row.playType;
  dahuaDeviceId.value = row.id
  if (row.playType === '2') {
    const ans = await startPlay(row.streamId);
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
    openPlay.value = true;
  } else if (row.playType === '3') {
    videoUrl.value = row.easyNTSUrl;
    title.value = row.name;
    openPlay.value = true;
  } else if (row.playType === '4') {
    // videoUrl.value = row.easyNTSUrl;
    // title.value = row.name;
    // openPlay.value = true;
    statusPlay.value = "SDK";
    SDKID.value = row.id;
    startRealPlay(row.id).then(res => {
      videoUrl.value = res.data.rtsp;
      openPlay.value = true;
    })
  }else if (row.playType === '1') {
    let newUrl = row.url.replace(/(channel=)(\d+)/, function(_, p1, p2) {
      return p1 + (parseInt(p2) + 1);
    });
    videoUrl.value = newUrl
    openPlay.value = true
  }

}

/**
 * 大华设备云台控制（开始）
 *
 * @param direction
 */
function ptzControlUpStartFun(direction) {
  ptzControlUpStart(dahuaDeviceId.value, direction, controSpeed.value)
}

/**
 * 大华设备云台控制（开始）
 *
 * @param direction
 */
function ptzControlUpEndFun(direction) {
  setTimeout(() => {
    ptzControlUpEnd(dahuaDeviceId.value, direction)
  }, 200)
}

/**
 * 大华设备抓图
 *
 * @param row
 */
function handleSnapPicture(row) {
  snapPicture(row.id).then(() => {
    proxy.$modal.msgSuccess("抓图成功");
  })
}

/**
 * 大华设备抓图列表
 *
 * @param row
 */
function handleSnapPictureList(row) {
  screenshotQueryParams.value.dahuaDeviceId = row.id
  screenshotQueryParams.value.name = row.name
  getListScreenshotFun();
  openSnapPicture.value = true
}

/**
 * 设备抓图列表
 */
function getListScreenshotFun() {
  listScreenshot(screenshotQueryParams.value).then(res => {
    screenshotList.value = res.rows
    screenshotTotal.value = res.total
  })
}

/**
 * 删除大华设备抓图
 *
 * @param row
 */
function handleRemoveScreenshot(row) {
  const _ids = row.id;
  proxy.$modal.confirm('是否删除大华设备抓图').then(function () {
    return removeScreenshot(_ids);
  }).then(() => {
    getListScreenshotFun();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/**
 * 大华设备定时抓图
 *
 * @param row
 */
function handleTimerCapturePicture(row) {
  timerCapturePicture(row.id).then(() => {
    proxy.$modal.msgSuccess("定时抓图开启成功");
  })
}

/**
 * 大华设备停止定时抓图
 */
function handleStopCapturePicture(row) {
  stopCapturePicture(row.id).then(() => {
    proxy.$modal.msgSuccess("停止定时抓图成功");
  })
}

/**
 * 大华设备控制
 */
function handleControl(row) {
  controlQueryParams.value.dahuaDeviceId = row.id
  controlQueryParams.value.name = row.name

  openControl.value = true
}

/**
 * 大华设备重启
 */
function handleReboot() {
  proxy.$modal.confirm('是否重启设备').then(function () {
    return reboot(controlQueryParams.value.dahuaDeviceId);
  }).then(() => {
    proxy.$modal.msgSuccess("重启成功");
  }).catch(() => {
  });
}

/**
 * 大华设备获取时间
 */
function handleGetTime(){
  getTime(controlQueryParams.value.dahuaDeviceId).then((res)=>{
    deviceTime.value = res.msg
  })
}

/**
 * 大华设备设置时间
 */
function handleSetTime(){
  setTime(controlQueryParams.value.dahuaDeviceId,controlQueryParams.value.date,controlQueryParams.value.type).then(()=>{
    proxy.$modal.msgSuccess("设置成功");
  })
}

// 多选框选中数据
function handleRegisterDeviceSelectionChange(selection) {
  ips.value = selection.map(item => item.ip);
  multipleRegisterDevice.value = !selection.length;
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
