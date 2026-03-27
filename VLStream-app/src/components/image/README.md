# 图片地址替换成网关地址处理组件image


## 说明

现在项目中的所有业务返回的地址都是没有经过处理的，例如：上传一张图片，回显时取的是 fastfds 接口返回的数据，
然后保存业务逻辑的时候把这个图片地址保存到了业务服务中。
问题点：项目迁移环境，网关地址变了，业务服务的数据还在，导致图片路径不对，


该组件将地址跟着网关地址一起变化


## 引入

```js
  import oort_image from './image/oort_image'
  Vue.component('oort-image', oort_image)
```

## 使用
### 组件式调用
eg:  image[]

```vue
  <oort-image
    v-for="(item,indexs) in images"
    :key="item.url"
    :img-url="item.url"
    :images="images"
    :index="indexs"
  />
```

### 函数式调用

示例
```vue
    <oort-image
      v-for="(item,indexs) in images"
      :key="item.url"
      :img-url="item.url"
      :images="images"
      :index="indexs"
    />
    export default {
     ...
      data() {
        return {
          images: [] // 图片数组
        }
      }
     methods: {
     }   
   }

```


