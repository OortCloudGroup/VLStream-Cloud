<!-- eslint-disable vue/no-deprecated-dollar-listeners-api -->
<!--
 * @Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-06-25 17:44:20
 * @Last Modified by:  兰舰
 * @Copyright 奥尔特云(深圳)智慧科技 aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="uploader">
    <van-overlay :show="popupShow" v-bind="$attrs" v-on="$listeners">
      <div class="div_overlay">
        <template v-if="isInCordovaEnv">
          <div class="div_overlay_option" @click="takePhoto(2)">
            <span>选择文件</span>
          </div>
        </template>
        <div v-else class="div_overlay_option">
          <van-uploader
            v-model="fileList"
            :accept="accept"
            :after-read="photoAlbumMethod"
            :preview-image="false"
          >
            <span>选择文件</span>
          </van-uploader>
        </div>
      </div>
    </van-overlay>
  </div>
</template>
<script>
import { uploadFile } from '@/api/fastdfs'
import config from '@/config/index.js'
import { showLoadingToast } from 'vant'
import bus from '@/utils/bus'
import { getToken } from '@/utils/cache'
export default {
  name: 'OortFileUpload',
  props: {
    accept: {
      type: String,
      default: '*/*'
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
    },
    uploadURL: {
      type: String,
      default: config.URL + config.gateWay + 'apaas-fastdfsservice/fastdfs/v1/uploadFile'
    }
  },
  data() {
    return {
      imgUrl: '',
      isInCordovaEnv: window.global.is_cordova, // 是否在cordova 的环境中， 若不是隐藏拍照， 使用vant 自带的方法即可以调用原生ios 或者 android
      fileList: [],
      popupShow: false,
      mediaType: 4
    }
  },
  watch: {
    show() {
      this.popupShow = this.show
      this.handlerPopupEvent()
    },
    file() {
      this.fileList = this.file
    }
  },
  created() {
    this.popupShow = this.show
    // 这里做一个监听， 用来是pop类的弹框出现在页面中是，点击物理返回键时会触发返回而不是
    bus.$on('popupHide', this.popShow)
    if (this.accept.includes('image')) {
      this.mediaType = 0
    }
    if (this.accept.includes('video')) {
      this.mediaType = 1
    }
    if (this.accept.includes('audio')) {
      this.mediaType = 3
    }
    if (this.accept.includes('video') && this.accept.includes('image')) {
      this.mediaType = 2
    }
  },
  beforeUnmount() {
    bus.$off('popupHide', this.popShow)
  },
  methods: {
    popShow() {
      this.popupShow = false
      this.$emit('update:show', this.popupShow)
    },
    handlerPopupEvent() {
      bus.$emit('popShow', this.popupShow)
    },
    takePhoto(srcType) {
      const params = {
        mQuality: 35, // 0-100  0 low  100 high
        destType: 1, // 0 base64图片数据   1，2 file uri
        srcType: srcType, // 0 图片库选择，1 照相机拍照，2 相册库选择
        targetWidth: 0, // 图片宽，不指定填0
        targetHeight: 0, // 图片高 ，不指定填0
        encodingType: 0, //  编码类型  0 jpeg , 1 png
        mediaType: this.mediaType, // 0 图片， 1 视频 ，2 图片视频  3 音频  4 附件
        allowEdit: false, // 0 不编辑 ，1 编辑
        correctOrientation: true, // 0 不校正， 1 校正
        saveToPhotoAlbum: false, // 0 不保存到相册， 1 保存到相册,
        selectNumLimit: this.numLimit - this.fileList.length// 重新相册选择的数量
      }
      window.androidFunUtils.takePhoto(params).then(res => {
        try {
          let tempArr = JSON.parse(res)
          let tempPromiseArr = []
          tempArr.forEach(item => {
            tempPromiseArr.push(this.uploadImage(item.split('?')[0]))
          })
          const toastIns = showLoadingToast({
            message: '文件上传中,请耐心等待...',
            forbidClick: true,
            overlay: true,
            duration: 0
          })
          Promise.all(tempPromiseArr).then(() => {
            toastIns.close()
          })
        } catch (e) {
          console.log(1, e)
        }
      })
    },
    uploadImage(fileURL) {
      return new Promise(resolve => {
        let ft = new window.FileTransfer()
        let options = new window.FileUploadOptions()
        options.headers = { accessToken: getToken(), ...config.headers }
        // 对应后台的字段
        options.fileName = fileURL.substr(fileURL.lastIndexOf('/') + 1)
        // 对应后台的字段
        options.fileName = decodeURIComponent(fileURL.substr(fileURL.lastIndexOf('/') + 1))
        let uri = this.uploadURL
        ft.upload(
          fileURL,
          encodeURI(uri),
          (msg) => {
            try {
              let res = JSON.parse(msg.response)
              if (res.code === 200) {
                this.imgUrl = res.data.url
                // this.upImage(res.data.url)
                this.uploadPhoto(res.data.duration, options.fileName)
                resolve(res)
              } else {
                resolve(res)
                console.log(res)
              }
            } catch (e) {
              alert('上传失败 uploadFile')
            }
          },
          () => {
            Toast.clear()
            // console.log(err)
          },
          options
        )
      })
    },
    uploadPhoto(duration, fileName) {
      if (!!this.imgUrl) {
        this.fileList.push({
          url: this.imgUrl,
          duration: Math.ceil(duration || 0),
          name: decodeURIComponent(fileName),
          fileType: this.accept,
          suffix: this.getSuffix(this.imgUrl),
          coverImg: ''
        })
      }
      this.$emit('getImgUrl', this.fileList, this.imgUrl)
    },
    photoAlbumMethod(file, detail) {
      const formD = new FormData()
      formD.append('file', file.file)
      uploadFile(formD).then(res => {
        this.fileList.splice(detail.index, 1)
        this.imgUrl = res.data.url
        const newFile = {
          url: res.data.url,
          fileType: file.file.type,
          suffix: this.getSuffix(this.imgUrl),
          name: file.file.name,
          size: file.file.size
        }
        if (res.data.duration) {
          newFile.duration = res.data.duration
        }
        this.fileList.push(newFile)
        this.$emit('getImgUrl', this.fileList, this.imgUrl)
      })
    },
    getSuffix(file) {
      let fileName = file.lastIndexOf('.') // 取到文件名开始到最后一个点的长度
      let fileNameLength = file.length // 取到文件名长度
      let fileFormat = file.substring(fileName + 1, fileNameLength)
      return fileFormat
    }
  }
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
