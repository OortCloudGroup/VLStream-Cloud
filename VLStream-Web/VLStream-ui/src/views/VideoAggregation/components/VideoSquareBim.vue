<template>
    <div class="bim">
        <nav v-if="scene.buildings.length > 1" class="building-list" :aria-label="$tp('选择楼栋')">
            <button :aria-pressed="activeBuilding === null" @click="chooseBuilding(null)">{{ $tp('全部楼栋') }}</button>
            <button v-for="(building, i) in scene.buildings" :key="i" :aria-pressed="activeBuilding === i" @click="chooseBuilding(i)">
                {{ name(building.name) }}
            </button>
        </nav>
        <div class="campus" :class="{ isolated: activeFloor !== null }">
            <figure v-for="building in drawings" :key="building.index">
                <svg :viewBox="building.viewBox" :aria-label="name(building.name)" class="building">
                    <defs>
                        <linearGradient :id="building.id + '-glass'" x2="1" y2=".25" gradientUnits="objectBoundingBox">
                            <stop stop-color="#809eaa"/><stop offset=".29" stop-color="#cededc"/>
                            <stop offset=".42" stop-color="#e8f2f1"/><stop offset=".45" stop-color="#b6ced3"/>
                            <stop offset=".76" stop-color="#94b5be"/><stop offset="1" stop-color="#d7e4e5"/>
                        </linearGradient>
                        <pattern :id="building.id + '-paving'" width="12" height="6" patternUnits="userSpaceOnUse">
                            <path d="M-6 0 6 6 18 0M-6 6 6 0 18 6" fill="none" stroke="#a6b7be" stroke-width=".45"/>
                        </pattern>
                    </defs>
                    <polygon :points="building.ground" transform="translate(0 4)" fill="#9aaeb6" stroke="#8a9fa9" />
                    <polygon :points="building.ground" fill="#e1e8e7" stroke="#bdcbc9" />
                    <polygon :points="building.ground" :fill="`url(#${building.id}-paving)`" />
                    <g v-for="floor in building.layers" :key="floor.index" class="slab-layer" stroke-linejoin="round">
                        <polygon :points="floor.rearGlass" :fill="`url(#${building.id}-glass)`" stroke="#839fac" stroke-width=".75" />
                        <polygon :points="floor.westGlass" fill="#a2c2ca" stroke="#7398a6" stroke-width=".75" />
                        <path :d="floor.glazing" fill="none" stroke="#73939c" stroke-width=".8" />
                        <polygon v-for="post in floor.posts" :key="post" :points="post" fill="#d7e4e6" stroke="#8fa3ac" stroke-width=".35" />
                        <path :d="floor.beams" fill="none" stroke="#eff4f4" stroke-width="2" />
                        <polygon :points="floor.slab" fill="#f4f5ee" stroke="#b4c1c5" stroke-width="1.5" />
                        <polygon :points="floor.front" fill="#c4d0d1" stroke="#98a9ac" stroke-width=".65" />
                        <polygon :points="floor.side" fill="#9baeb4" stroke="#869ca6" stroke-width=".65" />
                        <g v-for="(room, r) in floor.rooms" :key="r">
                            <title>{{ name(room.name) }}</title>
                            <polygon :points="room.plan" :fill="roomColors[r % roomColors.length]" stroke="#c7cbc5" stroke-width=".65" />
                            <polygon :points="room.divider" fill="#e2e5de" stroke="#a8b6b7" stroke-width=".55" />
                            <polygon :points="room.side" fill="#bacbd0" stroke="#97abb1" stroke-width=".55" />
                            <g v-for="(piece, f) in room.fixtures" :key="f" class="fixture" :class="piece.tone">
                                <polygon :points="piece.front" class="fixture-front" />
                                <polygon :points="piece.side" class="fixture-side" />
                                <polygon :points="piece.top" class="fixture-top" />
                            </g>
                        </g>
                        <path :d="`M${floor.nameAt.x + 5} ${floor.nameAt.y + 1}h-11`" class="level-tick" />
                        <text :x="floor.nameAt.x - 7" :y="floor.nameAt.y + 4" text-anchor="end" class="floor-tag">{{ floorTag(floor.name) }}<title>{{ name(floor.name) }}</title></text>
                    </g>
                    <g v-for="pin in building.pins" :key="pin.key" class="pin" :class="{ bound: canPlay(pin), chosen: selected === pin.key }"
                        :transform="`translate(${pin.x} ${pin.y})`" :role="canPlay(pin) ? 'button' : 'img'" :tabindex="canPlay(pin) ? 0 : undefined"
                        :aria-label="pinTitle(pin)" :aria-pressed="canPlay(pin) ? selected === pin.key : undefined"
                        @click="select(pin)" @keydown.enter.prevent="select(pin)" @keydown.space.prevent="select(pin)">
                        <title>{{ pinTitle(pin) }}</title>
                        <circle r="15" cy="-8" fill="transparent" />
                        <ellipse class="pin-foot" rx="4.5" ry="2.1" cy="1"/>
                        <g class="pin-symbol">
                            <path class="pin-body" d="M-9-12a4 4 0 0 1 4-4H5a4 4 0 0 1 4 4v7a3 3 0 0 1-3 3H2L0 1l-3-3h-3a3 3 0 0 1-3-3z" />
                            <rect x="-5" y="-11.5" width="7" height="5.5" rx="1" fill="white"/><path d="m2-10 4-2v6l-4-2z" fill="white" />
                        </g>
                    </g>
                </svg>
                <figcaption>{{ name(building.name) }}</figcaption>
            </figure>
        </div>
        <nav v-if="floorBuilding" class="floors" :aria-label="$tp('选择楼层')">
            <span>{{ $tp('楼层') }}</span>
            <button :aria-pressed="activeFloor === null" @click="chooseFloor(null)">{{ $tp('全部') }}</button>
            <button v-for="(floor,i) in floorBuilding.floors" :key="i" :aria-pressed="activeFloor === i" @click="chooseFloor(i)">
                {{ name(floor.name) }}
            </button>
        </nav>
        <div v-if="floorBuilding && activeFloor !== null" class="room-list">
            <div v-for="(room,r) in floorBuilding.floors[activeFloor]?.rooms" :key="r">
                <strong>{{ name(room.name) }}</strong>
                <button v-for="(pin,i) in room.cameras" :key="i" :disabled="!canPlay(pin)"
                    @click="select({ ...pin, key: [activeBuilding ?? 0, activeFloor, r, i].join('-'), location: [floorBuilding.name, floorBuilding.floors[activeFloor].name, room.name].join(' / ') })">{{ name(pin.label) }}</button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { computed, ref, useId, watch } from 'vue'
