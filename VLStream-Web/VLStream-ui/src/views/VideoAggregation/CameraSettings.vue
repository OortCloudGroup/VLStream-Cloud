<template>
  <div class="camera-settings">
    <!-- 导航面包屑 - 仅在独立页面模式显示 -->
    <div v-if="!deviceInfo" class="content-header">
      <div class="breadcrumb">
        <span class="breadcrumb-item" @click="goBack">设备列表</span>
        <span class="breadcrumb-separator">></span>
        <span class="breadcrumb-item active">设置摄像机</span>
      </div>
    </div>

    <div class="settings-container">
      <!-- 中间视频和控制区域 -->
      <div class="video-control-container">
        <!-- 显示设置标签页 -->
        <div class="settings-tabs">
          <div class="tab-header">
            <el-button 
              :class="['tab-btn', { active: activeTab === 'display' }]"
              @click="activeTab = 'display'"
            >
              显示设置
            </el-button>
            <el-button 
              :class="['tab-btn', { active: activeTab === 'osd' }]"
              @click="activeTab = 'osd'"
            >
              OSD设置
            </el-button>
          </div>
          
          <!-- 场景选择 -->
          <div v-if="activeTab === 'display'" class="scene-selector">
            <div class="scene-label">场景</div>
            <div class="scene-select-container">
              <el-select
                v-model="selectedScene"
                placeholder="请选择场景"
                class="scene-select"
                @change="handleSceneChange"
              >
                <el-option
                  v-for="scene in sceneOptions"
                  :key="scene.value"
                  :label="scene.label"
                  :value="scene.value"
                />
              </el-select>
            </div>
          </div>
        </div>

        <!-- 视频显示区域 -->
        <div class="video-display">
          <div class="video-container">
            <div class="video-content">
              <div class="video-timestamp">2021年04月15日</div>
              <!-- 模拟视频画面 -->
              <div class="video-placeholder">
                <div class="placeholder-content">
                  <div class="plant-image"></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 图像调节区域 -->
        <div class="image-adjustment">
          <!-- 显示设置内容 -->
          <div v-if="activeTab === 'display'" class="all-settings">
            <el-collapse v-model="activeCollapse">
              <el-collapse-item name="image-controls" title="图像调节">
                <div class="adjustment-controls">
                  <!-- 亮度 -->
                  <div class="control-item">
                    <span class="control-label">亮度</span>
                    <div class="slider-container">
                      <el-slider
                        v-model="imageSettings.brightness"
                        :min="0"
                        :max="100"
                        class="adjustment-slider"
                      />
                      <span class="value-display">{{ imageSettings.brightness }}</span>
                    </div>
                  </div>

                  <!-- 对比度 -->
                  <div class="control-item">
                    <span class="control-label">对比度</span>
                    <div class="slider-container">
                      <el-slider
                        v-model="imageSettings.contrast"
                        :min="0"
                        :max="100"
                        class="adjustment-slider"
                      />
                      <span class="value-display">{{ imageSettings.contrast }}</span>
                    </div>
                  </div>

                  <!-- 饱和度 -->
                  <div class="control-item">
                    <span class="control-label">饱和度</span>
                    <div class="slider-container">
                      <el-slider
                        v-model="imageSettings.saturation"
                        :min="0"
                        :max="100"
                        class="adjustment-slider"
                      />
                      <span class="value-display">{{ imageSettings.saturation }}</span>
                    </div>
                  </div>

                  <!-- 锐度 -->
                  <div class="control-item">
                    <span class="control-label">锐度</span>
                    <div class="slider-container">
                      <el-slider
                        v-model="imageSettings.sharpness"
                        :min="0"
                        :max="100"
                        class="adjustment-slider"
                      />
                      <span class="value-display">{{ imageSettings.sharpness }}</span>
                    </div>
                  </div>
                </div>
              </el-collapse-item>
              
              <el-collapse-item name="lighting" title="曝光">
                <div class="setting-content">
                  <!-- 曝光模式 -->
                  <div class="setting-item">
                    <label class="setting-label">曝光模式</label>
                    <el-select v-model="exposureSettings.mode" class="setting-select">
                      <el-option label="自动" value="auto" />
                      <el-option label="手动" value="manual" />
                      <el-option label="快门优先" value="shutter-priority" />
                      <el-option label="光圈优先" value="aperture-priority" />
                    </el-select>
                  </div>

                  <!-- 最大快门限制 -->
                  <div class="setting-item">
                    <label class="setting-label">最大快门限制</label>
                    <el-select v-model="exposureSettings.maxShutter" class="setting-select">
                      <el-option label="1/25" value="1/25" />
                      <el-option label="1/30" value="1/30" />
                      <el-option label="1/50" value="1/50" />
                      <el-option label="1/60" value="1/60" />
                      <el-option label="1/100" value="1/100" />
                      <el-option label="1/120" value="1/120" />
                    </el-select>
                  </div>

                  <!-- 最小快门限制 -->
                  <div class="setting-item">
                    <label class="setting-label">最小快门限制</label>
                    <el-select v-model="exposureSettings.minShutter" class="setting-select">
                      <el-option label="1/1000" value="1/1000" />
                      <el-option label="1/2000" value="1/2000" />
                      <el-option label="1/3000" value="1/3000" />
                      <el-option label="1/4000" value="1/4000" />
                      <el-option label="1/5000" value="1/5000" />
                      <el-option label="1/10000" value="1/10000" />
                    </el-select>
                  </div>

                  <!-- 增益限制 -->
                  <div class="setting-item">
                    <label class="setting-label">增益限制</label>
                    <div class="slider-container">
                      <el-slider
                        v-model="exposureSettings.gainLimit"
                        :min="0"
                        :max="100"
                        class="setting-slider"
                      />
                      <span class="value-display">{{ exposureSettings.gainLimit }}</span>
                    </div>
                  </div>

                  <!-- 低照度电子快门 -->
                  <div class="setting-item">
                    <label class="setting-label">低照度电子快门</label>
                    <el-select v-model="exposureSettings.lowLightShutter" class="setting-select">
                      <el-option label="关闭" value="off" />
                      <el-option label="开启" value="on" />
                      <el-option label="自动" value="auto" />
                    </el-select>
                  </div>
                </div>
              </el-collapse-item>
              
              <el-collapse-item name="focus" title="聚焦">
                <div class="setting-content">
                  <!-- 聚焦模式 -->
                  <div class="setting-item">
                    <label class="setting-label">聚焦模式</label>
                    <el-select v-model="focusSettings.mode" class="setting-select">
                      <el-option label="自动" value="auto" />
                      <el-option label="半自动" value="semi-auto" />
                      <el-option label="手动" value="manual" />
                    </el-select>
                  </div>

                  <!-- 最小聚焦距离 -->
                  <div class="setting-item">
                    <label class="setting-label">最小聚焦距离</label>
                    <el-select v-model="focusSettings.minDistance" class="setting-select">
                      <el-option label="0.5m" value="0.5m" />
                      <el-option label="1.0m" value="1.0m" />
                      <el-option label="1.5m" value="1.5m" />
                      <el-option label="2.0m" value="2.0m" />
                      <el-option label="3.0m" value="3.0m" />
                      <el-option label="5.0m" value="5.0m" />
                      <el-option label="无限远" value="infinity" />
                    </el-select>
                  </div>
                </div>
              </el-collapse-item>
              
              <el-collapse-item name="day-night" title="日夜转换">
                <div class="setting-content">
                  <!-- 日夜转换 -->
                  <div class="setting-item">
                    <label class="setting-label">日夜转换</label>
                    <el-select v-model="dayNightSettings.mode" class="setting-select">
                      <el-option label="自动" value="auto" />
                      <el-option label="白天" value="day" />
                      <el-option label="夜间" value="night" />
                      <el-option label="定时" value="scheduled" />
                    </el-select>
                  </div>

                  <!-- 灵敏度 -->
                  <div class="setting-item">
                    <label class="setting-label">灵敏度</label>
                    <el-select v-model="dayNightSettings.sensitivity" class="setting-select">
                      <el-option label="1" value="1" />
                      <el-option label="2" value="2" />
                      <el-option label="3" value="3" />
                      <el-option label="4" value="4" />
                      <el-option label="5" value="5" />
                    </el-select>
                  </div>

                  <!-- 防补光过曝 -->
                  <div class="setting-item">
                    <label class="setting-label">防补光过曝</label>
                    <el-select v-model="dayNightSettings.antiOverexposure" class="setting-select">
                      <el-option label="关闭" value="off" />
                      <el-option label="开启" value="on" />
                    </el-select>
                  </div>

                  <!-- 红外灯模式 -->
                  <div class="setting-item">
                    <label class="setting-label">红外灯模式</label>
                    <el-select v-model="dayNightSettings.infraredMode" class="setting-select">
                      <el-option label="自动" value="auto" />
                      <el-option label="开启" value="on" />
                      <el-option label="关闭" value="off" />
                      <el-option label="智能" value="smart" />
                    </el-select>
                  </div>

                  <!-- 亮度限制 -->
                  <div class="setting-item">
                    <label class="setting-label">亮度限制</label>
                    <div class="slider-container">
                      <el-slider
                        v-model="dayNightSettings.brightnessLimit"
                        :min="0"
                        :max="100"
                        class="setting-slider"
                      />
                      <span class="value-display">{{ dayNightSettings.brightnessLimit }}</span>
                    </div>
                  </div>
                </div>
              </el-collapse-item>
              
              <el-collapse-item name="backlight" title="背光">
                <div class="setting-content">
                  <!-- 背光补偿 -->
                  <div class="setting-item">
                    <label class="setting-label">背光补偿</label>
                    <el-select v-model="backlightSettings.compensation" class="setting-select">
                      <el-option label="关闭" value="off" />
                      <el-option label="开启" value="on" />
                      <el-option label="自动" value="auto" />
                    </el-select>
                  </div>

                  <!-- 宽动态 -->
                  <div class="setting-item">
                    <label class="setting-label">宽动态</label>
                    <el-select v-model="backlightSettings.wideDynamic" class="setting-select">
                      <el-option label="关闭" value="off" />
                      <el-option label="开启" value="on" />
                      <el-option label="自动" value="auto" />
                    </el-select>
                  </div>

                  <!-- 强光抑制 -->
                  <div class="setting-item">
                    <label class="setting-label">强光抑制</label>
                    <el-select v-model="backlightSettings.strongLightSuppression" class="setting-select">
                      <el-option label="关闭" value="off" />
                      <el-option label="开启" value="on" />
                      <el-option label="自动" value="auto" />
                    </el-select>
                  </div>
                </div>
              </el-collapse-item>
              
              <el-collapse-item name="white-balance" title="白平衡">
                <div class="setting-content">
                  <!-- 白平衡 -->
                  <div class="setting-item">
                    <label class="setting-label">白平衡</label>
                    <el-select v-model="whiteBalanceSettings.mode" class="setting-select">
                      <el-option label="自动白平衡" value="auto" />
                      <el-option label="手动白平衡" value="manual" />
                      <el-option label="白炽灯" value="incandescent" />
                      <el-option label="荧光灯" value="fluorescent" />
                      <el-option label="日光" value="daylight" />
                      <el-option label="阴天" value="cloudy" />
                      <el-option label="一次白平衡" value="one-push" />
                    </el-select>
                  </div>
                </div>
              </el-collapse-item>
              
              <el-collapse-item name="image-enhancement" title="图像增强">
                <div class="setting-content">
                  <!-- 数字降噪 -->
                  <div class="setting-item">
                    <label class="setting-label">数字降噪</label>
                    <el-select v-model="imageEnhancementSettings.digitalNoiseReduction" class="setting-select">
                      <el-option label="关闭" value="off" />
                      <el-option label="普通模式" value="normal" />
                      <el-option label="高级模式" value="advanced" />
                      <el-option label="自动" value="auto" />
                    </el-select>
                  </div>

                  <!-- 降噪等级 -->
                  <div class="setting-item">
                    <label class="setting-label">降噪等级</label>
                    <div class="slider-container">
                      <el-slider
                        v-model="imageEnhancementSettings.noiseReductionLevel"
                        :min="0"
                        :max="100"
                        class="setting-slider"
                      />
                      <span class="value-display">{{ imageEnhancementSettings.noiseReductionLevel }}</span>
                    </div>
                  </div>

                  <!-- 透雾模式 -->
                  <div class="setting-item">
                    <label class="setting-label">透雾模式</label>
                    <el-select v-model="imageEnhancementSettings.defogging" class="setting-select">
                      <el-option label="关闭" value="off" />
                      <el-option label="开启" value="on" />
                      <el-option label="自动" value="auto" />
                    </el-select>
                  </div>

                  <!-- 电子防抖 -->
                  <div class="setting-item">
                    <label class="setting-label">电子防抖</label>
                    <el-select v-model="imageEnhancementSettings.electronicStabilization" class="setting-select">
                      <el-option label="关闭" value="off" />
                      <el-option label="开启" value="on" />
                      <el-option label="自动" value="auto" />
                    </el-select>
                  </div>
                </div>
              </el-collapse-item>
              
              <el-collapse-item name="video-adjustment" title="视频调整">
                <div class="setting-content">
                  <!-- 镜像 -->
                  <div class="setting-item">
                    <label class="setting-label">镜像</label>
                    <el-select v-model="videoAdjustmentSettings.mirror" class="setting-select">
                      <el-option label="关闭" value="off" />
                      <el-option label="水平镜像" value="horizontal" />
                      <el-option label="垂直镜像" value="vertical" />
                      <el-option label="水平垂直镜像" value="both" />
                    </el-select>
                  </div>

                  <!-- 视频制式 -->
                  <div class="setting-item">
                    <label class="setting-label">视频制式</label>
                    <el-select v-model="videoAdjustmentSettings.videoStandard" class="setting-select">
                      <el-option label="PAL (50HZ)" value="PAL_50HZ" />
                      <el-option label="NTSC (60HZ)" value="NTSC_60HZ" />
                      <el-option label="AUTO" value="AUTO" />
                    </el-select>
                  </div>
                </div>
              </el-collapse-item>
              
              <el-collapse-item name="other" title="其他">
                <div class="setting-content">
                  <!-- 镜头初始化 -->
                  <div class="setting-item">
                    <label class="setting-label">镜头初始化</label>
                    <el-select v-model="otherSettings.lensInitialization" class="setting-select">
                      <el-option label="关闭" value="off" />
                      <el-option label="开启" value="on" />
                      <el-option label="自动" value="auto" />
                    </el-select>
                  </div>

                  <!-- 变倍限制 -->
                  <div class="setting-item">
                    <label class="setting-label">变倍限制</label>
                    <el-select v-model="otherSettings.zoomLimit" class="setting-select">
                      <el-option label="1" value="1" />
                      <el-option label="2" value="2" />
                      <el-option label="3" value="3" />
                      <el-option label="4" value="4" />
                      <el-option label="5" value="5" />
                      <el-option label="10" value="10" />
                      <el-option label="20" value="20" />
                      <el-option label="无限制" value="unlimited" />
                    </el-select>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>

          <!-- OSD设置内容 -->
          <div v-else-if="activeTab === 'osd'" class="osd-settings">
            <!-- OSD属性设置 -->
            <div class="osd-config-section">
              <div class="osd-config-item">
                <label class="osd-label">OSD属性</label>
                <el-select v-model="osdSettings.attributes" class="osd-select">
                  <el-option label="不透明，不闪烁" value="opaque-stable" />
                  <el-option label="透明，不闪烁" value="transparent-stable" />
                  <el-option label="不透明，闪烁" value="opaque-flashing" />
                  <el-option label="透明，闪烁" value="transparent-flashing" />
                </el-select>
              </div>

              <div class="osd-config-item">
                <label class="osd-label">OSD字体</label>
                <el-select v-model="osdSettings.font" class="osd-select">
                  <el-option label="自适应" value="adaptive" />
                  <el-option label="小号" value="small" />
                  <el-option label="中号" value="medium" />
                  <el-option label="大号" value="large" />
                </el-select>
              </div>

              <div class="osd-config-item">
                <label class="osd-label">OSD颜色</label>
                <el-select v-model="osdSettings.color" class="osd-select">
                  <el-option label="黑白自动" value="auto-bw" />
                  <el-option label="白色" value="white" />
                  <el-option label="黑色" value="black" />
                  <el-option label="红色" value="red" />
                  <el-option label="绿色" value="green" />
                  <el-option label="蓝色" value="blue" />
                </el-select>
              </div>

              <div class="osd-config-item">
                <label class="osd-label">对齐方式</label>
                <el-select v-model="osdSettings.alignment" class="osd-select">
                  <el-option label="自适应" value="adaptive" />
                  <el-option label="左对齐" value="left" />
                  <el-option label="居中" value="center" />
                  <el-option label="右对齐" value="right" />
                </el-select>
              </div>

              <div class="osd-save-section">
                <el-button type="primary" @click="saveOSDSettings" class="save-btn">
                  保存
                </el-button>
              </div>
            </div>
          </div>

          <!-- 恢复默认设置按钮 -->
          <div v-if="activeTab === 'display'" class="reset-container">
            <el-button @click="resetToDefault" class="reset-btn">恢复默认设置</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧控制区域 - PTZ和预制点面板水平对齐 -->
      <div class="right-controls-container" :class="{ 'osd-mode': activeTab === 'osd' }">
        <!-- PTZ控制区域 -->
        <div class="ptz-container">
          <PTZControl 
            :show-camera-management="false"
            @ptz-control="handlePTZControl"
            @zoom-control="handleZoomControl"
            @control-action="handleControlAction"
          />
        </div>

        <!-- 预制点面板 - 显示设置和OSD设置都显示 -->
        <PresetPanel 
          :preset-points="presetPoints"
          @preset-click="handlePresetClick"
          @osd-settings-save="handleOSDSettingsSave"
          :show-osd-features="activeTab === 'osd'"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import PTZControl from '@/components/PTZControl.vue'
