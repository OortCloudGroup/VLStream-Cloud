export const states = {
    QUEUED: ['排队中', 'info'], PREPARING: ['准备中', 'warning'], RUNNING: ['运行中', 'success'],
    DEGRADED: ['部分异常', 'warning'], STOPPING: ['停止中', 'warning'], UNKNOWN: ['状态待确认', 'warning'],
    COMPLETED: ['正常结束', 'success'], CANCELLED: ['已停止', 'info'], FAILED: ['运行失败', 'danger'], SKIPPED: ['已跳过', 'info']
}
export const terminal = state => ['COMPLETED', 'CANCELLED', 'FAILED', 'SKIPPED'].includes(state)
export const formatTime = value => value ? new Intl.DateTimeFormat('zh-CN', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(new Date(value)) : '—'
export const scheduleText = rule => {
    if (!rule) return '—'
    if (rule.kind === 'MANUAL') return `手工 · 运行 ${rule.durationSeconds} 秒`
    if (rule.kind === 'ONCE') return `单次 · ${rule.onceStart?.replace('T', ' ')} 至 ${rule.onceEnd?.replace('T', ' ')}`
    const days = ['一', '二', '三', '四', '五', '六', '日']
    return `${rule.kind === 'DAILY' ? '每天' : '每周' + (rule.weekdays || []).map(n => days[n - 1]).join('、')} · ${rule.startTime}—${rule.endTime}${rule.endTime < rule.startTime ? '（次日）' : ''}`
}
