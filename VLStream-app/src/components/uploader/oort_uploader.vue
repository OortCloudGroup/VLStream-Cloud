<!--
  * @Created by: 兰舰
  * Email: gglanjian@qq.com
  * Phone: 16620805419
  * @Date: 2025-06-25 17:46:39
  * @Last Modified by:  兰舰
  * @Copyright 奥尔特云(深圳)智慧科技 aPaaS-front-team. All rights reserved.
 !-->
<template>
  <div class="uploader">
    <van-overlay :show="popupShow" v-bind="$attrs">
      <div class="div_overlay">
        <template v-if="isInCordovaEnv">
          <div class="div_overlay_option" @click="takePhoto(1)">
            <span v-if="mediaType === 0">拍照</span>
            <span v-if="mediaType === 1">摄像</span>
            <span v-if="mediaType === 2">拍摄</span>
          </div>
          <van-divider />
          <div class="div_overlay_option" @click="takePhoto(2)">
            <span>从相册选择</span>
          </div>
          <van-divider />
        </template>
        <div v-else class="div_overlay_option">
          <van-uploader
            v-model="fileList"
            :accept="accept"
            :after-read="photoAlbumMethod"
            :preview-image="false"
          >
            <span>从相册选择</span>
          </van-uploader>
        </div>
      </div>
    </van-overlay>
  </div>
</template>
<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { uploadFile } from '@/api/fastdfs'
import config from '@/config/index.js'
import { showLoadingToast } from 'vant'
import bus from '@/utils/bus'
import getQueryUrlString from '@/utils/getQueryUrlString'
import { getToken } from '@/utils/cache'

const props = defineProps({
  accept: {
    type: String,
    default: 'image/*'
  },
  show: {
    type: Boolean,
    default: false
  },
  isImg: {
    type: Boolean,
    default: false
  },
  file: {
    type: Array,
    default: null
  },
  numLimit: {
    type: Number,
    default: 9
  }
})

const uploadURL = ref(config.URL + config.gateWay + 'apaas-fastdfsservice/fastdfs/v1/uploadFile')

const emit = defineEmits(['update:show', 'getImgUrl'])

// 响应式数据
const imgUrl = ref('')
const isInCordovaEnv = ref(window.global.is_cordova) // 是否在cordova 的环境中， 若不是隐藏拍照， 使用vant 自带的方法即可以调用原生ios 或者 android
const fileList = ref([])
const popupShow = ref(false)
const mediaType = ref(4) // 默认是是4 可传所有

// 监听 props.show
watch(
  () => props.show,
  (newVal) => {
    popupShow.value = newVal
    handlerPopupEvent()
  }
)

// 监听 props.file
watch(
  () => props.file,
  (newVal) => {
    fileList.value = newVal
  }
)

// 初始化
onMounted(() => {
  popupShow.value = props.show
  // 这里做一个监听， 用来是pop类的弹框出现在页面中是，点击物理返回键时会触发返回而不是
  bus.$on('popupHide', popShow)
  if (props.accept.includes('image')) {
    mediaType.value = 0
  }
  if (props.accept.includes('video')) {
    mediaType.value = 1
  }
  if (props.accept.includes('audio')) {
    mediaType.value = 3
  }
  if (props.accept.includes('video') && props.accept.includes('image')) {
    mediaType.value = 2
  }
})

// 清理
onBeforeUnmount(() => {
  bus.$off('popupHide', popShow)
})

// 方法
const popShow = () => {
  popupShow.value = false
  emit('update:show', popupShow.value)
}

const handlerPopupEvent = () => {
  bus.$emit('popShow', popupShow.value)
}

const takePhoto = async(srcType) => {
  const params = {
    mQuality: 35, // 0-100  0 low  100 high
    destType: 1, // 0 base64图片数据   1，2 file uri
    srcType: srcType, // 0 图片库选择，1 照相机拍照，2 相册库选择
    targetWidth: 0, // 图片宽，不指定填0
    targetHeight: 0, // 图片高 ，不指定填0
    encodingType: 0, //  编码类型  0 jpeg , 1 png
    mediaType: mediaType.value, // 0 图片， 1 视频 ，2 图片视频  3 音频  4 附件
    allowEdit: false, // 0 不编辑 ，1 编辑
    correctOrientation: true, // 0 不校正， 1 校正
    saveToPhotoAlbum: false, // 0 不保存到相册， 1 保存到相册,
    selectNumLimit: props.numLimit - fileList.value.length// 重新相册选择的数量
  }

  if (config.env === 'xh') {
    let res = await xhChooseFile()
    let tempPromiseArr = []
    tempPromiseArr.push(uploadImage(res))
    const toastIns = showLoadingToast({
      message: '文件上传中，请耐心等待...',
      forbidClick: true,
      overlay: true,
      duration: 0
    })
    Promise.all(tempPromiseArr).then(() => {
      toastIns.close()
    })
  } else {
    let res = await window.androidFunUtils.takePhoto(params)
    try {
      let tempArr = JSON.parse(res)
      let tempPromiseArr = []
      tempArr.forEach(item => {
        tempPromiseArr.push(uploadImage(item))
      })
      const toastIns = showLoadingToast({
        message: '文件上传中，请耐心等待...',
        forbidClick: true,
        overlay: true,
        duration: 0
      })
      Promise.all(tempPromiseArr).then(() => {
        toastIns.close()
      })
      // 超过五秒
      setTimeout(() => {
        toastIns.close()
      }, 10000)
    } catch (e) {
      console.error(e)
    }
  }
}

