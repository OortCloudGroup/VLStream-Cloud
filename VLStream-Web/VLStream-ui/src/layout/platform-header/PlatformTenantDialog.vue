<template>
  <div class="change-tenant-content">
    <div v-if="currentTenant" class="current-org-box">
      <div class="org-title"><div class="line" /><span>当前组织</span></div>
      <div class="current-org-main">
        <img v-if="currentTenant.company_logo" class="org-logo" :src="platformImageUrl(currentTenant.company_logo)" alt="">
        <oort-svg-icon v-else class="org-logo" name="tenant" />
        <div class="org-info">
          <div class="org-name">{{ currentTenant.tenant_name }}</div>
          <div class="org-tags">
            <div v-if="currentTenant.certified" class="certifi"><img :src="certifiedIcon"><span>已认证</span></div>
            <div v-else class="getCertifi" @click="ElMessage.warning('请前往统一用户平台认证')"><img :src="getCertifiIcon"><span>去认证</span></div>
          </div>
        </div>
      </div>
      <div class="cert-box">
        <el-descriptions :column="2" class="cert-info">
          <el-descriptions-item><template #label><img :src="legalIcon" class="info-icon">法定代表人</template>{{ currentTenant.legal_name }}</el-descriptions-item>
          <el-descriptions-item><template #label><img :src="phoneIcon" class="info-icon">联系电话</template>{{ currentTenant.phone }}</el-descriptions-item>
          <el-descriptions-item><template #label><img :src="codeIcon" class="info-icon">组织机构代码</template>{{ currentTenant.organization_code }}</el-descriptions-item>
          <el-descriptions-item><template #label><img :src="addressIcon" class="info-icon">地址</template>{{ currentTenant.ex_data.address }}</el-descriptions-item>
          <el-descriptions-item><template #label><img :src="dateIcon" class="info-icon">开通时间</template>{{ currentTenant.start_day }}</el-descriptions-item>
          <el-descriptions-item><template #label><img :src="dateIcon" class="info-icon">到期时间</template>{{ currentTenant.end_day }}</el-descriptions-item>
          <el-descriptions-item>
            <template #label><img :src="remarkIcon" class="info-icon">备注</template>
            <span>{{ showExpand ? shortRemark : fullRemark }}</span>
            <span v-if="fullRemark" class="expand-btn" @click="showExpand = !showExpand">{{ showExpand ? '展开' : '收起' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
    <div class="change-org-box">
      <div class="org-title"><div class="line" /><span>切换组织</span></div>
      <div class="change-org-list">
        <div v-for="org in tenantList" :key="org.tenant_id" class="org-card" @click="emit('switch-tenant', { id: org.tenant_id, name: org.tenant_name })">
          <img v-if="org.company_logo" class="org-card-logo" :src="platformImageUrl(org.company_logo)" alt="">
          <oort-svg-icon v-else class="org-card-logo" name="tenant" />
          <div class="org-card-info"><div class="org-card-name">{{ org.tenant_name }}</div><div class="org-card-desc">{{ org.remark || org.tenant_name }}</div></div>
        </div>
      </div>
      <div v-if="!loading && tenantList.length === 0" class="no-data">暂无可切换的组织</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPlatformTenantDetail, getPlatformUserTenants } from '@/api/platformHeader'
import certifiedIcon from '@/assets/img/unifi/certified_icon.png'
import getCertifiIcon from '@/assets/img/unifi/getCertifi.png'
import legalIcon from '@/assets/img/unifi/lxr_icon.png'
import phoneIcon from '@/assets/img/unifi/lxdh_icon.png'
import codeIcon from '@/assets/img/unifi/zzjgdm_icon.png'
import addressIcon from '@/assets/img/unifi/lxdz_icon.png'
import dateIcon from '@/assets/img/unifi/rq_icon.png'
import remarkIcon from '@/assets/img/unifi/bz_icon.png'

const props = defineProps({ tenantId: { type: String, default: '' } })
const emit = defineEmits(['switch-tenant'])
const tenantList = ref([])
const currentTenant = ref(null)
const fullRemark = ref('')
const showExpand = ref(true)
const loading = ref(true)
const shortRemark = computed(() => fullRemark.value.length > 56 ? `${fullRemark.value.slice(0, 56)}...` : fullRemark.value)

const platformOrigin = () => {
  try { return new URL(import.meta.env.VITE_PLATFORM_LOGIN_URL, window.location.origin).origin } catch (error) { return window.location.origin }
}
const platformImageUrl = value => {
  if (!value) return ''
  const imageUrl = String(value)
  const gatewayMatch = imageUrl.match(/\/(?:oort)?wj1\/(.*)$/i)
  if (gatewayMatch) return `${platformOrigin()}/bus/wj1/${gatewayMatch[1]}`
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl
  return new URL(imageUrl, `${platformOrigin()}/`).toString()
}

onMounted(async() => {
  const [listResult, detailResult] = await Promise.allSettled([
    getPlatformUserTenants(),
    getPlatformTenantDetail(props.tenantId)
  ])
  if (listResult.status === 'fulfilled') tenantList.value = (listResult.value || []).filter(item => item.tenant_id !== props.tenantId)
  if (detailResult.status === 'fulfilled') {
    currentTenant.value = { ...(detailResult.value || {}), ex_data: detailResult.value?.ex_data || {} }
    fullRemark.value = currentTenant.value.ex_data.remark || ''
  }
  loading.value = false
})
</script>

<style scoped lang="scss">
.current-org-box { margin-bottom: 32px; }
.org-title { margin-bottom: 24px; display: flex; align-items: center; gap: 8px; span { color: #191919; font-size: 18px; font-weight: 400; line-height: 28px; } .line { width: 4px; height: 22px; background-color: var(--el-color-primary); } }
.current-org-main { margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
.org-logo { width: 40px; height: 40px; border-radius: 8px; }
.org-info { display: flex; align-items: center; gap: 4px; }
.org-name { color: #333; font-size: 16px; font-weight: 600; }
.org-tags { cursor: pointer; }
.certifi { width: 74px; height: 23px; display: flex; align-items: center; justify-content: center; gap: 4px; border-radius: 12px; background-color: #e4edfe; img { width: 16px; height: 16px; } span { color: #2f69f8; font-size: 12px; } }
.getCertifi { display: flex; align-items: center; gap: 4px; img { width: 16px; height: 16px; } span { color: #2f69f8; font-size: 14px; } }
.cert-box { margin-bottom: 40px; border-radius: 4px; background: #f8fbfe !important; }
.cert-info { :deep(.el-descriptions__body) { padding: 16px 20px; border-radius: 4px; background: #f8fbfe !important; } :deep(.el-descriptions__cell) { width: 320px !important; } :deep(.el-descriptions__label) { color: #999; font-size: 14px; line-height: 19px; } :deep(.el-descriptions__content) { color: #333; font-size: 14px; line-height: 19px; } }
.info-icon { width: 20px; height: 20px; margin-right: 7px; vertical-align: middle; }
.expand-btn { color: #2856a5; font-size: 14px; cursor: pointer; }
.change-org-list { width: 100%; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; box-sizing: border-box; }
.org-card { width: 100%; min-width: 0; min-height: 137px; padding: 20px; display: flex; flex-direction: column; gap: 12px; overflow: hidden; border-radius: 4px; box-shadow: 0 0 10px rgb(0 0 0 / 7%); background: #fff; box-sizing: border-box; cursor: pointer; transition: all 0.3s ease; &:hover { background: var(--el-color-primary-hb); } }
.org-card-logo { width: 40px; height: 40px; flex-shrink: 0; border-radius: 8px; }
.org-card-info { min-width: 0; display: flex; flex-direction: column; gap: 8px; }
.org-card-name { overflow: hidden; color: #333; font-size: 16px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.org-card-desc { overflow: hidden; color: #999; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.no-data { width: 100%; height: 100px; display: flex; align-items: center; justify-content: center; color: #999; font-size: 14px; text-align: center; }
</style>
