import request from '@/utils/wvpRequest'
import { createDeviceSharingApi } from '@/utils/videoSquareSharing.mjs'
export const deviceSharingApi = createDeviceSharingApi(request)
