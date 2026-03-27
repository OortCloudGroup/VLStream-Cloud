<template>
  <div class="ptz-panel">
    <div class="control-wheel">
      <div class="dir up" @click="noop">
        <img src="@/assets/img/VLStreamCloud/PTZ/jt.png" alt="" />
      </div>
      <div class="dir left" @click="noop">
        <img src="@/assets/img/VLStreamCloud/PTZ/jt.png" alt="" />
      </div>
      <div class="dir center" @click="noop">
        <img src="@/assets/img/VLStreamCloud/PTZ/jz.png" alt="" />
      </div>
      <div class="dir right" @click="noop">
        <img src="@/assets/img/VLStreamCloud/PTZ/jt.png" alt="" />
      </div>
      <div class="dir down" @click="noop">
        <img src="@/assets/img/VLStreamCloud/PTZ/jt.png" alt="" />
      </div>
    </div>

    <div class="zoom-bar">
      <div @click="noop">
        <img src="@/assets/img/VLStreamCloud/PTZ/bs_d.png" alt="" />
      </div>
      <span class="zoom-label">变倍</span>
      <div @click="noop">
        <img src="@/assets/img/VLStreamCloud/PTZ/bs_a.png" alt="" />
      </div>
    </div>

    <div class="quick-grid">
      <div
        v-for="item in quickActions"
        :key="item.label"
        class="quick-item"
        @click="noop"
      >
        <div class="quick-icon">
          <img :src="item.icon" alt="" />
        </div>
        <div class="quick-text">
          {{ item.label }}
        </div>
      </div>
    </div>

    <div class="more-toggle" @click="showMore = !showMore">
      <span>更多</span>
      <van-icon :name="showMore ? 'arrow-up' : 'arrow-down'" />
    </div>

    <div v-if="showMore" class="more-grid">
      <div
        v-for="item in moreActions"
        :key="item.label"
        class="more-item"
        @click="noop"
      >
        <div class="more-icon">
          <img :src="item.icon" alt="" />
        </div>
        <div class="more-text">
          {{ item.label }}
        </div>
      </div>
    </div>

    <div class="footer-link" @click="noop">
      <img src="@/assets/img/VLStreamCloud/PTZ/sxj.png" alt="" />
      <div>
        摄像机管理后台
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import cd from '@/assets/img/VLStreamCloud/PTZ/cd.png'
import fzjj from '@/assets/img/VLStreamCloud/PTZ/fzjj.png'
import gq_a from '@/assets/img/VLStreamCloud/PTZ/gq_a.png'
import gq_d from '@/assets/img/VLStreamCloud/PTZ/gq_d.png'
import jj_d from '@/assets/img/VLStreamCloud/PTZ/jj_d.png'
import jtcsh from '@/assets/img/VLStreamCloud/PTZ/jtcsh.png'
import sdgz from '@/assets/img/VLStreamCloud/PTZ/sdgz.png'
import tj_a from '@/assets/img/VLStreamCloud/PTZ/tj_a.png'
import tj_d from '@/assets/img/VLStreamCloud/PTZ/tj_d.png'
import yjsw from '@/assets/img/VLStreamCloud/PTZ/yjsw.png'
import yjxh from '@/assets/img/VLStreamCloud/PTZ/yjxh.png'
import ddw from '@/assets/img/VLStreamCloud/PTZ/ddw.png'

const showMore = ref(false)

const quickActions = [
  { label: '调焦+', icon: tj_a },
  { label: '聚焦+', icon: jj_d },
  { label: '光圈+', icon: gq_a },
  { label: '调焦-', icon: tj_d },
  { label: '聚焦-', icon: jj_d },
  { label: '光圈-', icon: gq_d }
]

const moreActions = [
  { label: '灯光', icon: ddw },
  { label: '辅助聚焦', icon: fzjj },
  { label: '镜头初始化', icon: jtcsh },
  { label: '菜单', icon: cd },
  { label: '手动跟踪', icon: sdgz },
  { label: '3D定位', icon: ddw },
  { label: '一键巡航', icon: yjxh },
  { label: '一键守望', icon: yjsw }
]

const noop = () => {}
</script>

<style scoped lang="scss">
.ptz-panel {
  background: #f6f8ff;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.control-wheel {
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: rgba(227, 234, 255, 1);
  position: relative;
  display: grid;
  place-items: center;
}

.dir {
  width:44px;
  height:24.75px;
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1c6cff;
  font-size: 22px;
  img{
    width: 100%;
    height: 100%;
  }

  &.up {
    top: 12px;
    left: 50%;
    transform: translateX(-50%);
  }

  &.down {
    bottom: 12px;
    left: 50%;
    transform: translateX(-50%) rotate(180deg);
  }

  &.left {
    left: 12px;
    top: 50%;
    transform: translateY(-50%) rotate(270deg);
  }

  &.right {
    right: 12px;
    top: 50%;
    transform: translateY(-50%) rotate(90deg);
  }

  &.center {
    width:93.5px;
    height:93.5px;
    border-radius: 50%;
  }
}

.zoom-bar {
  width: 90%;
  max-width: 360px;
  background: rgba(227, 234, 255, 1);
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;

  .zoom-label {
    line-height: 19px;
    color: #1a53ff;
    font-size: 14px;
  }

  img{
    width: 24px;
    height: 24px;
  }
}

.quick-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 12px;
}

.quick-item {
  padding: 6px 7px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #333;
  height: 44px;
  background: #f0f2f5;
  border-radius: 8px;
  box-sizing: border-box;
}

.quick-icon {
  width: 32px;
  height: 32px;
  img{
    width: 100%;
    height: 100%;
  }
}

.quick-text {
  font-size: 14px;
}

.more-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #1c6cff;
  font-size: 14px;
}

.more-grid {
  padding: 12px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  background: #f0f2f5;
  border-radius: 8px;
}

.more-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 14px;
  color: #333;
}

.more-icon {
  width: 32px;
  height: 32px;
  img{
    width: 100%;
    height: 100%;
  }
}

.footer-link {
  height: 53px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  background: #e3eaff;
  border-radius: 8px;
  color: #1a53ff;
  font-size: 16px;
  img{
    width: 48px;
    height: 48px;
  }
}
</style>
