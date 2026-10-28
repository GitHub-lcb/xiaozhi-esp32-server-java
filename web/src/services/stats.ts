import { http } from './request'

export interface StatsOverview {
  deviceTotal: number
  deviceOnline: number
  messageToday: number
  message7d: number
  activeDevices7d: number
}

export interface DailyPoint {
  day: string
  total: number
}

export function fetchOverview() {
  return http.get<StatsOverview>('/stats/overview')
}

export function fetchDailyMessages(days = 7) {
  return http.get<DailyPoint[]>('/stats/messages/daily', { days })
}

export interface DialogueStats {
  turns: number
  totalTokens: number
  avgLatencyMs: number
}

export function fetchDialogueStats(days = 7) {
  return http.get<DialogueStats>('/stats/dialogue', { days })
}

export function fetchTopDevices(days = 7, limit = 5) {
  return http.get<Array<{ deviceId: string; total: number }>>('/stats/devices/top', { days, limit })
}
