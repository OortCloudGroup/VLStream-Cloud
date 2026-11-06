export const states = {
    QUEUED: ['排队中', 'info'], PREPARING: ['准备中', 'warning'], RUNNING: ['运行中', 'success'],
    DEGRADED: ['部分异常', 'warning'], STOPPING: ['停止中', 'warning'], UNKNOWN: ['状态待确认', 'warning'],
    COMPLETED: ['正常结束', 'success'], CANCELLED: ['已停止', 'info'], FAILED: ['运行失败', 'danger'], SKIPPED: ['已跳过', 'info']
}
export const terminal = state => ['COMPLETED', 'CANCELLED', 'FAILED', 'SKIPPED'].includes(state)
export const formatTime = (value, locale = 'zh-CN') => value ? new Intl.DateTimeFormat(locale, { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(new Date(value)) : '—'
export const scheduleText = (rule, translate = value => value, locale = 'zh-CN') => {
    if (!rule) return '—'
    if (rule.kind === 'MANUAL') return translate(`手工 · 运行 ${rule.durationSeconds} 秒`)
    if (rule.kind === 'ONCE') return translate(`单次 · ${rule.onceStart?.replace('T', ' ')} 至 ${rule.onceEnd?.replace('T', ' ')}`)
    const days = ['一', '二', '三', '四', '五', '六', '日']
    const weekdays = (rule.weekdays || []).map(n => locale === 'zh-CN' ? days[n - 1]
        : new Intl.DateTimeFormat(locale, { weekday: 'short', timeZone: 'UTC' }).format(new Date(Date.UTC(2024, 0, n))))
    const frequency = rule.kind === 'DAILY' ? translate('每天') : translate('每周') + (locale === 'zh-CN' ? '' : ' ') + weekdays.join(locale === 'zh-CN' ? '、' : ', ')
    return `${frequency} · ${rule.startTime}—${rule.endTime}${rule.endTime < rule.startTime ? translate('（次日）') : ''}`
}
