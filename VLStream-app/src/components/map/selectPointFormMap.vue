<template>
  <div class="select-point-form-map">
    <!-- 普通模式头部 -->
    <div v-if="!isAroundSearch" class="map-header">
      <span>点击地图选择位置</span>
    </div>

    <!-- 周边搜索模式头部 -->
    <div v-if="isAroundSearch" class="around-search-header">
      <div class="search-title">
        <div class="title-line" />
        <span>地图选点</span>
      </div>
      <div class="search-bar">
        <div class="search-input-wrapper">
          <van-icon class="search-icon" name="search" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索位置"
            class="search-input"
            @input="handleSearchInput"
            @focus="onSearchFocus"
          />
        </div>
      </div>
    </div>

    <div class="map-container">
      <LMap
        ref="mapRef"
        :base-map-options="baseMapOptions"
        :zoom="zoom"
        :center="center"
        :min-zoom="minZoom"
        :max-zoom="maxZoom"
        @mapClick="mapClick"
        @mapReady="mapReady"
        @selectResult="selectResult"
        @mapMove="onMapMove"
      />
    </div>

    <!-- 周边搜索结果列表 -->
    <div v-if="isAroundSearch && showSearchResults" class="search-results-panel">
      <div class="results-title">
        搜索结果
      </div>
      <div class="results-list">
        <div
          v-for="(item, index) in searchResults"
          :key="index"
          class="result-item"
          @click="selectSearchResult(item)"
        >
          <div class="location-icon">
            📍
          </div>
          <div class="result-content">
            <div class="result-name">
              {{ item.name }}
            </div>
            <div class="result-address">
              {{ item.address }}
            </div>
          </div>
        </div>
        <div v-if="searchResults.length === 0 && !isSearching" class="no-results">
          暂无搜索结果
        </div>
        <div v-if="isSearching" class="loading">
          搜索中...
        </div>
      </div>
    </div>

    <div class="bottom_btn">
      <button class="complete-button" @click="handleComplete">
        确定选择
      </button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, computed, ref } from 'vue'
import { debounce } from 'lodash-es'
import LMap from './leafletMap.vue'
import AppConfig from '@/config/AppConfig'
import { getSearchAround } from '@/api/geoserver'

const emit = defineEmits(['confirm'])

const result = ref(null)
const selectResult = (data) => {
  result.value = data.result
}
const handleComplete = () => {
  emit('confirm', result.value)
}

const props = defineProps({
  appId: {
    type: String,
    default: AppConfig.commandDispatch.appId
  },
  //  是否开启周边搜索的模式
  isAroundSearch: {
    type: Boolean,
    default: false
  }
})

// 周边搜索相关状态
const searchKeyword = ref('')
const searchResults = ref([])
const showSearchResults = ref(false)
const isSearching = ref(false)
const mapCenter = ref({
  lng: 0,
  lat: 0
})

// 使用地图配置store
import { useMapConfigStore } from '@/store/modules/mapConfig'
const mapConfigStore = useMapConfigStore()
// 从store中获取地图配置
const baseMapOptions = computed(() => mapConfigStore.baseMapOptions)
const center = computed(() => mapConfigStore.center)
const zoom = computed(() => mapConfigStore.zoom)
const minZoom = computed(() => mapConfigStore.minZoom)
const maxZoom = computed(() => mapConfigStore.maxZoom)

import { useUserStore } from '@/store/modules/useraPaas'
import { mlsBaseMapList } from '@/api/geoserver'
const store = useUserStore()

const initMapBaseConfig = async() => {
  const data = {
    accessToken: store.userInfo.accessToken,
    current: 1,
    size: 999,
    platformAppId: props.appId
  }
  const res = await mlsBaseMapList(data)
  // 保存底图列表
  res.data.records.forEach(item => {
    item.type = item.dataType
  })
  mapConfigStore.setBaseMapArray(res.data.records || [])
  // 获取当前生效的底图
  const currentMap = res.data.records.find((item) => item.status === 2)
  if (currentMap) {
    let tempBaseMapOptions = []
    if (currentMap.dataType === 'WMS') {
      tempBaseMapOptions = [{
        name: currentMap.name || '',
        url: currentMap.internetUrl, // 移动端的地址
        type: 'WMS',
        layer: currentMap.layer
      }]
    }
    if (currentMap.dataType === 'TILE') {
      // 将配置保存到store中
      let internetUrl = currentMap.internetUrl?.split(',')
      tempBaseMapOptions = internetUrl?.map(tt => ({
        name: currentMap.name,
        url: tt,
        type: currentMap.dataType
      }))
    }
    // 将配置保存到store中
    mapConfigStore.updateMapConfig({
      baseMapOptions: tempBaseMapOptions,
      maxZoom: currentMap.maxLevel,
      minZoom: currentMap.minLevel,
      zoom: currentMap.initLevel,
      center: [currentMap.centerXPoint, currentMap.centerYPoint]
    })
  }
}

// 地图移动事件，更新中心点
const onMapMove = (event) => {
  if (props.isAroundSearch && event && event.center) {
    mapCenter.value = [event.center.lng, event.center.lat]
  }
}

// 搜索输入处理 - 使用 lodash debounce
const handleSearchInput = debounce(() => {
  if (searchKeyword.value.trim()) {
    performSearch()
  } else {
    searchResults.value = []
    showSearchResults.value = false
  }
}, 500) // 防抖，500ms后执行搜索

// 搜索框获得焦点
const onSearchFocus = () => {
  if (searchResults.value.length > 0) {
    showSearchResults.value = true
  }
}

