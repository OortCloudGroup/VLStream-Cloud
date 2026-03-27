# 移动端弹框组件Popup


## 说明

为了解决`popup`弹出时点击安卓设备返回键，会导致跳转界面的问题，重新开发了组件`<oort-popup>`。
`<oort-popup>` 组件基于`<van-popup>`利用$atters和$listeners，配合`<nav-header-banner>`使用

## 引入

```js
import oort_popup from '@/components/popup/oort_popup'
Vue.component('oort-popup', oort_popup)
```

## 使用

和使用 Vant UI组件的<van-popup>,属性和方法一致，只不过时改成<oort-popup>

example 选择云课堂app的上传视频部分代码（有修改）
```vue
 <!--      点击课程类型弹出框-->
      <oort-popup v-model="show" style="width: 280px;min-height: 253.67px;" round>
        <div v-if="uid===''" class="releaseShip_bf_but">
          <van-button
            v-for="(item,index) in classifyNameList"
            :key="index"
            :class="courseType===item.classify_name?'releaseShip_color_1':'releaseShip_color_2'"
            plain
            hairline
            type="large"
            @click="kcType(item.classify)"
          >
            {{ item.classify_name }}
          </van-button>
        </div>
      </oort-popup>
```

