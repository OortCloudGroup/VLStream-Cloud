import { WidgetType } from '@codemirror/view'

export class PlaceholderWidget extends WidgetType {
  constructor(name) {
    super()
    this.name = name
  }
  eq(other) {
    return this.name === other.name
  }
  toDOM() {
    let elt = document.createElement('span')
    elt.style.cssText = `
        color: var(--el-color-primary);
        background: var(--el-color-primary-hb);`
    let text = this.name.split('.').pop()
    elt.textContent = text
    return elt
  }
  ignoreEvent() {
    return false
  }
}
