export const DEVICE_DRAG_TYPE = 'application/x-vls-device'
export function dragCatalogDevice(event, device) {
    if (!device || !event.dataTransfer) return
    event.stopPropagation()
    event.dataTransfer.effectAllowed = 'copy'
    event.dataTransfer.setData(DEVICE_DRAG_TYPE, String(device.id))
}
