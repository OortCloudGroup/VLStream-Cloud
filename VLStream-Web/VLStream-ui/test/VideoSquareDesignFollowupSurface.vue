<template>
    <div class="video-square fixture-surface">
        <div v-if="structured" class="main-content">
            <div class="main-layout"><div class="map-area">
                <slot name="controls" />
                <div class="square-sharing-entry"><slot name="sharing" /></div>
                <section class="square-video-stage"><slot name="video" /></section>
            </div></div>
            <slot name="catalog" />
        </div>
        <slot v-else />
    </div>
</template>

<script setup>
defineProps({ structured: Boolean })
</script>

<style scoped>
.fixture-surface { width: 100%; height: 100%; min-width: 0; min-height: 0; }
</style>
<style scoped src="../src/views/VideoAggregation/video-square-design.css"></style>
