export function buildTrainingOptions({ mode, epochs, batchSize, imgSize, autoPublish }) {
  if (!['auto', 'advanced'].includes(mode)) throw new Error('请选择有效的训练模式')
  const options = {
    epochs: Number(epochs),
    batchSize: mode === 'auto' ? 16 : Number(batchSize),
    imgSize: mode === 'auto' ? 640 : Number(imgSize)
  }
  if (!Number.isInteger(options.epochs) || options.epochs < 1 || options.epochs > 10000) throw new Error('训练轮数必须为 1 至 10000 的整数')
  if (!Number.isInteger(options.batchSize) || options.batchSize < 1 || options.batchSize > 256) throw new Error('批大小必须为 1 至 256 的整数')
  if (!Number.isInteger(options.imgSize) || options.imgSize < 320 || options.imgSize > 1280 || options.imgSize % 32 !== 0) throw new Error('输入尺寸必须为 320 至 1280 范围内的 32 的倍数')
  return { ...options, extraParams: JSON.stringify({ mode, autoPublish: autoPublish === 'yes' }) }
}
