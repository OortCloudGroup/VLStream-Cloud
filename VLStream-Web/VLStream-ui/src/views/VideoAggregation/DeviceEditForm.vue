<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div class="device-edit-form">
    <div class="edit-content">
      <!-- info -->
      <div class="section">
        <h3 class="section-title">基本信息</h3>

        <el-form
          :model="formData"
          :rules="formRules"
          ref="editFormRef"
          label-width="100px"
          class="edit-form"
        >
          <el-form-item label="视频流路径" prop="streamUrl" class="required-field">
            <el-input
              v-model="formData.streamUrl"
              placeholder="请输入视频流路径"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="设备名称" prop="deviceName">
            <el-input
              v-model="formData.deviceName"
              placeholder="自动生成名称"
            />
          </el-form-item>

          <el-form-item label="设备标签" prop="selectedTags">
            <TagSelector
              v-model="formData.selectedTags"
              placeholder="请选择设备标签"
              @change="handleTagChange"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- info -->
      <div class="section">
        <h3 class="section-title">更多信息</h3>

        <el-form
          :model="formData"
          label-width="100px"
          class="edit-form"
        >
          <el-form-item label="设备ID">
            <el-input
              v-model="formData.deviceId"
              placeholder="输入设备ID"
            />
          </el-form-item>

          <el-form-item label="类型">
            <el-select
              v-model="formData.deviceType"
              placeholder="选择设备类型"
              style="width: 100%"
            >
              <el-option label="摄像头" value="摄像头" />
              <el-option label="云台" value="云台" />
              <el-option label="球机" value="球机" />
              <el-option label="枪机" value="枪机" />
              <el-option label="半球" value="半球" />
            </el-select>
          </el-form-item>

          <el-form-item label="图片路径">
            <el-input
              v-model="formData.imagePath"
              placeholder="输入图片路径"
            />
          </el-form-item>

          <el-form-item label="经纬度坐标">
            <div class="coordinate-input">
              <el-input
                v-model="formData.longitude"
                placeholder="经度: 116.39139 或 116°23′29″E"
                class="coordinate-item"
              />
              <el-input
                v-model="formData.latitude"
                placeholder="纬度: 39.90917 或 39°54′26″N"
                class="coordinate-item"
              />
              <el-button type="primary" class="locate-btn" @click="openMapSelector">
                地图选点
              </el-button>
            </div>
          </el-form-item>

          <el-form-item label="高度位置">
            <div class="height-options">
              <el-radio-group v-model="formData.heightPosition">
                <el-radio label="高空">高空</el-radio>
                <el-radio label="地面">地面</el-radio>
                <el-radio label="地下">地下</el-radio>
                <el-radio label="其他">其他</el-radio>
              </el-radio-group>
            </div>
          </el-form-item>

          <el-form-item label="详细地址">
            <el-input
              v-model="formData.address"
              placeholder="输入详细地址：省/市/区（县）/街道（村）"
            />
          </el-form-item>

          <el-form-item label="区划选择">
            <el-cascader
              v-model="formData.region"
              :options="regionOptions"
              placeholder="选择区划"
              style="width: 100%"
              clearable
            />
          </el-form-item>

          <el-form-item label="备注">
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入备注信息"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- button -->
      <div class="edit-footer">
        <el-button @click="handleCancel" class="cancel-btn common_btn">取消</el-button>
        <el-button type="primary" @click="handleSave" class="save-btn common_btn">保存</el-button>
      </div>
    </div>

    <!--  -->
    <el-dialog
      v-model="mapDialogVisible"
      title="地图选点"
      width="45%"
    >
      <div class="map-container">
        <div class="map-toolbar">
          <el-button
            type="primary"
            :icon="Location"
            @click="getCurrentLocation"
            :loading="locating"
            size="small"
          >
            {{ locating ? '定位中...' : '定位到当前位置' }}
          </el-button>
          <div class="location-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>点击地图任意位置选择坐标，或使用GPS定位到当前位置</span>
          </div>
        </div>
        <div id="map-selector" class="map-area"></div>
        <div class="coordinate-display">
          <span>经度: {{ selectedCoordinate.longitude }}</span>
          <span>纬度: {{ selectedCoordinate.latitude }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="mapDialogVisible = false" class="common_btn">取消</el-button>
        <el-button type="primary" @click="confirmCoordinate" class="common_btn">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Location, InfoFilled } from '@element-plus/icons-vue'
