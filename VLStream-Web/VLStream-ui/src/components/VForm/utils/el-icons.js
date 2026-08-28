/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import {
  Edit, Minus, Plus, InfoFilled, Search, CirclePlus, Delete,
  ArrowDown, ArrowUp
} from '@element-plus/icons-vue'

export function registerIcon(app) {
  app.component('ElIconEdit', Edit)
  app.component('ElIconMinus', Minus)
  app.component('ElIconPlus', Plus)
  app.component('ElIconInfo', InfoFilled)
  app.component('ElIconSearch', Search)
  app.component('ElIconCirclePlusOutline', CirclePlus)
  app.component('ElIconDelete', Delete)
  app.component('ElIconArrowDown', ArrowDown)
  app.component('ElIconArrowUp', ArrowUp)
}
