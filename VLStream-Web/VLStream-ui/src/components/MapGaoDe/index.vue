<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <el-row :gutter="20">
    <el-col :span="12">
      <div style="display:flex;align-items: center">
        <el-input ref="addressInput" v-model="site" clearable :placeholder="$tp('请输入地址')">
          <template #prefix>
            <el-icon class="el-input__icon">
              <search />
            </el-icon>
          </template>
        </el-input>
        <el-button type="primary" style="margin-left: 20px" @click="onSearch">{{ $tp('搜索') }}</el-button>
      </div>
    </el-col>
    <el-col :span="12">
      <el-button style="float: right" type="primary" :disabled="mapLoading || !!mapError || MapLng === '' || MapLat === ''" @click="onConfirm">{{ $tp('确定') }}</el-button>
    </el-col>
  </el-row>
  <el-alert v-if="mapError" :title="$tp(mapError)" type="error" :closable="false" style="margin-top: 12px">
    <el-button size="small" @click="inGaDeMap">{{ $tp('重试') }}</el-button>
  </el-alert>
  <div ref="mapContainer" v-loading="mapLoading" style="margin-top: 20px" class="map"
    role="region" :aria-label="$tp('位置选择地图')"></div>
</template>

<script setup name="MapGaoDe">
import { loadAMap } from '@/utils/amap';
import { ref, getCurrentInstance, nextTick, onMounted, onBeforeUnmount } from 'vue';
import {getConfigKey} from "@/api/wvp/systemConfig";

const { proxy } = getCurrentInstance();

const keyword = ref('');
const site = ref('');
const searchBox = ref(null);
let map = null;
const mapContainer = ref(null);
const addressInput = ref(null);
const mapLoading = ref(false);
const mapError = ref('');
let initialization = null;
let generation = 0;
let selectionGeneration = 0;

const lng = ref(116.3912757);
const lat = ref(39.906217);

/* * need to component value */
const MapLng = ref('');
const MapLat = ref('');
const address = ref('');
const detailedStreet = ref('');

const props = defineProps({
  autoInitialize: { type: Boolean, default: true },
  position: {
    type: null,
    default: () => {}
  },
  toponym: {
    type: String,
    default: ''
  }
});

let coordinate = null;

