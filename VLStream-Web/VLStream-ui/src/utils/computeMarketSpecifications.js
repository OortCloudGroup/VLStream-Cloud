// Presentation snapshots only. These facts do not grant inventory, scheduling,
// billing or machine access. Recheck after hardware or market-spec changes.
// P100: authorized read-only lscpu, /proc/meminfo, df, lsblk and nvidia-smi,
// observed 2026-10-21. Unmounted disks are excluded from usable data storage.
// 6000D: exact market specification confirmed by the user on 2026-10-21;
// this is not the instance's customized/expanded filesystem configuration.
const p10099 = Object.freeze({
  gpuLabel: 'Tesla P100 / 16 GB', shared: true,
  cpuCount: 8, cpuUnit: 'vCPU', cpuModel: 'QEMU Virtual CPU 2.5+',
  memory: '15.6 GiB', systemDisk: '35.3 GiB', dataDiskShared: true,
  driver: '580.178.04', cudaMax: '13.0',
  source: 'SSH只读核验', observedDate: '2026-10-21',
  note: 'CPU和内存为共享主机可见总量；硬盘为已挂载文件系统总容量，不是剩余容量；未挂载磁盘不计入数据盘。'
})
const p100173 = Object.freeze({
  gpuLabel: 'Tesla P100 / 16 GB', shared: true,
  cpuCount: 16, cpuUnit: 'vCPU', cpuModel: 'Xeon E5-2667 v4',
  memory: '15.6 GiB', systemDisk: '47.1 GiB', dataDisk: '90.0 GiB',
  driver: '535.161.08', cudaMax: '12.2',
  source: 'SSH只读核验', observedDate: '2026-10-21',
  note: 'CPU和内存为共享主机可见总量；硬盘为已挂载文件系统总容量，不是剩余容量。'
})
const rtx6000d = Object.freeze({
  gpuLabel: 'RTX 6000D / 84 GB', shared: false,
  cpuCount: 22, cpuUnit: 'CORE', cpuModel: 'Xeon Platinum 8470Q',
  memory: '110 GB', systemDisk: '30 GB', dataDisk: '50 GB',
  driver: '595.71.05', cudaMax: '13.2',
  source: '用户确认市场规格', observedDate: '2026-10-21',
  note: '此处为确认的市场规格，不代表实例自定义扩容后的文件系统容量。'
})
const publicP10099Names = ['免费 GPU 99（P100）', 'OortCloud免费GPU服务器2', 'H800（P100）']
const publicP100173Names = ['免费 GPU 173（P100）', 'OortCloud免费GPU服务器1']
const public6000dNames = ['OortCloud RTX 6000D', 'RTX 6000D', '公司 RTX 6000D（收费待开通）']

export function getMarketSpecifications(resource) {
  if (!resource) return null
  const gpu = String(resource.gpuModel || '')
  const publicProjection = resource.id == null && !resource.serverGroup
  if (/Tesla P100-PCIE-16GB/i.test(gpu) && resource.billingType === 'FREE' &&
    resource.schedulingMode === 'SHARED_GPU_QUEUE') {
    if (resource.serverGroup === 'company-p100-99' ||
      publicProjection && publicP10099Names.includes(resource.name)) return p10099
    if (resource.serverGroup === 'company-p100-173' ||
      publicProjection && publicP100173Names.includes(resource.name)) return p100173
  }
  if (/RTX\s*6000D\s*84\s*GB/i.test(gpu) && resource.billingType === 'POINTS' &&
    (String(resource.id) === '792022144331380001' ||
      publicProjection && public6000dNames.includes(resource.name))) return rtx6000d
  return null
}

export function formatMarketCpu(specification, translate = value => value) {
  return specification
    ? `${specification.cpuCount} ${specification.cpuUnit === 'CORE' ? translate('核') : 'vCPU'}, ${specification.cpuModel}`
    : ''
}
