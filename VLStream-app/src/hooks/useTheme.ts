/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:54:35
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:54:35
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { ref, watchEffect } from 'vue'
import { getActiveThemeName, setActiveThemeName } from '@/utils/cache/localStorage'

const DEFAULT_THEME_NAME = 'dark'
type DefaultThemeNameType = typeof DEFAULT_THEME_NAME

/** 注册的主题名称, 其中 DefaultThemeNameType 是必填的 */
export type ThemeName = DefaultThemeNameType | 'light'

interface IThemeList {
  title: string
  name: ThemeName
}

/** 主题列表 */
const themeList: IThemeList[] = [
  {
    title: '默认',
    name: DEFAULT_THEME_NAME
  },
  {
    title: '亮色',
    name: 'dark'
  }
]

/** 正在应用的主题名称 */
const activeThemeName = ref<ThemeName>(getActiveThemeName() || DEFAULT_THEME_NAME)

const setTheme = (value: ThemeName) => {
  activeThemeName.value = value
}

/** 在 html 根元素上挂载 class */
const setHtmlClassName = (value: ThemeName) => {
  document.documentElement.className = value
}

const initTheme = () => {
  watchEffect(() => {
    const value = activeThemeName.value
    setHtmlClassName(value)
    setActiveThemeName(value)
  })
}

/** 主题 hook */
export function useTheme() {
  return { themeList, activeThemeName, initTheme, setTheme }
}