const inGaDeMap = async () => {
  const requestedGeneration = generation;
  await nextTick();
  if (requestedGeneration !== generation || !mapContainer.value?.isConnected) return;
  if (map) return;
  if (initialization) return initialization;
  const current = ++generation;
  const isCurrent = () => current === generation && mapContainer.value?.isConnected;
  mapLoading.value = true;
  mapError.value = '';

  let centerTimer;
  const centerConfig = Promise.race([
    getConfigKey("map_center").catch(() => null),
    new Promise(resolve => { centerTimer = setTimeout(() => resolve(null), 2000); })
  ]).finally(() => clearTimeout(centerTimer));
  initialization = Promise.all([loadAMap(), centerConfig]).then(async ([AMap, response]) => {
    if (!isCurrent()) return;
    const center = typeof response?.msg === 'string' ? response.msg.split(',').map(Number) : [];
    if (center.length === 2 && response.msg.split(',').every(value => value.trim() !== '') &&
      center.every(Number.isFinite) && Math.abs(center[0]) <= 90 && Math.abs(center[1]) <= 180) {
      [lat.value, lng.value] = center;
    }
    const position = ref(props.position);
    const toponym = ref(props.toponym);

    if (typeof toponym.value !== 'undefined') {
      site.value = toponym.value;
      address.value = toponym.value;
    }

    var geocoder = new AMap.Geocoder({
      radius: 1000,
      extensions: "all"
    });

    coordinate = [lng.value, lat.value];
    if (Array.isArray(position.value)) {
      if (position.value.length === 2 && position.value.every(value => ['string', 'number'].includes(typeof value) && String(value).trim() !== '' && Number.isFinite(Number(value))) &&
        Math.abs(Number(position.value[0])) <= 180 && Math.abs(Number(position.value[1])) <= 90) {
        coordinate = position.value.map(Number);
        [MapLng.value, MapLat.value] = coordinate;
        const selection = ++selectionGeneration;
        geocoder.getAddress(coordinate, function (status, result) {
          if (!isCurrent() || selection !== selectionGeneration) return;
          if (status === 'complete' && result.info === 'OK') {
            address.value = result.regeocode.addressComponent.province + '/' + result.regeocode.addressComponent.city + '/' + result.regeocode.addressComponent.district;
            detailedStreet.value = result.regeocode.addressComponent.township + '/' + result.regeocode.addressComponent.street + result.regeocode.addressComponent.streetNumber;
            site.value = result.regeocode.formattedAddress;
          }
        });
      } else {
        /* * Get current */
        let location = new AMap.Geolocation({
          enableHighAccuracy: true,
          timeout: 10000
        });
        const selection = ++selectionGeneration;

        location.getCurrentPosition(async (status, result) => {
          if (!isCurrent() || selection !== selectionGeneration) return;
          if (status === 'complete') {
            const res = result.position;
            lng.value = res.lng;
            lat.value = res.lat;

            geocoder.getAddress(res, function (status, result) {
              if (!isCurrent() || selection !== selectionGeneration) return;
              if (status === 'complete' && result.info === 'OK') {
                address.value = result.regeocode.addressComponent.province + result.regeocode.addressComponent.city + result.regeocode.addressComponent.district;
                detailedStreet.value = result.regeocode.addressComponent.township + result.regeocode.addressComponent.street + result.regeocode.addressComponent.streetNumber;
                site.value = result.regeocode.formattedAddress;
              }
            });
          }
        });

        coordinate = [lng.value, lat.value];
      }
    }

    /* * Initialize */
    map = new AMap.Map(mapContainer.value, {
      resizeEnable: true,
      zoom: 11,
      center: coordinate
    });

    //
    map.addControl(new AMap.Scale());

    AMapUI.loadUI(['control/BasicControl'], function (BasicControl) {
      if (!isCurrent()) return;
      //
      map.addControl(new BasicControl.Zoom({
        position: 'lt'
      }));

      // layer
      map.addControl(new BasicControl.LayerSwitcher({
        position: 'rt'
      }));
    });

    // in , in
    var hawkEye = new AMap.HawkEye();
    map.addControl(hawkEye);

    /* * */
    const marker = new AMap.Marker({
      position: coordinate,
    });
    map.add(marker);

    /* * */
    searchBox.value = new AMap.AutoComplete({
      input: addressInput.value?.input,
    });

    // event
    if (searchBox.value) {
      searchBox.value.on('select', (status, result) => {
        if (!isCurrent() || !status.poi?.location) return;
        selectionGeneration++;
        MapLng.value = status.poi.location.lng;
        MapLat.value = status.poi.location.lat;
        address.value = status.poi.district;
        detailedStreet.value = status.poi.address;

        map.clearMap();
        map.setCenter([status.poi.location.lng, status.poi.location.lat]);
        map.setZoom(18); // Set

        const marker = new AMap.Marker({
          title: status.poi.name,
          position: new AMap.LngLat(status.poi.location.lng, status.poi.location.lat),
        });

        map.add(marker);
      });
    }

    /* * event */
    map.on('click', (e) => {
      if (!isCurrent()) return;
      const selection = ++selectionGeneration;
      map.clearMap();
      var lnglat = e.lnglat;

      MapLng.value = lnglat.lng;
      MapLat.value = lnglat.lat;
      site.value = '';
      address.value = '';
      detailedStreet.value = '';

      map.setCenter([lnglat.lng, lnglat.lat]);
      map.setZoom(15);

      var geocoder = new AMap.Geocoder({
        radius: 1000,
        extensions: "all"
      });

      geocoder.getAddress(lnglat, function (status, result) {
        if (!isCurrent() || selection !== selectionGeneration) return;
        if (status === 'complete' && result.info === 'OK') {
          address.value = result.regeocode.addressComponent.province + result.regeocode.addressComponent.city + result.regeocode.addressComponent.district;
          detailedStreet.value = result.regeocode.addressComponent.township + result.regeocode.addressComponent.street + result.regeocode.addressComponent.streetNumber;
          site.value = result.regeocode.formattedAddress;
        }
      });

      const marker = new AMap.Marker({
        position: [e.lnglat.lng, e.lnglat.lat]
      });

      map.add(marker);
    });
  }).catch(() => {
    if (!isCurrent()) return;
    map?.destroy();
    map = null;
    mapError.value = '地图加载失败，请检查地图配置或网络后重试';
  }).finally(() => {
    if (current === generation) {
      initialization = null;
      mapLoading.value = false;
    }
  });
  return initialization;
};