import PresetPanel from '@/components/PresetPanel.vue'

// Props
const props = defineProps({
  deviceInfo: {
    type: Object,
    default: null
  }
})

// Emits
const emit = defineEmits(['back'])

const router = useRouter()

// 当前激活的标签页
const activeTab = ref('display')



// 折叠面板激活项
const activeCollapse = ref(['image-controls'])

// 图像设置参数
const imageSettings = ref({
  brightness: 50,
  contrast: 50,
  saturation: 50,
  sharpness: 50
})

// 曝光设置参数
const exposureSettings = ref({
  mode: 'auto',
  maxShutter: '1/25',
  minShutter: '1/3000',
  gainLimit: 50,
  lowLightShutter: 'off'
})

// 聚焦设置参数
const focusSettings = ref({
  mode: 'semi-auto',
  minDistance: '1.5m'
})

// 日夜转换设置参数
const dayNightSettings = ref({
  mode: 'auto',
  sensitivity: '2',
  antiOverexposure: 'off',
  infraredMode: 'auto',
  brightnessLimit: 50
})

// 背光设置参数
const backlightSettings = ref({
  compensation: 'off',
  wideDynamic: 'off',
  strongLightSuppression: 'off'
})

// 白平衡设置参数
const whiteBalanceSettings = ref({
  mode: 'auto'
})