import { buildingGeometry } from '@/utils/videoSquareScenes.mjs'
import { translatePhrase } from '@/i18n'
const props = defineProps({ scene: { type: Object, required: true }, deviceMap: { type: Map, required: true }, sample: Boolean, selected: String })
const emit = defineEmits(['select', 'clear'])
const roomColors = ['#e8e8df', '#e0e6e7', '#e8e0d1', '#dfe9e2']
const drawingId = 'square-building-' + useId()
const activeBuilding = ref(null)
const activeFloor = ref(null)
const floorBuilding = computed(() => props.scene.buildings[activeBuilding.value ?? (props.scene.buildings.length === 1 ? 0 : -1)])
const name = text => props.sample ? translatePhrase(text) : text
function floorTag(text) {
    const label = name(text)
    return Array.from(label).length > 6 ? Array.from(label).slice(0, 5).join('') + '…' : label
}
const canPlay = pin => !props.sample && props.deviceMap.has(pin.deviceId)
const pinTitle = pin => [name(pin.label), pin.location, canPlay(pin) ? props.deviceMap.get(pin.deviceId).deviceName : translatePhrase(pin.deviceId ? '设备不在当前目录' : '示意点位')].join(' · ')
function facade(points, bays) {
    const [baseLeft, baseRight, topRight, topLeft] = points.split(' ').map(point => point.split(',').map(Number))
    const between = (a, b, t) => a.map((value, i) => value + (b[i] - value) * t).join(',')
    const segments = [`M${between(baseLeft, topLeft, .68)}L${between(baseRight, topRight, .68)}`]
    for (let i = 1; i < bays; i++) segments.push(`M${between(baseLeft, baseRight, i / bays)}L${between(topLeft, topRight, i / bays)}`)
    return { panes: segments.join(' '), beam: `M${topLeft}L${topRight}` }
}
const drawings = computed(() => props.scene.buildings.flatMap((building, index) => {
    if (activeBuilding.value !== null && index !== activeBuilding.value) return []
    const geometry = buildingGeometry(building, activeFloor.value)
    const pins = geometry.layers.flatMap(floor => floor.rooms.flatMap(room => room.pins.map(pin => ({ ...pin, key: index + '-' + pin.key }))))
    const layers = geometry.layers.map(floor => {
        const back = facade(floor.rearGlass, 12), west = facade(floor.westGlass, 8)
        return { ...floor, glazing: back.panes + ' ' + west.panes, beams: back.beam + ' ' + west.beam }
    })
    return [{ ...geometry, layers, index, id: drawingId + '-' + index, name: building.name, pins }]
}))
function select(pin) { if (canPlay(pin)) emit('select', pin) }
function chooseFloor(index) { activeFloor.value = index; emit('clear') }
function chooseBuilding(index) { activeBuilding.value = index; chooseFloor(null) }
watch(() => props.scene.title, () => chooseBuilding(null))
watch(() => props.scene, () => {
    if (activeBuilding.value !== null && !props.scene.buildings[activeBuilding.value]) chooseBuilding(null)
    if (activeFloor.value !== null && !floorBuilding.value?.floors[activeFloor.value]) chooseFloor(null)
})
</script>

