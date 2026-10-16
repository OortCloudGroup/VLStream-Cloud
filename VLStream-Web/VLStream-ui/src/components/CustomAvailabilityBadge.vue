<template>
  <span class="availability-badge">
    <el-tooltip :content="details" placement="top">
      <el-tag :type="type">{{ $tp(label) }}</el-tag>
    </el-tooltip>
    <small v-if="showReason && state === 'UNKNOWN'" class="availability-reason">{{ $tp(reason) }}</small>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { translatePhrase as uiText } from '@/i18n'
import { customStatusDetail, customStatusExpiresAt } from '@/utils/customDeviceState'
const props = defineProps({ device: { type: Object, required: true }, now: { type: Number, required: true }, showReason: Boolean })
const state = computed(() => customStatusDetail(props.device, props.now))
const label = computed(() => ({ ONLINE: '在线', OFFLINE: '离线', AUTH_ERROR: '认证异常', STREAM_ERROR: '视频流异常' })[state.value] || '未知')
const type = computed(() => ({ ONLINE: 'success', OFFLINE: 'danger', AUTH_ERROR: 'warning', STREAM_ERROR: 'warning' })[state.value] || 'info')
const reasons = { AVAILABLE: '协议探测成功', NOT_CHECKED: '尚未检测', STALE: '状态证据已过期',
  UNREACHABLE: '网络连接失败或超时', AUTHENTICATION_FAILED: '视频源认证失败', AUTHENTICATION_UNSUPPORTED: '源认证方式暂不支持探测',
  STREAM_UNAVAILABLE: '视频源不可用', STREAM_TIMEOUT: '视频数据超时', NO_VIDEO_CONTENT: '未收到视频内容', NO_VIDEO_SDP: '无视频通道',
  ADDRESS_BLOCKED: '地址安全校验未通过', CONFIGURATION_ERROR: '视频源地址配置无效', UNSUPPORTED_CONTENT: '视频内容格式暂不支持探测',
  UNSUPPORTED_ENCRYPTED: '加密视频暂不支持轻量探测', MONITOR_BUSY: '正在等待检测', MONITOR_UNAVAILABLE: '状态服务暂不可用',
  UNSUPPORTED_SIGNAL_ONLY: '信令可达，视频尚未验证', UNSUPPORTED_STANDBY: '设备待机，未唤醒', UNSUPPORTED_PRESENCE: '未配置可信信令网关',
  UNSUPPORTED_VIDEO_METADATA: '未取得可验证的视频样本',
  UNSUPPORTED_CODEC: '编码格式暂不支持轻量探测', UNSUPPORTED_AGGREGATE: '聚合媒体暂未验证', UNSUPPORTED_CONTENT_LIMIT: '媒体数据超过轻量检测范围',
  STALE_HEARTBEAT: '设备心跳证据已过期', FRESH_HEARTBEAT: '授权目标心跳有效', heartbeat_timeout: '设备心跳超时' }
const time = value => value ? new Date(value).toLocaleString() : uiText('未上报')
const reason = computed(() => props.device.availability?.checkedAt > 0 && customStatusExpiresAt(props.device) <= props.now ? '状态证据已过期' :
  props.device.availability?.source === 'CUSTOM_MEDIA_RECEIVING' && props.device.availability?.reason === 'AVAILABLE' ? '当前视频数据持续接收' :
    reasons[props.device.availability?.reason] || '尚未检测')
const details = computed(() => `${uiText(reason.value)} · ${uiText('最近检查')}: ${time(props.device.availability?.checkedAt)} · ${uiText('最近可用')}: ${time(props.device.availability?.lastAvailableAt)}`)
</script>

<style scoped>
.availability-badge { display: inline-flex; flex-direction: column; align-items: flex-start; gap: 2px; }
.availability-reason { color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.3; white-space: normal; }
</style>
