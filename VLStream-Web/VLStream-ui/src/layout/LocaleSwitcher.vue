<template>
  <el-dropdown class="global-locale-switcher" trigger="click" @command="setLocale">
    <button type="button" class="locale-trigger" :aria-label="translatePhrase('切换语言')">
      <img :src="langIcon" alt="">
      <span>{{ currentLocaleOption.shortName }}</span>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="option in localeOptions"
          :key="option.code"
          :command="option.code"
          :disabled="currentLocale === option.code"
        >
          {{ option.name }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import langIcon from '@/assets/img/svg/platform-lang.svg'
import { currentLocale, currentLocaleOption, localeOptions, setLocale, translatePhrase } from '@/i18n'
</script>

<style scoped>
.global-locale-switcher {
  flex-shrink: 0;
}

.locale-trigger {
  min-height: 36px;
  padding: 5px 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  border-radius: 6px;
  color: #2856a5;
  background: transparent;
  cursor: pointer;
}

.locale-trigger:hover,
.locale-trigger:focus-visible {
  background: #f3f7fd;
  outline: none;
}

.locale-trigger img {
  width: 20px;
  height: 20px;
}

.locale-trigger span {
  max-width: 150px;
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1500px) {
  .locale-trigger {
    padding-inline: 6px;
  }

  .locale-trigger span {
    max-width: 88px;
    font-size: 13px;
  }
}

@media (max-width: 1180px) {
  .locale-trigger span {
    display: none;
  }
}
</style>
