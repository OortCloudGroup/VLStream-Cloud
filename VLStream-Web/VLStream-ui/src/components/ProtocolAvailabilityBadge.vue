<template>
  <el-tooltip :content="details" placement="top">
    <el-tag :type="type">{{ $tp(label) }}</el-tag>
  </el-tooltip>
</template>

<script setup>
import { computed } from 'vue'
import { translatePhrase as uiText } from '@/i18n'

const props = defineProps({ availability: { type: Object, default: null } })
const state = computed(() => props.availability?.state || 'UNKNOWN')
const label = computed(() => ({ ONLINE: '在线', OFFLINE: '离线', AUTH_ERROR: '认证失败', STREAM_ERROR: '视频源不可用' })[state.value] || '未知')
const type = computed(() => ({ ONLINE: 'success', OFFLINE: 'danger', AUTH_ERROR: 'warning', STREAM_ERROR: 'warning' })[state.value] || 'info')
const reasons = { NOT_CHECKED: '尚未检查', AVAILABLE: '协议探测成功', UNREACHABLE: '连接失败或超时',
  AUTHENTICATION_FAILED: '认证失败', AUTHENTICATION_UNSUPPORTED: '认证方式不支持', CONFIGURATION_ERROR: '设备地址配置无效',
  UNSUPPORTED: '暂不支持此播放类型的探测', STREAM_UNAVAILABLE: '视频源不可用', NO_VIDEO_SDP: '无视频通道',
  MANAGEMENT_UNAVAILABLE: 'ONVIF服务不可用', STALE: '状态已过期', MONITOR_UNAVAILABLE: '状态服务不可用' }
const time = value => value ? new Date(value).toLocaleString() : uiText('未上报')
const details = computed(() => `${uiText(reasons[props.availability?.reason] || '尚未检查')} · ${uiText('最近检查')}: ${time(props.availability?.checkedAt)} · ${uiText('最近可用')}: ${time(props.availability?.lastAvailableAt)}`)
</script>