// 图像增强设置参数
const imageEnhancementSettings = ref({
  digitalNoiseReduction: 'normal',
  noiseReductionLevel: 50,
  defogging: 'off',
  electronicStabilization: 'off'
})

// 视频调整设置参数
const videoAdjustmentSettings = ref({
  mirror: 'off',
  videoStandard: 'PAL_50HZ'
})

// 其他设置参数
const otherSettings = ref({
  lensInitialization: 'off',
  zoomLimit: '2'
})

// OSD设置参数
const osdSettings = ref({
  attributes: 'opaque-stable',
  font: 'adaptive', 
  color: 'auto-bw',
  alignment: 'adaptive'
})



// 场景选择相关
const selectedScene = ref('indoor')
const sceneOptions = ref([
  { label: '室内', value: 'indoor' },
  { label: '室外', value: 'outdoor' },
  { label: '夜间', value: 'night' },
  { label: '逆光', value: 'backlight' },
  { label: '自定义', value: 'custom' }
])

// 预制点数据
const presetPoints = ref([
  { id: 1, name: '预置点1' },
  { id: 2, name: '预置点1' },
  { id: 3, name: '预置点1' },
  { id: 4, name: '预置点1' },
  { id: 5, name: '预置点1' },
  { id: 6, name: '预置点1' },
  { id: 7, name: '预置点1' },
  { id: 8, name: '预置点1' },
  { id: 9, name: '预置点1' },
  { id: 10, name: '预置点1' }
])