import { validateStreamUrl, validateCoordinates, generateDeviceName, parseCoordinate } from './deviceUtils.js'
import TagSelector from '@/components/TagSelector.vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  },
  mode: {
    type: String,
    default: 'add', // 'add' | 'edit'
    validator: (value) => ['add', 'edit'].includes(value)
  }
})

const emit = defineEmits(['update:modelValue', 'save', 'cancel'])

// data
const editFormRef = ref(null)
const mapDialogVisible = ref(false)

// formdata
const formData = ref({
  streamUrl: '',
  deviceName: '',
  deviceId: '',
  deviceType: '',
  selectedTags: [],
  imagePath: '',
  longitude: '',
  latitude: '',
  heightPosition: '地面',
  address: '',
  region: [],
  remark: ''
})

// related
const selectedCoordinate = ref({
  longitude: '',
  latitude: ''
})

// instance
let mapInstance = null
let mapMarker = null
let currentLocationMarker = null

//
const locating = ref(false)

// data
const regionOptions = ref([
  {
    value: 'beijing',
    label: '北京市',
    children: [
      {
        value: 'haidian',
        label: '海淀区'
      },
      {
        value: 'chaoyang',
        label: '朝阳区'
      }
    ]
  },
  {
    value: 'shanghai',
    label: '上海市',
    children: [
      {
        value: 'huangpu',
        label: '黄浦区'
      },
      {
        value: 'xuhui',
        label: '徐汇区'
      }
    ]
  }
])

