import { eventLocationView, eventLocationCoordinate } from './eventLocation.mjs'

const yieldToBrowser = () => new Promise(resolve => setTimeout(resolve, 0))
const text = value => value == null ? '' : String(value)
const escapeMarkup = value => text(value).replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f\ufffe\uffff]/g, '').replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&apos;'
}[character]))

export function mediaLinks(value, baseUrl, convertUrl = value => value) {
    const values = Array.isArray(value) ? value : [value]
    const links = []
    for (const entry of values) {
        if (typeof entry !== 'string' || !entry.trim()) continue
        try {
            const url = new URL(convertUrl(entry.trim()), baseUrl)
            if (['http:', 'https:'].includes(url.protocol) && !url.username && !url.password) links.push(url.href)
        } catch { /* An invalid media reference has no downloadable link. */ }
    }
    return [...new Set(links)]
}

// The list endpoint caps each batch at 100. Stop on an incomplete or changed
// result instead of downloading a file that silently omits or repeats events.
export async function collectFilteredEvents(fetchPage, query, { assertActive, onProgress }) {
    const rows = []
    const ids = new Set()
    let total
    for (let page = 1; total === undefined || rows.length < total; page++) {
        assertActive()
        const response = await fetchPage({ ...query, page, pagesize: 100 })
        assertActive()
        if (response?.code !== 200) throw new Error(response?.msg || '获取导出数据失败')
        const batch = response.data?.list
        const count = Number(response.data?.count)
        if (!Array.isArray(batch) || !Number.isSafeInteger(count) || count < 0) throw new Error('导出接口返回数据无效')
        total ??= count
        if (count !== total || (!batch.length && rows.length < total)) throw new Error('事件列表已变化，请重新导出')
        for (const row of batch) {
            const id = text(row.id)
            if (!id || ids.has(id)) throw new Error('事件列表已变化，请重新导出')
            ids.add(id)
            rows.push(row)
        }
        if (rows.length > total) throw new Error('事件列表已变化，请重新导出')
        onProgress(rows.length, total)
        await yieldToBrowser()
    }
    return rows
}

export async function buildEventExportTable(rows, columns, options) {
    const { translate, assertActive, convertUrl, baseUrl, resolveLocation, sequence = index => index + 1 } = options
    const exportColumns = [{ key: 'sequence', label: translate('序号') }, ...columns.map(column => ({
        key: column.key, label: translate(column.label), media: ['pics', 'video'].includes(column.key)
    }))]
    const locations = new Map()
    const values = []
    for (const [index, row] of rows.entries()) {
        assertActive()
        const record = { sequence: sequence(index, row) }
        for (const column of columns) {
            let value = row[column.key]
            if (column.key === 'status') value = translate(({ 0: '待处理', 1: '已完成', 2: '正在处理', 3: '已关闭' })[row.status] || '未知状态')
            if (column.key === 'pics') value = mediaLinks(row.pics, baseUrl, convertUrl)
            if (column.key === 'video') value = mediaLinks(row.video?.length ? row.video : row.videoUrl, baseUrl, convertUrl)
            if (column.key === 'position') {
                value = eventLocationView(row).text
                const coordinate = eventLocationCoordinate(row)
                if (coordinate) {
                    const key = JSON.stringify(coordinate)
                    if (!locations.has(key)) locations.set(key, resolveLocation(coordinate))
                    const resolved = await locations.get(key)
                    assertActive()
                    value = resolved?.address || `${coordinate.lng}, ${coordinate.lat} (${coordinate.coordinateSystem})`
                }
            }
            record[column.key] = Array.isArray(value) ? value.map(text) : text(value)
        }
        values.push(record)
        if (index % 100 === 0) await yieldToBrowser()
    }
    return { columns: exportColumns, rows: values }
}

export function serializeEventTable(format, table, title) {
    const cellText = value => Array.isArray(value) ? value.join('\n') : text(value)
    if (format === 'JSON') return JSON.stringify(table, null, 2)
    if (format === 'CSV') {
        const quote = value => {
            let result = cellText(value)
            if (/^[\s\u0000-\u001f]*[=+@-]/.test(result)) result = `'${result}`
            return `"${result.replace(/"/g, '""')}"`
        }
        return '\uFEFF' + [table.columns.map(column => quote(column.label)).join(','),
            ...table.rows.map(row => table.columns.map(column => quote(row[column.key])).join(','))].join('\r\n')
    }
    if (format === 'XML') {
        return '<?xml version="1.0" encoding="UTF-8"?>\n<events>\n' + table.rows.map(row => '  <event>' +
            table.columns.map(column => `<${column.key}>${escapeMarkup(cellText(row[column.key]))}</${column.key}>`).join('') +
            '</event>').join('\n') + '\n</events>'
    }
    if (format !== 'HTML') throw new Error('不支持的导出格式')
    const cellHtml = (value, column) => column.media
        ? value.map(url => `<a href="${escapeMarkup(url)}" rel="noopener noreferrer">${escapeMarkup(url)}</a>`).join('<br>')
        : escapeMarkup(cellText(value))
    return `<!doctype html><html><head><meta charset="utf-8"><title>${escapeMarkup(title)}</title>
<style>body{font-family:Arial,sans-serif;margin:24px;color:#222}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ccc;padding:8px;text-align:left;vertical-align:top;white-space:pre-wrap;overflow-wrap:anywhere}th{background:#f2f4f8}a{display:inline-block;word-break:break-all}</style>
</head><body><h1>${escapeMarkup(title)}</h1><table><thead><tr>${table.columns.map(column => `<th>${escapeMarkup(column.label)}</th>`).join('')}</tr></thead><tbody>${table.rows.map(row => `<tr>${table.columns.map(column => `<td>${cellHtml(row[column.key], column)}</td>`).join('')}</tr>`).join('')}</tbody></table></body></html>`
}

