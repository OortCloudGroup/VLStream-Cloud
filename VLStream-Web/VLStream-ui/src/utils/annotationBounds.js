// Native image coordinates only. Preserve identity and labels when constraining geometry.
export function constrainAnnotation(annotation, width, height) {
  if (!(width > 0 && height > 0)) throw new Error('图片尺寸未就绪，请等待图片加载完成')
  if (annotation.type === 'rect') {
    const { x, y, width: w, height: h } = annotation
    if (![x, y, w, h].every(Number.isFinite) || w <= 0 || h <= 0) throw new Error('标注坐标或宽高无效')
    const left = Math.max(0, x), top = Math.max(0, y)
    const right = Math.min(width, x + w), bottom = Math.min(height, y + h)
    if (!(right > left && bottom > top)) throw new Error('标注框完全在图片外，请调整或删除该框')
    return { ...annotation, x: left, y: top, width: right - left, height: bottom - top }
  }
  if (annotation.type === 'circle') {
    const { cx, cy, r } = annotation
    if (![cx, cy, r].every(Number.isFinite) || r <= 0) throw new Error('圆形标注坐标无效')
    const radius = Math.min(r, cx, cy, width - cx, height - cy)
    if (!(radius > 0)) throw new Error('圆心需在图片范围内，请调整该标注')
    return { ...annotation, r: radius }
  }
  return annotation
}
