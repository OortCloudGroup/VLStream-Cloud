# 移动端图片预览组件ImagePreview


## 说明

为了解决`ImagePreview`组件，物理返回键引起的页面回退，基于oort-image-preview利用$atters和$listeners覆写，



## 引入

```js
  import oort_image_preview from './imagePreview/oort_image_preview'
  Vue.component('oort-image-preview', oort_image_preview)
```

## 使用
### 组件式调用
和使用 Vant UI组件的<oort-image-preview>,属性和方法一致，只不过时改成<oort-image-preview>

注意：oort-image-preview 要等组件渲染完成才能给 images 赋值

eg: 

```vue
 <oort-image-preview v-model="showPreview" :start-position="index" :images="imageArr" />
```

### 函数式调用

函数式调用   `OortImagePreview(options)`  options 参考vant ImagePreivw
示例
```vue

    import { OortImagePreview } from '@/util/OortIamagePreviw'

    export default {

     ...

     methods: {
      onClick(img, index) {
           OortImagePreview({
             images: img,
             startPosition: index,
             loop: false,
             closeOnPopstate: true
           })
        }   
      }   
   }

```


