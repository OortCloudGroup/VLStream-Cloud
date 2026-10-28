import { reverseGeocode } from '@/utils/amap'
import { createEventLocationResolver } from './eventLocationResolver.mjs'

// Reuse the established JS API key/security configuration; never introduce a Web-service secret.
export const lookupEventLocation = createEventLocationResolver(reverseGeocode)