// form
const formRules = computed(() => ({
  streamUrl: [
    { required: true, message: '请输入视频流路径', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && !validateStreamUrl(value)) {
          callback(new Error('请输入有效的视频流地址'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  deviceName: [
    { required: true, message: '请输入设备名称', trigger: 'blur' }
  ]
}))

// data
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    formData.value = {
      ...newValue,
      // selectedTags is array
      selectedTags: Array.isArray(newValue.selectedTags) ? newValue.selectedTags : []
    }

    // Generate device
    if (!formData.value.deviceName && formData.value.deviceId && formData.value.deviceType) {
      formData.value.deviceName = generateDeviceName(formData.value.deviceId, formData.value.deviceType)
    }
  }
}, { immediate: true, deep: true })

// deviceID and device , Generate
watch([() => formData.value.deviceId, () => formData.value.deviceType], () => {
  if (!formData.value.deviceName && formData.value.deviceId && formData.value.deviceType) {
    formData.value.deviceName = generateDeviceName(formData.value.deviceId, formData.value.deviceType)
  }
})

// method
const handleTagChange = (values) => {
  console.log('选择的标签数组:', values)
  formData.value.selectedTags = Array.isArray(values) ? values : []
  console.log('表单数据中的selectedTags:', formData.value.selectedTags)
}

const openMapSelector = () => {
  selectedCoordinate.value.longitude = formData.value.longitude
  selectedCoordinate.value.latitude = formData.value.latitude
  mapDialogVisible.value = true

  // Initialize component
  nextTick(() => {
    initMapSelector()
  })
}

const initMapSelector = () => {
  // instance
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
    mapMarker = null
    currentLocationMarker = null
  }

  // ( )
  const defaultLat = 39.9042
  const defaultLng = 116.4074

  // Get current
  let currentLat = defaultLat
  let currentLng = defaultLng

  if (selectedCoordinate.value.latitude && selectedCoordinate.value.longitude) {
    const parsedLat = parseFloat(selectedCoordinate.value.latitude)
    const parsedLng = parseFloat(selectedCoordinate.value.longitude)

    if (!isNaN(parsedLat) && !isNaN(parsedLng)) {
      currentLat = parsedLat
      currentLng = parsedLng
    }
  }

  // instance
  mapInstance = L.map('map-selector', {
    center: [currentLat, currentLng],
    zoom: 13,
    zoomControl: true,
    attributionControl: true
  })

  // layer ( service)
  const tileServers = [
    'https://tile.openstreetmap.de/{z}/{x}/{y}.png',
    'https://tiles.wmflabs.org/osm/{z}/{x}/{y}.png',
    'https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_all/{z}/{x}/{y}.png',
    'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}'
  ]

  L.tileLayer(tileServers[0], {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    maxZoom: 18,
    errorTileUrl: '/src/assets/error-tile.png' //
  }).addTo(mapInstance)

  // Custom
  const customIcon = L.icon({
    iconUrl: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDJDOC4xMzQgMiA1IDUuMTM0IDUgOUM1IDEyLjg3NSAxMiAyMiAxMiAyMkMxMiAyMiAxOSAxMi44NzUgMTkgOUMxOSA1LjEzNCAxNS44NjYgMiAxMiAyWk0xMiAxMkMxMC4zNDMgMTIgOSAxMC42NTcgOSA5QzkgNy4zNDMgMTAuMzQzIDYgMTIgNkMxMy42NTcgNiAxNSA3LjM0MyAxNSA5QzE1IDEwLjY1NyAxMy42NTcgMTIgMTIgMTJaIiBmaWxsPSIjNDA5RUZGIi8+Cjwvc3ZnPgo=',
    iconSize: [24, 24],
    iconAnchor: [12, 24],
    popupAnchor: [0, -24]
  })

  // if current ,
  if (selectedCoordinate.value.latitude && selectedCoordinate.value.longitude) {
    mapMarker = L.marker([currentLat, currentLng], { icon: customIcon }).addTo(mapInstance)
  }

  // event
  mapInstance.on('click', (e) => {
    const lat = e.latlng.lat.toFixed(6)
    const lng = e.latlng.lng.toFixed(6)

    // new in
    selectedCoordinate.value.latitude = lat
    selectedCoordinate.value.longitude = lng

    // old
    if (mapMarker) {
      mapInstance.removeLayer(mapMarker)
    }

    //
    if (currentLocationMarker) {
      mapInstance.removeLayer(currentLocationMarker)
      currentLocationMarker = null
    }

    // new
    mapMarker = L.marker([lat, lng], { icon: customIcon }).addTo(mapInstance)

    // : popup
    mapMarker.bindPopup(`
      <div style="text-align: center;">
        <strong>📍 选择位置</strong><br>
        经度: ${lng}<br>
        纬度: ${lat}
      </div>
    `).openPopup()
  })

  console.log('Leaflet地图初始化完成')
}

