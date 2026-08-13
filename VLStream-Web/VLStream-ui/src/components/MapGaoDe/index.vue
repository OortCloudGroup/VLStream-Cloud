<template>
  <el-row :gutter="20">
    <el-col :span="12">
      <div style="display:flex;align-items: center">
        <el-input v-model="site" clearable id="searchInput" placeholder="请输入地址">
          <template #prefix>
            <el-icon class="el-input__icon">
              <search />
            </el-icon>
          </template>
        </el-input>
        <el-button type="primary" style="margin-left: 20px" @click="onSearch">搜索</el-button>
      </div>
    </el-col>
    <el-col :span="12">
      <el-button style="float: right" type="primary" @click="onConfirm">确定</el-button>
    </el-col>
  </el-row>
  <div id="container" style="margin-top: 20px" class="map"></div>
</template>

<script setup name="MapGaoDe">
import AMapLoader from "@amap/amap-jsapi-loader";
import { onMounted, defineExpose, defineEmits } from 'vue';
import {getConfigKey} from "@/api/wvp/systemConfig";

const { proxy } = getCurrentInstance();

const keyword = ref('');
const site = ref('');
const searchBox = ref(null);
let map = null;

const lng = ref(116.3912757);
const lat = ref(39.906217);

/** 要传输给父组件的值 */
const MapLng = ref('');
const MapLat = ref('');
const address = ref('');
const detailedStreet = ref('');

const props = defineProps({
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
const map_key = ref();

const inGaDeMap = async () => {
  await getConfigKey("amap.security.key").then(response => {
    AMapSecurityConfig.value = response.msg;
  });

  await getConfigKey("amap.key").then(response => {
    map_key.value = response.msg;
  });

  await getConfigKey("map_center").then(response => {
    if (response.msg != ""){
      const coordStr = response.msg;
      const [latitude, longitude] = coordStr.split(",").map(item => parseFloat(item.trim()));
      lat.value = latitude;
      lng.value = longitude;
    }
  });

  AMapLoader.load({
    key: map_key.value,
    version: '2.0',
    plugins: [
      'AMap.AutoComplete',
      'AMap.PlaceSearch',
      "AMap.Geolocation",
      "AMap.Geocoder",
      "AMap.Marker",
      'AMap.ToolBar',
      'AMap.Scale',
      'AMap.HawkEye',
      'AMap.MapType',
      'AMap.Geolocation',
    ],
    AMapUI: {
      version: '1.1',
      plugins: []
    },
  }).then(async (AMap) => {
    const position = ref(props.position);
    const toponym = ref(props.toponym);

    if (typeof toponym.value !== 'undefined') {
      site.value = toponym.value;
    }

    var geocoder = new AMap.Geocoder({
      radius: 1000,
      extensions: "all"
    });

    if (typeof position.value !== 'undefined') {
      if (position.value[0] != "" && position.value[1] != "" && position.value[0] != undefined && position.value[1] != undefined) {
        coordinate = await [position.value[0], position.value[1]];
        geocoder.getAddress(coordinate, function (status, result) {
          if (status === 'complete' && result.info === 'OK') {
            address.value = result.regeocode.addressComponent.province + '/' + result.regeocode.addressComponent.city + '/' + result.regeocode.addressComponent.district;
            detailedStreet.value = result.regeocode.addressComponent.township + '/' + result.regeocode.addressComponent.street + result.regeocode.addressComponent.streetNumber;
            site.value = result.regeocode.formattedAddress;
          }
        });
      } else {
        /** 获取当前位置 */
        let location = new AMap.Geolocation({
          enableHighAccuracy: true,
          timeout: 10000
        });

        location.getCurrentPosition(async (status, result) => {
          if (status === 'complete') {
            const res = await result.position;
            lng.value = res.lng;
            lat.value = res.lat;

            geocoder.getAddress(res, function (status, result) {
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

    /** 初始化地图 */
    map = new AMap.Map('container', {
      resizeEnable: true,
      zoom: 11,
      center: coordinate
    });

    // 添加比例尺
    map.addControl(new AMap.Scale());

    AMapUI.loadUI(['control/BasicControl'], function (BasicControl) {
      // 添加一个缩放控件
      map.addControl(new BasicControl.Zoom({
        position: 'lt'
      }));

      // 图层切换控件
      map.addControl(new BasicControl.LayerSwitcher({
        position: 'rt'
      }));
    });

    // 在图面添加鹰眼控件，在地图右下角显示地图的缩略图
    var hawkEye = new AMap.HawkEye();
    map.addControl(hawkEye);

    /** 添加标记点 */
    const marker = new AMap.Marker({
      position: coordinate,
    });
    map.add(marker);

    /** 联想搜索 */
    searchBox.value = new AMap.AutoComplete({
      input: document.getElementById('searchInput'),
    });

    // 监听搜索结果事件
    if (searchBox.value) {
      searchBox.value.on('select', (status, result) => {
        MapLng.value = status.poi.location.lng;
        MapLat.value = status.poi.location.lat;
        address.value = status.poi.district;
        detailedStreet.value = status.poi.address;

        map.clearMap();
        map.setCenter([status.poi.location.lng, status.poi.location.lat]);
        map.setZoom(18); // 设置地图显示的缩放级别

        const marker = new AMap.Marker({
          title: status.poi.name,
          position: new AMap.LngLat(status.poi.location.lng, status.poi.location.lat),
        });

        map.add(marker);
      });
    }

    /** 点击地图事件 */
    map.on('click', (e) => {
      map.clearMap();
      var lnglat = e.lnglat;

      MapLng.value = lnglat.lng;
      MapLat.value = lnglat.lat;

      map.setCenter([lnglat.lng, lnglat.lat]);
      map.setZoom(15);

      var geocoder = new AMap.Geocoder({
        radius: 1000,
        extensions: "all"
      });

      geocoder.getAddress(lnglat, function (status, result) {
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
  });
};

var emit = defineEmits(['update-value']);

/** 子组件向父组件传值 */
const onConfirm = () => {
  if (site.value.length <= 0) {
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

/** 销毁地图 */
const Destruction = () => {
  site.value = '';
  map.destroy();
};

defineExpose({
  Destruction,
  inGaDeMap
});

/** 点击搜索事件 */
const poiS = ref(null);

const onSearch = () => {
  if (site.value.length <= 0) {
    proxy?.$modal.msgError("没有输入搜索内容");
  } else {
    map.setZoom(10);

    var placeSearch = new AMap.PlaceSearch({
      city: "010",
    });

    placeSearch.search(site.value, function (status, result) {
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

          /** 给标记点添加事件 */
          marker.on('click', (e) => {
            var geocoder = new AMap.Geocoder({
              radius: 1000,
              extensions: "all"
            });

            MapLng.value = e.lnglat.lng;
            MapLat.value = e.lnglat.lat;

            geocoder.getAddress(e.lnglat, function (status, result) {
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

const AMapSecurityConfig = ref();

onMounted(async () => {
  await getConfigKey("amap.security.key").then(response => {
    AMapSecurityConfig.value = response.msg;

    getConfigKey("amap.key").then(response => {
      map_key.value = response.msg;
      inGaDeMap();
    });
  });

  /** 安全密钥 */
  window._AMapSecurityConfig = {
    securityJsCode: AMapSecurityConfig.value,
  };
});


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
