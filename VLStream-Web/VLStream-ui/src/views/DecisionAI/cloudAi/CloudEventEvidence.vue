<template>
    <div v-loading="loading" class="cloud-ai-evidence-detail">
        <el-alert v-if="error" :title="error" type="error" :closable="false"><el-button link type="primary" @click="load">重新加载</el-button></el-alert>
        <template v-if="event"><div class="cloud-ai-inline" style="margin-bottom:20px"><el-tag>{{ event.event_type }}</el-tag><span>{{ event.detail.cameraName }} · {{ event.detail.modelName }}</span><span class="cloud-ai-secondary">{{ formatTime(event.detected_at) }}</span></div>
            <div class="cloud-ai-evidence"><figure v-for="(label, role) in { annotated: '算法标框', original: '原始画面' }" :key="role"><el-image v-if="event.media[role]" :src="event.media[role]" :preview-src-list="[event.media[role]]" fit="contain" style="width:100%;aspect-ratio:16/9;background:#152033;border-radius:6px"><template #error><span class="cloud-ai-help">图片暂不可用，请刷新</span></template></el-image><el-empty v-else :description="`${label}暂不可用`" :image-size="55" /><figcaption>{{ label }}</figcaption></figure></div>
            <section class="cloud-ai-section"><h3>告警录像 <el-tag size="small" :type="event.media_state === 'READY' ? 'success' : event.media_state === 'FAILED' ? 'danger' : 'warning'">{{ mediaLabels[event.media_state] || event.media_state }}</el-tag></h3><video v-if="event.media.video" :key="event.media.video" class="cloud-ai-video" :src="event.media.video" controls preload="metadata" playsinline @error="videoError = true" /><p v-if="videoError" class="cloud-ai-error">视频暂无法播放，请刷新证据或检查存储服务。</p><p v-if="event.media_state === 'PENDING'" class="cloud-ai-note">正在生成告警后的视频，完成后会补齐到这条事件。<el-button link type="primary" @click="load">刷新证据</el-button></p><p v-else-if="event.media_state === 'FAILED'" class="cloud-ai-error">{{ event.detail.mediaError || '录像生成失败，截图与检测结果已保留。' }}</p><p v-else class="cloud-ai-help">请求前 {{ event.detail.requestedPreSeconds }} 秒 / 后 {{ event.detail.requestedPostSeconds }} 秒；实际前 {{ Number(event.detail.actualPreSeconds || 0).toFixed(1) }} 秒 / 后 {{ Number(event.detail.actualPostSeconds || 0).toFixed(1) }} 秒。{{ event.media_state === 'PARTIAL' ? '启动时间或视频中断导致片段不完整。' : '' }}</p></section>
            <section class="cloud-ai-section"><h3>检测目标</h3><el-table :data="event.detail.detections || []" max-height="250"><el-table-column label="类别"><template #default="{ row }">{{ event.detail.labels?.[row.classId] || row.classId }}</template></el-table-column><el-table-column label="置信度"><template #default="{ row }">{{ (row.confidence * 100).toFixed(1) }}%</template></el-table-column><el-table-column label="目标位置"><template #default="{ row }">{{ row.box?.join(', ') }}</template></el-table-column></el-table></section>
            <p class="cloud-ai-help">事件编号：{{ event.eventId }} · 与主动安全共用事件及处置状态。</p>
        </template>
    </div>
</template>
<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { cloudAi, cloudError } from '@/api/cloudAi'
import { formatTime } from './presentation'
import './cloud-ai.css'
const props = defineProps({ eventId: { type: [String, Number], required: true } })
const event = ref(null), error = ref(''), loading = ref(false), videoError = ref(false)
const mediaLabels = { PENDING: '生成中', READY: '完整片段', PARTIAL: '部分片段', FAILED: '录像失败' }
let generation = 0
async function load() { const request = ++generation; loading.value = true; error.value = ''; videoError.value = false; try { const data = await cloudAi.get('events', props.eventId); if (request === generation) event.value = data } catch (failure) { if (request === generation) error.value = cloudError(failure) } finally { if (request === generation) loading.value = false } }
watch(() => props.eventId, () => { event.value = null; load() }, { immediate: true })
onUnmounted(() => generation++)
</script>
