<template>
    <div class="network-scroll" tabindex="0" role="region" :aria-label="$tp('设备连接拓扑')">
        <svg class="network" :viewBox="`0 0 ${graph.width} ${graph.height}`"
            :style="{ minWidth: graph.width * 0.80 + 'px', maxWidth: graph.width * 1.22 + 'px', minHeight: graph.height * 0.80 + 'px' }"
            :aria-label="$tp('设备连接拓扑')">
            <defs>
                <marker :id="arrowId" markerWidth="11" markerHeight="10" refX="10" refY="5" orient="auto" markerUnits="userSpaceOnUse">
                    <path d="M0 1 9 5 0 9 2.7 5Z" fill="var(--wire)" />
                </marker>
                <symbol id="sq-eq-camera" viewBox="0 0 48 42">
                    <path fill="#dce8ee" d="m2 11 24-5 18 6-5 10-26 3-10-6z"/>
                    <path fill="#9badbd" d="m3 18 10 4 27-4-2 6-25 3-10-6z"/>
                    <path fill="#456376" d="m28 15 15-3-4 10-12 2z"/><ellipse cx="35" cy="18" rx="4" ry="3.5" fill="#93c8df"/>
                    <ellipse cx="35" cy="18" rx="2.8" ry="2.4" fill="#294b65"/><circle cx="36" cy="17.4" r=".8" fill="#def5ff"/>
                    <path d="m16 26-2 6-7 2m0-4v8" fill="none" stroke="#788fa0" stroke-width="3"/>
                </symbol>
                <symbol id="sq-eq-dome" viewBox="0 0 48 42">
                    <path fill="#dee7ec" d="M6 17a18 12 0 0 1 36 0l-2 4H8z"/><path fill="#7d96a8" d="M9 19h30c0 9-6 15-15 15S9 27 9 19z"/>
                    <ellipse fill="#405b6e" cx="26" cy="24" rx="8" ry="7"/><circle cx="27" cy="23" r="3" fill="#97cbe3"/>
                </symbol>
                <symbol id="sq-eq-switch" viewBox="0 0 48 42">
                    <path fill="#c3d4dd" d="m3 16 10-8 33 3-8 10z"/><path fill="#647f91" d="m3 16 35 5v12L3 27z"/><path fill="#9aafbb" d="m38 21 8-10v12l-8 10z"/>
                    <path d="m8 21 24 3" fill="none" stroke="#d9edf7" stroke-width="3" stroke-dasharray="4 2"/>
                    <path d="m17 11 10 1m-9 1 10 1" stroke="#8a9fae" stroke-width=".9"/><circle cx="35" cy="28" r="1.3" fill="#7de6ab"/>
                </symbol>
                <symbol id="sq-eq-server" viewBox="0 0 48 42">
                    <path fill="#d3dfe6" d="m10 7 11-5 17 5-10 5z"/><path fill="#96acb9" d="m28 12 10-5v28l-10 5z"/><path fill="#607b8e" d="m10 7 18 5v28l-18-6z"/>
                    <path d="m13 14 12 3m-12 4 12 3m-12 4 12 3" stroke="#d5e6ef" stroke-width="2.5"/>
                </symbol>
                <symbol id="sq-eq-storage" viewBox="0 0 48 42">
                    <path fill="#8ca9b8" d="M8 10h32v21c0 9-32 9-32 0z"/><ellipse cx="24" cy="10" rx="16" ry="5.5" fill="#d5e4eb"/>
                    <path d="M9 17c4 6 25 6 30 0M9 24c4 6 25 6 30 0" fill="none" stroke="#e7f2f6" stroke-width="2"/>
                </symbol>
                <symbol id="sq-eq-display" viewBox="0 0 48 42">
                    <rect x="3" y="5" width="41" height="27" rx="2" fill="#667f90"/><rect x="6" y="8" width="35" height="20" rx="1" fill="#b4d6e6"/>
                    <path d="m7 25 8-7 6 2 9-9 10 9v7H7z" fill="#78a9bf"/>
                    <path d="M24 8v20M6 18h35" stroke="#5c8298" stroke-width=".9"/><path d="M24 32v4m-10 1h20" stroke="#849ba9" stroke-width="3"/>
                </symbol>
                <symbol id="sq-eq-cloud" viewBox="0 0 48 42"><path d="M13 32C1 32 1 17 13 17c1-14 21-14 23-1 13 1 12 16 2 16z" fill="#d4ebf5" stroke="#75adc7" stroke-width="2.5"/></symbol>
                <symbol id="sq-eq-mobile" viewBox="0 0 48 42"><rect x="14" y="2" width="21" height="38" rx="4" fill="#647b88"/><rect x="16" y="6" width="17" height="27" rx="1" fill="#b4d6e6"/><circle cx="25" cy="36" r="1.6" fill="#e1e9ef"/></symbol>
                <symbol id="sq-eq-drone" viewBox="0 0 48 42"><path d="m9 12 30 14m-30 0 30-14" stroke="#829aab" stroke-width="4"/><path d="M1 11h17m12 0h17M1 26h17m12 0h17" stroke="#435f70" stroke-width="2"/><ellipse cx="24" cy="19" rx="10" ry="6" fill="#c6d7e0"/><circle cx="24" cy="25" r="4" fill="#597d90"/></symbol>
                <symbol id="sq-eq-sensor" viewBox="0 0 48 42"><rect x="10" y="8" width="28" height="26" rx="5" fill="#b5cad7"/><circle cx="24" cy="21" r="8" fill="#647f91"/><circle cx="24" cy="21" r="3" fill="#c9ecf7"/></symbol>
            </defs>
            <g v-for="(d, index) in graph.edges" :key="scene.edges[index].from + ':' + scene.edges[index].to"
                class="connection" :class="{ traced: scene.edges[index].from === selected || scene.edges[index].to === selected }"
                fill="none" aria-hidden="true">
                <path class="wire" :d="d" :marker-end="`url(#${arrowId})`" />
                <path v-if="index < 8 || scene.edges[index].from === selected || scene.edges[index].to === selected"
                    class="signal" :style="{ animationDelay: (index % 4) * 95 + 'ms' }" :d="d" pathLength="100" />
            </g>
            <g v-for="node in graph.nodes" :key="node.id" :transform="`translate(${node.x} ${node.y})`">
                <foreignObject width="204" height="65" overflow="visible">
                    <component :is="deviceMap.has(node.deviceId) && !sample ? 'button' : 'div'" class="equipment"
                        :class="{ selected: selected === node.id }" :aria-pressed="deviceMap.has(node.deviceId) && !sample ? selected === node.id : undefined"
                        @click="select(node)">
                        <span class="equipment-icon">
                            <VideoSquareVector v-if="node.shapes" :shapes="node.shapes" width="45" height="42" />
                            <svg v-else width="45" height="42" aria-hidden="true"><use :href="'#sq-eq-' + node.kind" width="45" height="42" /></svg>
                        </span>
                        <span class="caption"><strong :title="node.label">{{ sample ? $tp(node.label) : node.label }}</strong>
                            <small v-if="deviceMap.has(node.deviceId)" class="live"><i :class="deviceMap.get(node.deviceId).status" />{{ deviceMap.get(node.deviceId).deviceName }}</small>
                            <small v-else>{{ $tp(node.deviceId ? '设备不在当前目录' : kinds[node.kind]) }}<template
                                v-if="!sample && !node.deviceId && cameraKinds.includes(node.kind)"> · {{ $tp('示意点位') }}</template></small>
                        </span>
                    </component>
                </foreignObject>
            </g>
        </svg>
    </div>