// GPS can
const getCurrentLocation = () => {
  if (!navigator.geolocation) {
    ElMessage.error('您的浏览器不支持地理位置服务')
    return
  }

  locating.value = true

  const options = {
    enableHighAccuracy: true, //
    timeout: 10000, // 10
    maximumAge: 60000 // 1
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      const lat = position.coords.latitude
      const lng = position.coords.longitude
      const accuracy = position.coords.accuracy

      console.log('获取到当前位置:', { lat, lng, accuracy })

      // new in
      selectedCoordinate.value.latitude = lat.toFixed(6)
      selectedCoordinate.value.longitude = lng.toFixed(6)

      // in current
      if (mapInstance) {
        mapInstance.setView([lat, lng], 15) //

        // old
        if (mapMarker) {
          mapInstance.removeLayer(mapMarker)
        }

        // current ( )
        const currentLocationIcon = L.icon({
          iconUrl: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMTIiIGN5PSIxMiIgcj0iMTAiIGZpbGw9IiNmZmZmZmYiIHN0cm9rZT0iIzAwYWFmZiIgc3Ryb2tlLXdpZHRoPSIyIi8+CjxjaXJjbGUgY3g9IjEyIiBjeT0iMTIiIHI9IjMiIGZpbGw9IiMwMGFhZmYiLz4KPC9zdmc+',
          iconSize: [24, 24],
          iconAnchor: [12, 12],
          popupAnchor: [0, -12]
        })

        // current
        mapMarker = L.marker([lat, lng], { icon: currentLocationIcon }).addTo(mapInstance)

        // info
        const accuracyText = accuracy < 100 ? `精度: ${Math.round(accuracy)}米` : '精度: 低'
        mapMarker.bindPopup(`
          <div style="text-align: center;">
            <strong>📍 当前位置</strong><br>
            经度: ${lng.toFixed(6)}<br>
            纬度: ${lat.toFixed(6)}<br>
            <small style="color: #666;">${accuracyText}</small>
          </div>
        `).openPopup()

        // (if info )
        if (accuracy && accuracy < 1000) {
          const accuracyCircle = L.circle([lat, lng], {
            radius: accuracy,
            color: '#00aaff',
            fillColor: '#00aaff',
            fillOpacity: 0.1,
            weight: 2,
            opacity: 0.6
          }).addTo(mapInstance)

          // , after
          currentLocationMarker = accuracyCircle
        }
      }

      locating.value = false
      ElMessage.success('定位成功！精度约 ' + Math.round(accuracy) + ' 米')
    },
    (error) => {
      locating.value = false
      let errorMessage = '定位失败'

      switch (error.code) {
        case error.PERMISSION_DENIED:
          errorMessage = '用户拒绝了地理位置请求，请在浏览器设置中允许位置访问'
          break
        case error.POSITION_UNAVAILABLE:
          errorMessage = '位置信息不可用，请检查GPS或网络连接'
          break
        case error.TIMEOUT:
          errorMessage = '定位请求超时，请重试'
          break
        default:
          errorMessage = '定位失败: ' + error.message
          break
      }

      console.error('定位错误:', error)
      ElMessage.error(errorMessage)
    },
    options
  )
}

const confirmCoordinate = () => {
  if (validateCoordinates(selectedCoordinate.value.longitude, selectedCoordinate.value.latitude)) {
    formData.value.longitude = selectedCoordinate.value.longitude
    formData.value.latitude = selectedCoordinate.value.latitude
    mapDialogVisible.value = false
    ElMessage.success('坐标已设置')
  } else {
    ElMessage.error('请选择有效的坐标点')
  }
}

