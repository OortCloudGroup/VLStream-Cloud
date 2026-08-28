/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

const modules = import.meta.glob('./*.vue', { eager: true })

export default {
  install(app) {
    for (const path in modules) {
      let cname = modules[path].default.name
      app.component(cname, modules[path].default)
    }
  }
}