</template>

<script setup>
import { computed, useId } from 'vue'
import { topologyLayout } from '@/utils/videoSquareScenes.mjs'
import VideoSquareVector from './VideoSquareVector.vue'
const props = defineProps({ scene: { type: Object, required: true }, deviceMap: { type: Map, required: true }, sample: Boolean, selected: String })
const emit = defineEmits(['select'])
const graph = computed(() => topologyLayout(props.scene))
const arrowId = 'square-arrow-' + useId()
const kinds = { camera: '摄像机', dome: '球机', drone: '无人机', mobile: '移动终端', switch: '交换机', server: '服务器', storage: '存储', cloud: '云平台', display: '显示终端', sensor: '传感器' }
const cameraKinds = ['camera', 'dome', 'drone', 'mobile']
function select(node) {
    if (!props.sample && props.deviceMap.has(node.deviceId)) emit('select', { ...node, key: node.id })
}
</script>

<style scoped>
.network-scroll { --wire: var(--scene-accent, #217cc5); display: flex; flex: 1; align-items: safe center; min-width: 0; min-height: 0; overflow: auto; overscroll-behavior: contain; padding: clamp(3px, 1.2vw, 16px); }
.network-scroll:focus-visible { outline: 2px solid var(--wire); outline-offset: -2px; }
.network { display: block; flex: 1 0 0; width: 100%; align-self: center; margin: auto; }
.wire { stroke: var(--wire); stroke-width: 1.7; }
.signal { stroke: var(--wire); stroke-linecap: round; stroke-width: 3.5; stroke-dasharray: 5 101; opacity: 0; pointer-events: none; }
.connection.traced .wire { stroke-width: 2.6; }
.equipment { display: flex; align-items: center; gap: 9px; width: 100%; height: 100%; padding: 6px 9px; box-sizing: border-box; background: linear-gradient(155deg,#fff 45%,#edf2f6); border: 1px solid #a7b8c6; border-radius: 6px; color: #203746; font: inherit; text-align: start; box-shadow: 0 2px 3px #142f4920; }
button.equipment { cursor: pointer; border-color: color-mix(in srgb, var(--wire) 54%, #9dadba); }
button.equipment:hover, .equipment.selected { border-color: var(--wire); box-shadow: 0 0 0 2px var(--wire); }
button.equipment:focus-visible { outline: 2px solid #172e42; outline-offset: 3px; box-shadow: 0 0 0 3px #fff; }
.equipment-icon { flex: none; width: 49px; height: 48px; display: grid; place-items: center; border-radius: 4px; border: 1px solid #d1dce3; background: linear-gradient(145deg,#fdfefe,#dce6ec); color: #2d455b; }
.caption { display: grid; gap: 5px; min-width: 0; }
.caption strong, .caption small { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.caption strong { font-size: 15px; font-weight: 600; }
.caption small { color: #627887; font-size: 11px; }
.live i { display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 4px; background: #a3afbc; }
.live i.ONLINE { background: #269e73; } .live i.OFFLINE { background: #c97b69; }
@media (prefers-reduced-motion: no-preference) {
    .signal { animation: carry-signal 1.8s ease-in-out 2; }
    .connection.traced .signal { animation-name: trace-signal; }
    .equipment { transition: border-color .16s, box-shadow .16s; }
}
@keyframes carry-signal { 0% { opacity: 0; stroke-dashoffset: 7; } 12%, 76% { opacity: .85; } 100% { opacity: 0; stroke-dashoffset: -99; } }
@keyframes trace-signal { 0% { opacity: 0; stroke-dashoffset: 7; } 15%, 83% { opacity: 1; } 100% { opacity: 0; stroke-dashoffset: -99; } }
</style>