const xhChooseFile = () => {
  return new Promise(resolve => {
    window.app.chooseFile((data) => {
      resolve(data.file_path)
    })
  })
}

const uploadImage = (fileURL) => {
  // 如果包含的是cover 就是视频带封面
  if (fileURL.includes('cover')) {
    return new Promise(resolve => {
      let coverFileUrl = getQueryUrlString(fileURL).cover
      if (!coverFileUrl) {
        console.error('获取cover出错', fileURL)
      }
      let uploadArr = [uploadLocalFilePath(fileURL.split('?')[0]), uploadLocalFilePath(coverFileUrl.split('?')[0])]
      Promise.all(uploadArr).then(respos => {
        let res = respos[0]
        let res2 = respos[1]
        let coverImage = ''
        if (res2.code === 200) {
          coverImage = res2.data.url
        }
        if (res.code === 200) {
          imgUrl.value = res.data.url
          uploadPhoto(res.data.duration, res.data.options.fileName, coverImage)
          resolve(res)
        } else {
          resolve(res)
        }
      })
    })
  } else {
    fileURL = fileURL.split('?')[0]
    return new Promise(resolve => {
      uploadLocalFilePath(fileURL.split('?')[0]).then((res) => {
        if (res.code === 200) {
          imgUrl.value = res.data.url
          // this.upImage(res.data.url)
          // 手机上传-文件大小res.data.size
          uploadPhoto(res.data.duration, res.data.options.fileName, '', res.data.size)
          resolve(res)
        } else {
          resolve(res)
        }
      })
    })
  }
}

const uploadLocalFilePath = (localPath) => {
  return new Promise(resolve => {
    let ft = new window.FileTransfer()
    let options = new window.FileUploadOptions()
    options.headers = { accessToken: getToken(), ...config.headers }
    // 对应后台的字段
    console.log('--options.headers--', options.headers)
    options.fileName = localPath.substr(localPath.lastIndexOf('/') + 1)
    let uri = uploadURL.value
    console.log('--localPath -----', localPath)
    console.log('uri--', uri)
    ft.upload(
      localPath,
      encodeURI(uri),
      (msg) => {
        try {
          let res = JSON.parse(msg.response)
          if (res.code === 200) {
            res.data.options = options
            resolve(res)
          }
        } catch (e) {
          alert('上传失败 uploadFile')
        }
      },
      (e) => {
        console.error('error', e)
      },
      options
    )
  })
}

const uploadPhoto = (duration, fileName, coverImg, fileSize) => {
  if (!!imgUrl.value) {
    // 手机上传-大小
    const newFile = {
      url: imgUrl.value,
      size: fileSize,
      duration: Math.ceil(duration || 0),
      name: decodeURIComponent(fileName),
      fileType: props.accept,
      suffix: getSuffix(imgUrl.value),
      coverImg: coverImg || ''
    }
    if (duration) {
      newFile.duration = duration
    }
    fileList.value.push(newFile)
  }
  emit('getImgUrl', fileList.value, imgUrl.value)
}

const photoAlbumMethod = (file, detail) => {
  const formD = new FormData()
  formD.append('file', file.file)
  uploadFile(formD).then(res => {
    fileList.value.splice(detail.index, 1)
    imgUrl.value = res.data.url
    // h5上传-名称大小
    const newFile = {
      url: res.data.url,
      name: file.file.name,
      size: res.data.size,
      fileType: file.file.type,
      suffix: getSuffix(imgUrl.value),
      coverImg: ''
    }
    if (res.data.duration) {
      newFile.duration = res.data.duration
    }
    fileList.value.push(newFile)
    emit('getImgUrl', fileList.value, imgUrl.value)
  })
}

const getSuffix = (file) => {
  let fileName = file.lastIndexOf('.') // 取到文件名开始到最后一个点的长度
  let fileNameLength = file.length // 取到文件名长度
  let fileFormat = file.substring(fileName + 1, fileNameLength)
  return fileFormat
}
</script>

<style lang="scss">
    .uploader .van-divider {
        margin: 0;
    }

    .uploader .van-uploader {
        width: 95.6%;
    }

    .uploader .van-uploader__input-wrapper {
        width: 100%;
        height: 66px;
        display: flex;
        align-items: center;
        justify-content: center;
    }
</style>

<style lang="scss" scoped>
    .div_overlay {
        width: 95.6%;
        background: #FFFFFF;
        margin-left: 2.2%;
        position: absolute;
        bottom: 4.4%;
        border-radius: 8px;

        span {
            font-size: 16px;
            color: #000000;
            letter-spacing: 0;
        }
    }

    .div_overlay_option {
        min-height: 60px;
        display: flex;
        align-items: center;
        justify-content: center;
    }
</style>