// 执行搜索
const performSearch = async() => {
  if (!searchKeyword.value.trim()) return

  isSearching.value = true
  showSearchResults.value = true

  try {
    // 如果还是没有中心点，使用默认中心点
    let centerPoint = mapCenter.value
    const postStr = {
      keyWord: searchKeyword.value,
      level: 12,
      queryRadius: 5000,
      pointLonlat: `${centerPoint[0]},${centerPoint[1]}`,
      queryType: 3,
      start: 0,
      count: 20
    }

    const res = await getSearchAround({
      postStr: JSON.stringify(postStr),
      type: 'query'
    })

    if (res.data) {
      let result = JSON.parse(res.data)
      searchResults.value = result.pois.map(poi => ({
        name: poi.name,
        address: poi.address,
        lonlat: poi.lonlat,
        phone: poi.phone,
        poiType: poi.poiType
      }))
    } else {
      searchResults.value = []
    }
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error('搜索失败:', err)
    searchResults.value = []
  } finally {
    isSearching.value = false
  }
}

// 选择搜索结果
const selectSearchResult = (item) => {
  if (item.lonlat) {
    const [lng, lat] = item.lonlat.split(',')
    console.log('lng', lng)
    console.log('lat', lat)
    result.value = {
      longitude: Number(lng),
      latitude: Number(lat),
      address: item.address + item.name,
      name: item.name
    }
  }
  console.log('result', result.value)
}

// 地图点击事件
const mapClick = (event) => {
  if (props.isAroundSearch) {
    // 周边搜索模式下，点击地图更新中心点并隐藏搜索结果
    if (event && event.latlng) {
      mapCenter.value = {
        lng: event.latlng.lng,
        lat: event.latlng.lat
      }
    }
    showSearchResults.value = false
  }
}

// 地图准备就绪
const mapReady = () => {
  mapCenter.value = center.value
}

const mapRef = ref(null)
onMounted(async() => {
  await initMapBaseConfig()
  mapRef.value.startDraw({ type: 'point', isContinue: true, isReturnResult: true })
})

</script>

<style lang="scss" scoped>

.select-point-form-map {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    border-top-left-radius: 5px;
    border-top-right-radius: 5px;
    position: relative;
}

.map-header {
    width: 100%;
    height: 32px;
    background-color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    border-top-left-radius: 5px;
    border-top-right-radius: 5px;
    span {
        font-size: 14px;
        font-weight: 600;
        color: #000;
    }
}

/* 周边搜索模式头部样式 */
.around-search-header {
    background-color: #fff;
    padding: 12px 16px;
    border-top-left-radius: 5px;
    border-top-right-radius: 5px;

    .search-title {
        display: flex;
        align-items: center;
        margin-bottom: 12px;

        .title-line {
            width: 4px;
            height: 16px;
            background-color: #2F69F8;
            margin-right: 8px;
            border-radius: 2px;
        }

        span {
            font-size: 16px;
            font-weight: 600;
            color: #000;
        }
    }

    .search-bar {
        .search-input-wrapper {
            position: relative;
            display: flex;
            align-items: center;
            background-color: #f5f5f5;
            border-radius: 20px;
            padding: 8px 16px;

            .search-icon {
                font-size: 16px;
                margin-right: 8px;
                color: #999;
            }

            .search-input {
                flex: 1;
                border: none;
                background: transparent;
                outline: none;
                font-size: 14px;
                color: #333;

                &::placeholder {
                    color: #999;
                }
            }
        }
    }
}

.map-container {
    width: 100%;
    flex: 1;
    position: relative;
}

/* 搜索结果面板样式 */
.search-results-panel {
    position: absolute;
    bottom: 60px;
    left: 0;
    right: 0;
    background-color: #fff;
    border-radius: 8px 8px 0 0;
    max-height: 300px;
    overflow-y: auto;
    z-index: 1000;
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);

    .results-title {
        padding: 12px 16px 8px;
        font-size: 14px;
        font-weight: 600;
        color: #333;
        border-bottom: 1px solid #f0f0f0;
    }

    .results-list {
        .result-item {
            display: flex;
            align-items: center;
            padding: 12px 16px;
            border-bottom: 1px solid #f0f0f0;
            cursor: pointer;
            transition: background-color 0.2s;

            &:hover {
                background-color: #f8f9fa;
            }

            &:last-child {
                border-bottom: none;
            }

            .location-icon {
                font-size: 16px;
                margin-right: 12px;
                color: #666;
            }

            .result-content {
                flex: 1;

                .result-name {
                    font-size: 14px;
                    font-weight: 600;
                    color: #333;
                    margin-bottom: 4px;
                }

                .result-address {
                    font-size: 12px;
                    color: #999;
                    line-height: 1.4;
                }
            }
        }

        .no-results, .loading {
            padding: 20px 16px;
            text-align: center;
            color: #999;
            font-size: 14px;
        }
    }
}

.bottom_btn {
    width: 100%;
    height: 60px;
    background-color: #fff;
    display: flex;
    justify-content: center;
    align-items: center;
    position: relative;
    z-index: 999;
}
.bottom_btn span {
    width: 100%;
    height: 100%;
    background-color: #fff;
    position: fixed;
}

.complete-button {
  width: 80%;
  margin: 0 auto;
  height: 36px;
  background: #2F69F8;
  border: none;
  border-radius: 18px;
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;

  &:active {
    background: #1A53FF;
  }
}

</style>

