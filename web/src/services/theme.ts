import { http } from './request'
import api from './api'

export interface ThemeResource {
  version: number
  roleId: number
  roleName: string
  wakeWords: string[]
  emojis: string[]
  theme: Record<string, unknown>
}

export function fetchThemeResource(roleId: number) {
  return http.get<ThemeResource>(`${api.theme}/${roleId}/resource`)
}