// 返回设备列表
const goBack = () => {
  // 如果在内嵌模式（有deviceInfo props），发出back事件
  if (props.deviceInfo) {
    emit('back')
  } else {
    // 独立页面模式，使用路由跳转
    router.push('/device-management')
  }
}

// 处理场景切换
const handleSceneChange = (value) => {
  console.log('切换场景:', value)
  ElMessage.success(`切换到场景: ${sceneOptions.value.find(s => s.value === value)?.label}`)
  
  // 根据不同场景自动调整图像参数
  switch (value) {
    case 'indoor':
      imageSettings.value = { brightness: 50, contrast: 50, saturation: 50, sharpness: 50 }
      break
    case 'outdoor':
      imageSettings.value = { brightness: 60, contrast: 55, saturation: 45, sharpness: 55 }
      break
    case 'night':
      imageSettings.value = { brightness: 70, contrast: 45, saturation: 40, sharpness: 45 }
      break
    case 'backlight':
      imageSettings.value = { brightness: 55, contrast: 60, saturation: 50, sharpness: 50 }
      break
    case 'custom':
      // 自定义场景保持当前设置
      break
  }
}

// PTZ控制事件处理
const handlePTZControl = (direction) => {
  console.log('PTZ控制:', direction)
  ElMessage.success(`PTZ ${direction} 控制`)
}