var emit = defineEmits(['update-value']);

/* * sub component component value */
const onConfirm = () => {
  if (mapLoading.value || mapError.value || !map) return;
  if (site.value.length <= 0 || (!address.value && !detailedStreet.value) ||
    [MapLng.value, MapLat.value].some(value => value === '' || value == null || !Number.isFinite(Number(value)))) {
    proxy?.$modal.msgWarning("未选择任何地址");
  } else {
    emit('update-value', {
      show: false,
      detailedStreet: detailedStreet.value,
      lng: MapLng.value,
      lat: MapLat.value,
      address: address.value,
    });
  }
};

/* * */
const Destruction = () => {
  generation++;
  selectionGeneration++;
  initialization = null;
  mapLoading.value = false;
  site.value = '';
  MapLng.value = '';
  MapLat.value = '';
  address.value = '';
  detailedStreet.value = '';
  map?.destroy();
  map = null;
  searchBox.value = null;
};

defineExpose({
  Destruction,
  inGaDeMap
});

/* * event */
const poiS = ref(null);

const onSearch = () => {
  if (!map || mapLoading.value) return;
  const current = generation;
  const selection = ++selectionGeneration;
  if (site.value.length <= 0) {
    proxy?.$modal.msgError("没有输入搜索内容");
  } else {
    map.setZoom(10);

    var placeSearch = new AMap.PlaceSearch({
      city: "010",
    });

    placeSearch.search(site.value, function (status, result) {
      if (current !== generation || selection !== selectionGeneration || !map) return;
      if (status === 'complete' && result.info === 'OK') {
        map.clearMap();
        poiS.value = result.poiList.pois;
        map.setCenter([poiS.value[0].location.lng, poiS.value[0].location.lat]);

        for (let i = 0; i < poiS.value.length; i++) {
          const poi = poiS.value[i];
          const marker = new AMap.Marker({
            title: poi.name,
            position: [poi.location.lng, poi.location.lat]
          });

          map.add(marker);

          /* * event */
          marker.on('click', (e) => {
            if (current !== generation || !map) return;
            const selected = ++selectionGeneration;
            var geocoder = new AMap.Geocoder({
              radius: 1000,
              extensions: "all"
            });

            MapLng.value = e.lnglat.lng;
            MapLat.value = e.lnglat.lat;

            geocoder.getAddress(e.lnglat, function (status, result) {
              if (current !== generation || selected !== selectionGeneration || !map) return;
              if (status === 'complete' && result.info === 'OK') {
                site.value = result.regeocode.formattedAddress;
                address.value = result.regeocode.addressComponent.province + '/' + result.regeocode.addressComponent.city + '/' + result.regeocode.addressComponent.district;
                detailedStreet.value = result.regeocode.addressComponent.township + '/' + result.regeocode.addressComponent.street + result.regeocode.addressComponent.streetNumber;
              }
            });

            map.setCenter([e.lnglat.lng, e.lnglat.lat]);
            map.setZoom(14);
          });
        }
      }
    });
  }
};

onMounted(async () => {
  if (props.autoInitialize) await inGaDeMap();
});
onBeforeUnmount(Destruction);


</script>

<style scoped>
.map {
  width: 100%;
  height: 550px;
  position: relative;
}

.amap-sug-result {
  z-index: 99999;
}
</style>
