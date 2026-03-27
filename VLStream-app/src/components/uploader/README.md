# 移动端图片上传组件


## 说明

为了实现上传图片有两种选择，拍照上传和相册内选择图片，基于van-overlay、van-uploader 利用$atters和$listeners覆写。



## 引入

```js
import oort_uploader from '@/components/uploader/oort_uploader'
Vue.component('oort-uploader', oort_uploader)
```

## 使用

和使用 Vant UI组件的<van-overlay>,属性和方法一致，只不过是改成<oort-uploader>。<br/>

eg: 

```vue
<oort-uploader :file="fileList" :show.sync="showUploader" @click="showUploader=false" @getImgUrl="getImg" />

getImg(file, image) {
   this.fileList = file //调用方获取图片列表
}
```
file：图片的URL数组

注意：sync file getImgUrl 是必须的