<style scoped>
.bim { --pin: var(--scene-accent, #2075be); display: flex; flex: 1; flex-direction: column; min-width: 0; min-height: 0; overflow: auto; padding: clamp(6px, 1.2vw, 18px); gap: 12px; }
.campus { flex: 1; display: flex; flex-wrap: wrap; justify-content: center; align-content: center; align-items: stretch; min-width: 0; gap: 18px; }
figure { flex: 1 1 245px; display: flex; flex-direction: column; min-width: 0; margin: 0; align-items: center; justify-content: center; gap: 2px; }
.building { display: block; width: 100%; height: clamp(230px, 44vh, 560px); max-width: 800px; overflow: visible; }
figcaption { max-width: 100%; box-sizing: border-box; padding: 3px 9px; border: 1px solid #c6d4db; border-radius: 4px; background: #f8fafb; color: #263e4e; font-size: 13px; font-weight: 600; overflow-wrap: anywhere; text-align: center; }
.floor-tag { fill: #233d4e; font-size: 8.5px; font-weight: 650; paint-order: stroke; stroke: #f9fbfb; stroke-width: 3.2px; stroke-linejoin: round; }
.level-tick { stroke: #809ba9; stroke-width: 1; }
.fixture { --top: #c1cacf; --front: #8b9ba2; --side: #778b94; stroke: #607580; stroke-width: .35; }
.fixture.wood { --top: #dcc7a6; --front: #b9a88b; --side: #aa957b; }
.fixture.upholstery { --top: #91a6b3; --front: #748e9d; --side: #627c8d; }
.fixture.plant { --top: #a6b999; --front: #89a17c; --side: #748c69; }
.fixture-top { fill: var(--top); }.fixture-front { fill: var(--front); }.fixture-side { fill: var(--side); }
.pin-foot { fill: #334e5d; opacity: .42; }
.pin-body { fill: #6f828c; stroke: #fff; stroke-width: 1.4px; }
.pin.bound { cursor: pointer; }
.pin.bound .pin-body { fill: color-mix(in srgb, var(--pin) 50%, #13364e); }
.pin.bound:is(:hover, :focus-visible, .chosen) .pin-body { stroke: #172f44; stroke-width: 2px; }
.pin.bound:is(:hover, :focus-visible, .chosen) .pin-symbol { transform: translateY(-2px); }
.pin:focus-visible { outline: none; }
.pin:focus-visible > circle { fill: #fff; fill-opacity: .9; stroke: #152c43; stroke-width: 1.4; }
.building-list, .floors, .room-list > div { display: flex; align-items: center; justify-content: center; flex-wrap: wrap; gap: 4px; border: 1px solid #bdcbd3; border-radius: 6px; background: #f7f9fa; color: #304a5b; padding: 4px; max-width: 100%; box-sizing: border-box; }
.building-list { align-self: flex-start; justify-content: flex-start; }
.floors { align-self: center; }
button { max-width: 100%; min-height: 31px; padding: 3px 9px; color: #314c60; background: transparent; border: 1px solid transparent; border-radius: 3px; font: inherit; font-size: 12px; overflow-wrap: anywhere; cursor: pointer; }
button:hover:not(:disabled) { background: #e4edf2; }
button[aria-pressed="true"] { color: #163c57; background: color-mix(in srgb,var(--pin) 12%,white); border-color: color-mix(in srgb,var(--pin) 56%,#9caeb9); font-weight: 650; }
button:focus-visible { outline: 2px solid #174f7a; outline-offset: 1px; }
.floors > span { padding-inline: 5px; font-size: 12px; }
.room-list { display: flex; gap: 7px; flex-wrap: wrap; justify-content: center; font-size: 12px; }
.room-list strong { padding-inline: 5px; }
.room-list button { border-color: #c2ced6; background: white; }
.room-list button:disabled { cursor: default; color: #60717e; background: #eff2f3; }
@media (prefers-reduced-motion: no-preference) {
    .isolated .slab-layer { animation: inspect-floor .28s ease-out; }
    .pin-symbol { transition: transform .16s ease-out; }
}
@keyframes inspect-floor { from { opacity: .65; transform: translateY(3px); } }
</style>