// 变倍控制事件处理
const handleZoomControl = (action) => {
  console.log('变倍控制:', action)
  ElMessage.success(`变倍 ${action}`)
}

// 功能控制事件处理
const handleControlAction = (action) => {
  console.log('控制操作:', action)
  ElMessage.success(`执行操作: ${action}`)
}

// 恢复默认设置
const resetToDefault = () => {
  imageSettings.value = {
    brightness: 50,
    contrast: 50,
    saturation: 50,
    sharpness: 50
  }
  
  exposureSettings.value = {
    mode: 'auto',
    maxShutter: '1/25',
    minShutter: '1/3000',
    gainLimit: 50,
    lowLightShutter: 'off'
  }
  
  focusSettings.value = {
    mode: 'semi-auto',
    minDistance: '1.5m'
  }
  
  dayNightSettings.value = {
    mode: 'auto',
    sensitivity: '2',
    antiOverexposure: 'off',
    infraredMode: 'auto',
    brightnessLimit: 50
  }
  
  backlightSettings.value = {
    compensation: 'off',
    wideDynamic: 'off',
    strongLightSuppression: 'off'
  }
  
  whiteBalanceSettings.value = {
    mode: 'auto'
  }
  
  imageEnhancementSettings.value = {
    digitalNoiseReduction: 'normal',
    noiseReductionLevel: 50,
    defogging: 'off',
    electronicStabilization: 'off'
  }
  
  videoAdjustmentSettings.value = {
    mirror: 'off',
    videoStandard: 'PAL_50HZ'
  }
  
  otherSettings.value = {
    lensInitialization: 'off',
    zoomLimit: '2'
  }
  
  ElMessage.success('已恢复默认设置')
}

