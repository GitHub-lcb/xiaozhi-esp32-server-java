import { http } from './request'
import api from './api'

export interface PluginInfo {
  id: string
  name: string
  version: string
  description: string
  enabled: boolean
}

export function queryPlugins() {
  return http.get<PluginInfo[]>(api.plugin)
}

export function togglePlugin(pluginId: string, enabled: boolean) {
  return http.post(`${api.plugin}/${pluginId}/toggle`, null, { params: { enabled } })
}