const handleSave = async () => {
  try {
    const valid = await editFormRef.value?.validate()
    if (valid) {
      //
      if (formData.value.longitude && formData.value.latitude) {
        if (!validateCoordinates(formData.value.longitude, formData.value.latitude)) {
          ElMessage.error('请输入有效的经纬度坐标')
          return
        }
      }

      // data, Convert to
      const saveData = { ...formData.value }

      // Convert to
      if (saveData.longitude) {
        const parsedLng = parseCoordinate(saveData.longitude)
        if (parsedLng !== null) {
          saveData.longitude = parsedLng.toString()
        }
      }

      if (saveData.latitude) {
        const parsedLat = parseCoordinate(saveData.latitude)
        if (parsedLat !== null) {
          saveData.latitude = parsedLat.toString()
        }
      }

      console.log('保存设备数据，转换后的坐标:', {
        原始经度: formData.value.longitude,
        转换后经度: saveData.longitude,
        原始纬度: formData.value.latitude,
        转换后纬度: saveData.latitude
      })

      emit('save', saveData)
    }
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

const handleCancel = () => {
  emit('cancel')
}

// form
const resetForm = () => {
  formData.value = {
    streamUrl: '',
    deviceName: '',
    deviceId: '',
    tag: '',
    selectedTags: [],
    imagePath: '',
    longitude: '',
    latitude: '',
    heightPosition: '地面',
    address: '',
    region: [],
    remark: ''
  }
  editFormRef.value?.resetFields()
}

// sub
onUnmounted(() => {
  // instance
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
    mapMarker = null
    currentLocationMarker = null
  }
})

// event, instance
watch(mapDialogVisible, (newValue) => {
  if (!newValue && mapInstance) {
    // instance
    mapInstance.remove()
    mapInstance = null
    mapMarker = null
    currentLocationMarker = null
  }
})

// method component
defineExpose({
  resetForm
})
</script>

<style scoped lang="scss">
.device-edit-form {
  .edit-content {
    padding: 20px;
  }

  .section {
    margin-bottom: 30px;

    .section-title {
      margin: 0 0 20px 0;
      font-size: 16px;
      font-weight: 600;
      color: #333;
      border-bottom: 2px solid #409eff;
      padding-bottom: 8px;
    }
  }

  .edit-form {
    .required-field {
      :deep(.el-form-item__label) {
        &::before {
          content: '*';
          color: #f56c6c;
          margin-right: 4px;
        }
      }
    }
  }

  .coordinate-input {
    display: flex;
    gap: 12px;
    align-items: center;

    .coordinate-item {
      flex: 1;
    }

    .locate-btn {
      flex-shrink: 0;
    }
  }

  .height-options {
    .el-radio-group {
      display: flex;
      gap: 20px;
    }
  }

  .edit-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding: 20px 0;
    border-top: 1px solid #ebeef5;
    margin-top: 30px;
  }

  //
  .map-container {
    .map-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      padding: 12px 16px;
      background: #f8f9fa;
      border-radius: 6px;
      border: 1px solid #e9ecef;

      .location-tip {
        display: flex;
        align-items: center;
        gap: 6px;
        color: #6c757d;
        font-size: 13px;

        .el-icon {
          font-size: 14px;
        }
      }
    }

    .map-area {
      height: 400px;
      border: 1px solid #dcdfe6;
      border-radius: 6px;
      margin-bottom: 16px;
      position: relative;

      // leaflet
      :deep(.leaflet-container) {
        height: 100%;
        width: 100%;
        border-radius: 6px;
      }

      //
      :deep(.leaflet-control-zoom) {
        margin: 10px;
      }

      :deep(.leaflet-control-attribution) {
        font-size: 10px;
      }

      // Custompopup
      :deep(.leaflet-popup-content-wrapper) {
        border-radius: 6px;
      }

      :deep(.leaflet-popup-content) {
        margin: 8px 12px;
        font-size: 13px;
      }
    }

    .coordinate-display {
      display: flex;
      gap: 20px;
      justify-content: center;
      font-family: monospace;
      color: #606266;
      font-size: 14px;
      font-weight: 500;

      span {
        padding: 6px 12px;
        background: #f0f9ff;
        border-radius: 6px;
        border: 1px solid #b3d8ff;
        min-width: 140px;
        text-align: center;
      }
    }
  }
}

//
@media (max-width: 768px) {
  .device-edit-form {
    .coordinate-input {
      flex-direction: column;
      align-items: stretch;
    }

    .height-options {
      .el-radio-group {
        flex-direction: column;
        gap: 12px;
      }
    }
  }
}

// TagSelector - need to
:deep(.tag-selector) {
  // (Level 0)
  .el-option-group__title {
    font-weight: 600 !important;
    color: #333 !important;
    padding: 8px 12px !important;
    background-color: #f8f9fa !important;
    border-bottom: 1px solid #e9ecef !important;
    font-size: 13px !important;
  }

  //
  .el-select__tags {
    .el-tag {
      margin-right: 6px !important;
      margin-bottom: 2px !important;

      &.el-tag--info {
        background-color: #f0f9ff !important;
        border-color: #b3d8ff !important;
        color: #409eff !important;
      }
    }
  }
}
</style>