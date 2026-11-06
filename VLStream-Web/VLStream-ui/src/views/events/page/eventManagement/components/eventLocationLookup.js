import { reverseGeocode } from '@/utils/osmGeocoding'
import { createEventLocationResolver } from './eventLocationResolver.mjs'

// Share the configured OSM address service with the location picker.
export const lookupEventLocation = createEventLocationResolver(reverseGeocode)