async function excelFile(table, assertActive) {
    const { default: ExcelJS } = await import('exceljs')
    assertActive()
    const workbook = new ExcelJS.Workbook()
    const sheet = workbook.addWorksheet('Events')
    // Excel supports one hyperlink per cell. Give each media link its own cell.
    const columns = table.columns.flatMap(column => {
        if (!column.media) return [{ ...column, index: null }]
        const count = table.rows.reduce((max, row) => Math.max(max, row[column.key].length), 1)
        return Array.from({ length: count }, (_, index) => ({ ...column, index,
            label: count > 1 ? `${column.label} ${index + 1}` : column.label }))
    })
    sheet.columns = columns.map(column => ({ header: column.label, width: column.media ? 40 : 24 }))
    sheet.views = [{ state: 'frozen', ySplit: 1 }]
    sheet.getRow(1).font = { bold: true }
    sheet.autoFilter = { from: { row: 1, column: 1 }, to: { row: 1, column: columns.length } }
    for (const [index, row] of table.rows.entries()) {
        assertActive()
        sheet.addRow(columns.map(column => {
            const value = row[column.key]
            if (column.media) {
                const link = value[column.index]
                return link ? { text: link, hyperlink: link } : ''
            }
            return Array.isArray(value) ? value.join('\n') : value
        })).alignment = { vertical: 'top', wrapText: true }
        if (index % 100 === 0) await yieldToBrowser()
    }
    const buffer = await workbook.xlsx.writeBuffer()
    assertActive()
    return new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
}

async function pdfFile(table, { title, fontUrl, assertActive }) {
    const [{ jsPDF }, { autoTable }, response] = await Promise.all([
        import('jspdf'), import('jspdf-autotable'), fetch(fontUrl)
    ])
    if (!response.ok) throw new Error('加载 PDF 字体失败')
    const bytes = new Uint8Array(await response.arrayBuffer())
    const chunks = []
    for (let index = 0; index < bytes.length; index += 32768) chunks.push(String.fromCharCode(...bytes.subarray(index, index + 32768)))
    assertActive()
    const doc = new jsPDF({ orientation: 'landscape', format: 'a3', compress: true, putOnlyUsedFonts: true })
    doc.addFileToVFS('EventFont.ttf', btoa(chunks.join('')))
    doc.addFont('EventFont.ttf', 'EventFont', 'normal')
    doc.setFont('EventFont')
    doc.setFontSize(14)
    doc.text(title, 10, 12)
    for (let offset = 0; offset < table.rows.length; offset += 1000) {
        assertActive()
        const batch = table.rows.slice(offset, offset + 1000)
        if (offset) doc.addPage()
        autoTable(doc, {
            startY: offset ? 10 : 18,
            margin: 10,
            head: [table.columns.map(column => column.label)],
            body: batch.map(row => table.columns.map(column => {
                const value = row[column.key]
                if (column.media) return value.map((_, index) => `${column.label} ${index + 1}`).join('\n')
                return Array.isArray(value) ? value.join('\n') : text(value)
            })),
            styles: { font: 'EventFont', fontStyle: 'normal', fontSize: 8, cellPadding: 2, overflow: 'linebreak' },
            headStyles: { fontStyle: 'normal', fillColor: [45, 83, 180] },
            rowPageBreak: 'avoid',
            didParseCell(data) {
                if (data.section === 'body' && table.columns[data.column.index].media) data.cell.styles.textColor = [28, 78, 180]
            },
            didDrawCell(data) {
                const column = table.columns[data.column.index]
                if (data.section !== 'body' || !column.media) return
                const links = batch[data.row.index][column.key]
                const lineHeight = doc.getLineHeight() / doc.internal.scaleFactor
                let startLine = 0
                data.cell.text.forEach((line, index) => {
                    const linkIndex = Number(/(\d+)$/.exec(line)?.[1]) - 1
                    const url = links[linkIndex]
                    if (url) {
                        doc.link(data.cell.x + 2, data.cell.y + 2 + startLine * lineHeight,
                            Math.max(data.cell.width - 4, 1), (index - startLine + 1) * lineHeight, { url })
                        startLine = index + 1
                    }
                })
            }
        })
        await yieldToBrowser()
    }
    assertActive()
    return doc.output('blob')
}

export async function createEventExportFile(format, table, options) {
    if (format === 'Excel') return { blob: await excelFile(table, options.assertActive), extension: 'xlsx' }
    if (format === 'PDF') return { blob: await pdfFile(table, options), extension: 'pdf' }
    const types = { HTML: 'text/html', XML: 'application/xml', JSON: 'application/json', CSV: 'text/csv' }
    return { blob: new Blob([serializeEventTable(format, table, options.title)], { type: `${types[format]};charset=utf-8` }),
        extension: format.toLowerCase() }
}
