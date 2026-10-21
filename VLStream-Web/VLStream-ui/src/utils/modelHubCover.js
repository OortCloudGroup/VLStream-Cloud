import orbit from '@/assets/model-hub-covers/orbit.svg'
import vision from '@/assets/model-hub-covers/vision.svg'
import layers from '@/assets/model-hub-covers/layers.svg'
import wave from '@/assets/model-hub-covers/wave.svg'
import network from '@/assets/model-hub-covers/network.svg'
import landscape from '@/assets/model-hub-covers/landscape.svg'

// Same asset order and identity hash as VLStreamSite's resourceCover.js.
export const DEFAULT_MODEL_HUB_COVERS = Object.freeze([orbit, vision, layers, wave, network, landscape])
const catalogNames = ['orbit', 'vision', 'layers', 'wave', 'network', 'landscape']

export const getCustomModelHubCover = (resource = {}) => [resource.cover, resource.cover_url, resource.coverUrl]
  .find(value => typeof value === 'string' && value.trim())?.trim() || ''

export function getDefaultModelHubCover(resource = {}) {
  const identity = resource.uid ?? resource.id ?? resource.repo_name ?? resource.alias ?? resource.title ?? resource.name ?? ''
  const seed = `model:${identity}`
  let hash = 2166136261
  for (let index = 0; index < seed.length; index += 1) {
    hash = Math.imul(hash ^ seed.charCodeAt(index), 16777619) >>> 0
  }
  return DEFAULT_MODEL_HUB_COVERS[hash % DEFAULT_MODEL_HUB_COVERS.length]
}

export function getModelHubCover(resource = {}) {
  const custom = getCustomModelHubCover(resource)
  // Existing Site records can store canonical default paths; bundle those locally
  // so they also work below VLS's /bus/vls-ui/ base path.
  const defaultIndex = catalogNames.findIndex(name => custom === `/covers/catalog/${name}.svg`)
  return defaultIndex >= 0 ? DEFAULT_MODEL_HUB_COVERS[defaultIndex] : custom || getDefaultModelHubCover(resource)
}