// 处理预制点点击
const handlePresetClick = (preset) => {
  console.log('点击预制点:', preset)
  ElMessage.success(`跳转到${preset.name}`)
}

// 保存OSD设置（主页面）
const saveOSDSettings = () => {
  console.log('保存OSD设置:', osdSettings.value)
  ElMessage.success('OSD设置已保存')
}

// 处理OSD设置保存（PresetPanel组件）
const handleOSDSettingsSave = (settings) => {
  console.log('保存预置点OSD设置:', settings)
  ElMessage.success('预置点OSD设置已保存')
}
</script>

<style scoped>
.camera-settings {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

/* 内容头部导航样式 */
.content-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: white;
}

.breadcrumb {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.breadcrumb-item {
  color: #409eff;
  cursor: pointer;
  transition: color 0.3s ease;
}

.breadcrumb-item:hover {
  color: #66b1ff;
}

.breadcrumb-item.active {
  color: #303133;
  cursor: default;
}

.breadcrumb-separator {
  margin: 0 8px;
  color: #c0c4cc;
}

/* 主要容器布局 */
.settings-container {
  flex: 1;
  display: flex;
  align-items: flex-start;
  gap: 0; /* 移除间隙 */
  padding: 0; /* 移除所有padding，让内容直接对齐 */
  margin: 0; /* 确保容器无外边距 */
  min-height: 0;
  overflow: hidden; /* 允许子元素处理自己的滚动 */
}

/* 中间视频和控制区域 */
.video-control-container {
  width: 25%;
  display: flex;
  flex-direction: column;
  gap: 0; /* 移除间隙 */
  margin-right: 0; /* 移除右边距 */
  margin-left: 0; /* 移除左边距，与面包屑边框对齐 */
  overflow: hidden; /* 让子元素处理滚动 */
  max-height: 100%; /* 限制最大高度为容器高度 */
}

/* 标签页 */
.settings-tabs {
  background: white;
  border-radius: 6px 6px 0 0; /* 只保留顶部圆角 */
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  margin: 0; /* 完全移除所有外边距 */
  flex-shrink: 0; /* 防止标签页被压缩 */
}

.tab-header {
  padding: 16px 0px 16px 0px; /* 移除左内边距，与左边框对齐 */
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 8px 16px;
  border: none;
  background: transparent; /* 移除背景色 */
  color: #909399; /* 默认灰色文字 */
  border-radius: 0; /* 移除圆角 */
  transition: all 0.3s ease;
  font-size: 16px;
}

.tab-btn:first-child {
  margin-left: 0; /* 移除额外左边距，容器已有20px内边距 */
}

.tab-btn:not(:first-child) {
  margin-left: 8px; /* 其他按钮正常间距 */
}

.tab-btn.active {
  background: transparent; /* 移除背景色 */
  color: #409eff; /* 蓝色文字表示激活状态 */
}

.tab-btn:hover:not(.active) {
  background: transparent; /* 移除背景色 */
  color: #409eff; /* 悬停时也变为蓝色 */
}

/* 场景选择器 */
.scene-selector {
  padding: 16px 20px 16px 0px; /* 移除左内边距，与左边框对齐 */
  display: flex;
  flex-direction: column; /* 改为垂直布局 */
  gap: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.scene-label {
  font-size: 16px; /* 与按钮文字大小一致 */
  color: #909399; /* 与按钮默认颜色一致 */
  padding: 8px 16px; /* 与按钮padding一致，实现左对齐 */
  margin: 0; /* 移除边距 */
}

.scene-select-container {
  padding-left: 16px; /* 与按钮左边距对齐 */
}

.scene-select {
  width: 200px;
}

/* 视频显示区域 */
.video-display {
  flex-shrink: 0; /* 防止视频区域被压缩 */
  background: white;
  border-radius: 0; /* 移除圆角 */
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin: 0; /* 移除外边距，由父容器控制 */
}

.video-container {
  position: relative;
  background: #000;
  border-radius: 4px;
  overflow: hidden;
}

.video-content {
  position: relative;
  width: 100%;
  height: 180px;
}

.video-timestamp {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  z-index: 10;
}

.video-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-content {
  text-align: center;
  color: white;
}

.plant-image {
  width: 200px;
  height: 150px;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 150"><rect width="200" height="150" fill="%23228B22"/><circle cx="100" cy="75" r="40" fill="%23006400"/><rect x="95" y="115" width="10" height="35" fill="%238B4513"/></svg>') no-repeat center;
  background-size: contain;
  margin: 0 auto;
}

/* 图像调节区域 */
.image-adjustment {
  background: white;
  border-radius: 0 0 6px 6px; /* 只保留底部圆角 */
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin: 0; /* 移除外边距，由父容器控制 */
  flex: 1; /* 允许内容区域扩展 */
  overflow-y: auto; /* 允许内容滚动 */
  min-height: 0; /* 确保可以收缩 */
}

.adjustment-controls {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 4px;
}

.control-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.control-label {
  width: 60px;
  font-size: 14px;
  color: #606266;
  flex-shrink: 0;
}

.slider-container {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.adjustment-slider {
  flex: 1;
}

.value-display {
  width: 30px;
  text-align: center;
  font-size: 14px;
  color: #606266;
}

.setting-content {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 4px;
  color: #606266;
  font-size: 14px;
}

/* 设置项样式 */
.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-label {
  font-size: 14px;
  color: #606266;
  min-width: 120px;
  text-align: left;
}

.setting-select {
  width: 150px;
}

.setting-slider {
  flex: 1;
  margin-right: 12px;
}

.reset-container {
  display: flex;
  justify-content: center;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.reset-btn {
  background: #409eff;
  color: white;
  border: none;
  padding: 8px 24px;
  border-radius: 4px;
}

.reset-btn:hover {
  background: #66b1ff;
}

/* 所有设置和OSD设置的布局支持 */
.all-settings {
  flex: 1;
  min-height: 0;
}

.osd-settings {
  flex: 1;
  min-height: 0;
}

/* OSD配置区域样式 */
.osd-config-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 4px;
}

.osd-config-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.osd-config-item:last-child {
  margin-bottom: 0;
}

.osd-label {
  font-size: 14px;
  color: #606266;
  min-width: 80px;
  text-align: left;
}

.osd-select {
  width: 180px;
}

.osd-save-section {
  display: flex;
  justify-content: center;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.save-btn {
  background: #409eff;
  color: white;
  border: none;
  padding: 8px 24px;
  border-radius: 4px;
}

.save-btn:hover {
  background: #66b1ff;
}

/* 右侧控制区域 - PTZ和预制点面板的容器 */
.right-controls-container {
  display: flex;
  align-items: flex-start;
  gap: 0; /* 无缝隙连接 */
  margin: 0;
  padding: 0; /* 确保容器本身无内边距 */
  /* 添加上边距，使其与视频显示区域顶部对齐 */
  margin-top: 180px; /* 增加偏移量，对齐视频播放区域 */
  flex-shrink: 0; /* 防止右侧控制区域被压缩 */
}

/* OSD模式下的对齐（场景选择区域隐藏） */
.right-controls-container.osd-mode {
  margin-top: 65px; /* 只有标签页区域(65px) */
}

/* PTZ控制区域 */
.ptz-container {
  width: 320px;
  height: auto;
  border-radius: 0;
  padding: 0; /* 移除内边距，让PTZ组件贴边 */
  margin: 0;
  overflow: visible;
}



/* 响应式设计 */
@media (max-width: 1200px) {
  .settings-container {
    flex-direction: column;
  }
  
  .video-control-container {
    width: 100%;
    margin-top: 0;
  }
  
  .right-controls-container {
    flex-direction: column;
    width: 100%;
  }
  
  .ptz-container {
    width: 100%;
    margin-top: 0;
  }
}


</style> 