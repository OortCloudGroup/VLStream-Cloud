/**
Created by 兰舰 on 2020/1/16  9:35
pc端 调试的登陆界面
*/
<template>
  <div class="page_container" style="background-color: #f7f8fa;">
    <NavHeaderBanner title="登陆账号（pc 调试专用）" />
    <div class="login_dev_body">
      <van-cell-group>
        <van-field v-model="username" style="margin-top: 8px" placeholder="请输入用户名" />
        <van-field v-model="password" style="margin-top: 8px" type="password" placeholder="请输入密码" />
      </van-cell-group>
      <div v-if="captchaID" class="login_content_field">
        <van-cell-group style="display: flex; flex-direction: row">
          <van-field v-model="code" placeholder="请输入图形验证码" />
          <img style="width: 40%;" :src="codePngUrl+ '/'+ captchaID + '.png?' + timeStamp" @click="timeStamp=new Date().getTime()" />
        </van-cell-group>
      </div>
      <div class="autoLogo_banner">
        <span style="font-size: 12px;color: #333333">自动登陆</span>
        <van-switch v-model="autoLogin" @change="autoLoginChange" />
      </div>
      <van-button type="primary" @click="loginBtn">
        登陆
      </van-button>
    </div>

    <!-- 租户选择弹窗 -->
    <oort-popup v-model="tenantVis" position="center" style="width: 85%; border-radius: 12px;">
      <div class="tenant_popup_container">
        <div class="tenant_popup_title">
          <span>请选择登录租户</span>
        </div>
        <div class="tenant_list">
          <div
            v-for="item in tenantList"
            :key="item.tenant_id"
            class="tenant_item"
            @click="tenantClickLogin(item)"
          >
            <div class="tenant_name">
              {{ item.tenant_name }}
            </div>
            <div class="tenant_info">
              {{ item.tenant_code }}
            </div>
          </div>
        </div>
      </div>
    </oort-popup>
  </div>
</template>

<script setup>

import { ref, onMounted } from 'vue'

import NavHeaderBanner from '@/components/navHeaderBanner'
import OortPopup from '@/components/popup/oort_popup'

import { getUserTenants } from '@/api/login/indexapaas.ts'
import { showToast } from 'vant'

const tenantList = ref([])
const tenantVis = ref(false)
const tempToken = ref('')
const getTenantInfo = () => {
  const params = {
    accessToken: tempToken.value
  }
  getUserTenants(params).then((res) => {
    if (res.code === 200) {
      tenantList.value = res.data.list
      // 只有一个租户直接登录
      if (tenantList.value.length === 1) {
        tenantClickLogin(tenantList.value[0])
      } else if (tenantList.value.length > 1) {
        // 多个租户，显示选择弹窗
        tenantVis.value = true
      } else {
        showToast('暂无租户信息')
      }
    }
  })
}

const emits = defineEmits(['loginSuccess'])
const tenantClickLogin = (item) => {
  const params = {
    accessToken: tempToken.value,
    tenant_id: item.tenant_id
  }
  store.login(params).then((res) => {
    if (res.code === 200) {
      // 清空错误
      showToast('登录成功')
      emits('loginSuccess')
    }
  })
}

const autoLogin = ref(false)

import { useUserStore } from '@/store/modules/useraPaas'
import Config from '@/config'
const store = useUserStore()
const username = ref('')
const password = ref('')
const captchaID = ref('')
const code = ref('')
const codePngUrl = ref(Config.URL + Config.gateWay + 'apaas-sso/captcha/v1/')

const timeStamp = ref(new Date().getTime())

const loginBtn = async() => {
  if (autoLogin.value) {
    const tempObj = {
      username: username.value,
      password: password.value
    }
    window.sessionStorage.setItem('autoLogin', JSON.stringify(tempObj))
  }
  store.getLogin({
    loginId: username.value,
    password: password.value,
    code: code.value,
    // tenant_id: loginForm.tenant_id,
    captchaID: captchaID.value
  }).then((res) => {
    if (res.code === 200) {
      tempToken.value = res.data.token
      getTenantInfo()
    } else {
      timeStamp.value = new Date().getTime()
      captchaID.value = res.data.CaptchaID
    }
  }).catch(() => {
  }).finally(() => {
  })
}

onMounted(() => {
  if (window.sessionStorage.getItem('autoLogin')) {
    try {
      const tempObj = JSON.parse(window.sessionStorage.getItem('autoLogin'))
      username.value = tempObj.username
      password.value = tempObj.password
      loginBtn()
    } catch (e) {
      // console.log(e)
    }
  }
})

</script>

<style lang="scss" scoped>
  .closeBtn {
    position: absolute;
    top: -10px;
    left: -10px;
    width: 25px;
    height: 25px;
  }
  .slide_area {
    z-index: 99;
    position: absolute;
    top: 68px;
    left: 10px;
    background-color: #F0F0F0;
    box-shadow: 1px 1px 5px 2px #33333370;
    padding: 20px;
  }
  .autoLogo_banner {
    display: flex;
    flex-direction: row;
    align-items: center;
    height: 48px;
    span {
      margin-left: 16px;
      margin-right: 16px;
    }
  }
  .login_dev_body {
    display: flex;
    justify-content: center;
    flex-direction: column;
  }

  /* 租户选择弹窗样式 */
  .tenant_popup_container {
    padding: 24px 16px;
    background: #fff;
    border-radius: 12px;
  }

  .tenant_popup_title {
    text-align: center;
    margin-bottom: 24px;
    font-size: 18px;
    font-weight: 600;
    color: #333333;
  }

  .tenant_list {
    max-height: 400px;
    overflow-y: auto;
  }

  .tenant_item {
    display: flex;
    flex-direction: column;
    padding: 16px 12px;
    margin-bottom: 12px;
    background: #f5f6f7;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s ease;
    border-left: 4px solid transparent;

    &:active {
      background: #e8e9eb;
      border-left-color: #1989fa;
    }

    &:hover {
      background: #e8e9eb;
      border-left-color: #1989fa;
    }
  }

  .tenant_name {
    font-size: 16px;
    font-weight: 500;
    color: #333333;
    margin-bottom: 6px;
  }

  .tenant_info {
    font-size: 12px;
    color: #999999;
  }
</style>
